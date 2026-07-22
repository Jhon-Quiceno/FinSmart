package com.smartfinance.backend.ia.repository;

import com.smartfinance.backend.ia.model.entity.AiUsageEvent;
import com.smartfinance.backend.ia.model.entity.AiUsageEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/**
 * Persistence access for {@link AiUsageEvent}, always scoped by owner.
 *
 * <p>Consumed by {@code AiUsageEventService#getUsageSummary} to build a per-period usage
 * breakdown; kept intentionally simple — no separate aggregation query beyond the projection
 * below, since the row volume per user per month is small.
 */
public interface AiUsageEventRepository extends JpaRepository<AiUsageEvent, Long> {

    List<AiUsageEvent> findAllByUser_IdAndCreatedAtBetween(Long userId, Instant start, Instant end);

    /**
     * Aggregated token usage per {@link AiUsageEventType} for a user within {@code [start, end)},
     * used by {@code AiUsageEventService#getUsageSummary} instead of summing
     * {@link #findAllByUser_IdAndCreatedAtBetween} in memory.
     *
     * <p>Filters to {@code success = true} rows only: since {@code AiUsageEventService#recordAttempt}
     * also persists a row for every provider attempt that failed over (see {@link AiUsageEvent}'s
     * class Javadoc), counting those here would inflate the user-facing "AI calls this month"
     * summary with attempts the user never actually saw fail — only the attempt that ultimately
     * answered (or a plain {@code record()} row, always {@code success = true}) should count.
     */
    @Query("""
            SELECT e.eventType AS eventType, COUNT(e) AS eventCount, COALESCE(SUM(e.tokensUsed), 0) AS totalTokens
            FROM AiUsageEvent e
            WHERE e.user.id = :userId AND e.createdAt >= :start AND e.createdAt < :end AND e.success = true
            GROUP BY e.eventType
            """)
    List<AiUsageEventTypeAggregate> aggregateByEventType(
            @Param("userId") Long userId,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    /** Projection backing {@link #aggregateByEventType}. */
    interface AiUsageEventTypeAggregate {
        AiUsageEventType getEventType();

        long getEventCount();

        long getTotalTokens();
    }
}
