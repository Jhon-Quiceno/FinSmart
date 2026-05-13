-- FinSmart Sprint 1: Initial Schema
-- Uses IF NOT EXISTS because the 'users' table already exists from Hibernate ddl-auto:update

-- ============================================================
-- USERS (already exists — IF NOT EXISTS makes this idempotent)
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id            BIGSERIAL    PRIMARY KEY,
    name          VARCHAR(120) NOT NULL,
    email         VARCHAR(180) NOT NULL UNIQUE,
    password_hash VARCHAR(120) NOT NULL,
    created_at    TIMESTAMP    DEFAULT NOW(),
    updated_at    TIMESTAMP    DEFAULT NOW()
);

-- ============================================================
-- CATEGORIES
-- ============================================================
CREATE TABLE IF NOT EXISTS categories (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name       VARCHAR(100) NOT NULL,
    type       VARCHAR(20)  NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    icon       VARCHAR(50),
    color      VARCHAR(7),
    created_at TIMESTAMP    DEFAULT NOW(),
    updated_at TIMESTAMP    DEFAULT NOW(),
    UNIQUE(user_id, name)
);

CREATE INDEX IF NOT EXISTS idx_categories_user_id ON categories(user_id);
CREATE INDEX IF NOT EXISTS idx_categories_type     ON categories(type);

-- ============================================================
-- INCOMES
-- ============================================================
CREATE TABLE IF NOT EXISTS incomes (
    id          BIGSERIAL      PRIMARY KEY,
    user_id     BIGINT         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id BIGINT         REFERENCES categories(id) ON DELETE SET NULL,
    amount      DECIMAL(15, 2) NOT NULL,
    description VARCHAR(255),
    date        DATE           NOT NULL,
    is_recurring BOOLEAN       DEFAULT FALSE,
    created_at  TIMESTAMP      DEFAULT NOW(),
    updated_at  TIMESTAMP      DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_incomes_user_id     ON incomes(user_id);
CREATE INDEX IF NOT EXISTS idx_incomes_category_id ON incomes(category_id);
CREATE INDEX IF NOT EXISTS idx_incomes_date        ON incomes(date);

-- ============================================================
-- EXPENSES
-- ============================================================
CREATE TABLE IF NOT EXISTS expenses (
    id          BIGSERIAL      PRIMARY KEY,
    user_id     BIGINT         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id BIGINT         REFERENCES categories(id) ON DELETE SET NULL,
    amount      DECIMAL(15, 2) NOT NULL,
    description VARCHAR(255),
    date        DATE           NOT NULL,
    is_recurring BOOLEAN       DEFAULT FALSE,
    created_at  TIMESTAMP      DEFAULT NOW(),
    updated_at  TIMESTAMP      DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_expenses_user_id     ON expenses(user_id);
CREATE INDEX IF NOT EXISTS idx_expenses_category_id ON expenses(category_id);
CREATE INDEX IF NOT EXISTS idx_expenses_date        ON expenses(date);

-- ============================================================
-- DEBTS
-- ============================================================
CREATE TABLE IF NOT EXISTS debts (
    id               BIGSERIAL      PRIMARY KEY,
    user_id          BIGINT         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name             VARCHAR(150)   NOT NULL,
    total_amount     DECIMAL(15, 2) NOT NULL,
    remaining_amount DECIMAL(15, 2) NOT NULL,
    interest_rate    DECIMAL(5, 2),
    minimum_payment  DECIMAL(15, 2),
    due_date         DATE,
    status           VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'PAID', 'DEFAULTED')),
    created_at       TIMESTAMP      DEFAULT NOW(),
    updated_at       TIMESTAMP      DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_debts_user_id ON debts(user_id);
CREATE INDEX IF NOT EXISTS idx_debts_status  ON debts(status);

-- ============================================================
-- RECURRING PAYMENTS
-- ============================================================
CREATE TABLE IF NOT EXISTS recurring_payments (
    id          BIGSERIAL      PRIMARY KEY,
    user_id     BIGINT         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id BIGINT         REFERENCES categories(id) ON DELETE SET NULL,
    name        VARCHAR(150)   NOT NULL,
    amount      DECIMAL(15, 2) NOT NULL,
    frequency   VARCHAR(20)    NOT NULL CHECK (frequency IN ('WEEKLY', 'BIWEEKLY', 'MONTHLY', 'YEARLY')),
    next_date   DATE           NOT NULL,
    is_active   BOOLEAN        DEFAULT TRUE,
    created_at  TIMESTAMP      DEFAULT NOW(),
    updated_at  TIMESTAMP      DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_recurring_payments_user_id ON recurring_payments(user_id);
CREATE INDEX IF NOT EXISTS idx_recurring_payments_next_date ON recurring_payments(next_date);

-- ============================================================
-- NOTIFICATIONS
-- ============================================================
CREATE TABLE IF NOT EXISTS notifications (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title      VARCHAR(150) NOT NULL,
    message    TEXT,
    type       VARCHAR(30)  NOT NULL CHECK (type IN ('INFO', 'WARNING', 'ALERT')),
    is_read    BOOLEAN      DEFAULT FALSE,
    created_at TIMESTAMP    DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_id  ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_is_read  ON notifications(is_read);
CREATE INDEX IF NOT EXISTS idx_notifications_type     ON notifications(type);
