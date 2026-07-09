-- DEFAULT TRUE: existing refresh tokens (issued before "remember me" existed) keep behaving
-- like persistent sessions, matching the behavior in place before this change.
ALTER TABLE refresh_tokens ADD COLUMN remember_me BOOLEAN NOT NULL DEFAULT TRUE;
