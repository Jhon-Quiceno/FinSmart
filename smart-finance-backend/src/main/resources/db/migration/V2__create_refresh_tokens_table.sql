CREATE TABLE refresh_tokens (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    token_id        UUID NOT NULL,
    user_id         BIGINT NOT NULL,
    token_hash      VARCHAR(120) NOT NULL,
    expires_at      TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    revoked_at      TIMESTAMPTZ,
    CONSTRAINT uk_refresh_tokens_token_id UNIQUE (token_id),
    CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash),
    -- ON DELETE CASCADE: deleting a user should not leave orphaned refresh tokens
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
);

-- UNIQUE constraints above already create implicit indexes on token_id and token_hash;
-- user_id has no unique constraint (one user can have many refresh tokens), so it needs an explicit index.
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
