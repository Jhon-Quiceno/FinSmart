DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'transaction_type') THEN
        CREATE TYPE transaction_type AS ENUM (
            'INCOME',
            'EXPENSE',
            'TRANSFER'
        );
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'payment_method_type') THEN
        CREATE TYPE payment_method_type AS ENUM (
            'CASH',
            'DEBIT_CARD',
            'CREDIT_CARD',
            'TRANSFER',
            'DIGITAL_WALLET'
        );
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS transactions (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_id       BIGINT            REFERENCES accounts(id) ON DELETE SET NULL,
    category_id      BIGINT            REFERENCES categories(id) ON DELETE SET NULL,
    type             transaction_type  NOT NULL,
    amount           NUMERIC(15, 2)    NOT NULL CHECK (amount > 0),
    description      VARCHAR(255),
    transaction_date DATE              NOT NULL,
    payment_method   payment_method_type,
    notes            TEXT,
    created_at       TIMESTAMP         NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP         NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_transactions_user_id ON transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_transactions_type ON transactions(type);
CREATE INDEX IF NOT EXISTS idx_transactions_transaction_date ON transactions(transaction_date);
CREATE INDEX IF NOT EXISTS idx_transactions_category_id ON transactions(category_id);
CREATE INDEX IF NOT EXISTS idx_transactions_account_id ON transactions(account_id);
