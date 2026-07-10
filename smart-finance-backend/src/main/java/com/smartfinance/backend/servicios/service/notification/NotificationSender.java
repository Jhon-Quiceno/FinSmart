package com.smartfinance.backend.servicios.service.notification;

/**
 * Port for a channel capable of delivering a notification outside the in-app list — currently
 * only email ({@link EmailNotificationSender}), but the abstraction exists so
 * {@link NotificationDispatcher} does not depend on a concrete transport.
 */
public interface NotificationSender {

    /**
     * Sends {@code subject}/{@code body} to {@code recipient} through this channel.
     *
     * <p>Takes an {@link EmailRecipient} rather than the {@code User} entity because
     * implementations (see {@link EmailNotificationSender}) may run on an {@code @Async} thread,
     * where a lazily-loaded entity proxy would no longer have an open persistence context to
     * resolve against.
     *
     * <p>Implementations must degrade silently (log and return) when the channel is not
     * configured, never throw to the caller — a missing/failed external channel must never
     * break the in-app notification that was already created.
     *
     * @param recipient plain-value recipient (no lazy state)
     * @param subject   short, user-facing subject/title
     * @param body      full, user-facing message body
     */
    void send(EmailRecipient recipient, String subject, String body);
}
