package com.smartfinance.backend.tarjetas.model.entity;

/**
 * Kind of ledger entry recorded against a {@link CreditCard} as a {@link CardMovement}.
 *
 * <p>{@link CreditCard#getCurrentBalance()} is always the signed sum of every movement:
 * {@code PURCHASE}, {@code INSTALLMENT_PURCHASE}, {@code INTEREST} and {@code FEE} increase it;
 * {@code PAYMENT} decreases it. Persisted as {@code VARCHAR} + {@code CHECK}, matching
 * {@link CardFranchise}'s convention (see {@code V18__create_card_movements.sql}).
 */
public enum CardMovementType {
    PURCHASE,
    INSTALLMENT_PURCHASE,
    PAYMENT,
    INTEREST,
    FEE
}
