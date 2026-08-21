package com.smartfinance.backend.servicios.model.entity;

import com.smartfinance.backend.usuario.model.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
 * An Expo push token registered by one of a {@link User}'s mobile devices.
 *
 * <p>One row per {@code (user, device)} — see {@code uk_push_tokens_user_device} in
 * {@code V26__create_push_tokens.sql} — so re-registering the same device (reinstall, rotated
 * Expo token) overwrites {@link #expoPushToken} instead of accumulating duplicate rows. A user
 * with no row here has never opted into push on any device; {@code NotificationDispatcher} sends
 * a push per registered token, so zero rows means zero push notifications, silently.
 */
@Entity
@Table(name = "push_tokens")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PushToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expo_push_token", nullable = false, length = 200)
    private String expoPushToken;

    @Column(name = "device_id", nullable = false, length = 120)
    private String deviceId;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
