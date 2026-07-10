package com.smartfinance.backend.gastos.model.dto;

import com.smartfinance.backend.gastos.model.entity.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload to create or update a {@link com.smartfinance.backend.gastos.model.entity.Category}.
 *
 * @param name name of the category, unique per user and type
 * @param type whether this category classifies income or expense records
 */
public record CategoryRequest(
        @NotBlank(message = "El nombre de la categoría es obligatorio")
        @Size(max = 100, message = "El nombre de la categoría no puede superar 100 caracteres")
        String name,
        @NotNull(message = "El tipo de categoría es obligatorio")
        CategoryType type
) {
}
