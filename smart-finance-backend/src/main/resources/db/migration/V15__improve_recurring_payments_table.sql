ALTER TABLE recurring_payments
    DROP CONSTRAINT IF EXISTS recurring_payments_frequency_check;

ALTER TABLE recurring_payments
    ADD COLUMN IF NOT EXISTS account_id BIGINT REFERENCES accounts(id) ON DELETE SET NULL;

ALTER TABLE recurring_payments
    ADD COLUMN IF NOT EXISTS payment_method payment_method_type;

ALTER TABLE recurring_payments
    ADD COLUMN IF NOT EXISTS end_date DATE;

ALTER TABLE recurring_payments
    ADD COLUMN IF NOT EXISTS reminder_days_before INT NOT NULL DEFAULT 3;

ALTER TABLE recurring_payments
    ADD CONSTRAINT recurring_payments_frequency_check
    CHECK (frequency IN ('DAILY', 'WEEKLY', 'BIWEEKLY', 'MONTHLY', 'YEARLY'));

CREATE INDEX IF NOT EXISTS idx_recurring_payments_is_active ON recurring_payments(is_active);
