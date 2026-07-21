-- Sprint 2, Frente 1: la importación de extractos bancarios registra un AiUsageEvent de tipo
-- STATEMENT_EXTRACT (ver AiUsageEventType) cada vez que se llama al proveedor de IA para
-- extraer movimientos de un extracto. Se recrea el CHECK constraint para incluir el nuevo valor.
ALTER TABLE ai_usage_events DROP CONSTRAINT ck_ai_usage_events_event_type;

ALTER TABLE ai_usage_events ADD CONSTRAINT ck_ai_usage_events_event_type
    CHECK (event_type IN ('CHAT', 'CATEGORIZE', 'INSIGHT', 'STATEMENT_EXTRACT'));
