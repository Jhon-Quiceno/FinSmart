package com.smartfinance.backend.servicios.model.dto;

/**
 * Read model for the current user's {@link com.smartfinance.backend.servicios.model.entity.NotificationPreference}.
 *
 * @param paymentReminders    whether {@link com.smartfinance.backend.servicios.model.entity.NotificationType#PAYMENT_REMINDER} is delivered
 * @param overspendAlerts     whether {@link com.smartfinance.backend.servicios.model.entity.NotificationType#OVERSPEND_ALERT} is delivered
 * @param weeklySummary       whether {@link com.smartfinance.backend.servicios.model.entity.NotificationType#WEEKLY_SUMMARY} is delivered
 * @param inactivityReminders whether {@link com.smartfinance.backend.servicios.model.entity.NotificationType#INACTIVITY_REMINDER} is delivered
 * @param cardCycleClose      whether {@link com.smartfinance.backend.servicios.model.entity.NotificationType#CARD_CYCLE_CLOSE} is delivered
 * @param emailEnabled        whether enabled notification types are also delivered by email
 */
public record NotificationPreferenceResponse(
        boolean paymentReminders,
        boolean overspendAlerts,
        boolean weeklySummary,
        boolean inactivityReminders,
        boolean cardCycleClose,
        boolean emailEnabled
) {
}
