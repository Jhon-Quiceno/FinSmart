package com.smartfinance.backend.ia.service.ai;

import com.smartfinance.backend.ia.model.entity.AiUsageEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Resolves which AI providers are enabled (configured with a non-blank API key) and in what
 * order the assistant should try them, from {@link AiProviderProperties}.
 *
 * @see AiChatOrchestrator
 */
@Component
public class AiProviderRegistry {

    private static final Logger log = LoggerFactory.getLogger(AiProviderRegistry.class);

    /**
     * Order used whenever {@code app.ai.priority} is unset or, after dropping unknown/blank/
     * duplicate entries, becomes empty — the confirmed default: Gemini -> NVIDIA -> OpenCode ->
     * OpenRouter -> Groq (see {@code docs/sprints/sprint5.md}). Gemini goes first: verified against
     * a real receipt photo (2026-07-21), NVIDIA's {@code nemotron-nano-12b-v2-vl} vision model
     * returns a 200 with syntactically valid JSON even when it hallucinates the receipt's contents
     * (wrong merchant/amount) — a failure mode {@code AiChatOrchestrator#completeVision} cannot
     * detect, since a well-formed-but-wrong JSON reply is indistinguishable from a correct one at
     * the HTTP/parsing layer. That means whichever provider is tried first effectively "wins" for
     * every vision call, regardless of accuracy. Gemini was empirically far more consistent on the
     * same photo across repeated calls, so it goes first; NVIDIA remains a real second line of
     * defense for when Gemini is unavailable (observed hitting a transient 503 under load during
     * this same verification). Groq is appended last: it has no real API key configured yet (see
     * {@code SupportedAiProvider#GROQ}), so it stays inert until an operator sets
     * {@code GROQ_API_KEY} — but it MUST be listed here, otherwise {@link #resolvePriorityOrder()}
     * would never try it even once a key is configured, since this list is also what an unset/empty
     * {@code app.ai.priority} falls back to.
     */
    private static final List<SupportedAiProvider> DEFAULT_PRIORITY = List.of(
            SupportedAiProvider.GEMINI,
            SupportedAiProvider.NVIDIA,
            SupportedAiProvider.OPENCODE,
            SupportedAiProvider.OPENROUTER,
            SupportedAiProvider.GROQ
    );

    private final AiProviderProperties properties;

    public AiProviderRegistry(AiProviderProperties properties) {
        this.properties = properties;
    }

    /**
     * @return every configured provider (non-blank API key), resolved with its model (configured
     *         or catalog default) and ordered per {@code app.ai.priority} (falling back to the
     *         confirmed default order when unset or empty after filtering)
     */
    public List<ResolvedAiProvider> enabledInPriorityOrder() {
        return resolveEnabled(resolvePriorityOrder());
    }

    /**
     * Same as {@link #enabledInPriorityOrder()}, but lets {@code app.ai.task-priority.<task>}
     * (see {@link AiProviderProperties#getTaskPriority()}) override the try-first order for one
     * specific AI-backed operation only, without changing the order every other task uses.
     *
     * <p>When no override is configured for {@code taskType} — the key is absent, or maps to a
     * list that becomes empty once unknown/blank entries are dropped (same filtering
     * {@link #resolvePriorityOrder()} already applies to {@code app.ai.priority}) — this delegates
     * directly to {@link #enabledInPriorityOrder()}, so a task with no override behaves exactly
     * like it did before this method existed (including respecting a custom, non-default
     * {@code app.ai.priority}).
     *
     * <p>Not used by {@code AiChatOrchestrator#completeVision}: that method always calls
     * {@link #enabledInPriorityOrder()} directly, because {@code ReceiptExtractionService} labels
     * its vision call with {@code AiUsageEventType.CATEGORIZE} — the same event type text
     * categorization uses — so keying vision on that event type would let a future
     * {@code app.ai.task-priority.categorize} override silently change the vision order too.
     *
     * @param taskType which AI-backed operation this call is for (see {@link AiUsageEventType})
     * @return the enabled providers, in the task's overridden order if one is configured, otherwise
     *         the global order
     */
    public List<ResolvedAiProvider> enabledInPriorityOrder(AiUsageEventType taskType) {
        List<String> taskOverride = properties.getTaskPriority().get(taskType.name().toLowerCase(Locale.ROOT));
        List<SupportedAiProvider> parsedOverride = parseConfiguredOrder(taskOverride);
        if (parsedOverride == null) {
            return enabledInPriorityOrder();
        }
        return resolveEnabled(parsedOverride);
    }

