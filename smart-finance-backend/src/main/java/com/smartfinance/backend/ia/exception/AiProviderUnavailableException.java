package com.smartfinance.backend.ia.exception;

/**
 * Raised when the AI provider cannot be reached at all (connection refused, DNS failure) or
 * responds with a server error (HTTP 5xx) that is not more specifically classified by another
 * {@link AiProviderException} subclass.
 */
public class AiProviderUnavailableException extends AiProviderException {

    public AiProviderUnavailableException(String providerName) {
        super(
                providerName,
                "El proveedor de IA no está disponible en este momento. Intenta de nuevo más tarde."
        );
    }
}
