package com.smartfinance.backend.mapper;

import com.smartfinance.backend.dto.expense.ExpenseRequest;
import com.smartfinance.backend.dto.expense.ExpenseResponse;
import com.smartfinance.backend.model.Expense;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ExpenseMapperTest {

    private final ExpenseMapper expenseMapper = Mappers.getMapper(ExpenseMapper.class);

    @Test
    void toResponseShouldHandleNullCategory() {
        Expense expense = new Expense(
                1L,
                null,
                null,
                new BigDecimal("90.00"),
                "Sin categoría",
                LocalDate.of(2026, 5, 18),
                false,
                "Efectivo",
                Instant.now(),
                Instant.now()
        );

        ExpenseResponse response = expenseMapper.toResponse(expense);

        assertNull(response.categoryId());
        assertEquals("Sin categoría", response.description());
    }

    @Test
    void toEntityShouldMapRequestFields() {
        ExpenseRequest request = new ExpenseRequest(
                7L,
                new BigDecimal("35.00"),
                "Comida",
                LocalDate.of(2026, 5, 18),
                false,
                "Tarjeta"
        );

        Expense expense = expenseMapper.toEntity(request);

        assertEquals(new BigDecimal("35.00"), expense.getAmount());
        assertEquals("Comida", expense.getDescription());
        assertEquals(LocalDate.of(2026, 5, 18), expense.getDate());
        assertEquals("Tarjeta", expense.getPaymentMethod());
    }
}
