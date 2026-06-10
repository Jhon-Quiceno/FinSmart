-- ============================================================
-- V22: Simplify lookup tables - Add VARCHAR columns to transactions
-- Replaces 3 FK columns (income_source_id, expense_payment_method_id, expense_type_id)
-- with direct string columns (income_source_name, expense_payment_method_name, expense_type_name)
-- ============================================================

-- Add new VARCHAR columns to transactions table
ALTER TABLE transactions 
    ADD COLUMN IF NOT EXISTS income_source_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS expense_payment_method_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS expense_type_name VARCHAR(100);

-- Migrate existing data from FK columns to new VARCHAR columns
-- Income sources
UPDATE transactions t
SET income_source_name = s.name
FROM income_sources s
WHERE t.income_source_id = s.id;

-- Expense payment methods
UPDATE transactions t
SET expense_payment_method_name = m.name
FROM expense_payment_methods m
WHERE t.expense_payment_method_id = m.id;

-- Expense types
UPDATE transactions t
SET expense_type_name = e.name
FROM expense_types e
WHERE t.expense_type_id = e.id;

-- Create indexes on new VARCHAR columns for query performance
CREATE INDEX IF NOT EXISTS idx_transactions_income_source_name ON transactions(income_source_name);
CREATE INDEX IF NOT EXISTS idx_transactions_expense_payment_method_name ON transactions(expense_payment_method_name);
CREATE INDEX IF NOT EXISTS idx_transactions_expense_type_name ON transactions(expense_type_name);

-- Drop old FK columns (they will be removed in the application code)
-- Note: We keep them in DB for rollback safety, but application no longer uses them
-- ALTER TABLE transactions DROP COLUMN IF EXISTS income_source_id;
-- ALTER TABLE transactions DROP COLUMN IF EXISTS expense_payment_method_id;
-- ALTER TABLE transactions DROP COLUMN IF NOT EXISTS expense_type_id;
