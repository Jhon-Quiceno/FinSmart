package com.smartfinance.backend.dto.notification;

import jakarta.validation.constraints.NotNull;

/**
 * Payload to replace the current user's {@link com.smartfinance.backend.model.NotificationPreference}.
 *
 * <p>All fields are required: this is a full replace ({@code PUT}), not a partial patch, so the
 * caller must send the complete set of toggles every time (mirroring the UI's settings form,
 * which always renders every toggle).
 *
 * @param paymentReminders    whether {@link com.smartfinance.backend.model.NotificationType#PAYMENT_REMINDER} is delivered
 * @param overspendAlerts     whether {@link com.smartfinance.backend.model.NotificationType#OVERSPEND_ALERT} is delivered
 * @param weeklySummary       whether {@link com.smartfinance.backend.model.NotificationType#WEEKLY_SUMMARY} is delivered
 * @param inactivityReminders whether {@link com.smartfinance.backend.model.NotificationType#INACTIVITY_REMINDER} is delivered
 * @param emailEnabled        whether enabled notification types are also delivered by email
 */
public record NotificationPreferenceRequest(
        @NotNull(message = "El campo paymentReminders es obligatorio")
        Boolean paymentReminders,
        @NotNull(message = "El campo overspendAlerts es obligatorio")
        Boolean overspendAlerts,
        @NotNull(message = "El campo weeklySummary es obligatorio")
        Boolean weeklySummary,
        @NotNull(message = "El campo inactivityReminders es obligatorio")
        Boolean inactivityReminders,
        @NotNull(message = "El campo emailEnabled es obligatorio")
        Boolean emailEnabled
) {
}
