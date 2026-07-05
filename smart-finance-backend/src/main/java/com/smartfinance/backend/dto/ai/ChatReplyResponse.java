package com.smartfinance.backend.dto.ai;

import java.time.Instant;

/**
 * Response for {@code POST /api/ai/chat}: the assistant's reply to the user's message, plus
 * which provider/model produced it.
 *
 * @param reply        the assistant's reply text, in Spanish
 * @param providerName display name of the AI provider that produced this reply
 * @param model        model identifier that produced this reply
 * @param createdAt    instant the assistant's reply was persisted
 */
public record ChatReplyResponse(
        String reply,
        String providerName,
        String model,
        Instant createdAt
) {
}
