package com.smartfinance.backend.ia.service.ai;

import com.smartfinance.backend.ia.exception.AiProviderException;
import com.smartfinance.backend.ia.exception.AiProviderNotConfiguredException;
import com.smartfinance.backend.ia.exception.AiProvidersExhaustedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Transparent failover across every configured AI provider, in priority order.
 *
 * <p>Every {@code AiChatService}/{@code AiInsightService}/{@code AiCategorizationService} call
 * goes through {@link #complete(List)} instead of resolving a single provider and calling
 * {@link AiChatClient} directly: if the currently-tried provider fails for any reason classified
 * by {@link AiProviderException} (auth, rate limit, timeout, missing model, general
 * unavailability), the next configured provider is tried automatically, with no difference
 * visible to the end user. Only when every configured provider has failed — or none is
 * configured at all — does the caller see a terminal, generic exception (see
 * {@link #GENERIC_MESSAGE}); which provider failed or why is never leaked to the caller (see
 * {@code docs/sprints/sprint5.md}).
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

    public AiChatOrchestrator(AiProviderRegistry registry, AiChatClient aiChatClient) {
        this.registry = registry;
        this.aiChatClient = aiChatClient;
    }

    /**
     * Sends {@code messages} to the first configured provider that succeeds, trying every
     * configured provider in priority order before giving up.
     *
     * @param messages the full conversation to send, in order (system prompt first)
     * @return the successful provider's reply
     * @throws AiProviderNotConfiguredException if no provider is configured at all
     * @throws AiProvidersExhaustedException    if every configured provider failed
     */
    public ChatCompletionResult complete(List<ChatMessage> messages) {
        List<ResolvedAiProvider> providers = registry.enabledInPriorityOrder();
        if (providers.isEmpty()) {
            throw new AiProviderNotConfiguredException(GENERIC_MESSAGE);
        }

        for (ResolvedAiProvider provider : providers) {
            try {
                ChatCompletionResult result = aiChatClient.complete(provider, messages);
                return result.withProvider(provider.name(), provider.model());
            } catch (AiProviderException ex) {
                log.warn("AI provider {} failed ({}); trying next configured provider",
                        provider.name(), ex.getClass().getSimpleName());
            }
        }

        throw new AiProvidersExhaustedException(GENERIC_MESSAGE);
    }

    /**
     * Sends a vision turn ({@code messages} built with {@link ChatMessage#userWithImage}) directly
     * to NVIDIA, bypassing the multi-provider failover that {@link #complete(List)} performs.
     *
     * <p>Vision is a capability only this project's NVIDIA-configured model provides: NVIDIA is the
     * only configured provider whose model was validated to actually read an image (see
     * {@code ReceiptExtractionService}'s Javadoc for the benchmark). The other catalog providers
     * (OpenCode's {@code big-pickle}, OpenRouter's {@code deepseek/deepseek-r1:free}) are text-only
     * models — if a vision call silently failed over to either the way {@link #complete(List)}
     * does, the provider would either error on the unexpected {@code image_url} content part or,
     * worse, ignore it and hallucinate an answer instead of actually reading the receipt. So this
     * method never loops over {@link AiProviderRegistry#enabledInPriorityOrder()}: it resolves NVIDIA
     * specifically and, if it is not configured/enabled at all, fails immediately with a clean
     * "vision unavailable" error rather than trying a model that cannot honor the request.
     *
     * @param messages the full conversation to send, in order (system prompt first), including
     *                 exactly one vision turn built with {@link ChatMessage#userWithImage}
     * @return NVIDIA's reply
     * @throws AiProviderNotConfiguredException if NVIDIA is not configured/enabled
     * @throws AiProviderException              if NVIDIA rejects the request or cannot be reached
     *                                           (see {@link AiChatClient#complete}) — propagated
     *                                           as-is since there is no fallback provider to try
     */
    public ChatCompletionResult completeVision(List<ChatMessage> messages) {
        ResolvedAiProvider nvidia = registry.enabledInPriorityOrder().stream()
                .filter(provider -> SupportedAiProvider.NVIDIA.key().equals(provider.name()))
                .findFirst()
                .orElseThrow(() -> new AiProviderNotConfiguredException(GENERIC_MESSAGE));
        if (nvidia.visionModel() == null || nvidia.visionModel().isBlank()) {
            throw new AiProviderNotConfiguredException(GENERIC_MESSAGE);
        }

        // NVIDIA's regular text model (nvidia.model()) is frequently NOT vision-capable — swap in
        // the vision-specific model before calling, never reuse the text one for an image turn.
        ResolvedAiProvider nvidiaVision = new ResolvedAiProvider(
                nvidia.name(), nvidia.baseUrl(), nvidia.apiKey(), nvidia.visionModel(), nvidia.visionModel()
        );
        ChatCompletionResult result = aiChatClient.complete(nvidiaVision, messages);
        return result.withProvider(nvidiaVision.name(), nvidiaVision.model());
    }
}
