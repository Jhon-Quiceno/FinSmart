package com.smartfinance.backend.ia.model.dto;

import com.smartfinance.backend.gastos.model.entity.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Payload for {@code POST /api/ai/categorize}: asks the assistant to suggest which of the
 * user's existing categories (income or expense) best matches a free-text description.
 *
 * @param description free-text description of the movement to classify
 * @param amount      optional amount, included in the prompt as extra context when present
 *                    (e.g. helps distinguish a small recurring charge from a large one-off
 *                    purchase)
 * @param type        category type to suggest from ({@link CategoryType#INCOME} or
 *                    {@link CategoryType#EXPENSE}); optional, defaults to {@code EXPENSE} when
 *                    absent so existing callers that don't send it (e.g. {@code ExpenseModal})
 *                    keep working unchanged
 */
public record CategorizeRequest(
        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
        String description,
        @Positive(message = "El monto debe ser mayor a cero")
        BigDecimal amount,
        CategoryType type
) {
    public CategorizeRequest {
        if (type == null) {
            type = CategoryType.EXPENSE;
        }
    }
}