    /**
     * Shared resolution step for both {@link #enabledInPriorityOrder()} and
     * {@link #enabledInPriorityOrder(AiUsageEventType)}: filters {@code order} down to providers
     * with a non-blank configured API key, resolving each one's model (configured or catalog
     * default).
     *
     * @param order the seed order to resolve against (already defaulted/expanded by the caller)
     */
    private List<ResolvedAiProvider> resolveEnabled(List<SupportedAiProvider> order) {
        List<ResolvedAiProvider> resolved = new ArrayList<>();
        for (SupportedAiProvider provider : order) {
            AiProviderProperties.ProviderCredentials credentials = properties.getProviders().get(provider.key());
            if (credentials == null || credentials.apiKey() == null || credentials.apiKey().isBlank()) {
                continue;
            }
            String model = credentials.model() != null && !credentials.model().isBlank()
                    ? credentials.model()
                    : provider.defaultModel();
            String visionModel = credentials.visionModel() != null && !credentials.visionModel().isBlank()
                    ? credentials.visionModel()
                    : provider.defaultVisionModel();
            resolved.add(new ResolvedAiProvider(provider.key(), provider.baseUrl(), credentials.apiKey(), model, visionModel));
        }
        return resolved;
    }

    /**
     * @return the status of all known providers, in catalog declaration order, never exposing
     *         the API key
     */
    public List<AiProviderStatus> status() {
        List<String> enabledNamesInOrder = enabledInPriorityOrder().stream().map(ResolvedAiProvider::name).toList();
        List<AiProviderStatus> statuses = new ArrayList<>();
        for (SupportedAiProvider provider : SupportedAiProvider.values()) {
            int index = enabledNamesInOrder.indexOf(provider.key());
            boolean configured = index >= 0;
            Integer priority = configured ? index + 1 : null;
            statuses.add(new AiProviderStatus(provider.key(), configured, priority));
        }
        return statuses;
    }

    /**
     * Logs, once on startup, which AI providers (if any) are enabled — the assistant is not core
     * to the product, so the application always starts regardless of the outcome (see
     * {@code docs/sprints/sprint5.md}).
     */
    @EventListener(ApplicationReadyEvent.class)
    public void logStartupStatus() {
        List<ResolvedAiProvider> enabled = enabledInPriorityOrder();
        if (enabled.isEmpty()) {
            log.warn("No AI provider is configured — the assistant will be unavailable until at least one "
                    + "<PROVIDER>_API_KEY is set");
            return;
        }
        String summary = enabled.stream()
                .map(provider -> provider.name() + " (" + provider.model() + ")")
                .reduce((a, b) -> a + " -> " + b)
                .orElse("");
        log.info("AI providers enabled in priority order: {}", summary);
    }

    /**
     * {@code app.ai.priority} only expresses a preferred try-first order — it is never an
     * allow-list. Any catalog provider not explicitly named (e.g. the operator set
     * {@code AI_PROVIDER_PRIORITY=nvidia} but also configured a valid {@code OPENROUTER_API_KEY})
     * is still appended afterward, in {@link #DEFAULT_PRIORITY} order, so a configured-but-unlisted
     * provider is always tried before giving up.
     */
    private List<SupportedAiProvider> resolvePriorityOrder() {
        List<SupportedAiProvider> parsed = parseConfiguredOrder(properties.getPriority());
        return parsed != null ? parsed : DEFAULT_PRIORITY;
    }

    /**
     * Generalization of the {@code app.ai.priority} parsing rule above, reused by
     * {@link #enabledInPriorityOrder(AiUsageEventType)} for {@code app.ai.task-priority.<task>}:
     * drops unknown/blank/duplicate entries from {@code configuredKeys} (keeping the first
     * occurrence of each), then appends any {@link #DEFAULT_PRIORITY} provider not yet listed —
     * same "preference, never an allow-list" rule.
     *
     * @param configuredKeys raw provider keys in configured order, or {@code null}/empty
     * @return the resolved order, or {@code null} when {@code configuredKeys} is null/empty or
     *         every entry in it was unknown/blank (i.e. nothing usable was configured) — callers
     *         use {@code null} to distinguish "fall back to some other order" from "here is one"
     */
    private List<SupportedAiProvider> parseConfiguredOrder(List<String> configuredKeys) {
        if (configuredKeys == null || configuredKeys.isEmpty()) {
            return null;
        }

        List<SupportedAiProvider> ordered = new ArrayList<>();
        Set<SupportedAiProvider> seen = new LinkedHashSet<>();
        for (String rawKey : configuredKeys) {
            SupportedAiProvider.fromKey(rawKey).ifPresent(provider -> {
                if (seen.add(provider)) {
                    ordered.add(provider);
                }
            });
        }
        if (ordered.isEmpty()) {
            return null;
        }
        for (SupportedAiProvider provider : DEFAULT_PRIORITY) {
            if (seen.add(provider)) {
                ordered.add(provider);
            }
        }
        return ordered;
    }

    /**
     * Read-only snapshot of a provider's configuration state, safe to expose over
     * {@code GET /api/ai/providers/status} — never carries the API key.
     *
     * @param name       the provider's display name (see {@link SupportedAiProvider#key()})
     * @param configured whether a non-blank API key is set for this provider
     * @param priority   this provider's 1-based position among enabled providers, or {@code null}
     *                   if not enabled
     */
    public record AiProviderStatus(String name, boolean configured, Integer priority) {
    }
}
