package com.smartfinance.backend.service.jobs;

import com.smartfinance.backend.model.NotificationType;
import com.smartfinance.backend.repository.CategoryTotalProjection;
import com.smartfinance.backend.repository.ExpenseRepository;
import com.smartfinance.backend.repository.IncomeRepository;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeeklySummaryJobTest {

    // Monday 2026-07-13; summarized week is 2026-07-06..2026-07-12 (ISO week 28 of 2026).
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-07-13T07:00:00Z"), ZoneOffset.UTC
    );
    private static final LocalDate WEEK_START = LocalDate.of(2026, 7, 6);
    private static final LocalDate WEEK_END = LocalDate.of(2026, 7, 12);

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private IncomeRepository incomeRepository;

    @Mock
    private NotificationDispatcher notificationDispatcher;

    private WeeklySummaryJob weeklySummaryJob;

    @Test
    void dispatchesWeeklySummaryForEachUserWithActivityInTheWindow() {
        weeklySummaryJob = new WeeklySummaryJob(expenseRepository, incomeRepository, notificationDispatcher, FIXED_CLOCK);
        when(incomeRepository.findDistinctUserIdsByDateBetween(WEEK_START, WEEK_END)).thenReturn(List.of(1L));
        when(expenseRepository.findDistinctUserIdsByDateBetween(WEEK_START, WEEK_END)).thenReturn(List.of());
        when(incomeRepository.sumAmountByUserAndPeriod(1L, WEEK_START, WEEK_END)).thenReturn(BigDecimal.valueOf(500));
        when(expenseRepository.sumAmountByUserAndPeriod(1L, WEEK_START, WEEK_END)).thenReturn(BigDecimal.valueOf(300));
        when(expenseRepository.findTopCategoriesByUserAndPeriod(1L, WEEK_START, WEEK_END)).thenReturn(List.of());

        weeklySummaryJob.sendWeeklySummaries();

        verify(notificationDispatcher).dispatch(
                eq(1L), eq(NotificationType.WEEKLY_SUMMARY), anyString(), anyString(), eq("weekly-summary:1:2026-W28")
        );
    }

    @Test
    void dedupesUsersPresentInBothIncomeAndExpenseActivityLists() {
        weeklySummaryJob = new WeeklySummaryJob(expenseRepository, incomeRepository, notificationDispatcher, FIXED_CLOCK);
        when(incomeRepository.findDistinctUserIdsByDateBetween(WEEK_START, WEEK_END)).thenReturn(List.of(2L));
        when(expenseRepository.findDistinctUserIdsByDateBetween(WEEK_START, WEEK_END)).thenReturn(List.of(2L));
        when(incomeRepository.sumAmountByUserAndPeriod(2L, WEEK_START, WEEK_END)).thenReturn(BigDecimal.ZERO);
        when(expenseRepository.sumAmountByUserAndPeriod(2L, WEEK_START, WEEK_END)).thenReturn(BigDecimal.valueOf(100));
        when(expenseRepository.findTopCategoriesByUserAndPeriod(2L, WEEK_START, WEEK_END)).thenReturn(List.of());

        weeklySummaryJob.sendWeeklySummaries();

        verify(notificationDispatcher, org.mockito.Mockito.times(1)).dispatch(
                eq(2L), eq(NotificationType.WEEKLY_SUMMARY), anyString(), anyString(), anyString()
        );
    }

    @Test
    void includesTopCategoryNameInMessageWhenPresent() {
        weeklySummaryJob = new WeeklySummaryJob(expenseRepository, incomeRepository, notificationDispatcher, FIXED_CLOCK);
        CategoryTotalProjection topCategory = mockProjection("Comida");
        when(incomeRepository.findDistinctUserIdsByDateBetween(WEEK_START, WEEK_END)).thenReturn(List.of(3L));
        when(expenseRepository.findDistinctUserIdsByDateBetween(WEEK_START, WEEK_END)).thenReturn(List.of());
        when(incomeRepository.sumAmountByUserAndPeriod(3L, WEEK_START, WEEK_END)).thenReturn(BigDecimal.valueOf(400));
        when(expenseRepository.sumAmountByUserAndPeriod(3L, WEEK_START, WEEK_END)).thenReturn(BigDecimal.valueOf(200));
        when(expenseRepository.findTopCategoriesByUserAndPeriod(3L, WEEK_START, WEEK_END)).thenReturn(List.of(topCategory));

        weeklySummaryJob.sendWeeklySummaries();

        org.mockito.ArgumentCaptor<String> messageCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(notificationDispatcher).dispatch(
                eq(3L), eq(NotificationType.WEEKLY_SUMMARY), anyString(), messageCaptor.capture(), anyString()
        );
        org.junit.jupiter.api.Assertions.assertTrue(messageCaptor.getValue().contains("Comida"));
    }

    @Test
    void doesNotDispatchWhenNoUserHasActivityInTheWindow() {
        weeklySummaryJob = new WeeklySummaryJob(expenseRepository, incomeRepository, notificationDispatcher, FIXED_CLOCK);
        when(incomeRepository.findDistinctUserIdsByDateBetween(WEEK_START, WEEK_END)).thenReturn(List.of());
        when(expenseRepository.findDistinctUserIdsByDateBetween(WEEK_START, WEEK_END)).thenReturn(List.of());

        weeklySummaryJob.sendWeeklySummaries();

        verify(notificationDispatcher, never()).dispatch(
                org.mockito.ArgumentMatchers.anyLong(), eq(NotificationType.WEEKLY_SUMMARY),
                anyString(), anyString(), anyString()
        );
    }

    private CategoryTotalProjection mockProjection(String categoryName) {
        CategoryTotalProjection projection = org.mockito.Mockito.mock(CategoryTotalProjection.class);
        when(projection.getCategoryName()).thenReturn(categoryName);
        return projection;
    }
}
