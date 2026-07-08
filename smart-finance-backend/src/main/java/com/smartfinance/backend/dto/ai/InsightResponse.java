package com.smartfinance.backend.dto.ai;

import java.time.Instant;

/**
 * Read model for an {@link com.smartfinance.backend.model.AiMessageKind#INSIGHT} row, returned
 * by {@code GET /api/ai/insights} (the latest one) and {@code POST /api/ai/insights/generate}
 * (the freshly generated one).
 *
 * @param id           insight identifier
 * @param content      the AI-generated recommendations text, in Spanish, bullet-formatted
 * @param providerName AI provider that generated this insight
 * @param model        model that generated this insight
 * @param createdAt    instant this insight was generated
 */
public record InsightResponse(
        Long id,
        String content,
        String providerName,
        String model,
        Instant createdAt
) {
}
