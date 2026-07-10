package com.smartfinance.backend.ia.exception;

/**
 * Raised when the AI provider rejects the request with an authentication/authorization error
 * (HTTP 401 or 403), meaning the API key configured by the app operator (via environment
 * variables) is invalid, revoked, or lacks permission for the requested model.
 *
 * <p>Caught by {@code com.smartfinance.backend.ia.service.ai.AiChatOrchestrator}, which tries the
 * next configured provider — this message is internal/diagnostic only (log-facing), it never
 * reaches the end user directly. See
 * {@link com.smartfinance.backend.common.exception.GlobalExceptionHandler} for the HTTP status mapping
 * on the rare path where it does surface.
 */
public class AiProviderAuthException extends AiProviderException {

    public AiProviderAuthException(String providerName) {
        super(
                providerName,
                "La API key configurada por el operador para este proveedor de IA fue rechazada."
        );
    }
}
