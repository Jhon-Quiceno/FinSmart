package com.smartfinance.backend.servicios.service.job;

import com.smartfinance.backend.servicios.model.entity.NotificationType;
import com.smartfinance.backend.gastos.repository.ExpenseRepository;
import com.smartfinance.backend.ingresos.repository.IncomeRepository;
import com.smartfinance.backend.common.repository.UserLastActivityProjection;
import com.smartfinance.backend.usuario.repository.UserRepository;
import com.smartfinance.backend.servicios.service.notification.NotificationDispatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class InactivityReminderJobTest {

    // Today = 2026-07-10; threshold = 2026-07-07 (3 days back).
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-07-10T09:00:00Z"), ZoneOffset.UTC
    );

    @Mock
    private UserRepository userRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private IncomeRepository incomeRepository;

    @Mock
    private NotificationDispatcher notificationDispatcher;

    private InactivityReminderJob inactivityReminderJob;

    @Test
    void remindsUserWhoseLastActivityIsOlderThanThreeDays() {
        inactivityReminderJob = new InactivityReminderJob(
                userRepository, expenseRepository, incomeRepository, notificationDispatcher, FIXED_CLOCK
        );
        UserLastActivityProjection lastExpense = projection(1L, LocalDate.of(2026, 7, 1));
        when(userRepository.findAllIdsWithLastLoginNotNull()).thenReturn(List.of(1L));
        when(expenseRepository.findLatestExpenseDatePerUser()).thenReturn(List.of(lastExpense));
        when(incomeRepository.findLatestIncomeDatePerUser()).thenReturn(List.of());

        inactivityReminderJob.remindInactiveUsers();

        verify(notificationDispatcher).dispatch(
                eq(1L), eq(NotificationType.INACTIVITY_REMINDER), anyString(), anyString(),
                eq("inactivity:1:2026-07-01")
        );
    }

    @Test
    void doesNotRemindUserWithRecentExpenseEvenIfIncomeIsOld() {
        inactivityReminderJob = new InactivityReminderJob(
                userRepository, expenseRepository, incomeRepository, notificationDispatcher, FIXED_CLOCK
        );
        UserLastActivityProjection recentExpense = projection(2L, LocalDate.of(2026, 7, 9));
        UserLastActivityProjection oldIncome = projection(2L, LocalDate.of(2026, 1, 1));
        when(userRepository.findAllIdsWithLastLoginNotNull()).thenReturn(List.of(2L));
        when(expenseRepository.findLatestExpenseDatePerUser()).thenReturn(List.of(recentExpense));
        when(incomeRepository.findLatestIncomeDatePerUser()).thenReturn(List.of(oldIncome));

        inactivityReminderJob.remindInactiveUsers();

        verify(notificationDispatcher, never()).dispatch(
                eq(2L), eq(NotificationType.INACTIVITY_REMINDER), anyString(), anyString(), anyString()
        );
    }

    @Test
    void remindsUserWithNoActivityAtAllUsingNeverAsDedupeSuffix() {
        inactivityReminderJob = new InactivityReminderJob(
                userRepository, expenseRepository, incomeRepository, notificationDispatcher, FIXED_CLOCK
        );
        when(userRepository.findAllIdsWithLastLoginNotNull()).thenReturn(List.of(3L));
        when(expenseRepository.findLatestExpenseDatePerUser()).thenReturn(List.of());
        when(incomeRepository.findLatestIncomeDatePerUser()).thenReturn(List.of());

        inactivityReminderJob.remindInactiveUsers();

        verify(notificationDispatcher).dispatch(
                eq(3L), eq(NotificationType.INACTIVITY_REMINDER), anyString(), anyString(),
                eq("inactivity:3:never")
        );
    }

    @Test
    void doesNotRemindUserWhoNeverLoggedIn() {
        inactivityReminderJob = new InactivityReminderJob(
                userRepository, expenseRepository, incomeRepository, notificationDispatcher, FIXED_CLOCK
        );
        when(userRepository.findAllIdsWithLastLoginNotNull()).thenReturn(List.of());

        inactivityReminderJob.remindInactiveUsers();

        verify(notificationDispatcher, never()).dispatch(
                org.mockito.ArgumentMatchers.anyLong(), eq(NotificationType.INACTIVITY_REMINDER),
                anyString(), anyString(), anyString()
        );
    }

    private UserLastActivityProjection projection(Long userId, LocalDate lastDate) {
        UserLastActivityProjection projection = org.mockito.Mockito.mock(UserLastActivityProjection.class);
        when(projection.getUserId()).thenReturn(userId);
        when(projection.getLastDate()).thenReturn(lastDate);
        return projection;
    }
}
