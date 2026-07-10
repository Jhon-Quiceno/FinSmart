package com.smartfinance.backend.analisis.model.dto;

/**
 * A single alert/recommendation/info entry produced by the rule-based recommendation engine,
 * returned by {@code GET /api/analysis/recommendations}.
 *
 * @param type       whether this entry is a hard {@link RecommendationType#ALERT}, a
 *                   {@link RecommendationType#RECOMMENDATION}, or purely informational
 * @param severity   relative urgency, for client-side sorting/styling
 * @param categoryId category this entry is about, or {@code null} when it is not
 *                   category-specific (e.g. the overall expense-ratio or debt-ratio alerts)
 * @param message    human-readable message in Spanish, ready to render as-is
 */
public record RecommendationResponse(
        RecommendationType type,
        Severity severity,
        Long categoryId,
        String message
) {
}
