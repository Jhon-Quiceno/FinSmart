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
    'EXPENSE',
    amount,
    description,
    date,
    CASE
        WHEN payment_method IS NULL THEN NULL
        WHEN UPPER(payment_method) LIKE '%EFECTIVO%' OR UPPER(payment_method) LIKE '%CASH%' THEN 'CASH'
        WHEN UPPER(payment_method) LIKE '%DEBIT%' OR UPPER(payment_method) LIKE '%DÉBIT%' OR UPPER(payment_method) LIKE '%DEBITO%' THEN 'DEBIT_CARD'
        WHEN UPPER(payment_method) LIKE '%CREDIT%' OR UPPER(payment_method) LIKE '%CRÉDIT%' OR UPPER(payment_method) LIKE '%CREDITO%' THEN 'CREDIT_CARD'
        WHEN UPPER(payment_method) LIKE '%TRANSFER%' THEN 'TRANSFER'
        WHEN UPPER(payment_method) LIKE '%NEQUI%' OR UPPER(payment_method) LIKE '%DAVIPLATA%' OR UPPER(payment_method) LIKE '%WALLET%' THEN 'DIGITAL_WALLET'
        ELSE NULL
    END::payment_method_type,
    CASE
        WHEN payment_method IS NULL THEN NULL
        WHEN UPPER(payment_method) LIKE '%EFECTIVO%' OR UPPER(payment_method) LIKE '%CASH%' THEN NULL
        WHEN UPPER(payment_method) LIKE '%DEBIT%' OR UPPER(payment_method) LIKE '%DÉBIT%' OR UPPER(payment_method) LIKE '%DEBITO%' THEN NULL
        WHEN UPPER(payment_method) LIKE '%CREDIT%' OR UPPER(payment_method) LIKE '%CRÉDIT%' OR UPPER(payment_method) LIKE '%CREDITO%' THEN NULL
        WHEN UPPER(payment_method) LIKE '%TRANSFER%' THEN NULL
        WHEN UPPER(payment_method) LIKE '%NEQUI%' OR UPPER(payment_method) LIKE '%DAVIPLATA%' OR UPPER(payment_method) LIKE '%WALLET%' THEN NULL
        ELSE payment_method
    END,
    created_at,
    updated_at
FROM expenses;
