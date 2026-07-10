package com.smartfinance.backend.servicios.service.job;

import com.smartfinance.backend.deudas.model.entity.Debt;
import com.smartfinance.backend.servicios.model.entity.NotificationType;
import com.smartfinance.backend.servicios.model.entity.RecurringPayment;
import com.smartfinance.backend.usuario.model.entity.User;
import com.smartfinance.backend.deudas.repository.DebtRepository;
import com.smartfinance.backend.servicios.repository.RecurringPaymentRepository;
import com.smartfinance.backend.servicios.service.notification.NotificationDispatcher;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentReminderJobTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-07-10T08:00:00Z"), ZoneOffset.UTC
    );

    @Mock
    private RecurringPaymentRepository recurringPaymentRepository;

    @Mock
    private DebtRepository debtRepository;

    @Mock
    private NotificationDispatcher notificationDispatcher;

    private PaymentReminderJob paymentReminderJob;

    @Test
    void remindsForRecurringPaymentDueWithinFiveDaysWithDedupeKeyByDate() {
        paymentReminderJob = new PaymentReminderJob(
                recurringPaymentRepository, debtRepository, notificationDispatcher, FIXED_CLOCK
        );
        RecurringPayment payment = buildRecurringPayment(5L, 10L, "Netflix", BigDecimal.valueOf(45000), LocalDate.of(2026, 7, 14));
        when(recurringPaymentRepository.findActiveByNextPaymentDateBetween(
                LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 15)
        )).thenReturn(List.of(payment));
        when(debtRepository.findWithBalanceByDueDateBetween(
                LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 15)
        )).thenReturn(List.of());

        paymentReminderJob.remindUpcomingPayments();

        verify(notificationDispatcher).dispatch(
                eq(10L), eq(NotificationType.PAYMENT_REMINDER), anyString(), anyString(),
                eq("payment-reminder:recurring:5:2026-07-14")
        );
    }

    @Test
    void remindsForDebtDueWithinFiveDaysWithDedupeKeyByDate() {
        paymentReminderJob = new PaymentReminderJob(
                recurringPaymentRepository, debtRepository, notificationDispatcher, FIXED_CLOCK
        );
        Debt debt = buildDebt(7L, 20L, "Tarjeta de crédito", BigDecimal.valueOf(300000), LocalDate.of(2026, 7, 11));
        when(recurringPaymentRepository.findActiveByNextPaymentDateBetween(
                LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 15)
        )).thenReturn(List.of());
        when(debtRepository.findWithBalanceByDueDateBetween(
                LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 15)
        )).thenReturn(List.of(debt));

        paymentReminderJob.remindUpcomingPayments();

        verify(notificationDispatcher).dispatch(
                eq(20L), eq(NotificationType.PAYMENT_REMINDER), anyString(), anyString(),
                eq("payment-reminder:debt:7:2026-07-11")
        );
    }

    @Test
    void doesNotDispatchWhenNoPaymentsOrDebtsAreDueSoon() {
        paymentReminderJob = new PaymentReminderJob(
                recurringPaymentRepository, debtRepository, notificationDispatcher, FIXED_CLOCK
        );
        when(recurringPaymentRepository.findActiveByNextPaymentDateBetween(
                LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 15)
        )).thenReturn(List.of());
        when(debtRepository.findWithBalanceByDueDateBetween(
                LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 15)
        )).thenReturn(List.of());

        paymentReminderJob.remindUpcomingPayments();

        verify(notificationDispatcher, never()).dispatch(
                anyLong(), eq(NotificationType.PAYMENT_REMINDER), anyString(), anyString(), anyString()
        );
    }

    @Test
    void dispatchesOnceForEachOfMultipleDuePayments() {
        paymentReminderJob = new PaymentReminderJob(
                recurringPaymentRepository, debtRepository, notificationDispatcher, FIXED_CLOCK
        );
        RecurringPayment first = buildRecurringPayment(1L, 100L, "Spotify", BigDecimal.valueOf(20000), LocalDate.of(2026, 7, 12));
        RecurringPayment second = buildRecurringPayment(2L, 101L, "Internet", BigDecimal.valueOf(90000), LocalDate.of(2026, 7, 13));
        when(recurringPaymentRepository.findActiveByNextPaymentDateBetween(
                LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 15)
        )).thenReturn(List.of(first, second));
        when(debtRepository.findWithBalanceByDueDateBetween(
                LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 15)
        )).thenReturn(List.of());

        paymentReminderJob.remindUpcomingPayments();

        verify(notificationDispatcher, times(2)).dispatch(
                anyLong(), eq(NotificationType.PAYMENT_REMINDER), anyString(), anyString(), anyString()
        );
    }

    private RecurringPayment buildRecurringPayment(Long id, Long userId, String name, BigDecimal amount, LocalDate nextPaymentDate) {
        RecurringPayment payment = new RecurringPayment();
        payment.setId(id);
        payment.setUser(buildUser(userId));
        payment.setName(name);
        payment.setAmount(amount);
        payment.setNextPaymentDate(nextPaymentDate);
        payment.setActive(true);
        return payment;
    }

    private Debt buildDebt(Long id, Long userId, String name, BigDecimal remainingAmount, LocalDate dueDate) {
        Debt debt = new Debt();
        debt.setId(id);
        debt.setUser(buildUser(userId));
        debt.setName(name);
        debt.setTotalAmount(remainingAmount);
        debt.setRemainingAmount(remainingAmount);
        debt.setDueDate(dueDate);
        return debt;
    }

    private User buildUser(Long userId) {
        User user = new User();
        user.setId(userId);
        return user;
    }
}
