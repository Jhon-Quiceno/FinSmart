package com.smartfinance.backend.service.jobs;

import com.smartfinance.backend.model.Debt;
import com.smartfinance.backend.model.NotificationType;
import com.smartfinance.backend.model.RecurringPayment;
import com.smartfinance.backend.repository.DebtRepository;
import com.smartfinance.backend.repository.RecurringPaymentRepository;
import com.smartfinance.backend.service.notification.NotificationDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Daily job that reminds users of upcoming payments — active recurring payments and debts whose
 * due date falls in the next {@value #REMINDER_WINDOW_DAYS} days — replacing the equivalent n8n
 * workflow (see {@code docs/sprints/sprint5.md}, item 11).
 *
 * <p>Both scans ({@link RecurringPaymentRepository#findActiveByNextPaymentDateBetween} and
 * {@link DebtRepository#findWithBalanceByDueDateBetween}) run once across every user rather than
 * once per user, so this job's cost does not grow with the number of registered users, only with
 * the number of payments/debts actually due soon.
 *
 * <p>The dedupe key includes the due date itself (not just the target id), so a reminder fires
 * again if the date is later rescheduled — see {@link NotificationDispatcher#dispatch}.
 *
 * <p>Not itself {@code @Transactional}: each repository scan already runs in its own read-only
 * transaction (Spring Data JPA's default for query methods), and every write happens inside
 * {@link NotificationDispatcher#dispatch}'s own transaction boundary. Wrapping this method in
 * {@code @Transactional(readOnly = true)} was misleading — it dispatches writes — and wrapping it
 * in a writable one would serve no purpose since nothing here needs the scans and the dispatches
 * to share a single transaction.
 */
@Component
@ConditionalOnProperty(prefix = "app.jobs", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PaymentReminderJob {

    private static final Logger log = LoggerFactory.getLogger(PaymentReminderJob.class);
    private static final int REMINDER_WINDOW_DAYS = 5;

    private final RecurringPaymentRepository recurringPaymentRepository;
    private final DebtRepository debtRepository;
    private final NotificationDispatcher notificationDispatcher;
    private final Clock clock;

    public PaymentReminderJob(
            RecurringPaymentRepository recurringPaymentRepository,
            DebtRepository debtRepository,
            NotificationDispatcher notificationDispatcher,
            Clock clock
    ) {
        this.recurringPaymentRepository = recurringPaymentRepository;
        this.debtRepository = debtRepository;
        this.notificationDispatcher = notificationDispatcher;
        this.clock = clock;
    }

    @Scheduled(cron = "${app.jobs.payment-reminder.cron:0 0 8 * * *}")
    public void remindUpcomingPayments() {
        LocalDate today = LocalDate.now(clock);
        LocalDate windowEnd = today.plusDays(REMINDER_WINDOW_DAYS);

        var duePayments = recurringPaymentRepository.findActiveByNextPaymentDateBetween(today, windowEnd);
        log.debug("payment_reminder_job_scan recurringPayments={} window={}..{}", duePayments.size(), today, windowEnd);
        duePayments.forEach(this::remindForRecurringPayment);

        var dueDebts = debtRepository.findWithBalanceByDueDateBetween(today, windowEnd);
        log.debug("payment_reminder_job_scan debts={} window={}..{}", dueDebts.size(), today, windowEnd);
        dueDebts.forEach(this::remindForDebt);
    }

    private void remindForRecurringPayment(RecurringPayment payment) {
        String title = "Recordatorio de pago: " + payment.getName();
        String message = "Tu servicio '" + payment.getName() + "' por $"
                + NotificationMessageFormatter.formatAmount(payment.getAmount())
                + " vence el " + NotificationMessageFormatter.formatDate(payment.getNextPaymentDate()) + ".";
        String dedupeKey = "payment-reminder:recurring:" + payment.getId() + ":" + payment.getNextPaymentDate();

        notificationDispatcher.dispatch(
                payment.getUser().getId(), NotificationType.PAYMENT_REMINDER, title, message, dedupeKey
        );
    }

    private void remindForDebt(Debt debt) {
        String title = "Recordatorio de pago: " + debt.getName();
        String message = "Tu deuda '" + debt.getName() + "' por $"
                + NotificationMessageFormatter.formatAmount(debt.getRemainingAmount())
                + " vence el " + NotificationMessageFormatter.formatDate(debt.getDueDate()) + ".";
        String dedupeKey = "payment-reminder:debt:" + debt.getId() + ":" + debt.getDueDate();

        notificationDispatcher.dispatch(
                debt.getUser().getId(), NotificationType.PAYMENT_REMINDER, title, message, dedupeKey
        );
    }
}
