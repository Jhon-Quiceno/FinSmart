package com.smartfinance.backend.tarjetas.model.entity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Locks the exact constant set of every {@code tarjetas} enum to what the corresponding database
 * {@code CHECK} constraint allows (see {@code V17__create_credit_cards.sql},
 * {@code V18__create_card_movements.sql}, {@code V20__create_installments.sql}). If a constant is
 * renamed, added or removed without updating the matching migration, {@code
 * spring.jpa.hibernate.ddl-auto=validate} would reject the value at runtime with a constraint
 * violation instead of a clear enum mismatch — this test catches the drift at compile/test time
 * instead.
 */
class CardEnumsTest {

    @Test
    void cardFranchiseShouldContainExactlyTheFourNetworksAllowedByTheChConstraint() {
        CardFranchise[] values = CardFranchise.values();

        Assertions.assertArrayEquals(
                new CardFranchise[] {CardFranchise.VISA, CardFranchise.MASTERCARD, CardFranchise.AMEX, CardFranchise.DINERS},
                values
        );
    }

    @Test
    void cardMovementTypeShouldContainExactlyTheFiveLedgerEntryKindsAllowedByTheCheckConstraint() {
        CardMovementType[] values = CardMovementType.values();

        Assertions.assertArrayEquals(
                new CardMovementType[] {
                        CardMovementType.PURCHASE,
                        CardMovementType.INSTALLMENT_PURCHASE,
                        CardMovementType.PAYMENT,
                        CardMovementType.INTEREST,
                        CardMovementType.FEE
                },
                values
        );
    }

    @Test
    void installmentStatusShouldContainExactlyPendingAndBilledAllowedByTheCheckConstraint() {
        InstallmentStatus[] values = InstallmentStatus.values();

        Assertions.assertArrayEquals(
                new InstallmentStatus[] {InstallmentStatus.PENDING, InstallmentStatus.BILLED},
                values
        );
    }
}
