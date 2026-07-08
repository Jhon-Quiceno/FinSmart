# Sprint 5 - Asistente IA Multi-Proveedor, Notificaciones y Automatizaciones Nativas (FinSmart)

Este sprint entrega el asistente IA conectado a datos reales del usuario, el sistema de notificaciones (in-app + email) y las automatizaciones que el plan original delegaba en n8n, ahora implementadas de forma nativa con Spring `@Scheduled` y eventos de aplicacion. n8n queda eliminado del stack.

## Antes de empezar

La rama de trabajo de este sprint se crea a partir de `feature/sprint-4-financial-engine-dashboard` (el Sprint 4 aun no esta integrado en `develop` y este sprint depende del motor financiero):

```bash
git checkout feature/sprint-4-financial-engine-dashboard
git checkout -b feature/sprint-5-ai-assistant-notifications
```

## Objetivo

1. Asistente IA multi-proveedor: el operador de la app configura una o mas API keys (NVIDIA NIM, OpenCode Zen, OpenRouter) como variables de entorno; el usuario final solo conversa con el asistente, sin cargar ninguna key. Si el proveedor que se esta usando falla (key invalida, rate limit, timeout, 5xx), el backend reintenta automaticamente con el siguiente proveedor configurado, de forma transparente para el usuario.
2. IA especializada en las finanzas del usuario: cada llamada inyecta contexto financiero real (resumen del motor de Sprint 4 + movimientos recientes + deudas + servicios) en el system prompt, en espanol.
3. Chat funcional en `/asistente-ia` con historial persistido, e insights financieros generados por IA en el dashboard y el asistente.
4. Sistema de notificaciones: entidad + endpoints + campana del navbar con badge de no leidas, y envio de email gratuito via Brevo SMTP (opcional y degradable: si no hay credenciales, solo in-app).
5. Automatizaciones nativas que reemplazan los 7 workflows n8n con igual o mejor resultado:
   - Recordatorios de pagos (job diario: servicios recurrentes y deudas que vencen en 3-5 dias).
   - Alertas de sobregasto (evento al crear gasto: si el gasto del mes supera el 80% del ingreso).
   - Resumen semanal (job semanal: ingresos vs gastos, mayor categoria, ahorro, recomendacion).
   - Prediccion fin de mes (servicio on-demand: promedio diario x dias restantes, alerta si proyeccion negativa).
   - Recomendaciones personalizadas con IA (contexto financiero -> proveedor IA configurado).
   - Clasificacion automatica de gastos (la IA sugiere categoria a partir de la descripcion).
   - Reactivacion (job diario: usuario sin registrar movimientos 3+ dias).
6. Preferencias de notificacion reales en `/configuracion` (los toggles mock pasan a persistir en backend).
7. Salud del esquema: indices compuestos que las consultas de jobs y analisis necesitan.

## Decisiones de arquitectura

1. **Sin n8n.** Todos los workflows se implementan con `@EnableScheduling` + `@Scheduled` y eventos de Spring (`ApplicationEventPublisher`) dentro del backend. Menos infraestructura, misma funcionalidad, testeable con JUnit.
2. **Un solo cliente IA OpenAI-compatible.** NVIDIA NIM (`https://integrate.api.nvidia.com/v1`), OpenCode Zen (`https://opencode.ai/zen/v1`) y OpenRouter (`https://openrouter.ai/api/v1`) hablan el mismo protocolo `/chat/completions`. Un unico `AiChatClient` basado en `RestClient` (incluido en Spring, sin dependencias nuevas) cubre todos los proveedores cambiando `baseUrl` + API key + `model`. Sin Spring AI: no aporta abstraccion util cuando todos los proveedores ya son OpenAI-shaped. Ya no se resuelve el proveedor por usuario: `AiChatOrchestrator` invoca `AiChatClient` con el proveedor que le entrega `AiProviderRegistry`, siguiendo el orden de prioridad configurado.
3. **API keys a nivel de aplicacion, con failover automatico.** Las API keys las configura el operador de la app via variables de entorno (`NVIDIA_API_KEY`, `OPENCODE_API_KEY`, `OPENROUTER_API_KEY`, mas los `_MODEL` opcionales). `AiProviderRegistry` las resuelve desde `app.ai.*` (bindeadas por `AiProviderProperties`) y expone la lista de proveedores habilitados en orden de prioridad (por defecto NVIDIA → OpenCode → OpenRouter, configurable con `AI_PROVIDER_PRIORITY`). No existe tabla en base de datos ni cifrado: las keys nunca tocan la base de datos. `AiChatOrchestrator` prueba cada proveedor habilitado en orden; si uno falla (auth, rate limit, timeout, 5xx) reintenta con el siguiente de forma transparente. El usuario final solo ve un mensaje generico de "no disponible" si fallan todos los proveedores configurados o si no hay ninguno — nunca se le informa cual proveedor fallo ni por que.
4. **"Entrenamiento" = inyeccion de contexto.** No se hace fine-tuning: el `FinancialContextBuilder` arma un system prompt en espanol con el resumen agregado del motor financiero (barato en tokens) + ultimas 10-15 transacciones + deudas y servicios activos. Si el usuario pregunta por periodos largos, se responde con agregados del motor, no volcando filas crudas.
5. **MVP sin streaming.** Respuesta JSON bloqueante (2-5s aceptable para el caso de uso); SSE queda como mejora futura.
6. **Email via Brevo SMTP** (300 emails/dia gratis permanente, sin dominio propio). `NotificationSender` como puerto con adaptadores in-app (siempre) y email (`@Async`, solo si hay credenciales configuradas). Otros canales evaluados quedan en `docs/notifications-future.md`.
7. **Deduplicacion de alertas.** Los jobs consultan `notifications` antes de crear: una alerta por (usuario, tipo, objetivo, periodo). Un job diario no debe repetir la misma alerta cada dia mientras la condicion se mantenga.
8. **Insights IA persistidos en `ai_messages`** con columna `kind` (`CHAT`/`INSIGHT`): el dashboard muestra el ultimo insight generado sin quemar rate limits de free tiers (NVIDIA ~40 RPM, OpenCode Zen limites propios del free tier); el usuario los regenera a demanda.

