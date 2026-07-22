package com.smartfinance.backend.ia.service.ai;

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

    NVIDIA("https://integrate.api.nvidia.com/v1", "meta/llama-3.1-70b-instruct", "nvidia/nemotron-nano-12b-v2-vl"),
    // Endpoint OpenAI-compatible oficial de Gemini (no la API nativa de Google, que fue la que le
    // dio problemas al operador antes). "gemini-3.5-flash" es multimodal: mismo modelo sirve para
    // texto y para imagen, por eso el 3er argumento repite el 2do.
    GEMINI("https://generativelanguage.googleapis.com/v1beta/openai", "gemini-3.5-flash", "gemini-3.5-flash"),
    // "big-pickle" dejó de responder en OpenCode Zen desde mayo 2026; "deepseek-v4-flash-free" es
    // el reemplazo gratuito confirmado funcionando en el mismo endpoint. Solo aplica cuando el
    // operador no fija OPENCODE_MODEL explícitamente.
    OPENCODE("https://opencode.ai/zen/v1", "deepseek-v4-flash-free", null),
    // "deepseek/deepseek-r1:free" fue retirado del catálogo gratuito de OpenRouter (confirmado
    // devolviendo 404 en vivo, "This model is unavailable for free"); "nvidia/nemotron-3-nano-30b-a3b:free"
    // es el reemplazo probado y funcionando en vivo (2026-07-21). El 3er argumento
    // ("nvidia/nemotron-nano-12b-v2-vl:free") es el fallback de visión (ver
    // AiChatOrchestrator#completeVision) — también confirmado vigente contra openrouter.ai/models
    // en la misma verificación.
    OPENROUTER("https://openrouter.ai/api/v1", "nvidia/nemotron-3-nano-30b-a3b:free", "nvidia/nemotron-nano-12b-v2-vl:free"),
    // Sin API key real configurada todavía (catálogo "listo pero inerte"): el modelo de texto
    // gratuito/rápido recomendado en la documentación pública de Groq al momento de escribir esto;
    // revisar si sigue siendo el default vigente cuando se active esta integración.
    GROQ("https://api.groq.com/openai/v1", "llama-3.3-70b-versatile", null);

    private final String baseUrl;
    private final String defaultModel;
    private final String defaultVisionModel;

    SupportedAiProvider(String baseUrl, String defaultModel, String defaultVisionModel) {
        this.baseUrl = baseUrl;
        this.defaultModel = defaultModel;
        this.defaultVisionModel = defaultVisionModel;
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
     * @return the model used for image-input calls (see {@code AiChatOrchestrator#completeVision})
     *         when the operator doesn't set an explicit {@code vision-model}, or {@code null} if
     *         this provider has no known vision-capable model in this project's catalog. NVIDIA,
     *         GEMINI, and OPENROUTER are the only entries with a non-null value; {@code completeVision} iterates
     *         every enabled provider in priority order and skips any whose value here is
     *         {@code null}/blank, so a provider gaining vision support later just needs this field
     *         filled in.
     */
    public String defaultVisionModel() {
        return defaultVisionModel;
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
