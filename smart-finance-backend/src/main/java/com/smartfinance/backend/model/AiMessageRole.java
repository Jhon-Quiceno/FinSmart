package com.smartfinance.backend.model;

/**
 * Author of an {@link AiMessage} turn in a conversation.
 *
 * <p>Persisted as a {@code VARCHAR} column with a database {@code CHECK} constraint (see
 * {@code V8__create_ai_messages.sql}), matching the rest of the project's simple
 * varchar-plus-check approach to enumerations (see {@link PaymentMethodType}).
 */
public enum AiMessageRole {
    USER,
    ASSISTANT
}
