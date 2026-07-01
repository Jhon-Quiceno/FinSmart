CREATE TABLE categories (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    name            VARCHAR(100) NOT NULL,
    type            VARCHAR(20) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ,
    -- ON DELETE CASCADE: a category has no meaning without its owning user.
    CONSTRAINT fk_categories_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_categories_user_name_type UNIQUE (user_id, name, type),
    CONSTRAINT ck_categories_type CHECK (type IN ('INCOME', 'EXPENSE'))
);

CREATE INDEX idx_categories_user_id ON categories (user_id);

CREATE TABLE incomes (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    category_id     BIGINT,
    amount          NUMERIC(15, 2) NOT NULL,
    description     VARCHAR(255),
    date            DATE NOT NULL,
    source          VARCHAR(100),
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ,
    -- ON DELETE CASCADE: income history has no meaning without its owning user.
    CONSTRAINT fk_incomes_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    -- ON DELETE SET NULL: deleting a category must not delete the income history that
    -- referenced it; the income becomes unclassified instead.
    CONSTRAINT fk_incomes_category FOREIGN KEY (category_id)
        REFERENCES categories (id) ON DELETE SET NULL,
    CONSTRAINT ck_incomes_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_incomes_user_id ON incomes (user_id);
CREATE INDEX idx_incomes_date ON incomes (date);

CREATE TABLE expenses (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    category_id     BIGINT,
    amount          NUMERIC(15, 2) NOT NULL,
    description     VARCHAR(255),
    date            DATE NOT NULL,
    payment_method  VARCHAR(20) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ,
    -- ON DELETE CASCADE: expense history has no meaning without its owning user.
    CONSTRAINT fk_expenses_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    -- ON DELETE SET NULL: deleting a category must not delete the expense history that
    -- referenced it; the expense becomes unclassified instead.
    CONSTRAINT fk_expenses_category FOREIGN KEY (category_id)
        REFERENCES categories (id) ON DELETE SET NULL,
    CONSTRAINT ck_expenses_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_expenses_payment_method CHECK (payment_method IN ('CASH', 'DEBIT_CARD', 'CREDIT_CARD', 'TRANSFER', 'OTHER'))
);

CREATE INDEX idx_expenses_user_id ON expenses (user_id);
CREATE INDEX idx_expenses_date ON expenses (date);
