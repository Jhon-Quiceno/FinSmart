ALTER TABLE expenses ADD COLUMN debt_payment_id BIGINT;

-- ON DELETE SET NULL: deleting a debt payment must not delete the expense history it
-- generated; the expense keeps its record but loses the debt-payment link.
ALTER TABLE expenses ADD CONSTRAINT fk_expenses_debt_payment
    FOREIGN KEY (debt_payment_id) REFERENCES debt_payments (id) ON DELETE SET NULL;

CREATE INDEX idx_expenses_debt_payment_id ON expenses (debt_payment_id);
