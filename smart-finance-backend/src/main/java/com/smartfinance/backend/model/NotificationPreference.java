package com.smartfinance.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Per-{@link User} toggles controlling which {@link NotificationType} categories are delivered
 * and whether email delivery is enabled.
 *
 * <p>Exactly one row per user ({@code uk_notification_preferences_user} in
 * {@code V7__create_notifications_and_preferences.sql}). {@code NotificationService} creates
 * this row lazily, with the same defaults as the database column defaults, the first time a
 * user's preferences are read or updated — a user who never visits the preferences screen
 * still gets sensible defaults from {@code NotificationDispatcher} (treated as "all enabled
 * except email" when no row exists yet), so this row is a persisted override, not a
 * precondition for notifications to work.
 */
@Entity
@Table(name = "notification_preferences")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "payment_reminders", nullable = false)
    private boolean paymentReminders;

    @Column(name = "overspend_alerts", nullable = false)
    private boolean overspendAlerts;

    @Column(name = "weekly_summary", nullable = false)
    private boolean weeklySummary;

    @Column(name = "inactivity_reminders", nullable = false)
    private boolean inactivityReminders;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
}
