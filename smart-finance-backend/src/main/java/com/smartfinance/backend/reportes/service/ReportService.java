package com.smartfinance.backend.reportes.service;

import com.smartfinance.backend.analisis.model.dto.AnalysisSummaryResponse;
import com.smartfinance.backend.analisis.service.FinancialAnalysisService;
import com.smartfinance.backend.reportes.model.dto.MonthlyReportResponse;
import com.smartfinance.backend.reportes.model.dto.ReportMovementRow;
import com.smartfinance.backend.gastos.model.entity.Expense;
import com.smartfinance.backend.ingresos.model.entity.Income;
import com.smartfinance.backend.gastos.repository.ExpenseRepository;
import com.smartfinance.backend.ingresos.repository.IncomeRepository;
import com.smartfinance.backend.common.security.SecurityUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Builds the current user's monthly report and its underlying movement list, backing
 * {@code GET /api/reports/monthly} and {@code GET /api/reports/export}.
 *
 * <p>{@link #getMonthlyReport} delegates entirely to
 * {@link FinancialAnalysisService#getSummary}, which already resolves the caller via
 * {@link SecurityUtils#getCurrentUserId()} and computes every figure this report exposes — this
 * class only reshapes that result, it never recomputes totals itself.
 */
@Service
public class ReportService {

    private final FinancialAnalysisService financialAnalysisService;
    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;

    public ReportService(
            FinancialAnalysisService financialAnalysisService,
            ExpenseRepository expenseRepository,
            IncomeRepository incomeRepository
    ) {
        this.financialAnalysisService = financialAnalysisService;
        this.expenseRepository = expenseRepository;
        this.incomeRepository = incomeRepository;
    }

    /**
     * Resolves the monthly financial breakdown for the current user and period, reusing
     * {@link FinancialAnalysisService#getSummary} as the single source of truth for the figures.
     *
     * @param year  requested year, or {@code null} to default to the current calendar year
     * @param month requested month (1-12), or {@code null} to default to the current month
     * @return the monthly report for the resolved period
     */
    public MonthlyReportResponse getMonthlyReport(Integer year, Integer month) {
        AnalysisSummaryResponse summary = financialAnalysisService.getSummary(year, month);
        return new MonthlyReportResponse(
                summary.periodYear(), summary.periodMonth(),
                summary.totalIncome(), summary.totalExpense(), summary.savings(),
                summary.expenseRatio(), summary.debtRatio(),
                summary.topCategories(), summary.monthlySeries()
        );
    }

    /**
     * Every income/expense movement for the current user within the resolved period, most recent
     * first, backing both the report detail view and the CSV/JSON export.
     *
     * @param year  requested year, or {@code null} to default to the current calendar year
     * @param month requested month (1-12), or {@code null} to default to the current month
     * @return combined income/expense movements, sorted by date descending
     */
    public List<ReportMovementRow> getMovements(Integer year, Integer month) {
        Long userId = SecurityUtils.getCurrentUserId();
        YearMonth period = resolvePeriod(year, month);
        LocalDate periodStart = period.atDay(1);
        LocalDate periodEnd = period.atEndOfMonth();

        List<Expense> expenses = expenseRepository.findByUserAndPeriod(userId, periodStart, periodEnd);
        List<Income> incomes = incomeRepository.findByUserAndPeriod(userId, periodStart, periodEnd);

        Stream<ReportMovementRow> expenseRows = expenses.stream().map(expense -> new ReportMovementRow(
                expense.getDate(), "EXPENSE",
                expense.getCategory() != null ? expense.getCategory().getName() : null,
                expense.getDescription(), expense.getAmount(), expense.getPaymentMethod()
        ));
        Stream<ReportMovementRow> incomeRows = incomes.stream().map(income -> new ReportMovementRow(
                income.getDate(), "INCOME",
                income.getCategory() != null ? income.getCategory().getName() : null,
                income.getDescription(), income.getAmount(), null
        ));

        return Stream.concat(expenseRows, incomeRows)
                .sorted(Comparator.comparing(ReportMovementRow::date).reversed())
                .toList();
    }

    /**
     * Resolves the requested period, defaulting to the current calendar year/month when either
     * argument is {@code null} — shared with {@link com.smartfinance.backend.reportes.controller.ReportController}
     * so the CSV/JSON export filename always matches the period actually reported.
     *
     * @param year  requested year, or {@code null} to default to the current calendar year
     * @param month requested month (1-12), or {@code null} to default to the current month
     * @return the resolved period
     */
    public static YearMonth resolvePeriod(Integer year, Integer month) {
        YearMonth now = YearMonth.now();
        int resolvedYear = year != null ? year : now.getYear();
        int resolvedMonth = month != null ? month : now.getMonthValue();
        return YearMonth.of(resolvedYear, resolvedMonth);
    }
}
