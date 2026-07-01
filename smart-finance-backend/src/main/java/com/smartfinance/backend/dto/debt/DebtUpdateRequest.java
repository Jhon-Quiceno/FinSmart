package com.smartfinance.backend.dto.debt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload to edit an existing {@link com.smartfinance.backend.model.Debt}.
 *
 * <p>Deliberately excludes {@code totalAmount} and {@code remainingAmount}: the remaining
 * balance must only ever change through a recorded
 * {@link com.smartfinance.backend.model.DebtPayment} — "nunca se sobrescribe sin dejar
 * registro" (traceability requirement from the Sprint 3 spec) — so both the total and the
 * remaining amount are immutable after creation via this endpoint.
 *
 * @param name         debt name/description
 * @param interestRate optional interest rate percentage
 * @param dueDate      optional due date
 */
public record DebtUpdateRequest(
        @NotBlank(message = "El nombre de la deuda es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String name,
        BigDecimal interestRate,
        LocalDate dueDate
) {
}
