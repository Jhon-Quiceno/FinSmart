package com.smartfinance.backend.ia.model.dto;

import com.smartfinance.backend.gastos.model.entity.CategoryType;

/**
 * Result of {@code AiCategorizationService#classifyMovement}: which kind of movement (income or
 * expense) the assistant decided a free-text description refers to, plus the matched category
 * from that decided type, if any.
 *
 * @param type         the decided movement type; falls back to {@link CategoryType#EXPENSE} when
 *                      the assistant's response could not be interpreted (see
 *                      {@code AiCategorizationService#classifyMovement})
 * @param categoryId   identifier of the matched category, or {@code null} when the user has no
 *                      categories of {@link #type} yet, or the assistant found no suitable match
 * @param categoryName name of the matched category, or {@code null} under the same conditions
 *                      as {@link #categoryId}
 */
public record MovementClassification(
        CategoryType type,
        Long categoryId,
        String categoryName
) {
}
