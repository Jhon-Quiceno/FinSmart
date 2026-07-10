package com.smartfinance.backend.gastos.model.dto;

import com.smartfinance.backend.gastos.model.entity.PaymentMethodType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Read model for an {@link com.smartfinance.backend.gastos.model.entity.Expense}.
 *
 * @param id            expense identifier
 * @param amount        expense amount
 * @param description   optional free-text note
 * @param date          date the expense was paid
 * @param paymentMethod method used to pay the expense
 * @param categoryId          identifier of the associated category, or {@code null} if unclassified
 * @param categoryName        name of the associated category, or {@code null} if unclassified
 * @param recurringPaymentId  identifier of the recurring payment this expense was generated
 *                            from, or {@code null} for a manually created expense
 */
public record ExpenseResponse(
        Long id,
        BigDecimal amount,
        String description,
        LocalDate date,
        PaymentMethodType paymentMethod,
        Long categoryId,
        String categoryName,
        Long recurringPaymentId
) {
}
