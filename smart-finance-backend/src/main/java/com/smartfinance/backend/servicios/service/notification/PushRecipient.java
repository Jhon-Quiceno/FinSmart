package com.smartfinance.backend.servicios.service.notification;

/**
 * Plain-value snapshot of the recipient of a push notification.
 *
 * <p>Mirrors {@link EmailRecipient}'s reasoning: {@link PushNotificationSender} implementations
 * (in particular {@link ExpoPushAdapter}, which delivers on an {@code @Async} thread) must never
 * receive a JPA entity such as {@code PushToken} or {@code User} — {@link NotificationDispatcher}
 * resolves these plain values before handing off to the async sender.
 *
 * @param userId         owner id, kept only for logging/correlation
 * @param expoPushToken  the Expo push token to deliver to
 */
public record PushRecipient(Long userId, String expoPushToken) {
}
