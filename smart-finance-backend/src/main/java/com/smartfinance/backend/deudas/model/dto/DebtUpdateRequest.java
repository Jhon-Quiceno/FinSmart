package com.smartfinance.backend.deudas.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload to edit an existing {@link com.smartfinance.backend.deudas.model.entity.Debt}.
 *
 * <p>Deliberately excludes {@code totalAmount} and {@code remainingAmount}: the remaining
 * balance must only ever change through a recorded
 * {@link com.smartfinance.backend.deudas.model.entity.DebtPayment} — "nunca se sobrescribe sin dejar
 * registro" (traceability requirement from the Sprint 3 spec) — so both the total and the
 * remaining amount are immutable after creation via this endpoint.
 *
 * @param name         debt name/description
 * @param interestRate optional interest rate percentage, cannot be negative
 * @param dueDate      optional due date
 */
public record DebtUpdateRequest(
        @NotBlank(message = "El nombre de la deuda es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String name,
        @DecimalMin(value = "0.0", inclusive = true, message = "La tasa de interés no puede ser negativa")
        BigDecimal interestRate,
        LocalDate dueDate
) {
}
