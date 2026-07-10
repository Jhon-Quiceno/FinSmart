package com.smartfinance.backend.reportes.model.dto;

import com.smartfinance.backend.analisis.model.dto.AnalysisSummaryResponse;
import com.smartfinance.backend.analisis.model.dto.MonthlySeriesPoint;
import com.smartfinance.backend.analisis.model.dto.TopCategoryResponse;

import java.math.BigDecimal;
import java.util.List;

/**
 * Monthly financial breakdown returned by {@code GET /api/reports/monthly}.
 *
 * <p>Reuses {@link com.smartfinance.backend.analisis.service.FinancialAnalysisService#getSummary} as the
 * single source of truth for these figures instead of recomputing them, so this report can never
 * drift from the dashboard's own numbers (see {@code ReportService#getMonthlyReport}). It carries
 * a subset of {@link AnalysisSummaryResponse}'s fields plus the resolved period, and reuses
 * {@link TopCategoryResponse} / {@link MonthlySeriesPoint} rather than duplicating those shapes.
 *
 * @param periodYear    resolved year of the reported period
 * @param periodMonth   resolved month of the reported period (1-12)
 * @param totalIncome   total income recorded in the period
 * @param totalExpense  total expense recorded in the period
 * @param savings       {@code totalIncome - totalExpense}
 * @param expenseRatio  {@code totalExpense / totalIncome} (0 when income is 0)
 * @param debtRatio     {@code totalDebt / totalIncome} (0 when income is 0)
 * @param topCategories expense-by-category ranking for the period, highest first
 * @param monthlySeries income vs. expense for the period and the 5 prior months, oldest first
 */
public record MonthlyReportResponse(
        int periodYear,
        int periodMonth,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal savings,
        BigDecimal expenseRatio,
        BigDecimal debtRatio,
        List<TopCategoryResponse> topCategories,
        List<MonthlySeriesPoint> monthlySeries
) {
}
