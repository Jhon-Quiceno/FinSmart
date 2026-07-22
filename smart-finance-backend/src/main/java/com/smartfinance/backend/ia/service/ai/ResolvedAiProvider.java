package com.smartfinance.backend.ia.service.ai;

/**
 * A fully resolved AI provider ready to be called by {@link AiChatClient}: fixed catalog data
 * ({@code baseUrl}) merged with environment-configured credentials ({@code apiKey}, {@code model}).
 *
 * <p>Produced by {@link AiProviderRegistry#enabledInPriorityOrder()}; replaces the former
 * per-user {@code AiProviderSetting} entity as {@link AiChatClient#complete}'s provider
 * parameter, decoupling the HTTP client from JPA.
 *
 * @param name        the provider's display name (see {@link SupportedAiProvider#key()})
 * @param baseUrl     the provider's fixed API base URL
 * @param apiKey      the provider's raw API key, read directly from the environment
 * @param model       the model identifier to request for text-only calls
 * @param visionModel the model identifier to request for image-input calls (see
 *                    {@link AiChatOrchestrator#completeVision}), or {@code null} if this provider
 *                    has no configured/known vision model — deliberately NOT reused from
 *                    {@code model}, since a provider's regular text model is frequently not
 *                    vision-capable at all (this bit the project once already: the NVIDIA text
 *                    model then configured, {@code nemotron-3-nano-30b-a3b}, silently rejected
 *                    image content when a vision call mistakenly reused it)
 */
public record ResolvedAiProvider(String name, String baseUrl, String apiKey, String model, String visionModel) {
}
