package com.smartfinance.backend.dto.income;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload to create or update an {@link com.smartfinance.backend.model.Income}.
 *
 * <p>{@link #date} uses {@link PastOrPresent} rather than allowing arbitrary future dates:
 * this sprint models income as money already received, not planned/scheduled income
 * (that belongs to a future recurring-payments feature), so a future date is rejected as
 * invalid input rather than silently accepted.
 *
 * @param amount      income amount, must be strictly positive
 * @param description optional free-text note
 * @param date        date the income was received, must not be in the future
 * @param source      optional free-text source of the income (e.g. "Salario", "Freelance")
 * @param categoryId  optional identifier of an existing category owned by the caller
 */
public record IncomeRequest(
        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser mayor a cero")
        BigDecimal amount,
        @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
        String description,
        @NotNull(message = "La fecha es obligatoria")
        @PastOrPresent(message = "La fecha no puede ser futura")
        LocalDate date,
        @Size(max = 100, message = "La fuente no puede superar 100 caracteres")
        String source,
        Long categoryId
) {
}
