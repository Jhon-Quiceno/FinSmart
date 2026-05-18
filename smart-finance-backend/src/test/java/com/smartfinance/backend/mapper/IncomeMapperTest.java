package com.smartfinance.backend.mapper;

import com.smartfinance.backend.dto.income.IncomeRequest;
import com.smartfinance.backend.dto.income.IncomeResponse;
import com.smartfinance.backend.model.Income;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class IncomeMapperTest {

    private final IncomeMapper incomeMapper = Mappers.getMapper(IncomeMapper.class);

    @Test
    void toResponseShouldHandleNullCategory() {
        Income income = new Income(
                1L,
                null,
                null,
                new BigDecimal("100.00"),
                "Sin categoría",
                LocalDate.of(2026, 5, 18),
                false,
                "Salario",
                Instant.now(),
                Instant.now()
        );

        IncomeResponse response = incomeMapper.toResponse(income);

        assertNull(response.categoryId());
        assertEquals("Sin categoría", response.description());
    }

    @Test
    void toEntityShouldMapRequestFields() {
        IncomeRequest request = new IncomeRequest(
                5L,
                new BigDecimal("250.00"),
                "Freelance",
                LocalDate.of(2026, 5, 18),
                true,
                "Freelance"
        );

        Income income = incomeMapper.toEntity(request);

        assertEquals(new BigDecimal("250.00"), income.getAmount());
        assertEquals("Freelance", income.getDescription());
        assertEquals(LocalDate.of(2026, 5, 18), income.getDate());
        assertEquals("Freelance", income.getSource());
    }
}
