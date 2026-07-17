CREATE TABLE installment_plans (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    movement_id         BIGINT NOT NULL,
    installment_count   INT NOT NULL,
    -- Frozen copy of CreditCard.monthly_rate at purchase time; never recalculated if the card's
    -- rate changes afterwards (spec "Tasa congelada ante cambios posteriores").
    rate_at_purchase    NUMERIC(6, 4) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- ON DELETE CASCADE: a plan has no meaning without its originating movement.
    CONSTRAINT fk_installment_plans_movement FOREIGN KEY (movement_id)
        REFERENCES card_movements (id) ON DELETE CASCADE,
    -- UNIQUE also provides the lookup index on movement_id, so no separate CREATE INDEX is
    -- needed (Postgres auto-indexes UNIQUE constraints).
    CONSTRAINT uq_installment_plans_movement UNIQUE (movement_id),
    CONSTRAINT ck_installment_plans_installment_count CHECK (installment_count >= 2),
    CONSTRAINT ck_installment_plans_rate_non_negative CHECK (rate_at_purchase >= 0)
);
