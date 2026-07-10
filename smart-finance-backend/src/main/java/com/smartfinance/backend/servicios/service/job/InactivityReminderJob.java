package com.smartfinance.backend.servicios.service.job;

import com.smartfinance.backend.servicios.model.entity.NotificationType;
import com.smartfinance.backend.gastos.repository.ExpenseRepository;
import com.smartfinance.backend.ingresos.repository.IncomeRepository;
import com.smartfinance.backend.common.repository.UserLastActivityProjection;
import com.smartfinance.backend.usuario.repository.UserRepository;
import com.smartfinance.backend.servicios.service.notification.NotificationDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Daily job that reminds users who have logged in at least once but recorded no income or
 * expense in the last {@value #INACTIVITY_THRESHOLD_DAYS} days, replacing the equivalent n8n
 * workflow (see {@code docs/sprints/sprint5.md}, item 13).
 *
 * <p>Users who never logged in ({@code last_login_at IS NULL}) are excluded — they have nothing
 * to be "inactive" from. "Latest activity" is the later of the user's most recent expense and
 * most recent income date, both fetched as a single grouped query per repository
 * ({@link ExpenseRepository#findLatestExpenseDatePerUser()},
 * {@link IncomeRepository#findLatestIncomeDatePerUser()}) rather than one query per user, so this
 * job's cost does not grow linearly with the number of eligible users.
 *
 * <p>The dedupe key embeds the last-activity date (or {@code "never"}), so a user who logs a new
 * transaction and then goes inactive again gets a fresh reminder for the new streak, rather than
 * being silenced forever by the first one.
 *
 * <p>Not itself {@code @Transactional}: each repository scan already runs in its own read-only
 * transaction (Spring Data JPA's default for query methods), and every write happens inside
 * {@link NotificationDispatcher#dispatch}'s own transaction boundary. Wrapping this method in
 * {@code @Transactional(readOnly = true)} was misleading — it dispatches writes.
 */
@Component
@ConditionalOnProperty(prefix = "app.jobs", name = "enabled", havingValue = "true", matchIfMissing = true)
public class InactivityReminderJob {

    private static final Logger log = LoggerFactory.getLogger(InactivityReminderJob.class);
    private static final int INACTIVITY_THRESHOLD_DAYS = 3;
    private static final String NEVER_ACTIVE_LABEL = "never";

    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final NotificationDispatcher notificationDispatcher;
    private final Clock clock;

    public InactivityReminderJob(
            UserRepository userRepository,
            ExpenseRepository expenseRepository,
            IncomeRepository incomeRepository,
            NotificationDispatcher notificationDispatcher,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.expenseRepository = expenseRepository;
        this.incomeRepository = incomeRepository;
        this.notificationDispatcher = notificationDispatcher;
        this.clock = clock;
    }

    @Scheduled(cron = "${app.jobs.inactivity-reminder.cron:0 0 9 * * *}")
    public void remindInactiveUsers() {
        List<Long> loggedInUserIds = userRepository.findAllIdsWithLastLoginNotNull();
        if (loggedInUserIds.isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now(clock);
        LocalDate threshold = today.minusDays(INACTIVITY_THRESHOLD_DAYS);

        Map<Long, LocalDate> lastExpenseByUser = toMap(expenseRepository.findLatestExpenseDatePerUser());
        Map<Long, LocalDate> lastIncomeByUser = toMap(incomeRepository.findLatestIncomeDatePerUser());

        log.debug("inactivity_reminder_job_scan candidates={} threshold={}", loggedInUserIds.size(), threshold);
        for (Long userId : loggedInUserIds) {
            LocalDate lastActivity = latest(lastExpenseByUser.get(userId), lastIncomeByUser.get(userId));
            if (lastActivity != null && !lastActivity.isBefore(threshold)) {
                continue;
            }

            remindUser(userId, lastActivity);
        }
    }

    private void remindUser(Long userId, LocalDate lastActivity) {
        String title = "Te extrañamos en FinSmart";
        String message = "No has registrado ingresos ni gastos en los últimos días. "
                + "Vuelve a FinSmart para mantener tus finanzas al día.";
        String activityLabel = lastActivity != null ? lastActivity.toString() : NEVER_ACTIVE_LABEL;
        String dedupeKey = "inactivity:" + userId + ":" + activityLabel;

        notificationDispatcher.dispatch(userId, NotificationType.INACTIVITY_REMINDER, title, message, dedupeKey);
    }

    private static LocalDate latest(LocalDate first, LocalDate second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.isAfter(second) ? first : second;
    }

    private static Map<Long, LocalDate> toMap(List<UserLastActivityProjection> projections) {
        Map<Long, LocalDate> map = new HashMap<>();
        projections.forEach(projection -> map.put(projection.getUserId(), projection.getLastDate()));
        return map;
    }
}
