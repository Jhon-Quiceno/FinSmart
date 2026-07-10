package com.smartfinance.backend.ia.service.ai;

/**
 * A fully resolved AI provider ready to be called by {@link AiChatClient}: fixed catalog data
 * ({@code baseUrl}) merged with environment-configured credentials ({@code apiKey}, {@code model}).
 *
 * <p>Produced by {@link AiProviderRegistry#enabledInPriorityOrder()}; replaces the former
 * per-user {@code AiProviderSetting} entity as {@link AiChatClient#complete}'s provider
 * parameter, decoupling the HTTP client from JPA.
 *
 * @param name    the provider's display name (see {@link SupportedAiProvider#key()})
 * @param baseUrl the provider's fixed API base URL
 * @param apiKey  the provider's raw API key, read directly from the environment
 * @param model   the model identifier to request
 */
public record ResolvedAiProvider(String name, String baseUrl, String apiKey, String model) {
}
