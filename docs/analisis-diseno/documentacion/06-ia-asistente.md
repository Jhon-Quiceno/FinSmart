# Asistente IA — KoroFin

> **Propósito:** Describir el diseño del asistente de inteligencia artificial multi-proveedor, su arquitectura, failover y componentes.

---

## 1. Arquitectura Multi-Proveedor

KoroFin no depende de un único proveedor de IA. El sistema está diseñado para soportar **múltiples proveedores** con **failover automático y transparente** para el usuario.

```
                    ┌─────────────────────────────┐
                    │   AiChatOrchestrator        │
                    │   (Router + Failover)       │
                    └──────────┬──────────────────┘
                               │
     ┌──────────┬──────────────┼──────────────┬──────────┐
     ▼          ▼              ▼              ▼          ▼
┌─────────┐┌─────────┐  ┌────────────┐┌──────────────┐┌─────────┐
│ Gemini  ││ NVIDIA  │  │ OpenCode   ││ OpenRouter    ││  Groq   │
│(prior.1)││(prior.2)│  │ Zen        ││               ││ (sin key│
│         ││         │  │ (prior. 3) ││ (prior. 4)    ││  real,  │
│         ││         │  │            ││               ││ inerte) │
└─────────┘└─────────┘  └────────────┘└──────────────┘└─────────┘
     │          │              │              │           │
     └──────────┴──────────────┼──────────────┴───────────┘
                                ▼
               ┌──────────────────────────┐
               │  OpenAI-compatible API   │
               │  POST /chat/completions  │
               └──────────────────────────┘
```

### Principios de Diseño

1. **API Keys del operador**: las claves se configuran como variables de entorno del operador de la app, no las carga cada usuario.
2. **Failover automático**: si el proveedor #1 falla, el `AiChatOrchestrator` reintenta con el #2, luego #3, etc.
3. **Transparencia**: el usuario nunca ve el cambio de proveedor — solo ve la respuesta.
4. **Catálogo fijo**: los proveedores soportados se definen en `SupportedAiProvider` y se resuelven por `AiProviderRegistry`.

---

## 2. Componentes del Sistema

### 2.1 Configuración (`AiProviderProperties`)

Bindea las variables de entorno a propiedades de la aplicación, una por proveedor del catálogo fijo (`SupportedAiProvider`): API key, modelo de texto y, cuando aplica, modelo de visión.
- `GEMINI_API_KEY`, `GEMINI_MODEL`, `GEMINI_VISION_MODEL`
- `NVIDIA_API_KEY`, `NVIDIA_MODEL`, `NVIDIA_VISION_MODEL`
- `OPENCODE_API_KEY`, `OPENCODE_MODEL` (sin modelo de visión)
- `OPENROUTER_API_KEY`, `OPENROUTER_MODEL`, `OPENROUTER_VISION_MODEL`
- `GROQ_API_KEY`, `GROQ_MODEL` (sin modelo de visión; catalogado pero sin key real configurada todavía)

### 2.2 Registro de Proveedores (`AiProviderRegistry`)

Determina qué proveedores están habilitados y en qué orden de prioridad intentarlos. Solo considera proveedores con API key configurada (no vacía) — un proveedor catalogado sin key nunca se intenta.

### 2.3 Orquestador (`AiChatOrchestrator`)

```
1. Recibe mensaje del usuario + contexto financiero
2. Obtiene lista de proveedores habilitados (ordenados por prioridad)
3. Para cada proveedor:
   a. Construye request POST /chat/completions
   b. Envía via RestClient
   c. Si éxito → devuelve respuesta
   d. Si error → registra fallo, pasa al siguiente
4. Si todos fallan → devuelve error al usuario
```

### 2.4 Cliente de Chat (`AiChatClient`)

Implementación con `RestClient` de Spring Boot 4 que apunta al endpoint OpenAI-compatible `{baseUrl}/chat/completions`.

### 2.5 Contexto Financiero (`FinancialContextBuilder`)

Construye el **system prompt** en español con datos reales del usuario:
- Resumen del motor financiero (balance, ratios)
- Movimientos recientes
- Deudas activas y próximas a vencer
- Servicios recurrentes próximos

### 2.6 Prioridad por Tipo de Tarea (`app.ai.task-priority.*`)

`AiProviderRegistry#enabledInPriorityOrder(AiUsageEventType)` permite sobreescribir el orden global de intento **solo para una operación puntual** (`chat`, `categorize`, `insight`, `statement_extract`), sin afectar a las demás:

