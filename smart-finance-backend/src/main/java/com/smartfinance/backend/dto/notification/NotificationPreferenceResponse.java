package com.smartfinance.backend.dto.notification;

/**
 * Read model for the current user's {@link com.smartfinance.backend.model.NotificationPreference}.
 *
 * @param paymentReminders    whether {@link com.smartfinance.backend.model.NotificationType#PAYMENT_REMINDER} is delivered
 * @param overspendAlerts     whether {@link com.smartfinance.backend.model.NotificationType#OVERSPEND_ALERT} is delivered
 * @param weeklySummary       whether {@link com.smartfinance.backend.model.NotificationType#WEEKLY_SUMMARY} is delivered
 * @param inactivityReminders whether {@link com.smartfinance.backend.model.NotificationType#INACTIVITY_REMINDER} is delivered
 * @param emailEnabled        whether enabled notification types are also delivered by email
 */
public record NotificationPreferenceResponse(
        boolean paymentReminders,
        boolean overspendAlerts,
        boolean weeklySummary,
        boolean inactivityReminders,
        boolean emailEnabled
) {
}
