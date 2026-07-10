package com.smartfinance.backend.ia.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Payload for {@code POST /api/ai/categorize}: asks the assistant to suggest which of the
 * user's existing expense categories best matches a free-text expense description.
 *
 * @param description free-text description of the expense to classify
 * @param amount      optional expense amount, included in the prompt as extra context when
 *                    present (e.g. helps distinguish a small recurring charge from a large
 *                    one-off purchase)
 */
public record CategorizeRequest(
        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
        String description,
        @Positive(message = "El monto debe ser mayor a cero")
        BigDecimal amount
) {
}
