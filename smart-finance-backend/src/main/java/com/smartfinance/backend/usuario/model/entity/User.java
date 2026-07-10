package com.smartfinance.backend.usuario.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@ToString(exclude = "passwordHash")
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 180)
    private String email;

    @Column(nullable = false, length = 120)
    private String passwordHash;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /**
     * Number of AI chat messages sent by this user during {@link #aiChatPeriod}. A dedicated
     * counter (rather than counting {@code ai_messages} rows) so it survives the login-time chat
     * history purge (see {@code UserService#login}) and can be reserved atomically (see
     * {@code UserRepository#reserveAiChatQuota}).
     */
    @Column(name = "ai_chat_used", nullable = false)
    private int aiChatUsed = 0;

    /**
     * UTC calendar month {@link #aiChatUsed} belongs to, formatted {@code "YYYY-MM"}. {@code null}
     * until the user's first AI chat message. When this no longer matches the current month, the
     * counter is treated as reset back to zero.
     */
    @Column(name = "ai_chat_period", length = 7)
    private String aiChatPeriod;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
}
