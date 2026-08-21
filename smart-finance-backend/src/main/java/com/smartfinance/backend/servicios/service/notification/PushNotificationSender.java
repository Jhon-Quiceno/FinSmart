package com.smartfinance.backend.servicios.service.notification;

/**
 * Port for a channel capable of delivering a notification to a registered mobile device —
 * currently only Expo push ({@link ExpoPushAdapter}).
 *
 * <p>Deliberately not a shared abstraction with {@link NotificationSender}: that port is bound
 * to {@link EmailRecipient} and injected into {@link NotificationDispatcher} as its sole
 * implementation, so a second {@code @Component implements NotificationSender} would make that
 * dependency ambiguous ({@code NoUniqueBeanDefinitionException}). A device can also have zero,
 * one, or several registered tokens for one user — unlike email, which is a single fixed
 * attribute of {@code User} — so {@link NotificationDispatcher} sends one push per token rather
 * than resolving a single recipient the way it does for email.
 */
public interface PushNotificationSender {

    /**
     * Sends {@code title}/{@code body} to {@code recipient} through this channel.
     *
     * <p>Implementations must degrade silently (log and return) on any failure — an
     * unreachable/misconfigured/expired push channel must never break the in-app notification
     * that was already created.
     *
     * @param recipient plain-value recipient (no lazy state)
     * @param title     short, user-facing title
     * @param body      full, user-facing message body
     */
    void send(PushRecipient recipient, String title, String body);
}
