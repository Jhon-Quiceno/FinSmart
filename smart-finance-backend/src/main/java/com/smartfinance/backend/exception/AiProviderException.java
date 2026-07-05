package com.smartfinance.backend.exception;

/**
 * Base type for every failure raised while calling an app-operator-configured AI provider through
 * {@code com.smartfinance.backend.service.ai.AiChatClient}.
 *
 * <p>Subclasses distinguish the specific failure mode (authentication, rate limit, missing
 * model, timeout, or general unavailability) with a factual, Spanish, internal/diagnostic
 * message. In normal operation these never reach a controller directly: they are caught by
 * {@code com.smartfinance.backend.service.ai.AiChatOrchestrator}, which tries the next configured
 * provider and only ever surfaces {@code AiChatOrchestrator.GENERIC_MESSAGE} to the end user once
 * every provider has failed. The per-subclass {@link GlobalExceptionHandler} mappings remain as a
 * defensive fallback.
 *
 * @see com.smartfinance.backend.service.ai.AiChatClient
 * @see com.smartfinance.backend.service.ai.AiChatOrchestrator
 */
public class AiProviderException extends RuntimeException {

    private final String providerName;

    public AiProviderException(String providerName, String message) {
        super(message);
        this.providerName = providerName;
    }

    public AiProviderException(String providerName, String message, Throwable cause) {
        super(message, cause);
        this.providerName = providerName;
    }

    /**
     * @return the display name of the AI provider that raised this failure (e.g. {@code "nvidia"})
     */
    public String getProviderName() {
        return providerName;
    }
}
