package com.smartfinance.backend.dto.category;

import com.smartfinance.backend.model.CategoryType;

import java.time.Instant;

public record CategoryResponse(
        Long id,
        String name,
        CategoryType type,
        String icon,
        String color,
        String description,
        boolean isSystem,
        Instant createdAt,
        Instant updatedAt
) {
}
