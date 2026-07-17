-- =============================================================================
-- DEV-ONLY SEED SCRIPT — NOT a Flyway migration.
-- =============================================================================
-- This file lives under db/dev-seed/ (NOT db/migration/) specifically so
-- Flyway ignores it. It must never run automatically against a shared/prod
-- database — it is meant to be executed manually against a local dev
-- database, to load realistic Sprint 4 data for a single existing user so the
-- financial-analysis dashboard has something real to show.
--
-- HOW TO RUN (manually, against your local dev database only):
--   psql -U <user> -d <database> -f src/main/resources/db/dev-seed/seed_jhon_quiceno.sql
-- or paste its contents into your SQL client of choice.
--
-- BEFORE RUNNING: verify user_id = 2 is actually Jhon Quiceno in your local DB:
--   SELECT id, name, email FROM users WHERE id = 2;
-- If that is not Jhon Quiceno, replace every literal `2` used below as the
-- target user id with the correct one (or create the user first).
--
-- IDEMPOTENCY: this script fully OWNS user 2's financial data. It starts by
-- deleting every income/expense/debt/recurring_payment/financial_analysis
-- row (and, via ON DELETE CASCADE, debt_payments) belonging to user 2, then
-- rebuilds everything from scratch. Safe to re-run any number of times —
-- it will never leave duplicate or near-duplicate rows behind.
--
-- DATA SHAPE — a financially average, mostly healthy earner, not a crisis
-- case (a single very-online-friendly persona, but the amounts and behavior
-- are meant to read as ordinary, not staged):
--   - Categories: "Salario" (INCOME), "Vivienda"/"Comida"/"Transporte"/
--     "Ocio"/"Salud" (EXPENSE).
--   - Income: a steady ~2,850,000 COP monthly salary, plus small freelance
--     income in 2 of the 6 months (side income isn't a monthly certainty for
--     most people). Savings stay positive every month.
--   - Expenses: naturally varying month to month (nobody spends the exact
--     same amount on groceries every month). The current month has a
--     one-off bump in "Comida" (a family event) that pushes it just past
--     30% of that month's income — enough to exercise the food-spending
--     recommendation rule without tipping the whole month into the red.
--   - One modest debt (a credit card balance, not a life-derailing loan).
--     Comparing total outstanding debt (a stock) against a single month's
--     income (a flow) means the >40% debt-ratio alert will still fire most
--     months even for a modest balance — that's inherent to the ratio the
--     sprint spec defines, not a sign the data is unrealistic.
--   - Recurring payment: "Internet hogar" (MONTHLY), paid every one of the
--     6 months via an Expense linked through recurring_payment_id.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- Reset: remove any prior run's data for user 2 before rebuilding.
-- ---------------------------------------------------------------------------
DELETE FROM expenses WHERE user_id = 2;
DELETE FROM incomes WHERE user_id = 2;
DELETE FROM debts WHERE user_id = 2; -- cascades to debt_payments (V4)
DELETE FROM recurring_payments WHERE user_id = 2;
DELETE FROM financial_analysis WHERE user_id = 2;
DELETE FROM categories WHERE user_id = 2;

-- ---------------------------------------------------------------------------
-- Categories
-- ---------------------------------------------------------------------------
INSERT INTO categories (user_id, name, type, created_at, updated_at)
VALUES
    (2, 'Salario', 'INCOME', now(), now()),
    (2, 'Vivienda', 'EXPENSE', now(), now()),
    (2, 'Comida', 'EXPENSE', now(), now()),
    (2, 'Transporte', 'EXPENSE', now(), now()),
    (2, 'Ocio', 'EXPENSE', now(), now()),
    (2, 'Salud', 'EXPENSE', now(), now());

-- ---------------------------------------------------------------------------
-- Recurring payment — Internet hogar, paid every month of the 6-month window.
-- ---------------------------------------------------------------------------
INSERT INTO recurring_payments (user_id, name, amount, frequency, next_payment_date, is_active, created_at, updated_at)
VALUES (
    2, 'Internet hogar', 89900.00, 'MONTHLY',
    (date_trunc('month', CURRENT_DATE) + interval '1 month' + interval '9 days')::date,
    TRUE, now(), now()
);

