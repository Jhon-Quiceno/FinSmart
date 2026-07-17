CREATE TABLE credit_cards (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    name                VARCHAR(150) NOT NULL,
    bank                VARCHAR(100),
    franchise           VARCHAR(20) NOT NULL,
    credit_limit        NUMERIC(15, 2) NOT NULL,
    monthly_rate        NUMERIC(6, 4) NOT NULL,
    cutoff_day          INT NOT NULL,
    payment_due_day     INT NOT NULL,
    current_balance     NUMERIC(15, 2) NOT NULL DEFAULT 0,
    last_cutoff_date    DATE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- ON DELETE CASCADE: a credit card has no meaning without its owning user.
    CONSTRAINT fk_credit_cards_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_credit_cards_franchise CHECK (franchise IN ('VISA', 'MASTERCARD', 'AMEX', 'DINERS')),
    CONSTRAINT ck_credit_cards_credit_limit_positive CHECK (credit_limit > 0),
    CONSTRAINT ck_credit_cards_monthly_rate_non_negative CHECK (monthly_rate >= 0),
    CONSTRAINT ck_credit_cards_cutoff_day CHECK (cutoff_day BETWEEN 1 AND 31),
    CONSTRAINT ck_credit_cards_payment_due_day CHECK (payment_due_day BETWEEN 1 AND 31),
    CONSTRAINT ck_credit_cards_balance CHECK (current_balance >= 0)
);

CREATE INDEX idx_credit_cards_user ON credit_cards (user_id);
-- Supports CardCycleCloseJob's cross-user scan for cards whose cutoff day is today (Fase B.4).
CREATE INDEX idx_credit_cards_cutoff_day ON credit_cards (cutoff_day);
