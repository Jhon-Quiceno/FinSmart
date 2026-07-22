package com.smartfinance.backend.ia.model.dto;

/**
 * What kind of financial question a free-text question refers to, as decided by
 * {@code FinancialSummaryQueryService#parseQuery}.
 */
public enum SummaryTopic {
    /** A question about movements (expenses/income/balance) — the default. */
    MOVEMENT,
    /** A question about debts (e.g. {@code "¿cómo voy con mis deudas?"}, {@code "cuánto debo"}). */
    DEBT
}
