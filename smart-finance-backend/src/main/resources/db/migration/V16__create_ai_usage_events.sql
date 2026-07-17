CREATE TABLE ai_usage_events (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    provider        VARCHAR(60) NOT NULL,
    event_type      VARCHAR(30) NOT NULL,
    tokens_used     INT NOT NULL DEFAULT 0,
    cost_estimate   NUMERIC(10, 6),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- ON DELETE CASCADE: un evento de uso de IA no tiene sentido sin el usuario que lo generó
    -- (mismo criterio que fk_ai_messages_user en V8__create_ai_messages.sql).
    CONSTRAINT fk_ai_usage_events_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_ai_usage_events_event_type CHECK (event_type IN ('CHAT', 'CATEGORIZE', 'INSIGHT')),
    CONSTRAINT ck_ai_usage_events_tokens_used_non_negative CHECK (tokens_used >= 0)
);

CREATE INDEX idx_ai_usage_events_user_created ON ai_usage_events (user_id, created_at);
