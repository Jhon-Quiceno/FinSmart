package com.smartfinance.backend.exception;

/**
 * Raised when the AI provider does not respond within the configured connect/read timeouts
 * (see {@code AiChatClient}'s 10s connect / 60s read timeouts).
 */
public class AiProviderTimeoutException extends AiProviderException {

    public AiProviderTimeoutException(String providerName) {
        super(
                providerName,
                "El proveedor de IA no respondió a tiempo. Intenta de nuevo."
        );
    }
}
