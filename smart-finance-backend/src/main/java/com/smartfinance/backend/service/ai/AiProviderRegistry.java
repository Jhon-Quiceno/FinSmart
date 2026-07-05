package com.smartfinance.backend.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
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
     * duplicate entries, becomes empty — the confirmed default: NVIDIA -> OpenCode ->
     * OpenRouter (see {@code docs/sprints/sprint5.md}).
     */
    private static final List<SupportedAiProvider> DEFAULT_PRIORITY = List.of(
            SupportedAiProvider.NVIDIA,
            SupportedAiProvider.OPENCODE,
            SupportedAiProvider.OPENROUTER
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
        List<ResolvedAiProvider> resolved = new ArrayList<>();
        for (SupportedAiProvider provider : resolvePriorityOrder()) {
            AiProviderProperties.ProviderCredentials credentials = properties.getProviders().get(provider.key());
            if (credentials == null || credentials.apiKey() == null || credentials.apiKey().isBlank()) {
                continue;
            }
            String model = credentials.model() != null && !credentials.model().isBlank()
                    ? credentials.model()
                    : provider.defaultModel();
            resolved.add(new ResolvedAiProvider(provider.key(), provider.baseUrl(), credentials.apiKey(), model));
        }
        return resolved;
    }

    /**
     * @return the status of all 3 known providers, in catalog declaration order, never exposing
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
        List<String> configuredPriority = properties.getPriority();
        if (configuredPriority == null || configuredPriority.isEmpty()) {
            return DEFAULT_PRIORITY;
        }

        List<SupportedAiProvider> ordered = new ArrayList<>();
        Set<SupportedAiProvider> seen = new LinkedHashSet<>();
        for (String rawKey : configuredPriority) {
            SupportedAiProvider.fromKey(rawKey).ifPresent(provider -> {
                if (seen.add(provider)) {
                    ordered.add(provider);
                }
            });
        }
        if (ordered.isEmpty()) {
            return DEFAULT_PRIORITY;
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
