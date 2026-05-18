ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS is_system BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE categories
    ALTER COLUMN user_id DROP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_categories_is_system ON categories(is_system);

INSERT INTO categories (user_id, name, type, icon, color, is_system, created_at, updated_at)
VALUES
    (NULL, 'Salario', 'INCOME', 'wallet', '#22C55E', TRUE, NOW(), NOW()),
    (NULL, 'Freelance', 'INCOME', 'briefcase', '#10B981', TRUE, NOW(), NOW()),
    (NULL, 'Alimentación', 'EXPENSE', 'utensils', '#F59E0B', TRUE, NOW(), NOW()),
    (NULL, 'Transporte', 'EXPENSE', 'car', '#3B82F6', TRUE, NOW(), NOW()),
    (NULL, 'Entretenimiento', 'EXPENSE', 'film', '#8B5CF6', TRUE, NOW(), NOW()),
    (NULL, 'Salud', 'EXPENSE', 'heart-pulse', '#EF4444', TRUE, NOW(), NOW()),
    (NULL, 'Educación', 'EXPENSE', 'book-open', '#6366F1', TRUE, NOW(), NOW()),
    (NULL, 'Servicios', 'EXPENSE', 'receipt', '#06B6D4', TRUE, NOW(), NOW()),
    (NULL, 'Ropa', 'EXPENSE', 'shirt', '#EC4899', TRUE, NOW(), NOW());
