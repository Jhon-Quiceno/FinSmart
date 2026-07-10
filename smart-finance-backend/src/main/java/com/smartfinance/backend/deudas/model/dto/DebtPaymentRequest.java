package com.smartfinance.backend.deudas.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload to register a {@link com.smartfinance.backend.deudas.model.entity.DebtPayment} against a debt.
 *
 * @param amount      payment amount, must be strictly positive; the service additionally
 *                    rejects amounts exceeding the debt's current remaining balance
 * @param paymentDate date the payment was made; defaults to today when omitted
 * @param note        optional free-text note
 */
public record DebtPaymentRequest(
        @NotNull(message = "El monto del abono es obligatorio")
        @Positive(message = "El monto del abono debe ser mayor a cero")
        BigDecimal amount,
        LocalDate paymentDate,
        @Size(max = 255, message = "La nota no puede superar 255 caracteres")
        String note
) {
}
