package com.smartfinance.backend.ia.model.dto;

import com.smartfinance.backend.ia.model.entity.AiUsageEventType;

import java.time.YearMonth;
import java.util.Map;

/**
 * Per-period breakdown of {@code ai_usage_events} for the current user, returned by
 * {@code AiUsageEventService#getUsageSummary}.
 *
 * <p>Distinct from {@link AiUsageResponse}: that one reports the USER-role monthly chat quota
 * ({@code app.ai.monthly-message-limit}); this one reports raw token consumption across every
 * tracked AI operation (chat, categorize, insight), regardless of the chat quota.
 *
 * @param period         the UTC calendar month this summary covers
 * @param totalTokens    sum of {@code tokensUsed} across every event in {@code period}
 * @param totalEvents    total number of tracked events in {@code period}
 * @param eventsByType   number of events per {@link AiUsageEventType} in {@code period}
 */
public record AiUsageEventSummaryResponse(
        YearMonth period,
        long totalTokens,
        long totalEvents,
        Map<AiUsageEventType, Long> eventsByType
) {
}
