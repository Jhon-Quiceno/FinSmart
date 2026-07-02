package com.smartfinance.backend.dto.recurring;

/**
 * Result of executing a recurring payment via {@code PATCH /api/recurring/{id}/pay}.
 *
 * <p>Design choice: rather than returning only the updated {@link RecurringPaymentResponse},
 * this composite also carries the identifier of the {@link com.smartfinance.backend.model.Expense}
 * created for this execution, so the caller can navigate to the generated expense without an
 * extra lookup request.
 *
 * @param recurringPayment updated recurring payment, with the recalculated
 *                         {@code nextPaymentDate}
 * @param expenseId        identifier of the expense created for this payment execution
 */
public record RecurringPaymentPayResponse(
        RecurringPaymentResponse recurringPayment,
        Long expenseId
) {
}
