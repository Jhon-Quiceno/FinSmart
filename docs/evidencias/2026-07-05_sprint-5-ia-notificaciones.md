# Evidencia Sprint 5 — Asistente IA Multi-Proveedor + Notificaciones

> **Fecha:** Julio 2026
> **Total de tareas:** 26/26 completadas
> **Migraciones:** V7 (notifications), V8 (ai_messages), V10 (índices de salud)

---

## 1. Objetivo

Implementar el asistente de IA multi-proveedor con failover automático, notificaciones in-app y email, y automatizaciones nativas del backend que reemplazan n8n.

## 2. Alcance Implementado

### Backend (15 tareas)
- Entidad `Notification` + CRUD endpoints (paginado, conteo, marcar leída)
- Entidad `NotificationPreference` + GET/PUT de preferencias
- `NotificationSender` (puerto) + adaptador in-app + `BrevoEmailAdapter` (async, degradable)
- `AiProviderRegistry` + `AiChatOrchestrator` (failover automático entre NVIDIA/OpenCode/OpenRouter)
- `AiChatClient` con RestClient OpenAI-compatible
- `FinancialContextBuilder` — system prompt con datos reales del usuario
- Entidad `AiMessage` + endpoints de chat e historial
- Insights IA (`GET /api/ai/insights`, `POST /api/ai/insights/generate`)
- Clasificación automática (`POST /api/ai/categorize`)
- `PaymentReminderJob` — diario: servicios/deudas que vencen en 3-5 días
- Alerta de sobregasto por evento (`ExpenseCreatedEvent` → si >80% → notificación)
- `WeeklySummaryJob` + `InactivityReminderJob`
- Predicción fin de mes (`GET /api/analysis/prediction`)
- Tests unitarios e integración

### Base de Datos (3 tareas)
- V7: tablas `notifications`, `notification_preferences`
- V8: tabla `ai_messages`
- V10: índices de salud (date en expenses/incomes, etc.)

### Frontend (8 tareas)
- Página `/asistente-ia` con chat real y selector de proveedor
- Panel de insights financieros con regeneración
- Navbar: badge de no leídas + panel de notificaciones
- Tarjeta de predicción fin de mes en dashboard
- Insights IA en dashboard
- `/configuracion`: estado de proveedores IA + preferencias
- Botón "Sugerir categoría" en formulario de gasto
- Tests (Vitest)

## 3. Decisiones Técnicas

| Decisión | Justificación |
|----------|---------------|
| API keys del operador (no BYOK) | Resiliencia: el operador cubre múltiples free tiers; el usuario nunca ve caídas |
| Event listener con AFTER_COMMIT + REQUIRES_NEW | Una notificación fallida no revierte el gasto |
| Cross de EmailRecipient con valores planos | Evita LazyInitializationException en thread @Async |
| Predicción pre-filtra usuarios con gastos | No escanea todos los usuarios activos innecesariamente |
| Si todos los proveedores fallan, no se persiste el mensaje | Consistencia: no hay respuesta sin mensaje del asistente |

## 4. Arquitectura de IA

```
AiChatOrchestrator
├── AiProviderRegistry (proveedores habilitados + prioridad)
├── FinancialContextBuilder (system prompt)
└── AiChatClient (RestClient → OpenAI-compatible)
    ├── NVIDIA NIM (prioridad 1)
    ├── OpenCode Zen (prioridad 2)
    └── OpenRouter (prioridad 3, fallback)
```

## 5. Automatizaciones Nativas

| Job | Schedule | Propósito |
|-----|----------|-----------|
| `PaymentReminderJob` | Diario | Notificar vencimientos en 3-5 días |
| `WeeklySummaryJob` | Semanal | Resumen de actividad + recomendaciones |
| `InactivityReminderJob` | Diario | Recordatorio si 3+ días sin actividad |
| `MonthEndPredictionJob` | Diario | Predecir saldo fin de mes + alertar si negativo |
| Overspend listener | Por evento | Alerta si gasto del mes >80% del ingreso |

## 6. Conclusión

El Sprint 5 es el más complejo del MVP y entrega el valor diferencial de FinSmart: IA multi-proveedor con failover transparente, notificaciones inteligentes y automatizaciones que reemplazan la infraestructura externa (n8n). La arquitectura está diseñada para ser resiliente, testeable y extensible.
