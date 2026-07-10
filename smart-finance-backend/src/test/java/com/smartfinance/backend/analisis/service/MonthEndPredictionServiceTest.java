package com.smartfinance.backend.analisis.service;

import com.smartfinance.backend.analisis.model.dto.PredictionResponse;
import com.smartfinance.backend.gastos.repository.ExpenseRepository;
import com.smartfinance.backend.ingresos.repository.IncomeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonthEndPredictionServiceTest {

    @Mock
    private IncomeRepository incomeRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private MonthEndPredictionService monthEndPredictionService;

    @Test
    void computePredictionOnDayOneUsesTodaysSpendAsAverage() {
        LocalDate today = LocalDate.of(2026, 7, 1);
        Long userId = 1L;
        when(expenseRepository.sumAmountByUserAndPeriod(userId, LocalDate.of(2026, 7, 1), today))
                .thenReturn(BigDecimal.valueOf(100));
        when(incomeRepository.sumAmountByUserAndPeriod(userId, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)))
                .thenReturn(BigDecimal.valueOf(4000));

        PredictionResponse result = monthEndPredictionService.computePrediction(userId, today);

        assertEquals(new BigDecimal("100.00"), result.avgDailySpend());
        assertEquals(30, result.daysRemaining());
        assertEquals(new BigDecimal("3100.00"), result.projectedExpense());
        assertEquals(new BigDecimal("900.00"), result.projectedBalance());
        assertEquals(new BigDecimal("130.00"), result.recommendedDailyMax());
        assertFalse(result.alert());
    }

    @Test
    void computePredictionOnLastDayReportsZeroTrueDaysRemainingWithoutPhantomSpend() {
        LocalDate today = LocalDate.of(2026, 7, 31);
        Long userId = 2L;
        when(expenseRepository.sumAmountByUserAndPeriod(userId, LocalDate.of(2026, 7, 1), today))
                .thenReturn(BigDecimal.valueOf(3100));
        when(incomeRepository.sumAmountByUserAndPeriod(userId, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)))
                .thenReturn(BigDecimal.valueOf(3000));

        PredictionResponse result = monthEndPredictionService.computePrediction(userId, today);

        // trueDaysRemaining is 0 on the last day: projectedExpense must equal spentSoFar exactly,
        // with no phantom extra day of avgDailySpend added on top (the pre-fix behavior).
        assertEquals(0, result.daysRemaining());
        assertEquals(new BigDecimal("100.00"), result.avgDailySpend());
        assertEquals(new BigDecimal("3100.00"), result.projectedExpense());
        assertEquals(new BigDecimal("-100.00"), result.projectedBalance());
        assertEquals(BigDecimal.ZERO.setScale(2), result.recommendedDailyMax());
        assertTrue(result.alert());
    }

    @Test
    void computePredictionWithZeroIncomeFloorsRecommendedDailyMaxAndAlerts() {
        LocalDate today = LocalDate.of(2026, 7, 15);
        Long userId = 3L;
        when(expenseRepository.sumAmountByUserAndPeriod(userId, LocalDate.of(2026, 7, 1), today))
                .thenReturn(BigDecimal.valueOf(1500));
        when(incomeRepository.sumAmountByUserAndPeriod(userId, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)))
                .thenReturn(BigDecimal.ZERO);

        PredictionResponse result = monthEndPredictionService.computePrediction(userId, today);

        assertEquals(16, result.daysRemaining());
        assertEquals(new BigDecimal("3100.00"), result.projectedExpense());
        assertEquals(new BigDecimal("-3100.00"), result.projectedBalance());
        assertEquals(BigDecimal.ZERO.setScale(2), result.recommendedDailyMax());
        assertTrue(result.alert());
    }

    @Test
    void computePredictionWithZeroExpensesProjectsFullIncomeAsBalance() {
        LocalDate today = LocalDate.of(2026, 7, 10);
        Long userId = 4L;
        when(expenseRepository.sumAmountByUserAndPeriod(userId, LocalDate.of(2026, 7, 1), today))
                .thenReturn(BigDecimal.ZERO);
        when(incomeRepository.sumAmountByUserAndPeriod(userId, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)))
                .thenReturn(BigDecimal.valueOf(2000));

        PredictionResponse result = monthEndPredictionService.computePrediction(userId, today);

        assertEquals(21, result.daysRemaining());
        assertEquals(BigDecimal.ZERO.setScale(2), result.avgDailySpend());
        assertEquals(BigDecimal.ZERO.setScale(2), result.projectedExpense());
        assertEquals(new BigDecimal("2000.00"), result.projectedBalance());
        assertEquals(new BigDecimal("95.24"), result.recommendedDailyMax());
        assertFalse(result.alert());
    }
}
