package com.smartfinance.backend.tarjetas.model.entity;

/**
 * Billing state of a single {@link Installment}.
 *
 * <p>Every installment starts {@code PENDING} and flips to {@code BILLED} exactly once, when
 * {@code CycleCloseService} (Fase B.4) materializes its interest into an aggregated
 * {@link CardMovementType#INTEREST} movement. Persisted as {@code VARCHAR} + {@code CHECK},
 * matching {@link CardFranchise}'s convention (see {@code V20__create_installments.sql}).
 */
public enum InstallmentStatus {
    PENDING,
    BILLED
}
