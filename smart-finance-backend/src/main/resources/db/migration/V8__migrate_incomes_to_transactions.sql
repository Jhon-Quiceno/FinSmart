INSERT INTO transactions (
    user_id,
    account_id,
    category_id,
    type,
    amount,
    description,
    transaction_date,
    payment_method,
    notes,
    created_at,
    updated_at
)
SELECT
    user_id,
    NULL,
    category_id,
    'INCOME',
    amount,
    description,
    date,
    NULL,
    source,
    created_at,
    updated_at
FROM incomes;
