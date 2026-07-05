-- Composite index replaces the single-column idx_debt_payments_debt_id from V4: ReportService
-- (Sprint 6) and any future per-debt payment history view filter by (debt_id, payment_date)
-- together, never by debt_id alone, so the composite index serves both that access pattern and
-- plain debt_id lookups, making the old single-column index redundant (same rationale as
-- V10__add_performance_indexes.sql's expenses/incomes composite indexes).
DROP INDEX IF EXISTS idx_debt_payments_debt_id;
CREATE INDEX idx_debt_payments_debt_date ON debt_payments (debt_id, payment_date);
