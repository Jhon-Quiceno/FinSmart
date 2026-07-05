package com.smartfinance.backend.dto.report;

import com.smartfinance.backend.model.PaymentMethodType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A single income or expense movement within a reported period, backing both
 * {@code GET /api/reports/monthly}'s underlying detail list and
 * {@code GET /api/reports/export} (CSV/JSON).
 *
 * @param date           date the movement was recorded
 * @param type           {@code "INCOME"} or {@code "EXPENSE"}
 * @param categoryName   category name, or {@code null} if unclassified
 * @param description    optional free-text note
 * @param amount         movement amount
 * @param paymentMethod  method used to pay, or {@code null} for an income movement
 */
public record ReportMovementRow(
        LocalDate date,
        String type,
        String categoryName,
        String description,
        BigDecimal amount,
        PaymentMethodType paymentMethod
) {
}
