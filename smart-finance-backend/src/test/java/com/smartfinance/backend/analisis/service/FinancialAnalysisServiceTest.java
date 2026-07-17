package com.smartfinance.backend.analisis.service;

import com.smartfinance.backend.analisis.model.dto.AnalysisSummaryResponse;
import com.smartfinance.backend.analisis.model.dto.MonthlySeriesPoint;
import com.smartfinance.backend.analisis.model.dto.RecommendationResponse;
import com.smartfinance.backend.analisis.model.dto.RecommendationType;
import com.smartfinance.backend.common.repository.MonthlyTotalProjection;
import com.smartfinance.backend.gastos.repository.CategoryTotalProjection;
import com.smartfinance.backend.deudas.repository.DebtRepository;
import com.smartfinance.backend.gastos.repository.ExpenseRepository;
import com.smartfinance.backend.analisis.repository.FinancialAnalysisRepository;
import com.smartfinance.backend.ingresos.repository.IncomeRepository;
import com.smartfinance.backend.tarjetas.repository.CreditCardRepository;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialAnalysisServiceTest {

    private static final int YEAR = 2026;
    private static final int MONTH = 6;
    private static final LocalDate PERIOD_START = LocalDate.of(YEAR, MONTH, 1);
    private static final LocalDate PERIOD_END = LocalDate.of(YEAR, MONTH, 30);

    @Mock
    private IncomeRepository incomeRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private DebtRepository debtRepository;

    @Mock
    private CreditCardRepository creditCardRepository;

    @Mock
    private FinancialAnalysisRepository financialAnalysisRepository;

    @InjectMocks
    private FinancialAnalysisService financialAnalysisService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getSummaryShouldCalculateBalanceIncomeExpenseAndSavings() {
        setAuthenticatedUser(1L);
        when(incomeRepository.sumAmountByUserAndPeriod(eq(1L), eq(PERIOD_START), eq(PERIOD_END)))
                .thenReturn(BigDecimal.valueOf(3000));
        when(expenseRepository.sumAmountByUserAndPeriod(eq(1L), eq(PERIOD_START), eq(PERIOD_END)))
                .thenReturn(BigDecimal.valueOf(1800));

        AnalysisSummaryResponse summary = financialAnalysisService.getSummary(YEAR, MONTH);

        Assertions.assertEquals(BigDecimal.valueOf(3000), summary.totalIncome());
        Assertions.assertEquals(BigDecimal.valueOf(1800), summary.totalExpense());
        Assertions.assertEquals(BigDecimal.valueOf(1200), summary.savings());
        Assertions.assertEquals(BigDecimal.valueOf(1200 * 12L), summary.annualSavingsProjection());
        Assertions.assertEquals(new BigDecimal("0.6000"), summary.expenseRatio());
    }

    @Test
    void getSummaryShouldSetExpenseAlertWhenRatioExceeds80Percent() {
        setAuthenticatedUser(1L);
        when(incomeRepository.sumAmountByUserAndPeriod(eq(1L), eq(PERIOD_START), eq(PERIOD_END)))
                .thenReturn(BigDecimal.valueOf(1000));
        when(expenseRepository.sumAmountByUserAndPeriod(eq(1L), eq(PERIOD_START), eq(PERIOD_END)))
                .thenReturn(BigDecimal.valueOf(850));

        AnalysisSummaryResponse summary = financialAnalysisService.getSummary(YEAR, MONTH);

        Assertions.assertTrue(summary.expenseAlert());
    }

    @Test
    void getSummaryShouldNotSetExpenseAlertWhenRatioIsExactly80Percent() {
        setAuthenticatedUser(1L);
        when(incomeRepository.sumAmountByUserAndPeriod(eq(1L), eq(PERIOD_START), eq(PERIOD_END)))
                .thenReturn(BigDecimal.valueOf(1000));
        when(expenseRepository.sumAmountByUserAndPeriod(eq(1L), eq(PERIOD_START), eq(PERIOD_END)))
                .thenReturn(BigDecimal.valueOf(800));

        AnalysisSummaryResponse summary = financialAnalysisService.getSummary(YEAR, MONTH);

        Assertions.assertFalse(summary.expenseAlert());
    }

    @Test
    void getSummaryShouldGuardAgainstZeroIncomeWhenComputingRatios() {
        setAuthenticatedUser(1L);
        when(expenseRepository.sumAmountByUserAndPeriod(eq(1L), eq(PERIOD_START), eq(PERIOD_END)))
                .thenReturn(BigDecimal.valueOf(500));
        when(debtRepository.sumRemainingAmountByUser(1L)).thenReturn(BigDecimal.valueOf(200));

        AnalysisSummaryResponse summary = financialAnalysisService.getSummary(YEAR, MONTH);

        Assertions.assertEquals(BigDecimal.ZERO, summary.expenseRatio());
        Assertions.assertEquals(BigDecimal.ZERO, summary.debtRatio());
        Assertions.assertFalse(summary.expenseAlert());
    }

    @Test
    void getSummaryShouldCalculateDebtRatioUsingRemainingAmountNotTotalAmount() {
        setAuthenticatedUser(1L);
        when(incomeRepository.sumAmountByUserAndPeriod(eq(1L), eq(PERIOD_START), eq(PERIOD_END)))
                .thenReturn(BigDecimal.valueOf(2000));
        when(debtRepository.sumRemainingAmountByUser(1L)).thenReturn(BigDecimal.valueOf(1000));

        AnalysisSummaryResponse summary = financialAnalysisService.getSummary(YEAR, MONTH);

        Assertions.assertEquals(BigDecimal.valueOf(1000), summary.totalDebt());
        Assertions.assertEquals(new BigDecimal("0.5000"), summary.debtRatio());
        verify(debtRepository).sumRemainingAmountByUser(1L);
    }

    @Test
    void getSummaryShouldLeaveDebtRatioUnchangedWhenUserHasNoCreditCards() {
        // Regresión: la integración cruzada con tarjetas (Fase B.2) es aditiva — un usuario sin
        // tarjetas no debe ver alterado su debtRatio. creditCardRepository no se stubea a
        // propósito: el default de Mockito (null) debe tratarse como 0 vía nullSafe().
        setAuthenticatedUser(1L);
        when(incomeRepository.sumAmountByUserAndPeriod(eq(1L), eq(PERIOD_START), eq(PERIOD_END)))
                .thenReturn(BigDecimal.valueOf(2000));
        when(debtRepository.sumRemainingAmountByUser(1L)).thenReturn(BigDecimal.valueOf(1000));

        AnalysisSummaryResponse summary = financialAnalysisService.getSummary(YEAR, MONTH);

        Assertions.assertEquals(BigDecimal.valueOf(1000), summary.totalDebt());
        Assertions.assertEquals(new BigDecimal("0.5000"), summary.debtRatio());
    }

    @Test
    void getSummaryShouldIncludeCreditCardBalanceInDebtRatio() {
        setAuthenticatedUser(1L);
        when(incomeRepository.sumAmountByUserAndPeriod(eq(1L), eq(PERIOD_START), eq(PERIOD_END)))
                .thenReturn(BigDecimal.valueOf(2000));
        when(debtRepository.sumRemainingAmountByUser(1L)).thenReturn(BigDecimal.valueOf(1000));
        when(creditCardRepository.sumCurrentBalanceByUser(1L)).thenReturn(BigDecimal.valueOf(500));

        AnalysisSummaryResponse summary = financialAnalysisService.getSummary(YEAR, MONTH);

        Assertions.assertEquals(BigDecimal.valueOf(1500), summary.totalDebt());
        Assertions.assertEquals(new BigDecimal("0.7500"), summary.debtRatio());
        verify(creditCardRepository).sumCurrentBalanceByUser(1L);
    }

    @Test
    void getSummaryShouldOrderTopCategoriesByAmountDescWithPercentageOfTotalExpense() {
        setAuthenticatedUser(1L);
        when(expenseRepository.sumAmountByUserAndPeriod(eq(1L), eq(PERIOD_START), eq(PERIOD_END)))
                .thenReturn(BigDecimal.valueOf(1000));

        CategoryTotalProjection food = mock(CategoryTotalProjection.class);
        when(food.getCategoryId()).thenReturn(4L);
        when(food.getCategoryName()).thenReturn("Comida");
        when(food.getTotal()).thenReturn(BigDecimal.valueOf(600));

        CategoryTotalProjection transport = mock(CategoryTotalProjection.class);
        when(transport.getCategoryId()).thenReturn(5L);
        when(transport.getCategoryName()).thenReturn("Transporte");
        when(transport.getTotal()).thenReturn(BigDecimal.valueOf(400));

        when(expenseRepository.findTopCategoriesByUserAndPeriod(eq(1L), eq(PERIOD_START), eq(PERIOD_END)))
                .thenReturn(List.of(food, transport));

        AnalysisSummaryResponse summary = financialAnalysisService.getSummary(YEAR, MONTH);

        Assertions.assertEquals(2, summary.topCategories().size());
        Assertions.assertEquals("Comida", summary.topCategories().get(0).categoryName());
        Assertions.assertEquals(new BigDecimal("0.6000"), summary.topCategories().get(0).percentage());
        Assertions.assertEquals("Transporte", summary.topCategories().get(1).categoryName());
        Assertions.assertEquals(new BigDecimal("0.4000"), summary.topCategories().get(1).percentage());
    }

    @Test
    void getSummaryShouldFallBackToSinCategoriaWhenCategoryNameIsNull() {
        setAuthenticatedUser(1L);
        when(expenseRepository.sumAmountByUserAndPeriod(eq(1L), eq(PERIOD_START), eq(PERIOD_END)))
                .thenReturn(BigDecimal.valueOf(300));

        CategoryTotalProjection unclassified = mock(CategoryTotalProjection.class);
        when(unclassified.getCategoryId()).thenReturn(null);
        when(unclassified.getCategoryName()).thenReturn(null);
        when(unclassified.getTotal()).thenReturn(BigDecimal.valueOf(300));

        when(expenseRepository.findTopCategoriesByUserAndPeriod(eq(1L), eq(PERIOD_START), eq(PERIOD_END)))
                .thenReturn(List.of(unclassified));

        AnalysisSummaryResponse summary = financialAnalysisService.getSummary(YEAR, MONTH);

        Assertions.assertEquals("Sin categoría", summary.topCategories().get(0).categoryName());
        Assertions.assertNull(summary.topCategories().get(0).categoryId());
    }

    @Test
    void getSummaryShouldUpsertSnapshotOnceInsteadOfFindThenSave() {
        setAuthenticatedUser(1L);
        when(incomeRepository.sumAmountByUserAndPeriod(eq(1L), eq(PERIOD_START), eq(PERIOD_END)))
                .thenReturn(BigDecimal.valueOf(1000));
        when(expenseRepository.sumAmountByUserAndPeriod(eq(1L), eq(PERIOD_START), eq(PERIOD_END)))
                .thenReturn(BigDecimal.valueOf(400));

        financialAnalysisService.getSummary(YEAR, MONTH);

        verify(financialAnalysisRepository, times(1)).upsertSnapshot(
                eq(1L), eq(YEAR), eq(MONTH),
                eq(BigDecimal.valueOf(1000)), eq(BigDecimal.valueOf(400)), eq(BigDecimal.valueOf(600)),
                any(BigDecimal.class), any(BigDecimal.class), eq(null)
        );
        verify(financialAnalysisRepository, never()).findByUser_IdAndPeriodYearAndPeriodMonth(anyLong(), any(), any());
        verify(financialAnalysisRepository, never()).save(any());
    }

    @Test
    void getSummaryShouldScopeAllQueriesToCurrentUserId() {
        setAuthenticatedUser(7L);

        financialAnalysisService.getSummary(YEAR, MONTH);

        // The main balance calculation calls sumAmountByUserAndPeriod once per repository, and
        // the 6-month series is now a single grouped query per repository (see
        // buildMonthlySeriesShould* tests below) — the important assertion here is that every
        // single call is scoped to userId 7, never another id.
        verify(incomeRepository).sumAmountByUserAndPeriod(eq(7L), any(), any());
        verify(expenseRepository).sumAmountByUserAndPeriod(eq(7L), any(), any());
        verify(incomeRepository).sumAmountByUserGroupedByMonth(eq(7L), any(), any());
        verify(expenseRepository).sumAmountByUserGroupedByMonth(eq(7L), any(), any());
        verify(debtRepository).sumRemainingAmountByUser(7L);
        verify(expenseRepository).findTopCategoriesByUserAndPeriod(eq(7L), any(), any());
        verify(incomeRepository).findRecentByUserId(eq(7L), any());
        verify(expenseRepository).findRecentByUserId(eq(7L), any());
    }

    @Test
    void getSummaryShouldDefaultToCurrentYearAndMonthWhenArgumentsAreNull() {
        setAuthenticatedUser(1L);

        AnalysisSummaryResponse summary = financialAnalysisService.getSummary(null, null);

        Assertions.assertEquals(LocalDate.now().getYear(), summary.periodYear());
        Assertions.assertEquals(LocalDate.now().getMonthValue(), summary.periodMonth());
    }

    @Test
    void getRecommendationsShouldFireFoodCategoryRuleWhenFoodCategoryExceeds30PercentOfIncome() {
        setAuthenticatedUser(1L);
        when(incomeRepository.sumAmountByUserAndPeriod(eq(1L), eq(PERIOD_START), eq(PERIOD_END)))
                .thenReturn(BigDecimal.valueOf(1000));
        when(expenseRepository.sumAmountByUserAndPeriod(eq(1L), eq(PERIOD_START), eq(PERIOD_END)))
                .thenReturn(BigDecimal.valueOf(350));

        CategoryTotalProjection food = mock(CategoryTotalProjection.class);
        when(food.getCategoryId()).thenReturn(4L);
        when(food.getCategoryName()).thenReturn("Alimentación");
        when(food.getTotal()).thenReturn(BigDecimal.valueOf(350));
        when(expenseRepository.findTopCategoriesByUserAndPeriod(eq(1L), eq(PERIOD_START), eq(PERIOD_END)))
                .thenReturn(List.of(food));

        List<RecommendationResponse> recommendations = financialAnalysisService.getRecommendations(YEAR, MONTH);

        Assertions.assertTrue(recommendations.stream().anyMatch(r ->
                r.type() == RecommendationType.RECOMMENDATION
                        && r.categoryId() != null && r.categoryId().equals(4L)
                        && r.message().toLowerCase().contains("comida")
        ));
    }

    @Test
    void getRecommendationsShouldFireGenericTopCategoryRuleWhenNonFoodCategoryExceeds30PercentOfIncome() {
        setAuthenticatedUser(1L);
        when(incomeRepository.sumAmountByUserAndPeriod(eq(1L), eq(PERIOD_START), eq(PERIOD_END)))
                .thenReturn(BigDecimal.valueOf(1000));
        when(expenseRepository.sumAmountByUserAndPeriod(eq(1L), eq(PERIOD_START), eq(PERIOD_END)))
                .thenReturn(BigDecimal.valueOf(350));

        CategoryTotalProjection housing = mock(CategoryTotalProjection.class);
        when(housing.getCategoryId()).thenReturn(6L);
        when(housing.getCategoryName()).thenReturn("Vivienda");
        when(housing.getTotal()).thenReturn(BigDecimal.valueOf(350));
        when(expenseRepository.findTopCategoriesByUserAndPeriod(eq(1L), eq(PERIOD_START), eq(PERIOD_END)))
                .thenReturn(List.of(housing));

        List<RecommendationResponse> recommendations = financialAnalysisService.getRecommendations(YEAR, MONTH);

        Assertions.assertTrue(recommendations.stream().anyMatch(r ->
                r.type() == RecommendationType.RECOMMENDATION
                        && r.categoryId() != null && r.categoryId().equals(6L)
                        && !r.message().toLowerCase().contains("comida")
        ));
    }

    @Test
    void getRecommendationsShouldNotFireTopCategoryRuleWhenBelow30Percent() {
        setAuthenticatedUser(1L);
        when(incomeRepository.sumAmountByUserAndPeriod(eq(1L), eq(PERIOD_START), eq(PERIOD_END)))
                .thenReturn(BigDecimal.valueOf(1000));
        when(expenseRepository.sumAmountByUserAndPeriod(eq(1L), eq(PERIOD_START), eq(PERIOD_END)))
                .thenReturn(BigDecimal.valueOf(200));

        CategoryTotalProjection food = mock(CategoryTotalProjection.class);
        when(food.getCategoryId()).thenReturn(4L);
        when(food.getCategoryName()).thenReturn("Comida");
        when(food.getTotal()).thenReturn(BigDecimal.valueOf(200));
        when(expenseRepository.findTopCategoriesByUserAndPeriod(eq(1L), eq(PERIOD_START), eq(PERIOD_END)))
                .thenReturn(List.of(food));

        List<RecommendationResponse> recommendations = financialAnalysisService.getRecommendations(YEAR, MONTH);

        Assertions.assertTrue(recommendations.stream().noneMatch(r -> r.categoryId() != null && r.categoryId().equals(4L)));
    }

    @Test
    void getRecommendationsShouldFireDebtRatioAlertWhenAbove40Percent() {
        setAuthenticatedUser(1L);
        when(incomeRepository.sumAmountByUserAndPeriod(eq(1L), eq(PERIOD_START), eq(PERIOD_END)))
                .thenReturn(BigDecimal.valueOf(1000));
        when(debtRepository.sumRemainingAmountByUser(1L)).thenReturn(BigDecimal.valueOf(500));

        List<RecommendationResponse> recommendations = financialAnalysisService.getRecommendations(YEAR, MONTH);

        Assertions.assertTrue(recommendations.stream().anyMatch(r ->
                r.type() == RecommendationType.ALERT && r.message().toLowerCase().contains("endeudamiento")
        ));
    }

    @Test
    void getRecommendationsShouldFireNegativeSavingsAlertWhenExpensesExceedIncome() {
        setAuthenticatedUser(1L);
        when(incomeRepository.sumAmountByUserAndPeriod(eq(1L), eq(PERIOD_START), eq(PERIOD_END)))
                .thenReturn(BigDecimal.valueOf(500));
        when(expenseRepository.sumAmountByUserAndPeriod(eq(1L), eq(PERIOD_START), eq(PERIOD_END)))
                .thenReturn(BigDecimal.valueOf(700));

        List<RecommendationResponse> recommendations = financialAnalysisService.getRecommendations(YEAR, MONTH);

        Assertions.assertTrue(recommendations.stream().anyMatch(r ->
                r.type() == RecommendationType.ALERT && r.message().toLowerCase().contains("negativo")
        ));
    }

    @Test
    void getRecommendationsShouldFireLowSavingsRecommendationWhenSavingsRatioBelow10Percent() {
        setAuthenticatedUser(1L);
        when(incomeRepository.sumAmountByUserAndPeriod(eq(1L), eq(PERIOD_START), eq(PERIOD_END)))
                .thenReturn(BigDecimal.valueOf(1000));
        when(expenseRepository.sumAmountByUserAndPeriod(eq(1L), eq(PERIOD_START), eq(PERIOD_END)))
                .thenReturn(BigDecimal.valueOf(950));

        List<RecommendationResponse> recommendations = financialAnalysisService.getRecommendations(YEAR, MONTH);

        Assertions.assertTrue(recommendations.stream().anyMatch(r ->
                r.type() == RecommendationType.RECOMMENDATION && r.message().toLowerCase().contains("ahorro")
        ));
    }

    @Test
    void getRecommendationsShouldReturnInfoEntryWhenNoRulesFire() {
        setAuthenticatedUser(1L);
        when(incomeRepository.sumAmountByUserAndPeriod(eq(1L), eq(PERIOD_START), eq(PERIOD_END)))
                .thenReturn(BigDecimal.valueOf(1000));
        when(expenseRepository.sumAmountByUserAndPeriod(eq(1L), eq(PERIOD_START), eq(PERIOD_END)))
                .thenReturn(BigDecimal.valueOf(500));

        List<RecommendationResponse> recommendations = financialAnalysisService.getRecommendations(YEAR, MONTH);

        Assertions.assertEquals(1, recommendations.size());
        Assertions.assertEquals(RecommendationType.INFO, recommendations.get(0).type());
    }

    @Test
    void getRecommendationsShouldScopeToCurrentUserId() {
        setAuthenticatedUser(9L);

        financialAnalysisService.getRecommendations(YEAR, MONTH);

        // 1 call for the main balance calculation; the monthly series is now a single grouped
        // query per repository (sumAmountByUserGroupedByMonth), not one call per month.
        verify(incomeRepository, times(1)).sumAmountByUserAndPeriod(eq(9L), any(), any());
        verify(expenseRepository, times(1)).sumAmountByUserAndPeriod(eq(9L), any(), any());
        verify(incomeRepository, times(1)).sumAmountByUserGroupedByMonth(eq(9L), any(), any());
        verify(expenseRepository, times(1)).sumAmountByUserGroupedByMonth(eq(9L), any(), any());
    }

    @Test
    void getRecommendationsShouldNotUpsertSnapshot() {
        setAuthenticatedUser(1L);
        when(incomeRepository.sumAmountByUserAndPeriod(eq(1L), eq(PERIOD_START), eq(PERIOD_END)))
                .thenReturn(BigDecimal.valueOf(1000));
        when(expenseRepository.sumAmountByUserAndPeriod(eq(1L), eq(PERIOD_START), eq(PERIOD_END)))
                .thenReturn(BigDecimal.valueOf(400));

        financialAnalysisService.getRecommendations(YEAR, MONTH);

        verify(financialAnalysisRepository, never()).upsertSnapshot(
                anyLong(), any(), any(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void buildMonthlySeriesShouldFillMissingMonthsWithZeroWhenNoDataReturned() {
        setAuthenticatedUser(1L);
        when(incomeRepository.sumAmountByUserGroupedByMonth(eq(1L), any(), any())).thenReturn(List.of());
        when(expenseRepository.sumAmountByUserGroupedByMonth(eq(1L), any(), any())).thenReturn(List.of());

        AnalysisSummaryResponse summary = financialAnalysisService.getSummary(YEAR, MONTH);

        List<MonthlySeriesPoint> series = summary.monthlySeries();
        Assertions.assertEquals(6, series.size());
        Assertions.assertTrue(series.stream().allMatch(point ->
                point.totalIncome().compareTo(BigDecimal.ZERO) == 0
                        && point.totalExpense().compareTo(BigDecimal.ZERO) == 0
        ));
        // 2026-06 is the current period; the series covers 2026-01 through 2026-06 in order.
        Assertions.assertEquals(2026, series.get(0).year());
        Assertions.assertEquals(1, series.get(0).month());
        Assertions.assertEquals(2026, series.get(5).year());
        Assertions.assertEquals(6, series.get(5).month());
    }

    @Test
    void buildMonthlySeriesShouldMapTotalsForMonthsWithDataAndZeroOutTheRest() {
        setAuthenticatedUser(1L);
        MonthlyTotalProjection marchIncome = mockMonthlyTotal(2026, 3, BigDecimal.valueOf(1500));
        MonthlyTotalProjection juneIncome = mockMonthlyTotal(2026, 6, BigDecimal.valueOf(2000));
        when(incomeRepository.sumAmountByUserGroupedByMonth(eq(1L), any(), any()))
                .thenReturn(List.of(marchIncome, juneIncome));

        MonthlyTotalProjection aprilExpense = mockMonthlyTotal(2026, 4, BigDecimal.valueOf(300));
        when(expenseRepository.sumAmountByUserGroupedByMonth(eq(1L), any(), any()))
                .thenReturn(List.of(aprilExpense));

        AnalysisSummaryResponse summary = financialAnalysisService.getSummary(YEAR, MONTH);

        List<MonthlySeriesPoint> series = summary.monthlySeries();
        MonthlySeriesPoint march = findPoint(series, 3);
        MonthlySeriesPoint april = findPoint(series, 4);
        MonthlySeriesPoint june = findPoint(series, 6);

        Assertions.assertEquals(BigDecimal.valueOf(1500), march.totalIncome());
        Assertions.assertEquals(BigDecimal.ZERO, march.totalExpense());
        Assertions.assertEquals(BigDecimal.ZERO, april.totalIncome());
        Assertions.assertEquals(BigDecimal.valueOf(300), april.totalExpense());
        Assertions.assertEquals(BigDecimal.valueOf(2000), june.totalIncome());
    }

    @Test
    void buildMonthlySeriesShouldQueryFullRangeFromOldestMonthFirstDayToCurrentMonthLastDay() {
        setAuthenticatedUser(1L);

        financialAnalysisService.getSummary(YEAR, MONTH);

        // Series covers the 6 months ending at 2026-06, so the oldest month is 2026-01: the
        // grouped query must span its first day (2026-01-01) through the current month's last
        // day (2026-06-30), not just the current month.
        verify(incomeRepository).sumAmountByUserGroupedByMonth(
                eq(1L), eq(LocalDate.of(2026, 1, 1)), eq(LocalDate.of(2026, 6, 30))
        );
        verify(expenseRepository).sumAmountByUserGroupedByMonth(
                eq(1L), eq(LocalDate.of(2026, 1, 1)), eq(LocalDate.of(2026, 6, 30))
        );
    }

    private static MonthlyTotalProjection mockMonthlyTotal(int year, int month, BigDecimal total) {
        MonthlyTotalProjection projection = mock(MonthlyTotalProjection.class);
        when(projection.getPeriodYear()).thenReturn(year);
        when(projection.getPeriodMonth()).thenReturn(month);
        when(projection.getTotal()).thenReturn(total);
        return projection;
    }

    private static MonthlySeriesPoint findPoint(List<MonthlySeriesPoint> series, int month) {
        return series.stream()
                .filter(point -> point.month() == month)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No series point found for month " + month));
    }

    private void setAuthenticatedUser(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null)
        );
    }
}
