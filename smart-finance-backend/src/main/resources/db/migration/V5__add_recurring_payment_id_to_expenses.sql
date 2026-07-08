ALTER TABLE expenses ADD COLUMN recurring_payment_id BIGINT;

-- ON DELETE SET NULL: deleting a recurring payment must not delete the expense history it
-- generated; the expense keeps its record but loses the recurring-payment link.
ALTER TABLE expenses ADD CONSTRAINT fk_expenses_recurring_payment
    FOREIGN KEY (recurring_payment_id) REFERENCES recurring_payments (id) ON DELETE SET NULL;

CREATE INDEX idx_expenses_recurring_payment_id ON expenses (recurring_payment_id);
