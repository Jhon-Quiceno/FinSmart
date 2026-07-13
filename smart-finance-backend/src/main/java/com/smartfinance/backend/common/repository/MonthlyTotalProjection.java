package com.smartfinance.backend.common.repository;

import java.math.BigDecimal;

/**
 * Projection of a total amount grouped by calendar month for one user, as returned by
 * {@code IncomeRepository#sumAmountByUserGroupedByMonth} and
 * {@code ExpenseRepository#sumAmountByUserGroupedByMonth}.
 *
 * <p>Backs {@code FinancialAnalysisService#buildMonthlySeries}, which resolves a fixed number of
 * months in a single grouped query per repository instead of one query per month — callers must
 * fill in any month absent from the result themselves (a month with no rows simply has no
 * projection here), the same "gap filling is the caller's job" pattern as
 * {@link com.smartfinance.backend.gastos.repository.CategoryTotalProjection}.
 */
public interface MonthlyTotalProjection {

    Integer getPeriodYear();

    Integer getPeriodMonth();

    BigDecimal getTotal();
}