- `app.ai.task-priority.chat`, `app.ai.task-priority.categorize`, `app.ai.task-priority.insight`, `app.ai.task-priority.statement_extract` (variables `AI_TASK_PRIORITY_CHAT`, etc.)
- Si no hay override configurado para una tarea (la clave está ausente o queda vacía tras filtrar entradas inválidas), esa tarea usa el orden global (`app.ai.priority` / `DEFAULT_PRIORITY`) sin cambios.
- Igual que `app.ai.priority`, es una **preferencia, no un allow-list**: un proveedor configurado pero no listado en el override se agrega al final, en el orden global.

**`AiChatOrchestrator#completeVision` nunca usa este mecanismo** — siempre llama a `enabledInPriorityOrder()` (el orden global), a propósito: `ReceiptExtractionService` etiqueta su llamada de visión con el mismo `AiUsageEventType.CATEGORIZE` que usa la categorización de texto. Si `completeVision` respetara `app.ai.task-priority.categorize`, un override pensado para categorización de texto cambiaría silenciosamente también el orden de la extracción de recibos por foto — dos operaciones distintas que hoy comparten el mismo tipo de evento por motivos de telemetría, no de negocio.

---

## 3. Capacidades del Asistente

### 3.1 Chat Contextual (`POST /api/ai/chat`)

El usuario conversa en lenguaje natural sobre sus finanzas. El sistema incluye el contexto financiero actual y el historial reciente.

**Ejemplos de preguntas:**
- "¿En qué me estoy gastando más este mes?"
- "¿Puedo gastarme $200 este fin de semana?"
- "¿Cómo puedo ahorrar para un viaje en 3 meses?"
- "¿Qué deuda debería pagar primero?"

### 3.2 Insights Financieros (`GET /api/ai/insights`)

Recomendaciones generadas por IA sobre los datos financieros del usuario. Se generan bajo demanda con `POST /api/ai/insights/generate`.

### 3.3 Clasificación Automática (`POST /api/ai/categorize`)

Dada una descripción de gasto, la IA sugiere la categoría existente más adecuada. Se usa desde el formulario de gasto en el frontend:

```
Input:  { "description": "McDonald's" }
Output: { "categoryId": 3, "categoryName": "Comida" }
```

### 3.4 Estado de Proveedores (`GET /api/ai/providers/status`)

Endpoint de solo lectura que expone qué proveedores están configurados y su estado (OK/ERROR). **Nunca expone las API keys**.

### 3.5 Visión / Multimodal (`AiChatOrchestrator#completeVision`)

Capacidad usada por `ReceiptExtractionService` para extraer datos (comercio, monto, fecha) de una foto de recibo enviada por el bot de Telegram o subida desde el frontend.

`completeVision` **siempre** usa el orden global de prioridad (nunca `app.ai.task-priority`, ver sección 2.6) e itera cada proveedor habilitado que además tenga un modelo de visión configurado, saltando los que no. De los 5 proveedores del catálogo, solo **Gemini, NVIDIA y OpenRouter** tienen modelo de visión (`defaultVisionModel()` no nulo); OpenCode y Groq no participan en este camino. El primero que responda con éxito gana:

```
Gemini (gemini-3.5-flash) → NVIDIA (nemotron-nano-12b-v2-vl) → OpenRouter (nemotron-nano-12b-v2-vl:free)
```

**Por qué el orden importa tanto como tener failover:** un JSON bien formado pero con datos alucinados (por ejemplo, NVIDIA leyendo mal el monto o el comercio de un recibo real) es indistinguible de una respuesta correcta a nivel HTTP/parsing — no hay forma de detectar esa falla automáticamente. Eso significa que, para la práctica, el proveedor que se prueba primero "gana" casi siempre, sin importar qué tan preciso sea. Gemini fue verificado empíricamente como más consistente que NVIDIA sobre la misma foto real en pruebas repetidas (2026-07-21), por eso va primero; NVIDIA sigue siendo una segunda línea de defensa real quedando disponible cuando Gemini falla (se observó un 503 transitorio bajo carga en la misma verificación).

---

## 4. Modelo de Datos

```sql
-- Tabla: ai_messages
CREATE TABLE ai_messages (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id),
    role       VARCHAR(10) NOT NULL CHECK (role IN ('user', 'assistant')),
    kind       VARCHAR(10) NOT NULL CHECK (kind IN ('CHAT', 'INSIGHT')),
    content    TEXT NOT NULL,
    provider   VARCHAR(50),
    model      VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ai_messages_user_created ON ai_messages(user_id, created_at);

-- Tabla: ai_usage_events (V16, + V25 telemetría por intento)
CREATE TABLE ai_usage_events (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL REFERENCES users(id),
    provider      VARCHAR(60) NOT NULL,
    event_type    VARCHAR(30) NOT NULL,
    tokens_used   INT NOT NULL,
    cost_estimate DECIMAL(10,6),
    latency_ms    INT,
    success       BOOLEAN NOT NULL DEFAULT true,
    error_type    VARCHAR(60),
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);
```

