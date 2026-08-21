package com.smartfinance.backend.servicios.service;

import com.smartfinance.backend.servicios.model.dto.NotificationPreferenceRequest;
import com.smartfinance.backend.servicios.model.dto.NotificationPreferenceResponse;
import com.smartfinance.backend.servicios.model.dto.NotificationResponse;
import com.smartfinance.backend.servicios.model.dto.PushTokenRequest;
import com.smartfinance.backend.common.exception.ResourceNotFoundException;
import com.smartfinance.backend.servicios.mapper.NotificationMapper;
import com.smartfinance.backend.servicios.mapper.NotificationPreferenceMapper;
import com.smartfinance.backend.servicios.model.entity.Notification;
import com.smartfinance.backend.servicios.model.entity.NotificationPreference;
import com.smartfinance.backend.servicios.model.entity.NotificationType;
import com.smartfinance.backend.servicios.model.entity.PushToken;
import com.smartfinance.backend.servicios.repository.NotificationPreferenceRepository;
import com.smartfinance.backend.servicios.repository.NotificationRepository;
import com.smartfinance.backend.servicios.repository.PushTokenRepository;
import com.smartfinance.backend.usuario.repository.UserRepository;
import com.smartfinance.backend.common.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Business logic for the current user's {@link Notification} records and
 * {@link NotificationPreference}.
 *
 * <p>Every read/write operation resolves the caller via {@link SecurityUtils#getCurrentUserId()}
 * and scopes strictly to that user, following the same ownership pattern as
 * {@code ExpenseService}. {@link #createNotification} is also called with an explicit
 * {@code userId} (not the current caller) because it is meant to be invoked from
 * {@code NotificationDispatcher}/scheduled jobs (Batch 3) acting on behalf of arbitrary users,
 * not from an authenticated request.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final UserRepository userRepository;
    private final NotificationMapper notificationMapper;
    private final NotificationPreferenceMapper notificationPreferenceMapper;
    private final PushTokenRepository pushTokenRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationPreferenceRepository notificationPreferenceRepository,
            UserRepository userRepository,
            NotificationMapper notificationMapper,
            NotificationPreferenceMapper notificationPreferenceMapper,
            PushTokenRepository pushTokenRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.userRepository = userRepository;
        this.notificationMapper = notificationMapper;
        this.notificationPreferenceMapper = notificationPreferenceMapper;
        this.pushTokenRepository = pushTokenRepository;
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUserId();
        return notificationRepository.findByUser_IdOrderByCreatedAtDesc(userId, pageable)
                .map(notificationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        return notificationRepository.countByUser_IdAndReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markAsRead(Long notificationId) {
        Long userId = SecurityUtils.getCurrentUserId();
        Notification notification = notificationRepository.findByIdAndUser_Id(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada"));

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
            notification = notificationRepository.save(notification);
        }

        return notificationMapper.toResponse(notification);
    }

    @Transactional
    public void markAllAsRead() {
        Long userId = SecurityUtils.getCurrentUserId();
        notificationRepository.markAllAsReadForUser(userId);
    }

    /**
     * Creates a notification for {@code userId}, skipping silently when {@code dedupeKey} is
     * non-null and a notification with the same {@code (userId, dedupeKey)} already exists.
     *
     * <p>The existence pre-check ({@link NotificationRepository#existsByUser_IdAndDedupeKey})
     * avoids an unnecessary insert attempt in the common case; the {@code saveAndFlush} +
     * {@link DataIntegrityViolationException} catch handles the race where two concurrent calls
     * (e.g. two job executions overlapping) both pass the pre-check before either commits — the
     * database's {@code uk_notifications_user_dedupe_key} partial unique index (see
     * {@code V7__create_notifications_and_preferences.sql}) rejects the second insert, and that
     * rejection is treated as an idempotent no-op rather than an error.
     *
     * <p>Runs in its own {@link Propagation#REQUIRES_NEW} transaction, as defense-in-depth: a
     * {@code DataIntegrityViolationException} from the dedupe race aborts only this method's
     * transaction, never a caller's, so callers invoking this from within a larger transaction
     * (e.g. {@code OverspendAlertListener}) can never have their own work rolled back by a
     * dedupe collision here. No current caller (see {@code NotificationDispatcher} and the
     * scheduled jobs) depends on this method joining an outer transaction.
     *
     * @param userId    owner of the notification
     * @param type      category of the notification
     * @param title     short, user-facing title (Spanish)
     * @param message   full, user-facing message body (Spanish)
     * @param dedupeKey key used to prevent duplicate notifications for the same
     *                  (user, target, period); {@code null} disables deduplication
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createNotification(Long userId, NotificationType type, String title, String message, String dedupeKey) {
        if (dedupeKey != null && notificationRepository.existsByUser_IdAndDedupeKey(userId, dedupeKey)) {
            log.debug("skip_duplicate_notification userId={} type={} dedupeKey={}", userId, type, dedupeKey);
            return;
        }

        Notification notification = new Notification();
        notification.setUser(userRepository.getReferenceById(userId));
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRead(false);
        notification.setDedupeKey(dedupeKey);

        try {
            notificationRepository.saveAndFlush(notification);
        } catch (DataIntegrityViolationException ex) {
            log.debug(
                    "skip_duplicate_notification_race userId={} type={} dedupeKey={}", userId, type, dedupeKey
            );
        }
    }

    @Transactional
    public NotificationPreferenceResponse getPreferences() {
        Long userId = SecurityUtils.getCurrentUserId();
        return notificationPreferenceMapper.toResponse(findOrCreatePreference(userId));
    }

    @Transactional
    public NotificationPreferenceResponse updatePreferences(NotificationPreferenceRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        NotificationPreference preference = findOrCreatePreference(userId);

        notificationPreferenceMapper.updateEntityFromRequest(request, preference);

        return notificationPreferenceMapper.toResponse(notificationPreferenceRepository.save(preference));
    }

    /**
     * Registers (or updates) an Expo push token for one of the current user's devices.
     *
     * <p>Upserts on {@code (user, deviceId)} rather than always inserting: the same device
     * reinstalling the app or having its token rotated by Expo must overwrite the existing row,
     * not accumulate duplicates (enforced at the database level by
     * {@code uk_push_tokens_user_device} in {@code V26__create_push_tokens.sql}).
     *
     * @param request the device's current token and a stable device identifier
     */
    @Transactional
    public void registerPushToken(PushTokenRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        PushToken pushToken = pushTokenRepository.findByUser_IdAndDeviceId(userId, request.deviceId())
                .orElseGet(() -> {
                    PushToken created = new PushToken();
                    created.setUser(userRepository.getReferenceById(userId));
                    created.setDeviceId(request.deviceId());
                    return created;
                });

        pushToken.setExpoPushToken(request.expoPushToken());
        pushTokenRepository.save(pushToken);
    }

    /**
     * Idempotent by design: deleting a deviceId that was never registered (permission denied,
     * running under Expo Go where remote push isn't available, etc.) still returns success — the
     * caller (logout flow) cannot know in advance whether a token was ever registered.
     *
     * @param deviceId the device identifier to remove
     */
    @Transactional
    public void unregisterPushToken(String deviceId) {
        Long userId = SecurityUtils.getCurrentUserId();
        pushTokenRepository.deleteByUser_IdAndDeviceId(userId, deviceId);
    }

    /**
     * Returns the user's persisted preferences, creating a row with the same defaults as the
     * database column defaults (see {@code V7__create_notifications_and_preferences.sql}) the
     * first time it is requested, so both {@link #getPreferences()} and
     * {@link #updatePreferences} always have a row to read/mutate.
     */
    private NotificationPreference findOrCreatePreference(Long userId) {
        return notificationPreferenceRepository.findByUser_Id(userId)
                .orElseGet(() -> {
                    NotificationPreference defaults = new NotificationPreference();
                    defaults.setUser(userRepository.getReferenceById(userId));
                    defaults.setPaymentReminders(true);
                    defaults.setOverspendAlerts(true);
                    defaults.setWeeklySummary(true);
                    defaults.setInactivityReminders(true);
                    defaults.setCardCycleClose(true);
                    defaults.setEmailEnabled(false);
                    return notificationPreferenceRepository.save(defaults);
                });
    }
}