## Alcance del Sprint 5

### Backend

1. Entidad `Notification` + Repository + Service — tipo, titulo, mensaje, `is_read`/`read_at`, por usuario.
2. Endpoints `GET /api/notifications` (paginado), `GET /api/notifications/unread-count`, `PATCH /api/notifications/{id}/read`, `PATCH /api/notifications/read-all`.
3. Entidad `NotificationPreference` + endpoints `GET/PUT /api/notifications/preferences` — toggles de recordatorios, sobregasto, resumen semanal, reactivacion y email.
4. `NotificationSender` (puerto) + adaptador in-app + `BrevoEmailAdapter` (`spring-boot-starter-mail`, `@Async`, degradable si no hay credenciales).
5. `SupportedAiProvider` (catalogo enum de los 3 proveedores conocidos) + `AiProviderProperties` (bindeo de variables de entorno) + `AiProviderRegistry` (resuelve proveedores habilitados y orden de prioridad) + `AiChatOrchestrator` (bucle de failover) + `GET /api/ai/providers/status` — solo lectura, nunca expone una key.
6. `AiChatClient` (`RestClient` -> `{baseUrl}/chat/completions`) + jerarquia `AiProviderException` (rate limit, auth, timeout, modelo inexistente) mapeada en `GlobalExceptionHandler` con mensajes claros en espanol.
7. `FinancialContextBuilder` — system prompt en espanol con resumen del motor financiero + movimientos recientes + deudas + servicios activos.
8. Entidad `AiMessage` (rol `USER`/`ASSISTANT`, `kind` `CHAT`/`INSIGHT`, contenido, proveedor/modelo usado) + `POST /api/ai/chat` (persiste pregunta y respuesta) + `GET /api/ai/chat/history` (paginado).
9. `GET /api/ai/insights` (ultimo insight) + `POST /api/ai/insights/generate` — recomendaciones personalizadas generadas por IA con el contexto del usuario.
10. `POST /api/ai/categorize` — sugiere categoria existente del usuario a partir de la descripcion del gasto.
11. `@EnableScheduling` + `PaymentReminderJob` (diario: servicios con `next_payment_date` y deudas con `due_date` a 3-5 dias -> notificacion + email).
12. Alerta de sobregasto por evento: al crear un gasto se publica `ExpenseCreatedEvent`; el listener recalcula el ratio del mes y notifica si supera el 80% (una vez por periodo).
13. `WeeklySummaryJob` (semanal: balance, mayor categoria, ahorro y recomendacion -> notificacion + email) e `InactivityReminderJob` (diario: sin movimientos 3+ dias, maximo una notificacion por racha).
14. `GET /api/analysis/prediction` — prediccion fin de mes: promedio de gasto diario x dias restantes, saldo proyectado y gasto maximo diario recomendado; el job diario alerta si la proyeccion es negativa.
15. Tests unitarios y de integracion de todo lo anterior (services + controllers + jobs con clock/fixtures controlados).

### Base de datos

1. **Migracion `V7`** — tablas `notifications` (indices `(user_id, is_read)` y `(user_id, created_at)`) y `notification_preferences` (`UNIQUE(user_id)`).
2. **Migracion `V8`** — tabla `ai_messages` (rol, kind, contenido, proveedor, modelo, indice `(user_id, created_at)`).
3. **Migracion `V10`** — indices de salud del esquema: compuestos `(user_id, date)` en `expenses` e `incomes` (reemplazan los simples de `date`), `(is_active, next_payment_date)` en `recurring_payments`, `(user_id, due_date)` en `debts`.

### Frontend

