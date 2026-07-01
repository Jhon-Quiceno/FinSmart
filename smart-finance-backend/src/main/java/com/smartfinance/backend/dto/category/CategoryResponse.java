package com.smartfinance.backend.dto.category;

import com.smartfinance.backend.model.CategoryType;

/**
 * Read model for a {@link com.smartfinance.backend.model.Category}.
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
