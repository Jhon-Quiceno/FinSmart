package com.smartfinance.backend.ia.model.dto;

/**
 * The date range a financial-summary question refers to, as decided by
 * {@code FinancialSummaryQueryService#parseQuery} from a free-text question sent through the
 * Telegram bot.
 */
public enum SummaryPeriod {
    /** Just today ({@code [today, today]}). */
    TODAY,
    /** Rolling 7-day window ending today ({@code [today - 6 days, today]}). */
    WEEK,
    /** Month-to-date ({@code [first day of the current month, today]}). */
    MONTH
}
