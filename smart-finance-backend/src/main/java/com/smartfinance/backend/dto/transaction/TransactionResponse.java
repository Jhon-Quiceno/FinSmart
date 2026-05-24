package com.smartfinance.backend.dto.transaction;

import com.smartfinance.backend.model.PaymentMethodType;
import com.smartfinance.backend.model.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Response payload representing a stored transaction.
 */
public record TransactionResponse(
        Long id,
        Long accountId,
        String accountName,
        Long categoryId,
        String categoryName,
        TransactionType type,
        BigDecimal amount,
        String description,
        LocalDate transactionDate,
        PaymentMethodType paymentMethod,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
