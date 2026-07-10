package com.smartfinance.backend.deudas.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload to create a {@link com.smartfinance.backend.deudas.model.entity.Debt}.
 *
 * @param name         debt name/description
 * @param totalAmount  total amount owed, must be strictly positive; also seeds
 *                     {@code remainingAmount} on creation
 * @param interestRate optional interest rate percentage, cannot be negative
 * @param dueDate      optional due date
 */
public record DebtRequest(
        @NotBlank(message = "El nombre de la deuda es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String name,
        @NotNull(message = "El monto total es obligatorio")
        @Positive(message = "El monto total debe ser mayor a cero")
        BigDecimal totalAmount,
        @DecimalMin(value = "0.0", inclusive = true, message = "La tasa de interés no puede ser negativa")
        BigDecimal interestRate,
        LocalDate dueDate
) {
}
