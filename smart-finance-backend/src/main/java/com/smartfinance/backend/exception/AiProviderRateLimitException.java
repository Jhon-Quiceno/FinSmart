package com.smartfinance.backend.exception;

/**
 * Raised when the AI provider rejects the request with a rate-limit error (HTTP 429), typically
 * because the user's free-tier quota (e.g. Gemini's ~10 requests/minute) was exceeded.
 */
public class AiProviderRateLimitException extends AiProviderException {

    public AiProviderRateLimitException(String providerName) {
        super(
                providerName,
                "El proveedor de IA alcanzó su límite de uso. Intenta de nuevo en unos minutos."
        );
    }
}
