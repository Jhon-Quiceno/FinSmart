package com.smartfinance.backend.analisis.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A single entry in the "last 10 transactions" list returned as part of
 * {@link AnalysisSummaryResponse}, merging {@code Income} and {@code Expense} records so the
 * dashboard can render one combined recent-activity feed from a single call.
 *
 * @param id           identifier of the underlying income or expense record
 * @param type         {@code "INCOME"} or {@code "EXPENSE"}
 * @param amount       transaction amount
 * @param description  optional free-text note
 * @param date         date the transaction was recorded
 * @param categoryName category name, or {@code null} if unclassified
 */
public record RecentTransactionResponse(
        Long id,
        String type,
        BigDecimal amount,
        String description,
        LocalDate date,
        String categoryName
) {
}
