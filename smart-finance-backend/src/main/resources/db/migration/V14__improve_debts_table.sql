DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'debt_type') THEN
        CREATE TYPE debt_type AS ENUM (
            'CREDIT_CARD',
            'PERSONAL_LOAN',
            'STUDENT_LOAN',
            'MORTGAGE',
            'FAMILY',
            'OTHER'
        );
    END IF;
END $$;

ALTER TABLE debts
    ADD COLUMN IF NOT EXISTS debt_type debt_type NOT NULL DEFAULT 'OTHER';

ALTER TABLE debts
    ADD COLUMN IF NOT EXISTS creditor_name VARCHAR(150);

ALTER TABLE debts
    ADD COLUMN IF NOT EXISTS start_date DATE;
