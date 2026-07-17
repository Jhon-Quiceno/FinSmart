package com.smartfinance.backend.tarjetas.model.dto;

import com.smartfinance.backend.tarjetas.model.entity.InstallmentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Modelo de lectura de una {@link com.smartfinance.backend.tarjetas.model.entity.Installment}
 * (cuota) de un plan de compra diferida.
 *
 * @param id              identificador de la cuota
 * @param number          número de cuota dentro del plan (1..installmentCount)
 * @param capitalAmount   capital de esta cuota, congelado al momento de la compra
 * @param interestAmount  interés de esta cuota, calculado sobre el saldo pendiente de la compra
 * @param dueDate         fecha de vencimiento de la cuota
 * @param status          estado de facturación de la cuota
 */
public record InstallmentResponse(
        Long id,
        Integer number,
        BigDecimal capitalAmount,
        BigDecimal interestAmount,
        LocalDate dueDate,
        InstallmentStatus status
) {
}