-- ---------------------------------------------------------------------------
-- Incomes — steady salary every month, freelance only in Mar and May.
-- ---------------------------------------------------------------------------
INSERT INTO incomes (user_id, category_id, amount, description, date, created_at, updated_at)
SELECT 2, (SELECT id FROM categories WHERE user_id = 2 AND name = 'Salario' AND type = 'INCOME'),
       amount, 'Salario mensual', txn_date, now(), now()
FROM (VALUES
    (5, 2850000.00, 1),  -- Feb
    (4, 2850000.00, 1),  -- Mar
    (3, 2850000.00, 1),  -- Abr
    (2, 2850000.00, 1),  -- May
    (1, 2850000.00, 1),  -- Jun
    (0, 2850000.00, 1)   -- Jul (current)
) AS t(months_ago, amount, day)
CROSS JOIN LATERAL (
    SELECT (date_trunc('month', CURRENT_DATE) - (t.months_ago || ' months')::interval + (t.day - 1 || ' days')::interval)::date AS txn_date
) d;

INSERT INTO incomes (user_id, category_id, amount, description, date, created_at, updated_at)
VALUES
    (2, NULL, 350000.00, 'Diseno de logo para un vecino',
        (date_trunc('month', CURRENT_DATE) - interval '4 months' + interval '14 days')::date, now(), now()),
    (2, NULL, 280000.00, 'Soporte tecnico a domicilio',
        (date_trunc('month', CURRENT_DATE) - interval '2 months' + interval '16 days')::date, now(), now());

