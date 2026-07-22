-- Per-attempt telemetry columns for ai_usage_events (see AiUsageEventService#recordAttempt and
-- AiChatOrchestrator's class Javadoc): latency, success/failure, and the failed attempt's
-- exception class, so a provider failover no longer disappears without a trace.
ALTER TABLE ai_usage_events ADD COLUMN latency_ms INT;
ALTER TABLE ai_usage_events ADD COLUMN success BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE ai_usage_events ADD COLUMN error_type VARCHAR(60);