Ver `03-modelo-datos.md` (secciones 2.14) para el detalle completo de columnas.

---

## 5. Transacciones y Consistencia

- **Por diseño**: si todos los proveedores de IA fallan, el mensaje del usuario NO se persiste en `ai_messages`.
- **Creación de gasto**: el listener de sobregasto usa `@TransactionalEventListener(AFTER_COMMIT)` con `REQUIRES_NEW` para que una notificación fallida no revierta el gasto.

---

## 6. Proveedores Soportados

| Proveedor | `baseUrl` | Modelo de texto (default) | Modelo de visión (default) | Prioridad global (default) |
|-----------|-----------|---------------------------|------------------------------|------------------------------|
| Gemini | `generativelanguage.googleapis.com/v1beta/openai` | `gemini-3.5-flash` | `gemini-3.5-flash` (multimodal, mismo modelo) | 1 |
| NVIDIA | `integrate.api.nvidia.com/v1` | `meta/llama-3.1-70b-instruct` | `nvidia/nemotron-nano-12b-v2-vl` | 2 |
| OpenCode Zen | `opencode.ai/zen/v1` | `deepseek-v4-flash-free` | — (sin modelo de visión) | 3 |
| OpenRouter | `openrouter.ai/api/v1` | `nvidia/nemotron-3-nano-30b-a3b:free` | `nvidia/nemotron-nano-12b-v2-vl:free` | 4 |
| Groq | `api.groq.com/openai/v1` | `llama-3.3-70b-versatile` | — (sin modelo de visión) | 5 (catalogado, sin API key real configurada todavía) |

El orden de arriba es `AiProviderRegistry.DEFAULT_PRIORITY` y solo se usa cuando `app.ai.priority` está vacío. `app.ai.priority` es una preferencia, no un allow-list: un proveedor configurado pero no listado igual se agrega al final, en este mismo orden.

---

## 7. Cuota y Costo por Usuario

### 7.1 Cuota mensual de chat (`V14`, `users.ai_chat_used` / `users.ai_chat_period`)

Tope duro de mensajes de chat IA por usuario y mes calendario (`AiMessageQuotaExceededException` cuando se supera), configurable vía `app.ai.monthly-message-limit`. Es un contador dedicado — no cuenta filas `ai_messages` — para sobrevivir a la purga de historial que hace `UserService#login`, y se reserva con un `UPDATE` atómico condicional para evitar una condición de carrera check-then-act entre requests concurrentes. Es un mecanismo de control de costo grueso, distinto y complementario a la telemetría de `ai_usage_events`.

### 7.2 Telemetría de uso (`V16`, `V25`, `ai_usage_events`)

Registro granular pensado para reporting y para un futuro metering de billing, con dos formas de escritura:

- **Por evento exitoso** (`AiUsageEventService#record`): una fila por llamada exitosa, sin latencia ni tipo de error.
- **Por intento** (`AiUsageEventService#recordAttempt`, `V25`): una fila por cada intento de proveedor dentro de una misma llamada de `complete`/`completeVision`, incluyendo los intentos que fallaron y derivaron al siguiente proveedor del failover. `success = false` distingue un intento fallido (con `latency_ms`/`error_type`) del que finalmente respondió.

Los agregados visibles al usuario (`GET /api/ai/usage`) filtran `success = true`, para que un proveedor que falló y derivó a otro no infle el conteo visible de "llamadas de IA este mes".

### 7.3 Estimación de costo (`AiProviderPricing`)

Tabla de precios estimados por 1000 tokens, simple y estática (sin configuración ni base de datos todavía), usada para poblar `AiUsageEvent#costEstimate`:

- Devuelve `0` para modelos gratuitos conocidos (el modelo free por default de OpenCode, o cualquier modelo con sufijo `:free` de OpenRouter).
- Devuelve un estimado para NVIDIA y Groq (proveedores pagos), con precios **placeholder no verificados** contra la página de precios real de ninguno de los dos — pendiente de reconciliar contra gasto real.
- Devuelve `null` (sin dato) para combinaciones proveedor/modelo sin precio conocido (OpenCode/OpenRouter en modelos no gratuitos).

---

*Documento de diseño del asistente IA — KoroFin*
