package com.smartfinance.backend.exception;

/**
 * Raised when the AI provider reports that the configured model does not exist or is not
 * accessible with the current API key (HTTP 404, or a non-404 error body that mentions the
 * model), meaning the operator configured (or the catalog default resolved to) a model
 * identifier the provider does not recognize.
 *
 * <p>Caught by {@code com.smartfinance.backend.service.ai.AiChatOrchestrator}, which tries the
 * next configured provider — this message is internal/diagnostic only (log-facing).
 */
public class AiProviderModelNotFoundException extends AiProviderException {

    public AiProviderModelNotFoundException(String providerName) {
        super(
                providerName,
                "El modelo configurado no está disponible en el proveedor de IA."
        );
    }
}