-- ---------------------------------------------------------------------------
-- Expenses — one row per category per month, amounts vary naturally.
-- Comida in the current month (Jul) is deliberately higher (~31.6% of that
-- month's income) to demonstrate the food-spending recommendation rule
-- without pushing the whole month's expense ratio past 80%.
-- ---------------------------------------------------------------------------
INSERT INTO expenses (user_id, category_id, amount, description, date, payment_method, created_at, updated_at)
SELECT 2, (SELECT id FROM categories WHERE user_id = 2 AND name = row.category AND type = 'EXPENSE'),
       row.amount, row.description,
       (date_trunc('month', CURRENT_DATE) - (row.months_ago || ' months')::interval + (row.day - 1 || ' days')::interval)::date,
       row.payment_method::varchar, now(), now()
FROM (VALUES
    -- Feb (months_ago=5)
    (5, 'Vivienda', 850000.00, 'Arriendo', 3, 'TRANSFER'),
    (5, 'Comida', 620000.00, 'Mercado del mes', 6, 'DEBIT_CARD'),
    (5, 'Transporte', 200000.00, 'Transporte publico y combustible', 9, 'CASH'),
    (5, 'Ocio', 140000.00, 'Cine y salidas', 20, 'CREDIT_CARD'),
    (5, 'Salud', 80000.00, 'Consulta medica', 13, 'DEBIT_CARD'),
    -- Mar (months_ago=4)
    (4, 'Vivienda', 850000.00, 'Arriendo', 3, 'TRANSFER'),
    (4, 'Comida', 680000.00, 'Mercado del mes', 6, 'DEBIT_CARD'),
    (4, 'Transporte', 210000.00, 'Transporte publico y combustible', 9, 'CASH'),
    (4, 'Ocio', 160000.00, 'Cine y salidas', 20, 'CREDIT_CARD'),
    -- Abr (months_ago=3)
    (3, 'Vivienda', 850000.00, 'Arriendo', 3, 'TRANSFER'),
    (3, 'Comida', 700000.00, 'Mercado del mes', 6, 'DEBIT_CARD'),
    (3, 'Transporte', 195000.00, 'Transporte publico y combustible', 9, 'CASH'),
    (3, 'Ocio', 120000.00, 'Cine y salidas', 20, 'CREDIT_CARD'),
    (3, 'Salud', 95000.00, 'Medicamentos', 13, 'DEBIT_CARD'),
    -- May (months_ago=2)
    (2, 'Vivienda', 850000.00, 'Arriendo', 3, 'TRANSFER'),
    (2, 'Comida', 650000.00, 'Mercado del mes', 6, 'DEBIT_CARD'),
    (2, 'Transporte', 205000.00, 'Transporte publico y combustible', 9, 'CASH'),
    (2, 'Ocio', 150000.00, 'Cine y salidas', 20, 'CREDIT_CARD'),
    -- Jun (months_ago=1)
    (1, 'Vivienda', 850000.00, 'Arriendo', 3, 'TRANSFER'),
    (1, 'Comida', 720000.00, 'Mercado del mes', 6, 'DEBIT_CARD'),
    (1, 'Transporte', 215000.00, 'Transporte publico y combustible', 9, 'CASH'),
    (1, 'Ocio', 130000.00, 'Cine y salidas', 20, 'CREDIT_CARD'),
    (1, 'Salud', 70000.00, 'Consulta medica', 13, 'DEBIT_CARD'),
    -- Jul (months_ago=0, current month)
    (0, 'Vivienda', 850000.00, 'Arriendo', 3, 'TRANSFER'),
    (0, 'Comida', 900000.00, 'Mercado del mes y cena de cumpleanos familiar', 6, 'DEBIT_CARD'),
    (0, 'Transporte', 200000.00, 'Transporte publico y combustible', 9, 'CASH'),
    (0, 'Ocio', 150000.00, 'Cine y salidas', 20, 'CREDIT_CARD')
) AS row(months_ago, category, amount, description, day, payment_method);

-- Internet hogar — paid every month, linked to the recurring payment above.
INSERT INTO expenses (user_id, category_id, amount, description, date, payment_method, recurring_payment_id, created_at, updated_at)
SELECT 2, NULL, 89900.00, 'Internet hogar',
       (date_trunc('month', CURRENT_DATE) - (months_ago || ' months')::interval + interval '1 day')::date,
       'OTHER',
       (SELECT id FROM recurring_payments WHERE user_id = 2 AND name = 'Internet hogar' ORDER BY id DESC LIMIT 1),
       now(), now()
FROM generate_series(0, 5) AS months_ago;

-- ---------------------------------------------------------------------------
-- Debt — one modest credit card balance (not a life-derailing loan). See the
-- header note above: comparing this stock against one month's income will
-- still push the debt ratio over 40%, which is expected given the sprint's
-- own ratio definition, not a sign the amount itself is unrealistic.
--
-- remaining_amount here already reflects the two DebtCharge rows below
-- (1,500,000 base + 320,000 + 85,000 = 1,905,000), since this script inserts
-- rows directly instead of going through DebtChargeService.
-- ---------------------------------------------------------------------------
INSERT INTO debts (user_id, name, total_amount, remaining_amount, interest_rate, due_date, created_at, updated_at)
VALUES (2, 'Tarjeta de credito', 2000000.00, 1905000.00, 2.10,
        (CURRENT_DATE + interval '6 months')::date, now(), now());

-- ---------------------------------------------------------------------------
-- Debt charges (Fase A) — two card purchases: one last month, one this month.
-- Demonstrates that a credit card debt can go UP, not just down with payments.
-- ---------------------------------------------------------------------------
INSERT INTO debt_charges (debt_id, amount, charge_date, description, created_at)
SELECT (SELECT id FROM debts WHERE user_id = 2 AND name = 'Tarjeta de credito'),
       amount, charge_date, description, now()
FROM (VALUES
    (320000.00, (date_trunc('month', CURRENT_DATE) - interval '1 month' + interval '17 days')::date,
        'Electrodomestico - Falabella'),
    (85000.00, (date_trunc('month', CURRENT_DATE) + interval '10 days')::date,
        'Mercado con tarjeta')
) AS t(amount, charge_date, description);
