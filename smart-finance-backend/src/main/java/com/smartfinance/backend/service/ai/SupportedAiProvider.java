package com.smartfinance.backend.service.ai;

import java.util.Locale;
import java.util.Optional;

/**
 * Fixed catalog of AI providers this project knows how to call through {@link AiChatClient}'s
 * OpenAI-compatible {@code /chat/completions} contract.
 *
 * <p>Each entry pins a fixed {@code baseUrl} and a sensible {@code defaultModel} so the app
 * operator only ever needs to set an API key (and, optionally, a different model) in the
 * environment for a provider to become active — see {@code docs/sprints/sprint5.md}, architecture
 * decision on app-level AI providers.
 */
public enum SupportedAiProvider {

    NVIDIA("https://integrate.api.nvidia.com/v1", "meta/llama-3.1-70b-instruct"),
    OPENCODE("https://opencode.ai/zen/v1", "big-pickle"),
    OPENROUTER("https://openrouter.ai/api/v1", "deepseek/deepseek-r1:free");

    private final String baseUrl;
    private final String defaultModel;

    SupportedAiProvider(String baseUrl, String defaultModel) {
        this.baseUrl = baseUrl;
        this.defaultModel = defaultModel;
    }

    /**
     * @return this provider's fixed OpenAI-compatible API base URL
     */
    public String baseUrl() {
        return baseUrl;
    }

    /**
     * @return the model used when the operator sets this provider's API key but no explicit model
     */
    public String defaultModel() {
        return defaultModel;
    }

    /**
     * @return the lowercase key identifying this provider in {@code application.properties}
     *         (e.g. {@code "nvidia"}), matching {@code AiProviderProperties}' provider map keys
     */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Looks up a provider by its {@link #key()}, case-insensitively.
     *
     * @param key the candidate key (e.g. {@code "Nvidia"}, {@code "NVIDIA"}, {@code "nvidia"})
     * @return the matching provider, or {@link Optional#empty()} if {@code key} is {@code null},
     *         blank, or does not match any known provider
     */
    public static Optional<SupportedAiProvider> fromKey(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        for (SupportedAiProvider provider : values()) {
            if (provider.key().equals(normalized)) {
                return Optional.of(provider);
            }
        }
        return Optional.empty();
    }
}
