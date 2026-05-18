package com.smartfinance.backend.dto.category;

import com.smartfinance.backend.model.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "El nombre de la categoría es obligatorio")
        @Size(max = 100, message = "El nombre de la categoría no puede superar 100 caracteres")
        String name,
        @NotNull(message = "El tipo de categoría es obligatorio")
        CategoryType type,
        @Size(max = 50, message = "El ícono no puede superar 50 caracteres")
        String icon,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "El color debe estar en formato hexadecimal #RRGGBB")
        String color
) {
}
