-- Sprint 2, Frente 2: vincula un chat de Telegram con un usuario de la aplicación, para que el
-- bot de Telegram (orquestado por n8n) pueda registrar gastos en nombre del usuario vinculado.
-- El vínculo se establece con un código de un solo uso generado desde la app (ver
-- TelegramLinkCodeStore) y confirmado por el bot; telegram_chat_id es único porque un mismo chat
-- de Telegram solo puede estar vinculado a un usuario a la vez.
CREATE TABLE telegram_links (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id          BIGINT NOT NULL,
    telegram_chat_id VARCHAR(50) NOT NULL UNIQUE,
    linked_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_telegram_links_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
