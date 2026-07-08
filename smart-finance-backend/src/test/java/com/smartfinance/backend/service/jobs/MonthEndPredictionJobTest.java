package com.smartfinance.backend.service.jobs;

import com.smartfinance.backend.dto.analysis.PredictionResponse;
import com.smartfinance.backend.model.NotificationType;
import com.smartfinance.backend.repository.ExpenseRepository;
import com.smartfinance.backend.service.MonthEndPredictionService;
import com.smartfinance.backend.service.notification.NotificationDispatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonthEndPredictionJobTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-07-15T08:30:00Z"), ZoneOffset.UTC
    );
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 15);
    private static final LocalDate PERIOD_START = LocalDate.of(2026, 7, 1);

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private MonthEndPredictionService monthEndPredictionService;

    @Mock
    private NotificationDispatcher notificationDispatcher;

    private MonthEndPredictionJob monthEndPredictionJob;

    @Test
    void dispatchesAlertWhenProjectedBalanceIsNegative() {
        monthEndPredictionJob = new MonthEndPredictionJob(expenseRepository, monthEndPredictionService, notificationDispatcher, FIXED_CLOCK);
        when(expenseRepository.findDistinctUserIdsByDateBetween(PERIOD_START, TODAY)).thenReturn(List.of(1L));
        when(monthEndPredictionService.computePrediction(1L, TODAY)).thenReturn(negativePrediction());

        monthEndPredictionJob.alertNegativeProjections();

        verify(notificationDispatcher).dispatch(
                eq(1L), eq(NotificationType.MONTH_END_PREDICTION), anyString(), anyString(), eq("month-end:1:2026-07")
        );
    }

    @Test
    void doesNotDispatchWhenProjectedBalanceIsNonNegative() {
        monthEndPredictionJob = new MonthEndPredictionJob(expenseRepository, monthEndPredictionService, notificationDispatcher, FIXED_CLOCK);
        when(expenseRepository.findDistinctUserIdsByDateBetween(PERIOD_START, TODAY)).thenReturn(List.of(2L));
        when(monthEndPredictionService.computePrediction(2L, TODAY)).thenReturn(positivePrediction());

        monthEndPredictionJob.alertNegativeProjections();

        verify(notificationDispatcher, never()).dispatch(
                eq(2L), eq(NotificationType.MONTH_END_PREDICTION), anyString(), anyString(), anyString()
        );
    }

    @Test
    void checksOnlyUsersWithExpensesThisMonthAndOnlyDispatchesForNegativeOnes() {
        monthEndPredictionJob = new MonthEndPredictionJob(expenseRepository, monthEndPredictionService, notificationDispatcher, FIXED_CLOCK);
        when(expenseRepository.findDistinctUserIdsByDateBetween(PERIOD_START, TODAY)).thenReturn(List.of(1L, 2L));
        when(monthEndPredictionService.computePrediction(1L, TODAY)).thenReturn(negativePrediction());
        when(monthEndPredictionService.computePrediction(2L, TODAY)).thenReturn(positivePrediction());

        monthEndPredictionJob.alertNegativeProjections();

        verify(notificationDispatcher).dispatch(
                eq(1L), eq(NotificationType.MONTH_END_PREDICTION), anyString(), anyString(), anyString()
        );
        verify(notificationDispatcher, never()).dispatch(
                eq(2L), eq(NotificationType.MONTH_END_PREDICTION), anyString(), anyString(), anyString()
        );
    }

    @Test
    void doesNotComputePredictionForUsersWithNoExpensesThisMonth() {
        monthEndPredictionJob = new MonthEndPredictionJob(expenseRepository, monthEndPredictionService, notificationDispatcher, FIXED_CLOCK);
        when(expenseRepository.findDistinctUserIdsByDateBetween(PERIOD_START, TODAY)).thenReturn(List.of());

        monthEndPredictionJob.alertNegativeProjections();

        verify(monthEndPredictionService, never()).computePrediction(anyLong(), eq(TODAY));
        verify(notificationDispatcher, never()).dispatch(anyLong(), eq(NotificationType.MONTH_END_PREDICTION), anyString(), anyString(), anyString());
    }

    private PredictionResponse negativePrediction() {
        return new PredictionResponse(
                new BigDecimal("3200.00"), new BigDecimal("-200.00"), new BigDecimal("100.00"), 1,
                BigDecimal.ZERO.setScale(2), true
        );
    }

    private PredictionResponse positivePrediction() {
        return new PredictionResponse(
                new BigDecimal("1000.00"), new BigDecimal("500.00"), new BigDecimal("50.00"), 10,
                new BigDecimal("50.00"), false
        );
    }
}
