package com.smartfinance.backend.dto.income;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record IncomeResponse(
        Long id,
        Long categoryId,
        BigDecimal amount,
        String description,
        LocalDate date,
        boolean isRecurring,
        String source,
        Instant createdAt,
        Instant updatedAt
) {
}
