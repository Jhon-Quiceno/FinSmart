CREATE TABLE ai_messages (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    role            VARCHAR(15) NOT NULL,
    kind            VARCHAR(15) NOT NULL DEFAULT 'CHAT',
    content         TEXT NOT NULL,
    provider_name   VARCHAR(60),
    model           VARCHAR(120),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- ON DELETE CASCADE: chat/insight history has no meaning without its owning user.
    CONSTRAINT fk_ai_messages_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_ai_messages_role CHECK (role IN ('USER', 'ASSISTANT')),
    CONSTRAINT ck_ai_messages_kind CHECK (kind IN ('CHAT', 'INSIGHT'))
);

CREATE INDEX idx_ai_messages_user_created ON ai_messages (user_id, created_at DESC);
