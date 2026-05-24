DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'account_type') THEN
        CREATE TYPE account_type AS ENUM (
            'CASH',
            'SAVINGS',
            'CHECKING',
            'CREDIT_CARD',
            'DIGITAL_WALLET',
            'OTHER'
        );
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS accounts (
    id         BIGSERIAL PRIMARY KEY,
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
