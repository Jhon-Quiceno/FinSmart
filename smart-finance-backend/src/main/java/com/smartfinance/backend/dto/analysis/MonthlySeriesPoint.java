package com.smartfinance.backend.dto.analysis;

import java.math.BigDecimal;

/**
 * One point of the 6-month income-vs-expense series returned as part of
 * {@link AnalysisSummaryResponse}, feeding the dashboard's "Ingresos vs Gastos" chart.
 *
 * @param year         calendar year of this point
 * @param month        calendar month of this point (1-12)
 * @param totalIncome  total income recorded in this month
 * @param totalExpense total expense recorded in this month
 */
public record MonthlySeriesPoint(
        int year,
        int month,
        BigDecimal totalIncome,
        BigDecimal totalExpense
) {
}
