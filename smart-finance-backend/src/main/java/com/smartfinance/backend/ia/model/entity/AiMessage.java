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

import java.time.Instant;

/**
 * A single turn of AI conversation or a persisted AI-generated insight, owned by a
 * {@link User}.
 *
 * <p>Unlike most entities in this project, rows are immutable once created — there is no
 * {@code updated_at} column (see {@code V8__create_ai_messages.sql}) — so this entity only
 * carries {@link #createdAt}. {@link #providerName} and {@link #model} record which AI
 * provider/model produced an {@link AiMessageRole#ASSISTANT} reply; both are {@code null} for
 * {@link AiMessageRole#USER} rows. The chat/insight endpoints that populate this entity are
 * introduced in Batch 2 of Sprint 5.
 */
@Entity
@Table(name = "ai_messages")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private AiMessageRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private AiMessageKind kind;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "provider_name", length = 60)
    private String providerName;

    @Column(length = 120)
    private String model;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
