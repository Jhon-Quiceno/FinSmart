CREATE TABLE financial_analysis (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    period_year         INT NOT NULL,
    period_month        INT NOT NULL,
    total_income        NUMERIC(15, 2) NOT NULL,
    total_expense       NUMERIC(15, 2) NOT NULL,
    savings             NUMERIC(15, 2) NOT NULL,
    expense_ratio       NUMERIC(8, 4) NOT NULL,
    debt_ratio          NUMERIC(8, 4) NOT NULL,
    top_category_id     BIGINT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- ON DELETE CASCADE: a monthly snapshot has no meaning without its owning user.
    CONSTRAINT fk_financial_analysis_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    -- ON DELETE SET NULL: deleting the top category must not delete the historical snapshot
    -- that referenced it, mirroring expenses/incomes category FK behavior (V3).
    CONSTRAINT fk_financial_analysis_top_category FOREIGN KEY (top_category_id)
        REFERENCES categories (id) ON DELETE SET NULL,
    CONSTRAINT uk_financial_analysis_user_period UNIQUE (user_id, period_year, period_month),
    CONSTRAINT ck_financial_analysis_month CHECK (period_month BETWEEN 1 AND 12),
    CONSTRAINT ck_financial_analysis_year CHECK (period_year BETWEEN 2000 AND 2100)
);

CREATE INDEX idx_financial_analysis_user_id ON financial_analysis (user_id);
