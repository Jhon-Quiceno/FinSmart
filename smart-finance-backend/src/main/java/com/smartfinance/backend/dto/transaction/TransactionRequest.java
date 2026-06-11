package com.smartfinance.backend.dto.transaction;

import com.smartfinance.backend.model.PaymentMethodType;
import com.smartfinance.backend.model.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request payload for creating or updating a transaction.
 */
public record TransactionRequest(
        @Positive(message = "La cuenta debe ser un identificador válido")
        Long accountId,
        @Positive(message = "La categoría debe ser un identificador válido")
        Long categoryId,
        @NotNull(message = "El tipo de transacción es obligatorio")
        TransactionType type,
        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor o igual a 0.01")
        BigDecimal amount,
        @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
        String description,
        @NotNull(message = "La fecha es obligatoria")
        @PastOrPresent(message = "La fecha no puede estar en el futuro")
        LocalDate transactionDate,
    PaymentMethodType paymentMethod,
    @Size(max = 100, message = "El nombre de la fuente de ingreso no puede superar 100 caracteres")
    String incomeSourceName,
    @Size(max = 100, message = "El nombre del método de pago no puede superar 100 caracteres")
    String expensePaymentMethodName,
    @Size(max = 100, message = "El nombre del tipo de gasto no puede superar 100 caracteres")
    String expenseTypeName,
    @Size(max = 1000, message = "Las notas no pueden superar 1000 caracteres")
    String notes
) {
}
