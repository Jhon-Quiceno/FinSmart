package com.smartfinance.backend.gastos.model.dto;

import com.smartfinance.backend.gastos.model.entity.PaymentMethodType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload to create or update an {@link com.smartfinance.backend.gastos.model.entity.Expense}.
 *
 * <p>{@link #date} uses {@link PastOrPresent} for the same reasoning as
 * {@link com.smartfinance.backend.ingresos.model.dto.IncomeRequest#date()}: this sprint models
 * expenses as money already paid, not planned/scheduled expenses.
 *
 * @param amount        expense amount, must be strictly positive
 * @param description   optional free-text note
 * @param date          date the expense was paid, must not be in the future
 * @param paymentMethod method used to pay the expense
 * @param categoryId    optional identifier of an existing category owned by the caller
 */
public record ExpenseRequest(
        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser mayor a cero")
        BigDecimal amount,
        @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
        String description,
        @NotNull(message = "La fecha es obligatoria")
        @PastOrPresent(message = "La fecha no puede ser futura")
        LocalDate date,
        @NotNull(message = "El método de pago es obligatorio")
        PaymentMethodType paymentMethod,
        Long categoryId
) {
}
