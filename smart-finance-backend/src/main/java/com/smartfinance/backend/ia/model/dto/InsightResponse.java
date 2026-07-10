package com.smartfinance.backend.ia.model.dto;

import java.time.Instant;

/**
 * Read model for an {@link com.smartfinance.backend.ia.model.entity.AiMessageKind#INSIGHT} row, returned
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
