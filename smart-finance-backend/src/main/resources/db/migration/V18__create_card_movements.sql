CREATE TABLE card_movements (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    card_id             BIGINT NOT NULL,
    type                VARCHAR(25) NOT NULL,
    amount              NUMERIC(15, 2) NOT NULL,
    -- Named "movement_date" (not the bare "date") to match this project's convention of
    -- prefixing date columns with their domain meaning (see charge_date/payment_date on
    -- debt_charges/debt_payments) and to avoid a bare "date" identifier.
    movement_date       DATE NOT NULL DEFAULT CURRENT_DATE,
    description         VARCHAR(255),
    -- Set only on aggregated INTEREST movements created by CardCycleCloseJob (Fase B.4); used
    -- for cycle-close idempotency/audit.
    cycle_close_date    DATE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- ON DELETE CASCADE: a movement has no meaning without its owning card.
    CONSTRAINT fk_card_movements_card FOREIGN KEY (card_id)
        REFERENCES credit_cards (id) ON DELETE CASCADE,
    CONSTRAINT ck_card_movements_type
        CHECK (type IN ('PURCHASE', 'INSTALLMENT_PURCHASE', 'PAYMENT', 'INTEREST', 'FEE')),
    CONSTRAINT ck_card_movements_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_card_movements_card_date ON card_movements (card_id, movement_date);
