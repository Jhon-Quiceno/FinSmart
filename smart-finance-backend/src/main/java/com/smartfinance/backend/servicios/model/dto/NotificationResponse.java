package com.smartfinance.backend.servicios.model.dto;

import com.smartfinance.backend.servicios.model.entity.NotificationType;

import java.time.Instant;

/**
 * Read model for a {@link com.smartfinance.backend.servicios.model.entity.Notification}.
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
