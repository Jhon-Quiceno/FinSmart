package com.smartfinance.backend.dto.ai;

/**
 * Response for {@code POST /api/ai/categorize}.
 *
 * @param categoryId   identifier of the matched category, or {@code null} when the user has no
 *                     expense categories yet, or the assistant found no suitable match
 * @param categoryName name of the matched category, or {@code null} under the same conditions
 *                     as {@link #categoryId}
 */
public record CategorizeResponse(
        Long categoryId,
        String categoryName
) {
}
