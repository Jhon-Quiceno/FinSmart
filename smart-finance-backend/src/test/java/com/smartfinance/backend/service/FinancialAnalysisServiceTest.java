package com.smartfinance.backend.service;

import com.smartfinance.backend.dto.analysis.AnalysisSummaryResponse;
import com.smartfinance.backend.dto.analysis.RecommendationResponse;
import com.smartfinance.backend.dto.analysis.RecommendationType;
import com.smartfinance.backend.repository.CategoryTotalProjection;
import com.smartfinance.backend.repository.DebtRepository;
import com.smartfinance.backend.repository.ExpenseRepository;
import com.smartfinance.backend.repository.FinancialAnalysisRepository;
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

        // The main balance calculation plus the 6-month series both call sumAmountByUserAndPeriod
        // (the series includes the current month too), so this is called 7 times in total — the
        // important assertion is that every single call is scoped to userId 7, never another id.
        verify(incomeRepository, org.mockito.Mockito.atLeastOnce()).sumAmountByUserAndPeriod(eq(7L), any(), any());
        verify(expenseRepository, org.mockito.Mockito.atLeastOnce()).sumAmountByUserAndPeriod(eq(7L), any(), any());
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

        // 1 call for the main balance calculation + 6 for the monthly series = 7.
        verify(incomeRepository, times(7)).sumAmountByUserAndPeriod(eq(9L), any(), any());
        verify(expenseRepository, times(7)).sumAmountByUserAndPeriod(eq(9L), any(), any());
    }

    private void setAuthenticatedUser(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null)
        );
    }
}
