-- Preferencias de usuario (tema, moneda, idioma) - M3 del track movil
-- (docs/plan-sprints-movil-nativo.md). Columnas escalares 1:1 en users, mismo criterio que
-- ai_chat_used/ai_chat_period en V14__add_ai_quota_to_users.sql: no hay necesidad de una tabla
-- user_preferences aparte para tres valores simples sin historial propio.
ALTER TABLE users ADD COLUMN theme VARCHAR(10) NOT NULL DEFAULT 'SYSTEM';
ALTER TABLE users ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'COP';
ALTER TABLE users ADD COLUMN language VARCHAR(5) NOT NULL DEFAULT 'ES';

ALTER TABLE users ADD CONSTRAINT chk_users_theme CHECK (theme IN ('SYSTEM', 'LIGHT', 'DARK'));
ALTER TABLE users ADD CONSTRAINT chk_users_language CHECK (language IN ('ES', 'EN'));
ALTER TABLE users ADD CONSTRAINT chk_users_currency CHECK (currency IN ('COP', 'USD', 'MXN', 'ARS', 'EUR'));
