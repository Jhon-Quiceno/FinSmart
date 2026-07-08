package com.smartfinance.backend.dto.debt;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Read model for a {@link com.smartfinance.backend.model.DebtPayment}.
 *
 * @param id          payment identifier
 * @param debtId      identifier of the debt this payment was applied to
 * @param amount      payment amount
 * @param paymentDate date the payment was made
 * @param note        optional free-text note
 * @param createdAt   creation timestamp
 * @param expenseId   identifier of the {@link com.smartfinance.backend.model.Expense} created
 *                    alongside this payment (see {@code DebtPaymentService#createPayment})
 */
public record DebtPaymentResponse(
        Long id,
        Long debtId,
        BigDecimal amount,
        LocalDate paymentDate,
        String note,
        Instant createdAt,
        Long expenseId
) {
}
