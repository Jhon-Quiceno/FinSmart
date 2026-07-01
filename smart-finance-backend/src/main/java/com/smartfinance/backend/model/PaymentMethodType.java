package com.smartfinance.backend.model;

/**
 * Payment method used to settle an {@link Expense}.
 *
 * <p>Persisted as a {@code VARCHAR} column with a database {@code CHECK} constraint
 * (see {@code V3__create_categories_incomes_expenses.sql}), matching the rest of the
 * project's simple varchar-plus-check approach to enumerations rather than a native
 * PostgreSQL enum type.
 */
public enum PaymentMethodType {
    CASH,
    DEBIT_CARD,
    CREDIT_CARD,
    TRANSFER,
    OTHER
}
