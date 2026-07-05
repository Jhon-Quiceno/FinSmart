-- Composite indexes replace the single-column date indexes from V3: Sprint 5's jobs (Batch 3)
-- and the Sprint 4 financial analysis engine always filter by (user_id, date) together, never
-- by date alone, so the composite index serves both that access pattern and plain user_id
-- lookups, making the old single-column date indexes redundant.
DROP INDEX IF EXISTS idx_expenses_date;
CREATE INDEX idx_expenses_user_date ON expenses (user_id, date);

DROP INDEX IF EXISTS idx_incomes_date;
CREATE INDEX idx_incomes_user_date ON incomes (user_id, date);

-- Supports PaymentReminderJob (Sprint 5, Batch 3): scans active recurring payments due soon.
CREATE INDEX idx_recurring_payments_active_next_date ON recurring_payments (is_active, next_payment_date);

-- Supports PaymentReminderJob (Sprint 5, Batch 3): scans debts due soon per user.
CREATE INDEX idx_debts_user_due_date ON debts (user_id, due_date);
