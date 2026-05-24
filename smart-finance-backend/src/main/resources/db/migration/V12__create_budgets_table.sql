CREATE TABLE IF NOT EXISTS budgets (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id BIGINT        REFERENCES categories(id) ON DELETE SET NULL,
    name        VARCHAR(100)  NOT NULL,
    amount      NUMERIC(15, 2) NOT NULL,
    period      VARCHAR(20)   NOT NULL DEFAULT 'MONTHLY'
                 CHECK (period IN ('WEEKLY', 'MONTHLY', 'YEARLY')),
    start_date  DATE          NOT NULL,
    end_date    DATE,
    is_active   BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_budgets_user_id ON budgets(user_id);
CREATE INDEX IF NOT EXISTS idx_budgets_category_id ON budgets(category_id);
