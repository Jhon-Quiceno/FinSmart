package com.smartfinance.backend.dto.income;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IncomeRequest(
        @Positive(message = "La categoría debe ser un identificador válido")
        Long categoryId,
        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor o igual a 0.01")
        BigDecimal amount,
        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
        String description,
        @NotNull(message = "La fecha es obligatoria")
        @PastOrPresent(message = "La fecha no puede estar en el futuro")
        LocalDate date,
        Boolean isRecurring,
        @Size(max = 50, message = "La fuente no puede superar 50 caracteres")
        String source
) {
}
