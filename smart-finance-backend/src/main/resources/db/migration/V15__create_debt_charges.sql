CREATE TABLE debt_charges (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    debt_id         BIGINT NOT NULL,
    amount          NUMERIC(15, 2) NOT NULL,
    charge_date     DATE NOT NULL DEFAULT CURRENT_DATE,
    description     VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- ON DELETE CASCADE: a charge record has no meaning without its owning debt.
    CONSTRAINT fk_debt_charges_debt FOREIGN KEY (debt_id)
        REFERENCES debts (id) ON DELETE CASCADE,
    CONSTRAINT ck_debt_charges_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_debt_charges_debt_id ON debt_charges (debt_id);
