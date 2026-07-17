package com.smartfinance.backend.ia.service;

import com.smartfinance.backend.ia.model.dto.AiUsageEventSummaryResponse;
import com.smartfinance.backend.ia.model.entity.AiUsageEvent;
import com.smartfinance.backend.ia.model.entity.AiUsageEventType;
import com.smartfinance.backend.ia.repository.AiUsageEventRepository;
import com.smartfinance.backend.usuario.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.Map;

/**
 * Writes and summarizes {@link AiUsageEvent} rows — the operational tracking of every successful
 * call to {@code AiChatOrchestrator#complete}, independent of the user-facing monthly chat quota
 * enforced by {@code AiChatService#reserveMonthlyQuota}.
 *
 * <p>{@link #record} is called by {@code AiChatService}, {@code AiCategorizationService}, and
 * {@code AiInsightService} right after a successful provider reply. It deliberately swallows any
 * persistence failure: a tracking event that fails to save must never turn an otherwise
 * successful AI reply into an error for the end user (see the {@code try/catch} below).
 */
@Service
public class AiUsageEventService {

    private static final Logger log = LoggerFactory.getLogger(AiUsageEventService.class);

    private final AiUsageEventRepository usageEventRepository;
    private final UserRepository userRepository;

    public AiUsageEventService(AiUsageEventRepository usageEventRepository, UserRepository userRepository) {
        this.usageEventRepository = usageEventRepository;
        this.userRepository = userRepository;
    }

    /**
     * Persists one {@link AiUsageEvent} row for {@code userId}. Never throws: any failure (e.g. a
     * transient database issue) is logged at {@code WARN} and swallowed, since tracking is
     * best-effort and must not block or fail the AI response already returned to the caller.
     *
     * @param userId     the user the AI call was made on behalf of
     * @param provider   the provider that actually answered (see {@code ChatCompletionResult#providerName})
     * @param eventType  which AI-backed operation produced this event
     * @param tokensUsed total tokens consumed (prompt + completion); {@code 0} when the provider
     *                   did not report usage
     * @param costEstimate estimated cost of the call, or {@code null} when there is no pricing data yet
     */
    @Transactional
    public void record(Long userId, String provider, AiUsageEventType eventType, int tokensUsed, BigDecimal costEstimate) {
        try {
            AiUsageEvent event = new AiUsageEvent();
            event.setUser(userRepository.getReferenceById(userId));
            event.setProvider(provider);
            event.setEventType(eventType);
            event.setTokensUsed(Math.max(tokensUsed, 0));
            event.setCostEstimate(costEstimate);
            usageEventRepository.save(event);
        } catch (RuntimeException ex) {
            log.warn("ai_usage_event_tracking_failed userId={} provider={} eventType={}", userId, provider, eventType, ex);
        }
    }

    /**
     * Summarizes {@code userId}'s tracked AI usage for the given UTC calendar month.
     */
    @Transactional(readOnly = true)
    public AiUsageEventSummaryResponse getUsageSummary(Long userId, YearMonth period) {
        Instant start = period.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = period.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        Map<AiUsageEventType, Long> eventsByType = new EnumMap<>(AiUsageEventType.class);
        long totalTokens = 0;
        long totalEvents = 0;
        for (AiUsageEventRepository.AiUsageEventTypeAggregate aggregate
                : usageEventRepository.aggregateByEventType(userId, start, end)) {
            eventsByType.put(aggregate.getEventType(), aggregate.getEventCount());
            totalTokens += aggregate.getTotalTokens();
            totalEvents += aggregate.getEventCount();
        }

        return new AiUsageEventSummaryResponse(period, totalTokens, totalEvents, eventsByType);
    }
}
