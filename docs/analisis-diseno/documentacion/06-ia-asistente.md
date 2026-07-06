# Asistente IA — FinSmart

> **Propósito:** Describir el diseño del asistente de inteligencia artificial multi-proveedor, su arquitectura, failover y componentes.

---

## 1. Arquitectura Multi-Proveedor

FinSmart no depende de un único proveedor de IA. El sistema está diseñado para soportar **múltiples proveedores** con **failover automático y transparente** para el usuario.

```
                    ┌─────────────────────────────┐
                    │   AiChatOrchestrator        │
                    │   (Router + Failover)       │
                    └──────────┬──────────────────┘
                               │
          ┌────────────────────┼────────────────────┐
          ▼                    ▼                    ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│   NVIDIA NIM    │  │  OpenCode Zen   │  │   OpenRouter    │
│   llama-3-70b   │  │   gpt-4o-mini   │  │  claude-3-haiku │
│  (prioridad 1)  │  │ (prioridad 2)   │  │ (prioridad 3)   │
└─────────────────┘  └─────────────────┘  └─────────────────┘
         │                    │                    │
         └────────────────────┼────────────────────┘
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

Bindea las variables de entorno a propiedades de la aplicación:
- `NVIDIA_API_KEY`, `NVIDIA_BASE_URL`
- `OPENCODE_API_KEY`, `OPENCODE_BASE_URL`
- `OPENROUTER_API_KEY`, `OPENROUTER_BASE_URL`

### 2.2 Registro de Proveedores (`AiProviderRegistry`)

Determina qué proveedores están habilitados y en qué orden de prioridad intentarlos. Solo considera proveedores con API key configurada.

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
```

---

## 5. Transacciones y Consistencia

- **Por diseño**: si todos los proveedores de IA fallan, el mensaje del usuario NO se persiste en `ai_messages`.
- **Creación de gasto**: el listener de sobregasto usa `@TransactionalEventListener(AFTER_COMMIT)` con `REQUIRES_NEW` para que una notificación fallida no revierta el gasto.

---

## 6. Proveedores Soportados

| Proveedor | Modelos Típicos | Prioridad (default) |
|-----------|----------------|---------------------|
| NVIDIA NIM | llama-3-70b, mixtral-8x22b | 1 (primer intento) |
| OpenCode Zen | gpt-4o-mini, claude-3-haiku | 2 |
| OpenRouter | múltiples modelos | 3 (fallback) |

---

*Documento de diseño del asistente IA — FinSmart MVP*
