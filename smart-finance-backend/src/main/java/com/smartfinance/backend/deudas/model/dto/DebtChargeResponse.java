package com.smartfinance.backend.deudas.model.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Read model for a {@link com.smartfinance.backend.deudas.model.entity.DebtCharge}.
 *
 * @param id          charge identifier
 * @param debtId      identifier of the debt this charge was applied to
 * @param amount      charge amount
 * @param chargeDate  date the charge was made
 * @param description optional free-text description
 * @param createdAt   creation timestamp
 */
public record DebtChargeResponse(
        Long id,
        Long debtId,
        BigDecimal amount,
        LocalDate chargeDate,
        String description,
        Instant createdAt
) {
}
