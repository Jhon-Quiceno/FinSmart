ALTER TABLE notifications
    DROP CONSTRAINT IF EXISTS notifications_type_check;

ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS related_entity_type VARCHAR(50);

ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS related_entity_id BIGINT;

ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS scheduled_for TIMESTAMP;

ALTER TABLE notifications
    ADD CONSTRAINT notifications_type_check
    CHECK (type IN ('INFO', 'WARNING', 'ALERT', 'RECOMMENDATION'));

CREATE INDEX IF NOT EXISTS idx_notifications_scheduled_for ON notifications(scheduled_for);
