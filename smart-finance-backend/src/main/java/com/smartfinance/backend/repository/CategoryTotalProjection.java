package com.smartfinance.backend.repository;

import java.math.BigDecimal;

/**
 * Projection of the total {@link com.smartfinance.backend.model.Expense} amount grouped by
 * category for a user/period, as returned by
 * {@link ExpenseRepository#findTopCategoriesByUserAndPeriod}.
 *
 * <p>{@link #getCategoryId()} and {@link #getCategoryName()} are {@code null} for expenses left
 * unclassified (see {@link com.smartfinance.backend.model.Expense#getCategory()}); callers must
 * apply the "Sin categoría" fallback themselves rather than relying on the query, so the
 * fallback stays testable without a real database.
 */
public interface CategoryTotalProjection {

    Long getCategoryId();

    String getCategoryName();

    BigDecimal getTotal();
}
