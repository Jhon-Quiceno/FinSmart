package com.smartfinance.backend.servicios.service.job;

import com.smartfinance.backend.gastos.event.ExpenseCreatedEvent;
import com.smartfinance.backend.servicios.model.entity.NotificationType;
import com.smartfinance.backend.gastos.repository.ExpenseRepository;
import com.smartfinance.backend.ingresos.repository.IncomeRepository;
import com.smartfinance.backend.servicios.service.notification.NotificationDispatcher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Locale;

/**
 * Reacts to {@link ExpenseCreatedEvent} by recomputing the current month's expense-to-income
 * ratio for the expense's owner and dispatching an overspend alert once it crosses 80%,
 * replacing the equivalent n8n workflow (see {@code docs/sprints/sprint5.md}, item 12).
 *
 * <p>Runs only {@link TransactionPhase#AFTER_COMMIT}: the expense that triggers this event is
 * created inside {@code ExpenseService.createExpense}'s own transaction, and running the
 * overspend check (and its notification write) inside that same transaction let a dedupe-race
 * {@code DataIntegrityViolationException} from {@code NotificationService.createNotification}
 * poison and silently roll back the caller's transaction. Deferring to after-commit guarantees
 * the expense is durably persisted before this listener ever runs, so it can no longer affect
 * the expense's outcome. It opens its own transaction with
 * {@link Propagation#REQUIRES_NEW} because an {@code AFTER_COMMIT} listener otherwise executes
 * outside of any live transactional context, and a plain {@code @Transactional} would not start
 * a new one there.
 *
 * <p>Never depends on {@link com.smartfinance.backend.common.security.SecurityUtils}: the owner is
 * read from the event itself, so this listener works whether triggered from a request thread or
 * (in the future) any non-request context.
 */
@Component
public class OverspendAlertListener {

    private static final BigDecimal OVERSPEND_THRESHOLD = new BigDecimal("0.80");
    private static final int RATIO_SCALE = 4;

    private final IncomeRepository incomeRepository;
    private final ExpenseRepository expenseRepository;
    private final NotificationDispatcher notificationDispatcher;
    private final Clock clock;

    public OverspendAlertListener(
            IncomeRepository incomeRepository,
            ExpenseRepository expenseRepository,
            NotificationDispatcher notificationDispatcher,
            Clock clock
    ) {
        this.incomeRepository = incomeRepository;
        this.expenseRepository = expenseRepository;
        this.notificationDispatcher = notificationDispatcher;
        this.clock = clock;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onExpenseCreated(ExpenseCreatedEvent event) {
        YearMonth period = YearMonth.from(LocalDate.now(clock));
        LocalDate periodStart = period.atDay(1);
        LocalDate periodEnd = period.atEndOfMonth();
        Long userId = event.userId();

        BigDecimal totalIncome = nullSafe(incomeRepository.sumAmountByUserAndPeriod(userId, periodStart, periodEnd));
        if (totalIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal totalExpense = nullSafe(expenseRepository.sumAmountByUserAndPeriod(userId, periodStart, periodEnd));
        BigDecimal ratio = totalExpense.divide(totalIncome, RATIO_SCALE, RoundingMode.HALF_UP);
        if (ratio.compareTo(OVERSPEND_THRESHOLD) <= 0) {
            return;
        }

        String title = "Alerta de sobregasto";
        String message = String.format(
                Locale.ROOT, "Ya gastaste el %.0f%% de tus ingresos este mes. Revisa tu presupuesto.",
                ratio.multiply(BigDecimal.valueOf(100))
        );
        String dedupeKey = String.format(Locale.ROOT, "overspend:%d:%d-%02d", userId, period.getYear(), period.getMonthValue());

        notificationDispatcher.dispatch(userId, NotificationType.OVERSPEND_ALERT, title, message, dedupeKey);
    }

    private static BigDecimal nullSafe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
