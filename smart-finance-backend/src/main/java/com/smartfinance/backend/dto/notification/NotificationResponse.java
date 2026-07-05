package com.smartfinance.backend.dto.notification;

import com.smartfinance.backend.model.NotificationType;

import java.time.Instant;

/**
 * Read model for a {@link com.smartfinance.backend.model.Notification}.
 *
 * @param id        notification identifier
 * @param type      category of the notification
 * @param title     short, user-facing title
 * @param message   full, user-facing message body
 * @param read      whether the current user has marked this notification as read
 * @param readAt    instant the notification was marked as read, or {@code null} if unread
 * @param createdAt instant the notification was created
 */
public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String message,
        boolean read,
        Instant readAt,
        Instant createdAt
) {
}
