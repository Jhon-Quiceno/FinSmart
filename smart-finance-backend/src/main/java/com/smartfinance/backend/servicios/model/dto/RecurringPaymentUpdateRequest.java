package com.smartfinance.backend.servicios.model.dto;

import com.smartfinance.backend.servicios.model.entity.RecurringFrequency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Payload to edit an existing {@link com.smartfinance.backend.servicios.model.entity.RecurringPayment}.
 *
 * <p>Deliberately excludes {@code nextPaymentDate} and {@code isActive}: the next payment
 * date only advances through {@code RecurringPaymentService#payRecurringPayment} (the
 * {@code /pay} endpoint), and the active flag only flips through
 * {@code RecurringPaymentService#toggleRecurringPayment} (the {@code /toggle} endpoint).
 *
 * @param name      recurring payment name/description
 * @param amount    amount charged each cycle, must be strictly positive
 * @param frequency billing cycle
 */
public record RecurringPaymentUpdateRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String name,
        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser mayor a cero")
        BigDecimal amount,
        @NotNull(message = "La frecuencia es obligatoria")
        RecurringFrequency frequency
) {
}
