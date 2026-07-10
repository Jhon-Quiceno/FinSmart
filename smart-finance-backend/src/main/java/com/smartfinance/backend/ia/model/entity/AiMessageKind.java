package com.smartfinance.backend.ia.model.entity;

/**
 * Purpose of an {@link AiMessage} row.
 *
 * <p>{@link #CHAT} rows are turns in the free-form conversation on {@code /asistente-ia}
 * (Batch 2). {@link #INSIGHT} rows are AI-generated financial recommendations persisted so the
 * dashboard can show the latest one without burning the free-tier rate limit of the configured
 * provider on every page load (see {@code docs/sprints/sprint5.md}, architecture decision 8).
 *
 * <p>Persisted as a {@code VARCHAR} column with a database {@code CHECK} constraint (see
 * {@code V8__create_ai_messages.sql}), matching the rest of the project's simple
 * varchar-plus-check approach to enumerations (see {@link PaymentMethodType}).
 */
public enum AiMessageKind {
    CHAT,
    INSIGHT
}
