CREATE TABLE push_tokens (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    expo_push_token VARCHAR(200) NOT NULL,
    device_id       VARCHAR(120) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- ON DELETE CASCADE: a push token has no meaning without its owning user.
    CONSTRAINT fk_push_tokens_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    -- One row per (user, device): re-registering the same device (reinstall, rotated Expo
    -- token) updates the existing row instead of accumulating duplicates.
    CONSTRAINT uk_push_tokens_user_device UNIQUE (user_id, device_id)
);

CREATE INDEX idx_push_tokens_user ON push_tokens (user_id);
