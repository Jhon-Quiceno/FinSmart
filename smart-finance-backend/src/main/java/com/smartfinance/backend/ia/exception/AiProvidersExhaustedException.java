package com.smartfinance.backend.ia.exception;

/**
 * Raised when every configured AI provider failed to answer a request (see
 * {@code com.smartfinance.backend.ia.service.ai.AiChatOrchestrator}).
 *
 * <p>Deliberately does NOT extend {@link AiProviderException}: unlike that hierarchy, this
 * exception must never carry or leak which specific provider failed, so it cannot be mistaken
 * for a per-provider error anywhere in the exception chain.
 */
public class AiProvidersExhaustedException extends RuntimeException {

    public AiProvidersExhaustedException(String message) {
        super(message);
    }
}
