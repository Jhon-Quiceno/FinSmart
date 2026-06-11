-- FinSmart: Consolidated initial schema
-- Drops tables from features that were never implemented (debts, budgets, goals, etc.)
-- Creates only the tables used by the actual Java code

-- ============================================================
-- DROP UNUSED TABLES (from removed features)
-- ============================================================
DROP TABLE IF EXISTS financial_goals CASCADE;
DROP TABLE IF EXISTS budgets CASCADE;
DROP TABLE IF EXISTS debts CASCADE;
DROP TABLE IF EXISTS recurring_payments CASCADE;
DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS incomes CASCADE;
DROP TABLE IF EXISTS expenses CASCADE;

-- ============================================================
-- ENUMS
-- ============================================================
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'account_type') THEN
        CREATE TYPE account_type AS ENUM ('CASH', 'SAVINGS', 'CHECKING', 'CREDIT_CARD', 'DIGITAL_WALLET', 'OTHER');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'transaction_type') THEN
        CREATE TYPE transaction_type AS ENUM ('INCOME', 'EXPENSE', 'TRANSFER');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'payment_method_type') THEN
        CREATE TYPE payment_method_type AS ENUM ('CASH', 'DEBIT_CARD', 'CREDIT_CARD', 'TRANSFER', 'DIGITAL_WALLET');
    END IF;
END $$;

-- ============================================================
-- USERS
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id            BIGSERIAL    PRIMARY KEY,
    name          VARCHAR(120) NOT NULL,
    email         VARCHAR(180) NOT NULL UNIQUE,
    password_hash VARCHAR(120) NOT NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMP,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ============================================================
-- REFRESH TOKENS
-- ============================================================
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    token_id    UUID        NOT NULL UNIQUE,
    user_id     BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(120) NOT NULL UNIQUE,
    expires_at  TIMESTAMP   NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    revoked_at  TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

-- ============================================================
-- CATEGORIES
-- ============================================================
CREATE TABLE IF NOT EXISTS categories (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       REFERENCES users(id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    type        VARCHAR(20)  NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    icon        VARCHAR(50),
    color       VARCHAR(7),
    description VARCHAR(255),
    is_system   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, name)
);

CREATE INDEX IF NOT EXISTS idx_categories_user_id ON categories(user_id);
CREATE INDEX IF NOT EXISTS idx_categories_type ON categories(type);
CREATE INDEX IF NOT EXISTS idx_categories_is_system ON categories(is_system);

-- ============================================================
-- ACCOUNTS
-- ============================================================
CREATE TABLE IF NOT EXISTS accounts (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name       VARCHAR(100) NOT NULL,
    type       account_type NOT NULL,
    balance    NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    currency   VARCHAR(3)   NOT NULL DEFAULT 'COP',
    color      VARCHAR(7),
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_accounts_user_id ON accounts(user_id);

-- ============================================================
-- TRANSACTIONS
-- ============================================================
CREATE TABLE IF NOT EXISTS transactions (
    id                        BIGSERIAL PRIMARY KEY,
    user_id                   BIGINT            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_id                BIGINT            REFERENCES accounts(id) ON DELETE SET NULL,
    category_id               BIGINT            REFERENCES categories(id) ON DELETE SET NULL,
    type                      transaction_type  NOT NULL,
    amount                    NUMERIC(15, 2)    NOT NULL CHECK (amount > 0),
    description               VARCHAR(255),
    transaction_date          DATE              NOT NULL,
    payment_method            payment_method_type,
    income_source_name        VARCHAR(100),
    expense_payment_method_name VARCHAR(100),
    expense_type_name         VARCHAR(100),
    notes                     TEXT,
    created_at                TIMESTAMP         NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMP         NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_transactions_user_id ON transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_transactions_type ON transactions(type);
CREATE INDEX IF NOT EXISTS idx_transactions_transaction_date ON transactions(transaction_date);
CREATE INDEX IF NOT EXISTS idx_transactions_category_id ON transactions(category_id);
CREATE INDEX IF NOT EXISTS idx_transactions_account_id ON transactions(account_id);

-- ============================================================
-- SEED: Default system categories
-- ============================================================
INSERT INTO categories (user_id, name, type, icon, color, is_system, created_at, updated_at)
SELECT NULL, 'Salario', 'INCOME', 'wallet', '#22C55E', TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Salario' AND is_system = TRUE);

INSERT INTO categories (user_id, name, type, icon, color, is_system, created_at, updated_at)
SELECT NULL, 'Freelance', 'INCOME', 'briefcase', '#10B981', TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Freelance' AND is_system = TRUE);

INSERT INTO categories (user_id, name, type, icon, color, is_system, created_at, updated_at)
SELECT NULL, 'Alimentación', 'EXPENSE', 'utensils', '#F59E0B', TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Alimentación' AND is_system = TRUE);

INSERT INTO categories (user_id, name, type, icon, color, is_system, created_at, updated_at)
SELECT NULL, 'Transporte', 'EXPENSE', 'car', '#3B82F6', TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Transporte' AND is_system = TRUE);

INSERT INTO categories (user_id, name, type, icon, color, is_system, created_at, updated_at)
SELECT NULL, 'Entretenimiento', 'EXPENSE', 'film', '#8B5CF6', TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Entretenimiento' AND is_system = TRUE);

INSERT INTO categories (user_id, name, type, icon, color, is_system, created_at, updated_at)
SELECT NULL, 'Salud', 'EXPENSE', 'heart-pulse', '#EF4444', TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Salud' AND is_system = TRUE);

INSERT INTO categories (user_id, name, type, icon, color, is_system, created_at, updated_at)
SELECT NULL, 'Educación', 'EXPENSE', 'book-open', '#6366F1', TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Educación' AND is_system = TRUE);

INSERT INTO categories (user_id, name, type, icon, color, is_system, created_at, updated_at)
SELECT NULL, 'Servicios', 'EXPENSE', 'receipt', '#06B6D4', TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Servicios' AND is_system = TRUE);

INSERT INTO categories (user_id, name, type, icon, color, is_system, created_at, updated_at)
SELECT NULL, 'Ropa', 'EXPENSE', 'shirt', '#EC4899', TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Ropa' AND is_system = TRUE);

-- ============================================================
-- SEED: Default "Efectivo" account for each user
-- ============================================================
INSERT INTO accounts (user_id, name, type, balance, currency, is_active, created_at, updated_at)
SELECT u.id, 'Efectivo', 'CASH'::account_type, 0.00, 'COP', TRUE, NOW(), NOW()
FROM users u
WHERE NOT EXISTS (
    SELECT 1 FROM accounts a WHERE a.user_id = u.id AND a.name = 'Efectivo'
);
