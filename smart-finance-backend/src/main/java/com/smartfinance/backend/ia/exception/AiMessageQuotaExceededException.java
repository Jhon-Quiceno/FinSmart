package com.smartfinance.backend.ia.exception;

/**
 * Raised when the current user has already sent {@code app.ai.monthly-message-limit} USER-role
 * AI chat messages in the current UTC calendar month (see {@code AiChatService#chat}), before
 * any AI provider is contacted.
 */
public class AiMessageQuotaExceededException extends RuntimeException {

    public AiMessageQuotaExceededException(String message) {
        super(message);
    }
}
