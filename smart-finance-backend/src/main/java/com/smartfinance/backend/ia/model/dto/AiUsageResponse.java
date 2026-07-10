package com.smartfinance.backend.ia.model.dto;

import java.time.Instant;

/**
 * Response for {@code GET /api/ai/chat/usage}: the current user's AI chat quota usage for the
 * ongoing UTC calendar month.
 *
 * @param used      number of USER-role AI chat messages sent so far this UTC calendar month
 * @param limit     maximum number of USER-role AI chat messages allowed per UTC calendar month
 *                  (see {@code app.ai.monthly-message-limit})
 * @param remaining messages left before {@code limit} is reached, never negative
 * @param resetsAt  instant the quota resets — start of the next UTC calendar month
 */
public record AiUsageResponse(
        int used,
        int limit,
        int remaining,
        Instant resetsAt
) {
}
