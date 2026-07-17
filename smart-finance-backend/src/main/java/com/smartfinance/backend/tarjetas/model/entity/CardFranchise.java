package com.smartfinance.backend.tarjetas.model.entity;

/**
 * Card network of a {@link CreditCard}.
 *
 * <p>Persisted as a {@code VARCHAR} column with a database {@code CHECK} constraint (see
 * {@code V17__create_credit_cards.sql}), matching the rest of the project's simple
 * varchar-plus-check approach to enumerations rather than a native PostgreSQL enum type (see
 * {@code com.smartfinance.backend.gastos.model.entity.PaymentMethodType}).
 */
public enum CardFranchise {
    VISA,
    MASTERCARD,
    AMEX,
    DINERS
}
