package com.smartfinance.backend.service.jobs;

import com.smartfinance.backend.dto.analysis.PredictionResponse;
import com.smartfinance.backend.model.NotificationType;
import com.smartfinance.backend.repository.ExpenseRepository;
import com.smartfinance.backend.service.MonthEndPredictionService;
import com.smartfinance.backend.service.notification.NotificationDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;

/**
 * Daily job that checks every active user's month-end spending projection
 * ({@link MonthEndPredictionService#computePrediction}) and alerts when it is negative,
 * replacing the equivalent n8n workflow (see {@code docs/sprints/sprint5.md}, item 14).
 *
 * <p>Kept as its own job (single responsibility) rather than folded into
 * {@link PaymentReminderJob}: the two run on independent schedules and reason about unrelated
 * domain concepts (upcoming due dates vs. a running spend projection).
 *
 * <p>The dedupe key is per (user, month), not per day, so a user with a persistently negative
 * projection is alerted once for the whole month rather than every day the job runs.
 *
 * <p>Only scans users with at least one expense recorded so far this month (via
 * {@link ExpenseRepository#findDistinctUserIdsByDateBetween}, the same cheap distinct-id
 * approach {@code WeeklySummaryJob} uses), instead of every active user: with zero expenses
 * this month, {@code avgDailySpend} and {@code projectedExpense} are both zero, so
 * {@code projectedBalance} can never be negative and {@code alert} can never be {@code true} —
 * scanning those users would always be wasted work.
 *
 * <p>Not itself {@code @Transactional}: the candidate scan and
 * {@link MonthEndPredictionService#computePrediction} already run in their own read-only
 * transactions, and every write happens inside {@link NotificationDispatcher#dispatch}'s own
 * transaction boundary. Wrapping this method in {@code @Transactional(readOnly = true)} was
 * misleading — it dispatches writes.
 */
@Component
@ConditionalOnProperty(prefix = "app.jobs", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MonthEndPredictionJob {

    private static final Logger log = LoggerFactory.getLogger(MonthEndPredictionJob.class);

    private final ExpenseRepository expenseRepository;
    private final MonthEndPredictionService monthEndPredictionService;
    private final NotificationDispatcher notificationDispatcher;
    private final Clock clock;

    public MonthEndPredictionJob(
            ExpenseRepository expenseRepository,
            MonthEndPredictionService monthEndPredictionService,
            NotificationDispatcher notificationDispatcher,
            Clock clock
    ) {
        this.expenseRepository = expenseRepository;
        this.monthEndPredictionService = monthEndPredictionService;
        this.notificationDispatcher = notificationDispatcher;
        this.clock = clock;
    }

    @Scheduled(cron = "${app.jobs.month-end-prediction.cron:0 30 8 * * *}")
    public void alertNegativeProjections() {
        LocalDate today = LocalDate.now(clock);
        LocalDate periodStart = YearMonth.from(today).atDay(1);
        List<Long> candidateUserIds = expenseRepository.findDistinctUserIdsByDateBetween(periodStart, today);

        log.debug("month_end_prediction_job_scan candidates={} today={}", candidateUserIds.size(), today);
        for (Long userId : candidateUserIds) {
            PredictionResponse prediction = monthEndPredictionService.computePrediction(userId, today);
            if (prediction.alert()) {
                alertUser(userId, prediction, YearMonth.from(today));
            }
        }
    }

    private void alertUser(Long userId, PredictionResponse prediction, YearMonth period) {
        String title = "Proyección de fin de mes en rojo";
        String message = String.format(
                Locale.ROOT,
                "Con tu ritmo de gasto actual, terminarías el mes con un saldo proyectado de $%s. "
                        + "Ajusta tu presupuesto para no quedar en negativo.",
                NotificationMessageFormatter.formatAmount(prediction.projectedBalance())
        );
        String dedupeKey = String.format(Locale.ROOT, "month-end:%d:%d-%02d", userId, period.getYear(), period.getMonthValue());

        notificationDispatcher.dispatch(userId, NotificationType.MONTH_END_PREDICTION, title, message, dedupeKey);
    }
}
