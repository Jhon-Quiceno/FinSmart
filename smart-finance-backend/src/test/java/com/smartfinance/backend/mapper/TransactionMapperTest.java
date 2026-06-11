package com.smartfinance.backend.mapper;

import com.smartfinance.backend.dto.transaction.TransactionRequest;
import com.smartfinance.backend.dto.transaction.TransactionResponse;
import com.smartfinance.backend.model.PaymentMethodType;
import com.smartfinance.backend.model.Transaction;
import com.smartfinance.backend.model.TransactionType;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TransactionMapperTest {

    private final TransactionMapper transactionMapper = Mappers.getMapper(TransactionMapper.class);

    @Test
    void toResponseShouldHandleNullCategoryAndAccount() {
        Transaction transaction = new Transaction(
                1L,
                null,
                null,
                null,
                TransactionType.INCOME,
                new BigDecimal("100.00"),
                "Sin categoría",
                LocalDate.of(2026, 5, 18),
                PaymentMethodType.CASH,
                null,
                null,
                null,
                null,
                Instant.now(),
                Instant.now()
        );

        TransactionResponse response = transactionMapper.toResponse(transaction);

        assertNull(response.categoryId());
        assertNull(response.accountId());
        assertEquals("Sin categoría", response.description());
    }

    @Test
    void toEntityShouldMapRequestFields() {
        TransactionRequest request = new TransactionRequest(
                5L,
                7L,
                TransactionType.EXPENSE,
                new BigDecimal("35.00"),
                "Comida",
                LocalDate.of(2026, 5, 18),
                PaymentMethodType.CREDIT_CARD,
                null,
                null,
                null,
                "Cena"
        );

        Transaction transaction = transactionMapper.toEntity(request);

        assertEquals(TransactionType.EXPENSE, transaction.getType());
        assertEquals(new BigDecimal("35.00"), transaction.getAmount());
        assertEquals("Comida", transaction.getDescription());
        assertEquals(LocalDate.of(2026, 5, 18), transaction.getTransactionDate());
        assertEquals(PaymentMethodType.CREDIT_CARD, transaction.getPaymentMethod());
        assertEquals("Cena", transaction.getNotes());
    }
}