1. Pagina `/asistente-ia` conectada al backend — chat real contra `POST /api/ai/chat`, historial desde `GET /api/ai/chat/history`, selector de proveedor/modelo activo, estados de carga y errores del proveedor en espanol.
2. Panel de insights financieros del asistente alimentado por `GET /api/ai/insights` + boton regenerar.
3. Navbar: badge de no leidas real (`GET /api/notifications/unread-count`) + panel conectado a `GET /api/notifications` + marcar leida/todas.
4. Tarjeta de prediccion fin de mes en dashboard conectada a `GET /api/analysis/prediction`.
5. Seccion de insights IA en dashboard (ultimo insight generado + link al asistente).
6. `/configuracion`: tarjeta de solo lectura con el estado de los proveedores IA (cuales de los 3 conocidos estan configurados y su orden de prioridad, alimentada por `GET /api/ai/providers/status`; sin alta/edicion/borrado, eso lo hace el operador via `.env`) y preferencias de notificacion persistidas.
7. Boton "Sugerir categoria" (IA) en el formulario de gasto conectado a `POST /api/ai/categorize`.
8. Tests de servicios/schemas nuevos (vitest) actualizados.

## Definicion de terminado (DoD)

1. Con al menos una API key configurada en el entorno (NVIDIA, OpenCode Zen u OpenRouter), el chat responde en espanol usando los datos financieros reales del usuario autenticado, y la conversacion persiste en `ai_messages`.
2. Si el proveedor que se esta probando falla, el sistema reintenta automaticamente con el siguiente proveedor configurado sin que el usuario note diferencia alguna; solo se muestra el mensaje generico de no disponibilidad si fallan todos los proveedores configurados o si no hay ninguno.
3. Las API keys viven unicamente en variables de entorno/el `.env` del operador: nunca se guardan en la base de datos, nunca se loguean, y ningun endpoint las devuelve.
4. Los 7 workflows n8n originales tienen equivalente nativo funcionando (jobs + evento + endpoints IA), con deduplicacion verificada: correr el job dos veces no duplica notificaciones.
5. Campana del navbar sin mocks: badge, listado y marcar leidas operan contra el backend, filtrado siempre por `userId`.
6. Sin credenciales de email configuradas, todo funciona igual solo con notificaciones in-app (degradacion silenciosa, logueada).
7. Errores de proveedor IA (key invalida, rate limit, timeout, modelo inexistente) llegan al frontend como mensajes claros en espanol, no como stack traces.
8. Migraciones `V7`, `V8` y `V10` verificadas contra Postgres real; `mvnw test` y suite frontend (test/lint/build) en verde.
9. Probado end-to-end con el usuario de desarrollo Jhon Quiceno (`user_id = 2`).

## Referencia de endpoints (Sprint 5)

```http
# Asistente IA
POST   /api/ai/chat
GET    /api/ai/chat/history
GET    /api/ai/insights
POST   /api/ai/insights/generate
POST   /api/ai/categorize
GET    /api/ai/providers/status

# Notificaciones
GET    /api/notifications
GET    /api/notifications/unread-count
PATCH  /api/notifications/{id}/read
PATCH  /api/notifications/read-all
GET    /api/notifications/preferences
PUT    /api/notifications/preferences

# Motor financiero
GET    /api/analysis/prediction
```

## Referencia de proveedores IA (free tiers, jul-2026)

| Proveedor | Base URL | Modelos ejemplo | Free tier |
|-----------|----------|-----------------|-----------|
| NVIDIA NIM | `https://integrate.api.nvidia.com/v1` | Llama 3.1, DeepSeek, Mixtral, Nemotron | ~40 RPM global por key |
| OpenCode Zen | `https://opencode.ai/zen/v1` | `big-pickle`, `deepseek-v4-flash-free`, `mimo-v2.5-free`, `north-mini-code-free`, `nemotron-3-ultra-free` | Modelos `-free` sin costo, sujetos a limites propios del proveedor |
| OpenRouter | `https://openrouter.ai/api/v1` | `deepseek/deepseek-r1:free` y 28+ `:free` | 20 RPM, 50 req/dia sin creditos |

## Variables de entorno nuevas

```properties
# Proveedores de IA (a nivel de aplicacion)
# Las API keys las carga el operador de la app, no los usuarios finales.
# Un proveedor se activa solo si su *_API_KEY tiene valor. Se necesita AL MENOS UNO
# para que el asistente funcione; si no hay ninguno, responde "no disponible".
# Si un proveedor falla, se prueba el siguiente automaticamente (failover transparente).
AI_PROVIDER_PRIORITY=nvidia,opencode,openrouter   # opcional; este es el orden por defecto

NVIDIA_API_KEY=
NVIDIA_MODEL=meta/llama-3.1-70b-instruct
OPENCODE_API_KEY=
OPENCODE_MODEL=big-pickle
OPENROUTER_API_KEY=
OPENROUTER_MODEL=deepseek/deepseek-r1:free

BREVO_SMTP_LOGIN=          # opcional: sin esto, solo notificaciones in-app
BREVO_SMTP_KEY=            # opcional
MAIL_FROM=                 # opcional: remitente verificado en Brevo
```
