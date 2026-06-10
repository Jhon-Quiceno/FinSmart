-- ============================================================
-- V23: Rollback V22 - Simplify lookup tables
-- Restores the 3 FK columns and removes the VARCHAR columns
-- ============================================================

-- Drop indexes on VARCHAR columns
DROP INDEX IF EXISTS idx_transactions_income_source_name;
DROP INDEX IF EXISTS idx_transactions_expense_payment_method_name;
DROP INDEX IF EXISTS idx_transactions_expense_type_name;

-- Drop VARCHAR columns
ALTER TABLE transactions DROP COLUMN IF EXISTS income_source_name;
ALTER TABLE transactions DROP COLUMN IF EXISTS expense_payment_method_name;
ALTER TABLE transactions DROP COLUMN IF EXISTS expense_type_name;

-- Note: The FK columns (income_source_id, expense_payment_method_id, expense_type_id)
-- were not dropped in V22, so they remain intact for rollback
