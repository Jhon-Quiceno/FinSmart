package com.smartfinance.backend.ia.model.entity;

/**
 * Which AI-backed operation produced an {@link AiUsageEvent} row: a chat reply, a category
 * suggestion, a generated financial insight, or a bank statement transaction extraction.
 *
 * <p>Persisted as a {@code VARCHAR} column with a database {@code CHECK} constraint (see
 * {@code V16__create_ai_usage_events.sql}, extended by {@code V23__add_statement_extract_event_type.sql}),
 * matching the rest of the project's simple varchar-plus-check approach to enumerations (see
 * {@link AiMessageKind}).
 */
public enum AiUsageEventType {
    CHAT,
    CATEGORIZE,
    INSIGHT,
    STATEMENT_EXTRACT
}
