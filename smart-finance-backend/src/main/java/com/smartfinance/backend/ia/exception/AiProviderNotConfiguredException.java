package com.smartfinance.backend.ia.exception;

/**
 * Raised when an AI feature (chat, insights, categorization) is invoked but the app operator has
 * not configured any AI provider at all (see
 * {@code com.smartfinance.backend.ia.service.ai.AiProviderRegistry}).
 *
 * <p>Unlike {@link AiProviderException} and its subclasses, this does not represent a failed
 * call to a provider — it means no provider was ever reached because none is configured yet —
 * so it intentionally does not extend that hierarchy.
 */
public class AiProviderNotConfiguredException extends RuntimeException {

    public AiProviderNotConfiguredException(String message) {
        super(message);
    }
}
