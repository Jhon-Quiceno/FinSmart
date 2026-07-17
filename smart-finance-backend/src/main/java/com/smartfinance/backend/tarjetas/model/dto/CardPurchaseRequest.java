package com.smartfinance.backend.tarjetas.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload para registrar una compra ({@link com.smartfinance.backend.tarjetas.model.entity.CardMovementType#PURCHASE}
 * o {@code INSTALLMENT_PURCHASE}) contra una tarjeta.
 *
 * @param amount           monto de la compra, debe ser positivo; el servicio además rechaza
 *                         montos que superen el cupo disponible de la tarjeta
 * @param date             fecha de la compra; por defecto hoy cuando se omite
 * @param description      descripción libre opcional
 * @param installmentCount número de cuotas ({@code null} o {@code 1} = compra simple, una sola
 *                         cuota). Valores entre 2 y 48 son válidos a nivel de formato, pero en
 *                         esta fase (B.2) el servicio los rechaza — el diferido a cuotas se
 *                         habilita en Fase B.3.
 */
public record CardPurchaseRequest(
        @NotNull(message = "El monto de la compra es obligatorio")
        @Positive(message = "El monto de la compra debe ser mayor a cero")
        BigDecimal amount,
        @PastOrPresent(message = "La fecha no puede ser futura")
        LocalDate date,
        @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
        String description,
        @Min(value = 1, message = "El número de cuotas debe ser al menos 1")
        @Max(value = 48, message = "El número de cuotas no puede superar 48")
        Integer installmentCount
) {
}
