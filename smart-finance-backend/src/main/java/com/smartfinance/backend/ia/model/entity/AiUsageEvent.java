package com.smartfinance.backend.ia.model.entity;

import com.smartfinance.backend.usuario.model.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A single record of AI provider usage, owned by a {@link User}.
 *
 * <p>Two call sites write rows here:
 * <ul>
 *     <li>{@code AiUsageEventService#record} — one row per successful call, written directly by a
 *         caller like {@code StatementAiExtractionService} that does not go through per-attempt
 *         telemetry. Always {@link #success} {@code true}, {@link #latencyMs}/{@link #errorType}
 *         {@code null}.</li>
 *     <li>{@code AiUsageEventService#recordAttempt} — one row per provider ATTEMPT within a single
 *         {@code AiChatOrchestrator#complete}/{@code completeVision} call made with an
 *         {@code AiCallContext}, including attempts that failed over (see
 *         {@code AiChatOrchestrator}'s class Javadoc). {@link #success} distinguishes a failed
 *         attempt (latency and {@link #errorType} populated, no tokens/cost) from the one that
 *         ultimately answered (tokens, cost, latency populated, {@link #errorType} {@code null}).</li>
 * </ul>
 *
 * <p>Because failed attempts are also persisted here, {@code AiUsageEventRepository#aggregateByEventType}
 * — which backs the user-facing monthly usage summary — filters to {@code success = true} rows
 * only, so a provider that failed over does not inflate the user's visible "AI calls this month"
 * count.
 *
 * <p>Like {@link AiMessage}, rows are immutable once created — there is no {@code updated_at}
 * column (see {@code V16__create_ai_usage_events.sql}) — so this entity only carries
 * {@link #createdAt}. {@link #costEstimate} is nullable whenever the provider/model combination has
 * no known pricing yet (see {@code AiProviderPricing}).
 */
@Entity
@Table(name = "ai_usage_events")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiUsageEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 60)
    private String provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private AiUsageEventType eventType;

    @Column(name = "tokens_used", nullable = false)
    private int tokensUsed;

    @Column(name = "cost_estimate", precision = 10, scale = 6)
    private BigDecimal costEstimate;

    /**
     * How long this specific provider attempt took, in milliseconds, or {@code null} for rows
     * written by {@code AiUsageEventService#record} (which doesn't measure it).
     */
    @Column(name = "latency_ms")
    private Integer latencyMs;

    /**
     * Whether this attempt succeeded. Always {@code true} for rows written by
     * {@code AiUsageEventService#record}; for {@code recordAttempt} rows, {@code false} marks a
     * provider that failed over to the next one.
     */
    @Column(nullable = false)
    private boolean success = true;

    /**
     * The failed attempt's exception class simple name (e.g. {@code "AiProviderTimeoutException"}),
     * or {@code null} for a successful attempt.
     */
    @Column(name = "error_type", length = 60)
    private String errorType;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
