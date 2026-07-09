-- Dedicated per-user AI chat quota counter, decoupled from ai_messages rows. Counting CHAT-kind
-- ai_messages for the quota (the previous approach) broke as soon as UserService#login purges
-- those same rows on every login, silently resetting the quota. ai_chat_period holds the UTC
-- calendar month the counter belongs to ('YYYY-MM'); ai_chat_used resets whenever the stored
-- period no longer matches the current one. Keeping this as a plain counter (rather than a row
-- count) also lets the quota be reserved with a single atomic guarded UPDATE, closing the
-- check-then-act race where two concurrent requests could both read a stale "under limit" count.
ALTER TABLE users ADD COLUMN ai_chat_used INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN ai_chat_period VARCHAR(7);
