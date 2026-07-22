package com.smartfinance.backend.ia.service.ai;

import com.smartfinance.backend.ia.exception.AiProviderException;
import com.smartfinance.backend.ia.exception.AiProviderNotConfiguredException;
import com.smartfinance.backend.ia.exception.AiProvidersExhaustedException;
import com.smartfinance.backend.ia.service.AiUsageEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Transparent failover across every configured AI provider, in priority order.
 *
 * <p>Every {@code AiChatService}/{@code AiInsightService}/{@code AiCategorizationService} call
 * goes through {@link #complete(List, AiCallContext)} instead of resolving a single provider and
 * calling {@link AiChatClient} directly: if the currently-tried provider fails for any reason
 * classified by {@link AiProviderException} (auth, rate limit, timeout, missing model, general
 * unavailability), the next configured provider is tried automatically, with no difference
 * visible to the end user. Only when every configured provider has failed — or none is
 * configured at all — does the caller see a terminal, generic exception (see
 * {@link #GENERIC_MESSAGE}); which provider failed or why is never leaked to the caller (see
 * {@code docs/sprints/sprint5.md}).
 *
 * <p><b>Per-attempt telemetry:</b> when a caller supplies a non-null {@link AiCallContext}, every
 * attempt within the failover loop — both the ones that fail over and the one that ultimately
 * answers — is recorded via {@link AiUsageEventService#recordAttempt}, with latency, success/
 * failure, and (for failures) the raised exception's simple class name. This can only happen here,
 * inside the loop itself, because a caller only ever sees the final winning result (or the terminal
 * exception): it has no visibility into how many providers were tried before that, or how long each
 * one took. Callers that pass {@code ctx == null} (or use the no-context overloads) get the exact
 * same failover behavior with no telemetry recorded — this is what
 * {@code StatementAiExtractionService} does today, since it is out of scope for this telemetry work
 * and keeps recording its own usage event directly.
 *
 * <p><b>Per-task provider priority:</b> {@link #complete(List, AiCallContext)} also uses
 * {@code ctx}, when present, to resolve providers via
 * {@link AiProviderRegistry#enabledInPriorityOrder(com.smartfinance.backend.ia.model.entity.AiUsageEventType)}
 * instead of the global {@link AiProviderRegistry#enabledInPriorityOrder()} — this lets an operator
 * configure a different try-first provider order per AI-backed operation (chat, categorization,
 * insights, ...) via {@code app.ai.task-priority.<task>}, instead of one order for everything. A
 * task with no override configured behaves exactly as before (falls back to the global order).
 * {@link #completeVision(List, AiCallContext)} deliberately does NOT do this — see that method's
 * Javadoc for why.
 */
@Service
public class AiChatOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AiChatOrchestrator.class);

    /**
     * Neutral, non-actionable message shown to the end user whenever the assistant could not
     * produce a reply — reused verbatim by {@code GlobalExceptionHandler} so this exact text is
     * defined in exactly one place.
     */
    public static final String GENERIC_MESSAGE =
            "El asistente no está disponible en este momento. Inténtalo de nuevo más tarde.";

    private final AiProviderRegistry registry;
    private final AiChatClient aiChatClient;
    private final AiUsageEventService aiUsageEventService;
    private final AiProviderPricing aiProviderPricing;

    public AiChatOrchestrator(
            AiProviderRegistry registry,
            AiChatClient aiChatClient,
            AiUsageEventService aiUsageEventService,
            AiProviderPricing aiProviderPricing
    ) {
        this.registry = registry;
        this.aiChatClient = aiChatClient;
        this.aiUsageEventService = aiUsageEventService;
        this.aiProviderPricing = aiProviderPricing;
    }

    /**
     * Same as {@link #complete(List, AiCallContext)} with no telemetry recorded — see the class
     * Javadoc.
     */
    public ChatCompletionResult complete(List<ChatMessage> messages) {
        return complete(messages, null);
    }

    /**
     * Sends {@code messages} to the first configured provider that succeeds, trying every
     * configured provider in priority order before giving up.
     *
     * @param messages the full conversation to send, in order (system prompt first)
     * @param ctx      attribution for per-attempt telemetry (see the class Javadoc), or
     *                 {@code null} to skip recording telemetry entirely; when non-null, its
     *                 {@code eventType} also selects the provider order (see the class Javadoc's
     *                 "Per-task provider priority" section)
     * @return the successful provider's reply
     * @throws AiProviderNotConfiguredException if no provider is configured at all
     * @throws AiProvidersExhaustedException    if every configured provider failed
     */
    public ChatCompletionResult complete(List<ChatMessage> messages, AiCallContext ctx) {
        List<ResolvedAiProvider> providers = ctx != null
                ? registry.enabledInPriorityOrder(ctx.eventType())
                : registry.enabledInPriorityOrder();
        if (providers.isEmpty()) {
            throw new AiProviderNotConfiguredException(GENERIC_MESSAGE);
        }

        for (ResolvedAiProvider provider : providers) {
            long startNanos = System.nanoTime();
            try {
                ChatCompletionResult result = aiChatClient.complete(provider, messages);
                int latencyMs = elapsedMs(startNanos);
                ChatCompletionResult stamped = result.withProvider(provider.name(), provider.model());
                recordAttemptIfPresent(ctx, provider, stamped, latencyMs, true, null);
                return stamped;
            } catch (AiProviderException ex) {
                int latencyMs = elapsedMs(startNanos);
                log.warn("AI provider {} failed ({}); trying next configured provider",
                        provider.name(), ex.getClass().getSimpleName());
                recordAttemptIfPresent(ctx, provider, null, latencyMs, false, ex.getClass().getSimpleName());
            }
        }

        throw new AiProvidersExhaustedException(GENERIC_MESSAGE);
    }

    /**
     * Same as {@link #completeVision(List, AiCallContext)} with no telemetry recorded — see the
     * class Javadoc.
     */
    public ChatCompletionResult completeVision(List<ChatMessage> messages) {
        return completeVision(messages, null);
    }

    /**
     * Sends a vision turn ({@code messages} built with {@link ChatMessage#userWithImage}) to the
     * first configured, vision-capable provider that succeeds, trying every configured provider
     * with a non-blank {@link ResolvedAiProvider#visionModel()} in priority order before giving up
     * — the same transparent-failover shape as {@link #complete(List, AiCallContext)}, just
     * restricted to the subset of configured providers this project's catalog knows to actually
     * have a vision-capable model (see {@link SupportedAiProvider#defaultVisionModel()}).
     *
     * <p>Configured providers with no known vision model (a {@code null}/blank
     * {@link ResolvedAiProvider#visionModel()}) are skipped entirely: they are never even attempted,
     * since an OpenAI-compatible provider given an unexpected {@code image_url} content part will
     * either reject the request outright or, worse, silently ignore it and hallucinate an answer
     * instead of actually reading the image. Today NVIDIA and OpenRouter have a configured vision
     * model in the catalog; OpenCode does not.
     *
     * <p>Regardless of which provider is tried, this method always swaps in
     * {@link ResolvedAiProvider#visionModel()} as the model to request — a provider's regular text
     * model is frequently NOT vision-capable at all (this bit the project once already: NVIDIA's
     * text model then configured, {@code nemotron-3-nano-30b-a3b}, silently rejected image content
     * when a vision call mistakenly reused it), so the text model is never reused for a vision turn.
     *
     * @param messages the full conversation to send, in order (system prompt first), including
     *                 exactly one vision turn built with {@link ChatMessage#userWithImage}
     * @param ctx      attribution for per-attempt telemetry (see the class Javadoc), or
     *                 {@code null} to skip recording telemetry entirely
     * @return the successful provider's reply
     * @throws AiProviderNotConfiguredException if no enabled provider has a vision model configured
     * @throws AiProvidersExhaustedException    if every vision-capable configured provider failed
     */
    public ChatCompletionResult completeVision(List<ChatMessage> messages, AiCallContext ctx) {
        List<ResolvedAiProvider> visionProviders = registry.enabledInPriorityOrder().stream()
                .filter(provider -> provider.visionModel() != null && !provider.visionModel().isBlank())
                .toList();
        if (visionProviders.isEmpty()) {
            throw new AiProviderNotConfiguredException(GENERIC_MESSAGE);
        }

        for (ResolvedAiProvider provider : visionProviders) {
            // The regular text model (provider.model()) is frequently NOT vision-capable — swap in
            // the vision-specific model before calling, never reuse the text one for an image turn.
            ResolvedAiProvider visionProvider = new ResolvedAiProvider(
                    provider.name(), provider.baseUrl(), provider.apiKey(), provider.visionModel(), provider.visionModel()
            );
            long startNanos = System.nanoTime();
            try {
                ChatCompletionResult result = aiChatClient.complete(visionProvider, messages);
                int latencyMs = elapsedMs(startNanos);
                ChatCompletionResult stamped = result.withProvider(visionProvider.name(), visionProvider.model());
                recordAttemptIfPresent(ctx, visionProvider, stamped, latencyMs, true, null);
                return stamped;
            } catch (AiProviderException ex) {
                int latencyMs = elapsedMs(startNanos);
                log.warn("AI provider {} failed on a vision turn ({}); trying next vision-capable configured provider",
                        visionProvider.name(), ex.getClass().getSimpleName());
                recordAttemptIfPresent(ctx, visionProvider, null, latencyMs, false, ex.getClass().getSimpleName());
            }
        }

        throw new AiProvidersExhaustedException(GENERIC_MESSAGE);
    }

    /** Records one attempt's telemetry through {@link AiUsageEventService#recordAttempt}, or does nothing if {@code ctx == null}. */
    private void recordAttemptIfPresent(
            AiCallContext ctx, ResolvedAiProvider provider, ChatCompletionResult result, int latencyMs, boolean success, String errorType
    ) {
        if (ctx == null) {
            return;
        }
        int tokensUsed = result != null ? totalTokens(result) : 0;
        BigDecimal costEstimate = success ? aiProviderPricing.estimateCost(provider.name(), provider.model(), tokensUsed) : null;
        aiUsageEventService.recordAttempt(
                ctx.userId(), provider.name(), ctx.eventType(), tokensUsed, costEstimate, latencyMs, success, errorType
        );
    }

    /** Sums {@code promptTokens + completionTokens}, treating either as {@code 0} when the provider did not report it. */
    private static int totalTokens(ChatCompletionResult result) {
        int prompt = result.promptTokens() != null ? result.promptTokens() : 0;
        int completion = result.completionTokens() != null ? result.completionTokens() : 0;
        return prompt + completion;
    }

    /** Milliseconds elapsed since {@code startNanos} (see {@link System#nanoTime()}), rounded down. */
    private static int elapsedMs(long startNanos) {
        return (int) ((System.nanoTime() - startNanos) / 1_000_000);
    }
}
