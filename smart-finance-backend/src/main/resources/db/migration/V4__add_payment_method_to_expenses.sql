ALTER TABLE expenses
    ADD COLUMN IF NOT EXISTS payment_method VARCHAR(30);
