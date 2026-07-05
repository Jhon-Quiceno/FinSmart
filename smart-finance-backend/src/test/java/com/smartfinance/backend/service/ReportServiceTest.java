package com.smartfinance.backend.service;

import com.smartfinance.backend.dto.analysis.AnalysisSummaryResponse;
import com.smartfinance.backend.dto.report.MonthlyReportResponse;
import com.smartfinance.backend.dto.report.ReportMovementRow;
import com.smartfinance.backend.model.Category;
import com.smartfinance.backend.model.Expense;
import com.smartfinance.backend.model.Income;
import com.smartfinance.backend.model.PaymentMethodType;
import com.smartfinance.backend.repository.ExpenseRepository;
import com.smartfinance.backend.repository.IncomeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    private static final int YEAR = 2026;
    private static final int MONTH = 6;
    private static final LocalDate PERIOD_START = LocalDate.of(YEAR, MONTH, 1);
    private static final LocalDate PERIOD_END = LocalDate.of(YEAR, MONTH, 30);

    @Mock
    private FinancialAnalysisService financialAnalysisService;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private IncomeRepository incomeRepository;

    @InjectMocks
    private ReportService reportService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMonthlyReportShouldMapAnalysisSummaryFieldsWithoutRecomputing() {
        AnalysisSummaryResponse summary = new AnalysisSummaryResponse(
                YEAR, MONTH,
                BigDecimal.valueOf(3000), BigDecimal.valueOf(1800), BigDecimal.valueOf(1200),
                new BigDecimal("0.6000"), false,
                BigDecimal.valueOf(500), new BigDecimal("0.1667"), BigDecimal.valueOf(14400),
                List.of(), List.of(), List.of()
        );
        when(financialAnalysisService.getSummary(YEAR, MONTH)).thenReturn(summary);

        MonthlyReportResponse report = reportService.getMonthlyReport(YEAR, MONTH);

        Assertions.assertEquals(YEAR, report.periodYear());
        Assertions.assertEquals(MONTH, report.periodMonth());
        Assertions.assertEquals(BigDecimal.valueOf(3000), report.totalIncome());
        Assertions.assertEquals(BigDecimal.valueOf(1800), report.totalExpense());
        Assertions.assertEquals(BigDecimal.valueOf(1200), report.savings());
        Assertions.assertEquals(new BigDecimal("0.6000"), report.expenseRatio());
        Assertions.assertEquals(new BigDecimal("0.1667"), report.debtRatio());
        Assertions.assertTrue(report.topCategories().isEmpty());
        Assertions.assertTrue(report.monthlySeries().isEmpty());
    }

    @Test
    void getMovementsShouldCombineExpensesAndIncomesSortedByDateDescending() {
        setAuthenticatedUser(1L);

        Category foodCategory = new Category();
        foodCategory.setName("Comida");

        Expense expense = new Expense();
        expense.setDate(LocalDate.of(YEAR, MONTH, 10));
        expense.setAmount(BigDecimal.valueOf(50));
        expense.setDescription("Supermercado");
        expense.setPaymentMethod(PaymentMethodType.DEBIT_CARD);
        expense.setCategory(foodCategory);

        Income income = new Income();
        income.setDate(LocalDate.of(YEAR, MONTH, 15));
        income.setAmount(BigDecimal.valueOf(1500));
        income.setDescription("Sueldo");
        income.setCategory(null);

        when(expenseRepository.findByUserAndPeriod(eq(1L), eq(PERIOD_START), eq(PERIOD_END)))
                .thenReturn(List.of(expense));
        when(incomeRepository.findByUserAndPeriod(eq(1L), eq(PERIOD_START), eq(PERIOD_END)))
                .thenReturn(List.of(income));

        List<ReportMovementRow> movements = reportService.getMovements(YEAR, MONTH);

        Assertions.assertEquals(2, movements.size());
        Assertions.assertEquals("INCOME", movements.get(0).type());
        Assertions.assertEquals(LocalDate.of(YEAR, MONTH, 15), movements.get(0).date());
        Assertions.assertNull(movements.get(0).categoryName());
        Assertions.assertNull(movements.get(0).paymentMethod());

        Assertions.assertEquals("EXPENSE", movements.get(1).type());
        Assertions.assertEquals("Comida", movements.get(1).categoryName());
        Assertions.assertEquals(PaymentMethodType.DEBIT_CARD, movements.get(1).paymentMethod());
    }

    @Test
    void getMovementsShouldScopeQueriesToCurrentUserId() {
        setAuthenticatedUser(7L);

        reportService.getMovements(YEAR, MONTH);

        verify(expenseRepository).findByUserAndPeriod(eq(7L), any(), any());
        verify(incomeRepository).findByUserAndPeriod(eq(7L), any(), any());
    }

    @Test
    void getMovementsShouldDefaultToCurrentYearAndMonthWhenArgumentsAreNull() {
        setAuthenticatedUser(1L);
        java.time.YearMonth now = java.time.YearMonth.now();

        reportService.getMovements(null, null);

        verify(expenseRepository).findByUserAndPeriod(eq(1L), eq(now.atDay(1)), eq(now.atEndOfMonth()));
    }

    private void setAuthenticatedUser(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null)
        );
    }
}
