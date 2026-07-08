package com.smartfinance.backend.service.jobs;

import com.smartfinance.backend.model.NotificationType;
import com.smartfinance.backend.repository.CategoryTotalProjection;
import com.smartfinance.backend.repository.ExpenseRepository;
import com.smartfinance.backend.repository.IncomeRepository;
import com.smartfinance.backend.service.notification.NotificationDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Weekly job (defaults to Monday 07:00) that sends every user with any activity in the prior
 * calendar week a balance summary — income vs. expense, top category and net savings —
 * replacing the equivalent n8n workflow (see {@code docs/sprints/sprint5.md}, item 13).
 *
 * <p>The window is {@code [today - 7, today - 1]} (the 7 days before the run date), so a Monday
 * run summarizes the just-completed Monday-through-Sunday week. The set of users to summarize is
 * built from two cheap distinct-id scans ({@link IncomeRepository#findDistinctUserIdsByDateBetween}
 * and {@link ExpenseRepository#findDistinctUserIdsByDateBetween}) rather than iterating over
 * every registered user, so an inactive user costs nothing here.
 *
 * <p>Not itself {@code @Transactional}: each repository scan already runs in its own read-only
 * transaction (Spring Data JPA's default for query methods), and every write happens inside
 * {@link NotificationDispatcher#dispatch}'s own transaction boundary. Wrapping this method in
 * {@code @Transactional(readOnly = true)} was misleading — it dispatches writes.
 */
@Component
@ConditionalOnProperty(prefix = "app.jobs", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WeeklySummaryJob {

    private static final Logger log = LoggerFactory.getLogger(WeeklySummaryJob.class);
    private static final int WINDOW_DAYS = 7;

    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final NotificationDispatcher notificationDispatcher;
    private final Clock clock;

    public WeeklySummaryJob(
            ExpenseRepository expenseRepository,
            IncomeRepository incomeRepository,
            NotificationDispatcher notificationDispatcher,
            Clock clock
    ) {
        this.expenseRepository = expenseRepository;
        this.incomeRepository = incomeRepository;
        this.notificationDispatcher = notificationDispatcher;
        this.clock = clock;
    }

    @Scheduled(cron = "${app.jobs.weekly-summary.cron:0 0 7 * * MON}")
    public void sendWeeklySummaries() {
        LocalDate today = LocalDate.now(clock);
        LocalDate windowStart = today.minusDays(WINDOW_DAYS);
        LocalDate windowEnd = today.minusDays(1);

        Set<Long> activeUserIds = new LinkedHashSet<>();
        activeUserIds.addAll(incomeRepository.findDistinctUserIdsByDateBetween(windowStart, windowEnd));
        activeUserIds.addAll(expenseRepository.findDistinctUserIdsByDateBetween(windowStart, windowEnd));

        log.debug("weekly_summary_job_scan users={} window={}..{}", activeUserIds.size(), windowStart, windowEnd);
        activeUserIds.forEach(userId -> sendSummaryForUser(userId, windowStart, windowEnd));
    }

    private void sendSummaryForUser(Long userId, LocalDate windowStart, LocalDate windowEnd) {
        BigDecimal income = nullSafe(incomeRepository.sumAmountByUserAndPeriod(userId, windowStart, windowEnd));
        BigDecimal expense = nullSafe(expenseRepository.sumAmountByUserAndPeriod(userId, windowStart, windowEnd));
        BigDecimal savings = income.subtract(expense);

        List<CategoryTotalProjection> topCategories = expenseRepository.findTopCategoriesByUserAndPeriod(userId, windowStart, windowEnd);
        String topCategoryName = topCategories.isEmpty() ? null : topCategories.get(0).getCategoryName();

        String title = "Resumen semanal";
        String message = buildMessage(income, expense, savings, topCategoryName);
        String dedupeKey = buildDedupeKey(userId, windowEnd);

        notificationDispatcher.dispatch(userId, NotificationType.WEEKLY_SUMMARY, title, message, dedupeKey);
    }

    private String buildMessage(BigDecimal income, BigDecimal expense, BigDecimal savings, String topCategoryName) {
        StringBuilder message = new StringBuilder("Esta semana: ingresos $")
                .append(NotificationMessageFormatter.formatAmount(income))
                .append(", gastos $")
                .append(NotificationMessageFormatter.formatAmount(expense));

        if (topCategoryName != null) {
            message.append(" (mayor gasto en '").append(topCategoryName).append("')");
        }

        if (savings.compareTo(BigDecimal.ZERO) < 0) {
            message.append(". Gastaste $").append(NotificationMessageFormatter.formatAmount(savings.abs()))
                    .append(" más de lo que ingresaste.");
        } else {
            message.append(". Ahorraste $").append(NotificationMessageFormatter.formatAmount(savings)).append('.');
        }

        return message.toString();
    }

    /** {@code weekly-summary:{userId}:{iso-year}-W{iso-week}}, one alert per user per ISO week. */
    private String buildDedupeKey(Long userId, LocalDate windowEnd) {
        WeekFields weekFields = WeekFields.ISO;
        int isoYear = windowEnd.get(weekFields.weekBasedYear());
        int isoWeek = windowEnd.get(weekFields.weekOfWeekBasedYear());
        return String.format(Locale.ROOT, "weekly-summary:%d:%d-W%02d", userId, isoYear, isoWeek);
    }

    private static BigDecimal nullSafe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
