package com.smartfinance.backend.servicios.model.entity;

/**
 * Billing cycle of a {@link RecurringPayment}.
 *
 * <p>Persisted as a {@code VARCHAR} column with a database {@code CHECK} constraint
 * (see {@code V4__create_debts_debt_payments_recurring_payments.sql}), matching the rest of
 * the project's simple varchar-plus-check approach to enumerations (see
 * {@link PaymentMethodType}) rather than a native PostgreSQL enum type.
 */
public enum RecurringFrequency {
    MONTHLY,
    WEEKLY
}
