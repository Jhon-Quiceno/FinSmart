-- Fase B.4: CardCycleCloseJob dispatches a CARD_CYCLE_CLOSE notification when a credit card's
-- billing cycle closes. Extends the notifications CHECK constraint and adds the matching
-- preference toggle, mirroring V7__create_notifications_and_preferences.sql's other types.
ALTER TABLE notifications DROP CONSTRAINT ck_notifications_type;
ALTER TABLE notifications ADD CONSTRAINT ck_notifications_type CHECK (type IN (
    'PAYMENT_REMINDER', 'OVERSPEND_ALERT', 'WEEKLY_SUMMARY',
    'INACTIVITY_REMINDER', 'MONTH_END_PREDICTION', 'SYSTEM', 'CARD_CYCLE_CLOSE'
));

ALTER TABLE notification_preferences ADD COLUMN card_cycle_close BOOLEAN NOT NULL DEFAULT TRUE;
