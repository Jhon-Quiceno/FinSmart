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
 * A single record of AI provider usage, owned by a {@link User} — one row per successful call to
 * {@code AiChatOrchestrator#complete}, written by {@code AiUsageEventService#record} right after
 * {@code AiChatService}, {@code AiCategorizationService}, or {@code AiInsightService} gets a
 * reply back.
 *
 * <p>Like {@link AiMessage}, rows are immutable once created — there is no {@code updated_at}
 * column (see {@code V16__create_ai_usage_events.sql}) — so this entity only carries
 * {@link #createdAt}. {@link #costEstimate} is nullable because there is no per-provider pricing
 * table yet; it is populated only once one exists.
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

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
