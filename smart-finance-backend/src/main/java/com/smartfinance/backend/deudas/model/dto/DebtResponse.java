package com.smartfinance.backend.deudas.model.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Read model for a {@link com.smartfinance.backend.deudas.model.entity.Debt}.
 *
 * @param id               debt identifier
 * @param name             debt name/description
 * @param totalAmount      total amount owed, fixed at creation
 * @param remainingAmount  current outstanding balance, only reduced via recorded payments
 * @param interestRate     optional interest rate percentage
 * @param dueDate          optional due date
 * @param createdAt        creation timestamp
 * @param updatedAt        last update timestamp
 */
public record DebtResponse(
        Long id,
        String name,
        BigDecimal totalAmount,
        BigDecimal remainingAmount,
        BigDecimal interestRate,
        LocalDate dueDate,
        Instant createdAt,
        Instant updatedAt
) {
}
