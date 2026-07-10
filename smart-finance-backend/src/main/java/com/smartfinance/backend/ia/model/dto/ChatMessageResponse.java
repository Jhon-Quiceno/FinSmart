package com.smartfinance.backend.ia.model.dto;

import com.smartfinance.backend.ia.model.entity.AiMessageRole;

import java.time.Instant;

/**
 * A single turn in {@code GET /api/ai/chat/history}.
 *
 * <p>{@link #providerName} and {@link #model} are {@code null} for {@link AiMessageRole#USER}
 * turns (see {@code AiMessage} Javadoc) and populated for {@link AiMessageRole#ASSISTANT} turns.
 *
 * @param id           message identifier
 * @param role         author of this turn
 * @param content      message text
 * @param providerName AI provider that produced this turn, or {@code null} for a user turn
 * @param model        model that produced this turn, or {@code null} for a user turn
 * @param createdAt    instant this turn was persisted
 */
public record ChatMessageResponse(
        Long id,
        AiMessageRole role,
        String content,
        String providerName,
        String model,
        Instant createdAt
) {
}
