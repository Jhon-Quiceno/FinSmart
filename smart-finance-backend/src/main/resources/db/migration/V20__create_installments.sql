CREATE TABLE installments (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    plan_id                 BIGINT NOT NULL,
    number                  INT NOT NULL,
    capital_amount          NUMERIC(15, 2) NOT NULL,
    interest_amount         NUMERIC(15, 2) NOT NULL,
    due_date                DATE NOT NULL,
    status                  VARCHAR(15) NOT NULL DEFAULT 'PENDING',
    -- Set by CardCycleCloseJob (Fase B.4) when this installment's interest is materialized into
    -- an aggregated INTEREST card_movement; NULL while PENDING.
    interest_movement_id    BIGINT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- ON DELETE CASCADE: an installment has no meaning without its owning plan.
    CONSTRAINT fk_installments_plan FOREIGN KEY (plan_id)
        REFERENCES installment_plans (id) ON DELETE CASCADE,
    -- ON DELETE SET NULL: keeps the (already billed) installment row intact if its aggregated
    -- INTEREST movement is ever removed, mirroring V21's fk_expenses_card_movement precedent.
    CONSTRAINT fk_installments_interest_movement FOREIGN KEY (interest_movement_id)
        REFERENCES card_movements (id) ON DELETE SET NULL,
    CONSTRAINT ck_installments_status CHECK (status IN ('PENDING', 'BILLED')),
    CONSTRAINT ck_installments_number_positive CHECK (number > 0),
    CONSTRAINT ck_installments_capital_amount_positive CHECK (capital_amount > 0),
    CONSTRAINT ck_installments_interest_amount_non_negative CHECK (interest_amount >= 0)
);

CREATE INDEX idx_installments_plan_status_due ON installments (plan_id, status, due_date);
