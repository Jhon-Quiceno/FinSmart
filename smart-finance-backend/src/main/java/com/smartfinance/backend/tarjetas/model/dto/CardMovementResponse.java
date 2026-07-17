package com.smartfinance.backend.tarjetas.model.dto;

import com.smartfinance.backend.tarjetas.model.entity.CardMovementType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Modelo de lectura de un {@link com.smartfinance.backend.tarjetas.model.entity.CardMovement}.
 *
 * @param id                identificador del movimiento
 * @param cardId            identificador de la tarjeta a la que pertenece el movimiento
 * @param type               tipo de movimiento
 * @param amount            monto del movimiento, siempre positivo
 * @param date              fecha del movimiento
 * @param description       descripción libre opcional
 * @param cardBalanceAfter  saldo de la tarjeta luego de aplicar este movimiento
 * @param expenseId         identificador del {@link com.smartfinance.backend.gastos.model.entity.Expense}
 *                          creado junto con este movimiento (solo compras; {@code null} en pagos)
 * @param installmentPlanId identificador del plan de cuotas asociado (solo compras diferidas;
 *                          {@code null} en esta fase, ya que las compras a cuotas se habilitan en
 *                          Fase B.3)
 * @param createdAt         marca de tiempo de creación
 */
public record CardMovementResponse(
        Long id,
        Long cardId,
        CardMovementType type,
        BigDecimal amount,
        LocalDate date,
        String description,
        BigDecimal cardBalanceAfter,
        Long expenseId,
        Long installmentPlanId,
        Instant createdAt
) {
}
