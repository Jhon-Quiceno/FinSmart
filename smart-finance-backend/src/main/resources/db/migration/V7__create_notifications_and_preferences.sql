CREATE TABLE notifications (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    type            VARCHAR(40) NOT NULL,
    title           VARCHAR(150) NOT NULL,
    message         TEXT NOT NULL,
    is_read         BOOLEAN NOT NULL DEFAULT FALSE,
    read_at         TIMESTAMPTZ,
    dedupe_key      VARCHAR(120),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- ON DELETE CASCADE: a notification has no meaning without its owning user.
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_notifications_type CHECK (type IN (
        'PAYMENT_REMINDER', 'OVERSPEND_ALERT', 'WEEKLY_SUMMARY',
        'INACTIVITY_REMINDER', 'MONTH_END_PREDICTION', 'SYSTEM'
    ))
);

CREATE INDEX idx_notifications_user_read ON notifications (user_id, is_read);
CREATE INDEX idx_notifications_user_created ON notifications (user_id, created_at DESC);
-- Partial unique index backing NotificationService#createNotification's dedupe mechanism:
-- Sprint 5 jobs (Batch 3) pass a dedupe_key (e.g. "PAYMENT_REMINDER:debt:12:2026-07") so a job
-- that runs twice for the same (user, target, period) does not create a duplicate notification.
-- NULL dedupe_key values (ad-hoc notifications with no dedupe semantics) are excluded from the
-- uniqueness check since PostgreSQL never considers NULLs equal to each other.
CREATE UNIQUE INDEX uk_notifications_user_dedupe_key ON notifications (user_id, dedupe_key)
    WHERE dedupe_key IS NOT NULL;

CREATE TABLE notification_preferences (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id                 BIGINT NOT NULL,
    payment_reminders       BOOLEAN NOT NULL DEFAULT TRUE,
    overspend_alerts        BOOLEAN NOT NULL DEFAULT TRUE,
    weekly_summary          BOOLEAN NOT NULL DEFAULT TRUE,
    inactivity_reminders    BOOLEAN NOT NULL DEFAULT TRUE,
    email_enabled           BOOLEAN NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- ON DELETE CASCADE: preferences have no meaning without their owning user.
    CONSTRAINT fk_notification_preferences_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_notification_preferences_user UNIQUE (user_id)
);
