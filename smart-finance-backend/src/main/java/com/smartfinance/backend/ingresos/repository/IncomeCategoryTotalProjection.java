package com.smartfinance.backend.ingresos.repository;

import java.math.BigDecimal;

/**
 * Projection of the total {@link com.smartfinance.backend.ingresos.model.entity.Income} amount grouped
 * by category for a user/period, as returned by {@link IncomeRepository#findTopCategoriesByUserAndPeriod}.
 *
 * <p>Mirrors {@code com.smartfinance.backend.gastos.repository.CategoryTotalProjection}, kept as a
 * separate type instead of reused across domains so {@code ingresos} does not depend on {@code gastos}
 * for something as basic as a category total projection.
 *
 * <p>{@link #getCategoryId()} and {@link #getCategoryName()} are {@code null} for income left
 * unclassified (see {@link com.smartfinance.backend.ingresos.model.entity.Income#getCategory()}); callers
 * must apply the "Sin categoría" fallback themselves rather than relying on the query, so the fallback
 * stays testable without a real database.
 */
public interface IncomeCategoryTotalProjection {

    Long getCategoryId();

    String getCategoryName();

    BigDecimal getTotal();
}
