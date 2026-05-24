INSERT INTO accounts (
    user_id,
    name,
    type,
    balance,
    currency,
    is_active,
    created_at,
    updated_at
)
SELECT
    u.id,
    'Efectivo',
    'CASH'::account_type,
    0.00,
    'COP',
    TRUE,
    NOW(),
    NOW()
FROM users u
WHERE NOT EXISTS (
    SELECT 1
    FROM accounts a
    WHERE a.user_id = u.id
      AND a.name = 'Efectivo'
);
