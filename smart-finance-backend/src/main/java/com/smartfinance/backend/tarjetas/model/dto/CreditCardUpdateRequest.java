package com.smartfinance.backend.tarjetas.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Payload to edit an existing {@link com.smartfinance.backend.tarjetas.model.entity.CreditCard}.
 *
 * <p>Deliberately excludes {@code franchise}, {@code creditLimit} and {@code currentBalance}:
 * the card's network and credit limit are fixed at creation, and the balance must only ever
 * change through a recorded {@link com.smartfinance.backend.tarjetas.model.entity.CardMovement}
 * — mirrors {@link com.smartfinance.backend.deudas.model.dto.DebtUpdateRequest}'s exclusion of
 * {@code totalAmount}/{@code remainingAmount} for the same traceability reason.
 *
 * @param name          card name/description
 * @param bank          optional issuing bank
 * @param monthlyRate   effective monthly interest rate, cannot be negative
 * @param cutoffDay     billing cycle cutoff day of month, 1-31
 * @param paymentDueDay payment due day of month, 1-31
 */
public record CreditCardUpdateRequest(
        @NotBlank(message = "El nombre de la tarjeta es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String name,
        @Size(max = 100, message = "El banco no puede superar 100 caracteres")
        String bank,
        @NotNull(message = "La tasa mensual es obligatoria")
        @DecimalMin(value = "0.0", inclusive = true, message = "La tasa mensual no puede ser negativa")
        BigDecimal monthlyRate,
        @NotNull(message = "El día de corte es obligatorio")
        @Min(value = 1, message = "El día de corte debe estar entre 1 y 31")
        @Max(value = 31, message = "El día de corte debe estar entre 1 y 31")
        Integer cutoffDay,
        @NotNull(message = "El día de pago es obligatorio")
        @Min(value = 1, message = "El día de pago debe estar entre 1 y 31")
        @Max(value = 31, message = "El día de pago debe estar entre 1 y 31")
        Integer paymentDueDay
) {
}
