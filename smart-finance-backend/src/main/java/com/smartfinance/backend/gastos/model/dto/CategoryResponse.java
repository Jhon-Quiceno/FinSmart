package com.smartfinance.backend.gastos.model.dto;

import com.smartfinance.backend.gastos.model.entity.CategoryType;

/**
 * Read model for a {@link com.smartfinance.backend.gastos.model.entity.Category}.
 *
 * @param id   category identifier
 * @param name category name
 * @param type whether this category classifies income or expense records
 */
public record CategoryResponse(
        Long id,
        String name,
        CategoryType type
) {
}
