package com.smartfinance.backend.dto.recurring;

import com.smartfinance.backend.model.RecurringFrequency;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Read model for a {@link com.smartfinance.backend.model.RecurringPayment}.
 *
 * <p>The component is named {@code isActive} (rather than {@code active}) so it serializes to
 * a JSON {@code isActive} field, matching the Sprint 3 API contract.
 *
 * @param id              recurring payment identifier
 * @param name            recurring payment name/description
 * @param amount          amount charged each cycle
 * @param frequency       billing cycle
 * @param nextPaymentDate date of the next scheduled execution
 * @param isActive        whether the recurring payment is currently active
 * @param createdAt       creation timestamp
 * @param updatedAt       last update timestamp
 */
public record RecurringPaymentResponse(
        Long id,
        String name,
        BigDecimal amount,
        RecurringFrequency frequency,
        LocalDate nextPaymentDate,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {
}
