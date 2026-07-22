package com.smartfinance.backend.ia.service;

import com.smartfinance.backend.common.security.SecurityUtils;
import com.smartfinance.backend.ia.model.dto.AiUsageEventSummaryResponse;
import com.smartfinance.backend.ia.model.entity.AiUsageEvent;
import com.smartfinance.backend.ia.model.entity.AiUsageEventType;
import com.smartfinance.backend.ia.repository.AiUsageEventRepository;
import com.smartfinance.backend.usuario.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.Map;

/**
 * Writes and summarizes {@link AiUsageEvent} rows — the operational tracking of AI provider usage,
 * independent of the user-facing monthly chat quota enforced by
 * {@code AiChatService#reserveMonthlyQuota}.
 *
 * <p>{@link #record} is called directly by {@code StatementAiExtractionService} (the one caller
 * left outside the per-attempt telemetry work — see {@code AiChatOrchestrator}'s class Javadoc)
 * right after a successful provider reply. Every other AI-backed feature
 * ({@code AiChatService}, {@code AiCategorizationService}, {@code AiInsightService},
 * {@code FinancialSummaryQueryService}, {@code ReceiptExtractionService}) instead passes an
 * {@code AiCallContext} into {@code AiChatOrchestrator#complete}/{@code completeVision}, which
 * calls {@link #recordAttempt} itself for every attempt in its failover loop — see that class for
 * why per-attempt telemetry can only be captured from inside the loop, not by the caller after the
 * fact. Both methods deliberately swallow any persistence failure: a tracking event that fails to
 * save must never turn an otherwise successful AI reply into an error for the end user (see the
 * {@code try/catch} in each).
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
     * Persists one {@link AiUsageEvent} row for a single provider ATTEMPT within
     * {@code AiChatOrchestrator#complete}/{@code completeVision} — called for every provider tried
     * in the failover loop, both the ones that failed over and the one that ultimately answered
     * (see {@link AiUsageEvent}'s class Javadoc). Never throws, same best-effort guarantee as
     * {@link #record}.
     *
     * <p>Runs in {@link Propagation#REQUIRES_NEW} rather than joining the caller's transaction:
     * {@code AiChatOrchestrator#complete} is typically invoked from inside a broader
     * {@code @Transactional} method (e.g. {@code AiChatService#chat}), and a telemetry INSERT
     * failure must never mark that outer transaction rollback-only — the same defensive concern
     * documented at length in {@code FinancialSummaryQueryService}/{@code ReceiptExtractionService}
     * for crossing a {@code @Transactional} proxy boundary, just with an explicit new propagation
     * here instead of simply omitting the annotation (this method's own INSERT must commit
     * independently of whatever the caller's transaction ultimately does, including a caller-side
     * rollback for an unrelated reason after the AI call already succeeded).
     *
     * @param userId       the user the AI call was made on behalf of
     * @param provider     the provider this specific attempt was made against
     * @param eventType    which AI-backed operation produced this event
     * @param tokensUsed   total tokens consumed by this attempt; {@code 0} for a failed attempt or
     *                     when the provider did not report usage
     * @param costEstimate estimated cost of this attempt (see {@code AiProviderPricing}), or
     *                     {@code null} for a failed attempt or when there is no pricing data yet
     * @param latencyMs    how long this attempt took, in milliseconds
     * @param success      whether this attempt succeeded
     * @param errorType    the failed attempt's exception class simple name, or {@code null} for a
     *                     successful attempt
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAttempt(
            Long userId,
            String provider,
            AiUsageEventType eventType,
            int tokensUsed,
            BigDecimal costEstimate,
            Integer latencyMs,
            boolean success,
            String errorType
    ) {
        try {
            AiUsageEvent event = new AiUsageEvent();
            event.setUser(userRepository.getReferenceById(userId));
            event.setProvider(provider);
            event.setEventType(eventType);
            event.setTokensUsed(Math.max(tokensUsed, 0));
            event.setCostEstimate(costEstimate);
            event.setLatencyMs(latencyMs);
            event.setSuccess(success);
            event.setErrorType(errorType);
            usageEventRepository.save(event);
        } catch (RuntimeException ex) {
            log.warn("ai_usage_event_attempt_tracking_failed userId={} provider={} eventType={} success={}",
                    userId, provider, eventType, success, ex);
        }
    }

    /**
     * Summarizes the current authenticated user's tracked AI usage for the given UTC calendar
     * month. Convenience overload for {@code AiUsageEventController} — mirrors the pattern where
     * controllers never call {@link SecurityUtils} directly (see {@code AiChatService#getUsage}).
     */
    @Transactional(readOnly = true)
    public AiUsageEventSummaryResponse getUsageSummary(YearMonth period) {
        return getUsageSummary(SecurityUtils.getCurrentUserId(), period);
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
