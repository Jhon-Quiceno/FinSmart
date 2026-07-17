package com.smartfinance.backend.servicios.model.entity;

/**
 * Category of a {@link Notification}, used both to render an icon/label in the UI and to look
 * up the matching toggle in {@link NotificationPreference} (see
 * {@code NotificationDispatcher#isEnabledFor}).
 *
 * <p>Persisted as a {@code VARCHAR} column with a database {@code CHECK} constraint (see
 * {@code V7__create_notifications_and_preferences.sql}), matching the rest of the project's
 * simple varchar-plus-check approach to enumerations (see {@link PaymentMethodType}) rather
 * than a native PostgreSQL enum type.
 *
 * <p>{@link #MONTH_END_PREDICTION} and {@link #SYSTEM} have no dedicated preference toggle —
 * they are always delivered in-app when created.
 *
 * <p>{@link #CARD_CYCLE_CLOSE} was added in Fase B.4 for {@code CardCycleCloseJob}
 * ({@code tarjetas/service/job/}), fired once per credit card cycle close.
 */
public enum NotificationType {
    PAYMENT_REMINDER,
    OVERSPEND_ALERT,
    WEEKLY_SUMMARY,
    INACTIVITY_REMINDER,
    MONTH_END_PREDICTION,
    SYSTEM,
    CARD_CYCLE_CLOSE
}
