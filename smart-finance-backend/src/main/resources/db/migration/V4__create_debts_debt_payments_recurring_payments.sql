CREATE TABLE debts (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    name                VARCHAR(150) NOT NULL,
    total_amount        NUMERIC(15, 2) NOT NULL,
    remaining_amount    NUMERIC(15, 2) NOT NULL,
    interest_rate       NUMERIC(5, 2),
    due_date            DATE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- ON DELETE CASCADE: a debt has no meaning without its owning user.
    CONSTRAINT fk_debts_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_debts_total_amount_positive CHECK (total_amount > 0),
    CONSTRAINT ck_debts_remaining_amount_non_negative CHECK (remaining_amount >= 0)
);

CREATE INDEX idx_debts_user_id ON debts (user_id);

CREATE TABLE debt_payments (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    debt_id         BIGINT NOT NULL,
    amount          NUMERIC(15, 2) NOT NULL,
    payment_date    DATE NOT NULL DEFAULT CURRENT_DATE,
    note            VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- ON DELETE CASCADE: a payment record has no meaning without its owning debt.
    CONSTRAINT fk_debt_payments_debt FOREIGN KEY (debt_id)
        REFERENCES debts (id) ON DELETE CASCADE,
    CONSTRAINT ck_debt_payments_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_debt_payments_debt_id ON debt_payments (debt_id);

CREATE TABLE recurring_payments (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    name                VARCHAR(150) NOT NULL,
    amount              NUMERIC(15, 2) NOT NULL,
    frequency           VARCHAR(20) NOT NULL,
    next_payment_date   DATE NOT NULL,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- ON DELETE CASCADE: a recurring payment has no meaning without its owning user.
    CONSTRAINT fk_recurring_payments_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_recurring_payments_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_recurring_payments_frequency CHECK (frequency IN ('MONTHLY', 'WEEKLY'))
);

CREATE INDEX idx_recurring_payments_user_id ON recurring_payments (user_id);
-- Supports Sprint 5's cron reminder job, which will scan for recurring payments due soon/today.
CREATE INDEX idx_recurring_payments_next_date ON recurring_payments (next_payment_date);
