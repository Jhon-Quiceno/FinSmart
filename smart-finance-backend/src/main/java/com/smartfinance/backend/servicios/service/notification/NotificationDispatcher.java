package com.smartfinance.backend.servicios.service.notification;

import com.smartfinance.backend.servicios.model.entity.NotificationPreference;
import com.smartfinance.backend.servicios.model.entity.NotificationType;
import com.smartfinance.backend.servicios.repository.NotificationPreferenceRepository;
import com.smartfinance.backend.usuario.repository.UserRepository;
import com.smartfinance.backend.servicios.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates delivery of a notification across every configured channel for its
 * {@link NotificationType}: always in-app (via {@link NotificationService}) when the type is
 * enabled for the user, plus email (via {@link NotificationSender}) when the user has enabled
 * email delivery.
 *
 * <p>This is the entry point Batch 3's scheduled jobs and event listeners are meant to call —
 * they should never call {@link NotificationService#createNotification} directly, since doing
 * so would skip the preference check and email fan-out implemented here.
 *
 * <p>A user with no {@link NotificationPreference} row yet (has never visited the preferences
 * screen) is treated as if every type were enabled and email were disabled, mirroring the
 * column defaults in {@code V7__create_notifications_and_preferences.sql} — this avoids forcing
 * a preference row to exist before any notification can be delivered.
 */
@Service
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final NotificationSender notificationSender;

    public NotificationDispatcher(
            NotificationPreferenceRepository notificationPreferenceRepository,
            NotificationService notificationService,
            UserRepository userRepository,
            NotificationSender notificationSender
    ) {
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.notificationSender = notificationSender;
    }

    /**
     * Delivers a notification of {@code type} to {@code userId} through every channel enabled
     * for that type, or skips entirely when the user has disabled that type.
     *
     * @param userId    owner of the notification
     * @param type      category of the notification
     * @param title     short, user-facing title (Spanish)
     * @param message   full, user-facing message body (Spanish)
     * @param dedupeKey key used to prevent duplicate in-app notifications; forwarded as-is to
     *                  {@link NotificationService#createNotification}
     */
    @Transactional
    public void dispatch(Long userId, NotificationType type, String title, String message, String dedupeKey) {
        NotificationPreference preference = notificationPreferenceRepository.findByUser_Id(userId).orElse(null);

        if (!isEnabledFor(preference, type)) {
            log.debug("notification_skipped_by_preference userId={} type={}", userId, type);
            return;
        }

        notificationService.createNotification(userId, type, title, message, dedupeKey);

        boolean emailEnabled = preference != null && preference.isEmailEnabled();
        if (emailEnabled) {
            // A real fetch (not getReferenceById) is required here: the recipient's fields are
            // read into a plain EmailRecipient before being handed to the @Async sender, and a
            // lazy proxy would throw LazyInitializationException once resolved on that thread.
            userRepository.findById(userId).ifPresent(user -> {
                EmailRecipient recipient = new EmailRecipient(user.getId(), user.getEmail(), user.getName());
                notificationSender.send(recipient, title, message);
            });
        }
    }

    private boolean isEnabledFor(NotificationPreference preference, NotificationType type) {
        if (preference == null) {
            return true;
        }

        return switch (type) {
            case PAYMENT_REMINDER -> preference.isPaymentReminders();
            case OVERSPEND_ALERT -> preference.isOverspendAlerts();
            case WEEKLY_SUMMARY -> preference.isWeeklySummary();
            case INACTIVITY_REMINDER -> preference.isInactivityReminders();
            case CARD_CYCLE_CLOSE -> preference.isCardCycleClose();
            case MONTH_END_PREDICTION, SYSTEM -> true;
        };
    }
}
