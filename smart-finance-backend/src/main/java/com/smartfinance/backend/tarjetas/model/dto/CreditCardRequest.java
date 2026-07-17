package com.smartfinance.backend.tarjetas.model.dto;

import com.smartfinance.backend.tarjetas.model.entity.CardFranchise;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Payload to create a {@link com.smartfinance.backend.tarjetas.model.entity.CreditCard}.
 *
 * @param name          card name/description
 * @param bank          optional issuing bank
 * @param franchise     card network
 * @param creditLimit   total credit limit, must be strictly positive
 * @param monthlyRate   effective monthly interest rate, cannot be negative
 * @param cutoffDay     billing cycle cutoff day of month, 1-31
 * @param paymentDueDay payment due day of month, 1-31
 */
public record CreditCardRequest(
        @NotBlank(message = "El nombre de la tarjeta es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String name,
        @Size(max = 100, message = "El banco no puede superar 100 caracteres")
        String bank,
        @NotNull(message = "La franquicia es obligatoria")
        CardFranchise franchise,
        @NotNull(message = "El cupo es obligatorio")
        @Positive(message = "El cupo debe ser mayor a cero")
        BigDecimal creditLimit,
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
