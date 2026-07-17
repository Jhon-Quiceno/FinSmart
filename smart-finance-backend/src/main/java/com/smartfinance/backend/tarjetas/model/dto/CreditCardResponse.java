package com.smartfinance.backend.tarjetas.model.dto;

import com.smartfinance.backend.tarjetas.model.entity.CardFranchise;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Read model for a {@link com.smartfinance.backend.tarjetas.model.entity.CreditCard}.
 *
 * @param id              card identifier
 * @param name            card name/description
 * @param bank            issuing bank, may be null
 * @param franchise       card network
 * @param creditLimit     total credit limit, fixed at creation
 * @param monthlyRate     effective monthly interest rate
 * @param cutoffDay       billing cycle cutoff day of month
 * @param paymentDueDay   payment due day of month
 * @param currentBalance  current outstanding balance, only changed via recorded movements
 * @param availableCredit derived quota, {@code creditLimit - currentBalance}
 * @param lastCutoffDate  date of the last closed billing cycle, null if never closed
 * @param createdAt       creation timestamp
 * @param updatedAt       last update timestamp
 */
public record CreditCardResponse(
        Long id,
        String name,
        String bank,
        CardFranchise franchise,
        BigDecimal creditLimit,
        BigDecimal monthlyRate,
        Integer cutoffDay,
        Integer paymentDueDay,
        BigDecimal currentBalance,
        BigDecimal availableCredit,
        LocalDate lastCutoffDate,
        Instant createdAt,
        Instant updatedAt
) {
}
