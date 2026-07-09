package com.smartfinance.backend.dto.income;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Read model for an {@link com.smartfinance.backend.model.Income}.
 *
 * @param id           income identifier
 * @param amount       income amount
 * @param description  optional free-text note
 * @param date         date the income was received
 * @param categoryId   identifier of the associated category, or {@code null} if unclassified
 * @param categoryName name of the associated category, or {@code null} if unclassified
 */
public record IncomeResponse(
        Long id,
        BigDecimal amount,
        String description,
        LocalDate date,
        Long categoryId,
        String categoryName
) {
}
