package com.smartfinance.backend.deudas.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload to register a {@link com.smartfinance.backend.deudas.model.entity.DebtCharge} against a debt.
 *
 * @param amount      charge amount, must be strictly positive; increments the debt's remaining
 *                    balance instead of decrementing it (mirror of
 *                    {@link com.smartfinance.backend.deudas.model.dto.DebtPaymentRequest})
 * @param chargeDate  date the charge was made; defaults to today when omitted
 * @param description optional free-text description
 */
public record DebtChargeRequest(
        @NotNull(message = "El monto del cargo es obligatorio")
        @Positive(message = "El monto del cargo debe ser mayor a cero")
        BigDecimal amount,
        @PastOrPresent(message = "La fecha no puede ser futura")
        LocalDate chargeDate,
        @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
        String description
) {
}
