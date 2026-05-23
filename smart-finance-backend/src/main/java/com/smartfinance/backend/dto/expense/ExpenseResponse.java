package com.smartfinance.backend.dto.expense;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ExpenseResponse(
        Long id,
        Long categoryId,
        String categoryName,
        BigDecimal amount,
        String description,
        LocalDate date,
        boolean isRecurring,
        String paymentMethod,
        Instant createdAt,
        Instant updatedAt
) {
}
