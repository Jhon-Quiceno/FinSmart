package com.smartfinance.backend.service.ai;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/**
 * Binds {@code app.ai.*} from the environment: which providers are configured (by API key) and
 * in what priority order the assistant should try them (see {@code docs/sprints/sprint5.md}).
 *
 * <p>Declared as a plain mutable class (Lombok {@link Getter}/{@link Setter}) rather than a
 * record because Spring's {@code @ConfigurationProperties} JavaBeans binding needs setters on the
 * top-level bound type; the nested {@link ProviderCredentials} can safely be an immutable record
 * since Spring supports constructor binding for nested {@code @ConfigurationProperties} types.
 *
 * @see AiProviderRegistry
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.ai")
public class AiProviderProperties {

    /**
     * Provider keys (see {@link SupportedAiProvider#key()}) in the order they should be tried,
     * e.g. {@code ["nvidia", "opencode", "openrouter"]}. Unknown, blank, or duplicate
     * entries are ignored by {@link AiProviderRegistry}.
     */
    private List<String> priority = List.of();

    /**
     * Per-provider credentials, keyed by {@link SupportedAiProvider#key()} (e.g. {@code "nvidia"}).
     */
    private Map<String, ProviderCredentials> providers = Map.of();

    /**
     * Maximum number of {@code USER}-role AI chat messages a single user may send per UTC
     * calendar month, enforced by {@code AiChatService#chat} before any provider is contacted.
     */
    private int monthlyMessageLimit = 5;

    /**
     * @param apiKey the provider's raw API key, or blank/{@code null} if not configured
     * @param model  the model identifier to use, or blank/{@code null} to fall back to
     *               {@link SupportedAiProvider#defaultModel()}
     */
    public record ProviderCredentials(String apiKey, String model) {
    }
}
