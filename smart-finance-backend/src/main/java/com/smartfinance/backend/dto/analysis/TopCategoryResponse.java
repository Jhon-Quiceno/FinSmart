package com.smartfinance.backend.dto.analysis;

import java.math.BigDecimal;

/**
 * A single entry in the current period's expense-by-category ranking.
 *
 * @param categoryId   category identifier, or {@code null} for unclassified expenses
 * @param categoryName category name, or {@code "Sin categoría"} for unclassified expenses
 * @param totalAmount  total amount spent in this category during the period
 * @param percentage   {@code totalAmount} as a fraction of the period's total expense
 *                     (e.g. {@code 0.3250} for 32.5%), not of income
 */
public record TopCategoryResponse(
        Long categoryId,
        String categoryName,
        BigDecimal totalAmount,
        BigDecimal percentage
) {
}
