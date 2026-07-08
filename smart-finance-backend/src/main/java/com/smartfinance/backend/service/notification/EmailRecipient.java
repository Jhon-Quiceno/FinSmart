package com.smartfinance.backend.service.notification;

/**
 * Plain-value snapshot of the recipient of an email notification.
 *
 * <p>Exists so {@link NotificationSender} implementations (in particular
 * {@link EmailNotificationSender}, which delivers on an {@code @Async} thread) never receive a
 * JPA entity such as {@code User}. A lazy-loaded proxy passed across that async boundary would
 * throw {@code LazyInitializationException} once the originating persistence context/transaction
 * has already closed — {@link NotificationDispatcher} resolves these plain values with an
 * eager fetch before handing off to the async sender.
 *
 * @param userId owner id, kept only for logging/correlation
 * @param email  recipient's email address
 * @param name   recipient's display name
 */
public record EmailRecipient(Long userId, String email, String name) {
}
