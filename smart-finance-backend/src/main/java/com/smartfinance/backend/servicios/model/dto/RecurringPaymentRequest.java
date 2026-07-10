package com.smartfinance.backend.servicios.model.dto;

import com.smartfinance.backend.servicios.model.entity.RecurringFrequency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload to create a {@link com.smartfinance.backend.servicios.model.entity.RecurringPayment}.
 *
 * @param name             recurring payment name/description
 * @param amount           amount charged each cycle, must be strictly positive
 * @param frequency        billing cycle
 * @param firstPaymentDate date of the first scheduled payment; seeds {@code nextPaymentDate}
 */
public record RecurringPaymentRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String name,
        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser mayor a cero")
        BigDecimal amount,
        @NotNull(message = "La frecuencia es obligatoria")
        RecurringFrequency frequency,
        @NotNull(message = "La fecha del primer pago es obligatoria")
        LocalDate firstPaymentDate
) {
}
