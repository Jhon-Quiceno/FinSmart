# Sprint 2 - Extractos Bancarios y Primer Flujo de n8n (KoroFin SaaS)

Segundo sprint de la fase SaaS. Ataca el Nivel 2 del roadmap unificado (`docs/roadmap-saas-cuentas-reales.md`,
seccion "Nivel 2 — Importar extractos bancarios"): datos reales del usuario sin pedir
credenciales bancarias, sin terceros ni carga regulatoria. En paralelo, construye el primer
flujo real de automatizacion sobre la infraestructura de n8n que ya esta corriendo en Docker
desde la fase de inicio de SaaS: un bot de Telegram para registrar gastos por chat.

## Antes de empezar

La rama de trabajo de este sprint se crea a partir de `develop`:

```bash
git checkout develop
git checkout -b feature/sprint-2-extractos-bancarios-telegram-bot
```

## Objetivo

1. Nivel 2 del roadmap unificado: el usuario sube el extracto de su banco (CSV/Excel) y la
   app parsea, deduplica contra lo ya registrado, sugiere categoria con la IA existente, y
   crea los ingresos/gastos reales solo despues de que el usuario confirma un preview —
   nunca se escribe nada a la base de datos sin confirmacion explicita.
2. Primer flujo real de n8n: un bot de Telegram vinculado a la cuenta de KoroFin del usuario,
   que permite registrar un gasto mandando un mensaje de texto libre (ej. "Uber 15000"),
   reutilizando el mismo `categorize()` que ya usa el quick-add web.
3. Limpieza operativa: retirar el test manual de humo de email que ya cumplio su proposito
   (verificar la migracion a Resend).

## Decisiones de arquitectura

1. **La importacion es un flujo preview-confirm de dos pasos, sin persistir nada en el primer
   paso.** `POST /api/statement-imports/preview` parsea el archivo, corre deduplicacion y
   `categorize()` sobre cada fila, y devuelve el resultado sin tocar la base de datos. Recien
   `POST /api/statement-imports/confirm` con las filas que el usuario aprobo (pudiendo
   destildar duplicados o filas mal parseadas) crea los `Income`/`Expense` reales. Evita el
   peor escenario de un importador: escribir basura a la base de datos por un parser con
   bugs. No hay tabla intermedia de "batch" en esta v1 — el preview vive solo en memoria del
   request, no se persiste ni se puede recuperar si el usuario cierra la pagina a mitad de
   camino (aceptable para v1; si se vuelve un problema real, la Fase 2 de esto seria
   persistir el batch).
2. **Parsers por banco detras de una interfaz comun (`BankStatementParser`), arrancando con
   Bancolombia y Davivienda** (los dos bancos con mayor participacion de mercado en
   Colombia). Cada parser es un adaptador de un formato de archivo especifico a una lista de
   `ParsedTransaction` (fecha, descripcion, monto con signo, linea cruda para debug). **El
   formato exacto de columnas de cada banco debe validarse contra un extracto real antes de
   dar el parser por terminado** — mismo tipo de advertencia que ya se uso para la
   amortizacion de tarjetas en la Fase B: no confiar en la memoria del formato, confirmar
   contra el archivo real.
3. **Deduplicacion por fecha + monto + descripcion (match exacto en fecha/monto, similitud
   de texto en descripcion)** contra `expenses`/`incomes` existentes del usuario, en una
   ventana de +-3 dias (el extracto y el registro manual pueden diferir en un dia por huso
   horario o fecha de proceso del banco). Una fila marcada como posible duplicado se muestra
   igual en el preview pero destildada por default — el usuario decide si de verdad es una
   fila nueva.
4. **El vinculo de Telegram usa un codigo de un solo uso, no OAuth.** El usuario genera un
   codigo desde `/configuracion` en la app (`POST /api/integrations/telegram/link-code`,
   autenticado con JWT normal), se lo manda al bot con `/start <codigo>`, y n8n llama a
   `POST /api/integrations/telegram/confirm-link` (autenticado con un secreto compartido de
   webhook, no JWT de usuario) para completar el vinculo `telegram_chat_id -> user_id`. Es
   deliberadamente mas simple que el modelo `integration_credentials` (OAuth, tokens
   cifrados) que el roadmap bocetea para Gmail — Telegram no entrega tokens de acceso a
   terceros, solo un `chat_id` estable. Ese modelo generico de credenciales queda para
   cuando el Sprint 3 (correo) lo necesite de verdad.
5. **Los endpoints que llama n8n (`/api/integrations/telegram/*`) NO pasan por el
   `JwtAuthenticationFilter` normal** — son server-to-server, autenticados con un secreto
   compartido (`app.integrations.telegram.webhook-secret`, header
   `X-Telegram-Webhook-Secret`) verificado en un filtro dedicado, y agregados a la lista
   `permitAll()` de `SecurityConfig` (el filtro propio rechaza con 401 si el secreto no
   matchea, antes de llegar al controller). Mismo criterio de "server-to-server con secreto
   compartido" que ya se uso para los secrets de GitHub Actions, aplicado ahora a
   nivel de request HTTP.
6. **El mensaje de Telegram reutiliza el mismo `categorize()` que el quick-add web, no un
   parser nuevo.** El texto libre del mensaje ("Uber 15000") se resuelve con la misma logica
   de extraccion de monto + `categorize()` que ya existe en `components/quick-add/` — la
   diferencia es que la creacion del `Expense` ocurre en el backend a partir del webhook de
   n8n, no de una llamada autenticada del usuario vía JWT.

## Alcance del Sprint 2

### Backend

**Frente 1 — Importacion de extractos bancarios**

1. Dominio nuevo `extractos/`: interfaz `BankStatementParser` con metodo
   `List<ParsedTransaction> parse(InputStream file)`.
2. `ParsedTransaction` (record): `date`, `description`, `amount` (con signo: positivo
   ingreso, negativo gasto — o el criterio que use cada banco, normalizado por el parser),
   `rawLine`.
3. `BankType` (enum): `BANCOLOMBIA`, `DAVIVIENDA`.
4. `BancolombiaCsvParser`, `DaviviendaCsvParser` — implementaciones concretas. **Validar el
   formato real de columnas de cada banco antes de dar por terminada esta tarea** (decision
   de arquitectura 2).
5. `StatementImportService`:
   - `preview(MultipartFile file, BankType bank, Long userId)` — parsea, corre
     deduplicacion contra `Expense`/`Income` del usuario (decision de arquitectura 3), corre
     `categorize()` sobre cada fila no duplicada, devuelve `List<ImportPreviewRow>`
     (transaccion parseada + `isDuplicate` + `suggestedCategoryId` + `suggestedCategoryName`
     + `movementType` sugerido INCOME/EXPENSE segun el signo). No persiste nada.
   - `confirm(List<ImportConfirmRequest> rows, Long userId)` — crea `Income`/`Expense` reales
     solo para las filas que el usuario confirmo, con la categoria que el usuario eligio
     (puede ser distinta a la sugerida).
6. Endpoint `POST /api/statement-imports/preview` (multipart: archivo + `bank`) →
   `List<ImportPreviewRow>`.
7. Endpoint `POST /api/statement-imports/confirm` (`List<ImportConfirmRequest>`) → cantidad
   de movimientos creados.
8. `@Valid` en los DTOs de entrada (tamano maximo de archivo, tipo de contenido CSV/XLSX),
   mensajes de error en espanol via `GlobalExceptionHandler`.

**Frente 2 — Vinculo e ingesta de Telegram (n8n)**

9. Migracion nueva: tabla `telegram_links` (`id`, `user_id` FK, `telegram_chat_id`
   UNIQUE, `linked_at`).
10. `TelegramLinkService`: `generateLinkCode(userId)` (codigo de un solo uso, TTL corto en
    memoria, no persistido — expira si no se usa), `confirmLink(code, chatId)` (valida el
    codigo, crea la fila en `telegram_links`).
11. `TelegramExpenseService`: `registerFromMessage(chatId, text)` — resuelve
    `telegram_links` → `userId`, reusa el parser de texto libre + `categorize()` del
    quick-add para extraer monto/descripcion/categoria, crea el `Expense`, devuelve un texto
    de confirmacion para que n8n se lo responda al usuario en Telegram.
12. `TelegramWebhookFilter` (o interceptor dedicado) — valida
    `X-Telegram-Webhook-Secret` contra `app.integrations.telegram.webhook-secret` antes de
    dejar pasar la request a los endpoints de `/api/integrations/telegram/*`. 401 con
    mensaje generico si no matchea (nunca confirmar si el secreto estaba "cerca" de ser
    correcto).
13. Endpoints:
    - `POST /api/integrations/telegram/link-code` (JWT normal, usuario autenticado) → genera
      el codigo de vinculo.
    - `POST /api/integrations/telegram/confirm-link` (secreto de webhook) → `{ code,
      chatId }` → vincula.
    - `POST /api/integrations/telegram/expenses` (secreto de webhook) → `{ chatId, text }` →
      crea el gasto, devuelve el texto de confirmacion.
14. `SecurityConfig`: agregar las 3 rutas de `/api/integrations/telegram/confirm-link` y
    `/api/integrations/telegram/expenses` a `permitAll()` (protegidas por el filtro de
    secreto, no por JWT); `/link-code` sigue detras del JWT normal (ya cubierto por
    `anyRequest().authenticated()`).

**Frente 3 — Limpieza operativa**

15. Retirar `EmailSmokeManualTest.java` (archivo local sin trackear) una vez confirmado que
    ya no hace falta para verificar entrega de Resend — accion manual del desarrollador, no
    requiere commit (el archivo nunca estuvo en git).

### Base de datos

1. **Migracion (verificar el ultimo `V` usado en el repo antes de aplicar — a la fecha de
   este sprint, Fase B llego hasta `V22__add_card_cycle_close_notification_type.sql`)** —
   tabla `telegram_links`: `id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY`, `user_id
   BIGINT NOT NULL` FK a `users(id) ON DELETE CASCADE`, `telegram_chat_id VARCHAR(50) NOT
   NULL UNIQUE`, `linked_at TIMESTAMPTZ NOT NULL DEFAULT now()`. Indice
   `idx_telegram_links_chat_id` (ya cubierto por el UNIQUE, no hace falta uno separado).
2. Sin migraciones para el Frente 1 (importacion de extractos) — el preview no persiste
   nada, y `confirm()` crea `Income`/`Expense` reales usando las tablas ya existentes, sin
   columnas nuevas.

### Frontend

1. Pagina nueva `app/importar/page.tsx` — selector de banco, subida de archivo, tabla de
   preview (checkbox por fila, categoria editable, indicador visual de "posible duplicado"),
   boton "Confirmar seleccionados".
2. `lib/services/statement-import.service.ts` — `previewStatement(file, bank)`,
   `confirmImport(rows)`.
3. `lib/types/statement-import.ts` — tipos del preview y la confirmacion.
4. Entrada "Importar extracto" accesible desde `/gastos` o `/ingresos` (boton, no
   necesariamente un item de sidebar nuevo — evaluar durante la implementacion cual da mejor
   UX sin forzar una seccion completa para un flujo de un solo uso).
5. En `/configuracion`: seccion "Integraciones" con boton "Vincular Telegram" que muestra el
   codigo de un solo uso y las instrucciones (`/start <codigo>` al bot).
6. `lib/services/telegram-integration.service.ts` — `generateLinkCode()`.

## Definicion de terminado (DoD)

1. `POST /api/statement-imports/preview` parsea un extracto real de Bancolombia y de
   Davivienda sin errores, marca correctamente los duplicados contra datos ya cargados, y
   sugiere categoria via IA para las filas no duplicadas.
2. `POST /api/statement-imports/confirm` crea unicamente los `Income`/`Expense` de las filas
   que el usuario selecciono, con la categoria final que el usuario eligio (sugerida o
   corregida).
3. Migracion de `telegram_links` verificada contra Postgres real; `mvnw test` en verde.
4. Un mensaje de texto libre enviado al bot de Telegram ("Uber 15000") crea un `Expense` real
   en la cuenta vinculada, con monto/descripcion/categoria resueltos igual que el quick-add
   web.
5. Los endpoints de `/api/integrations/telegram/*` rechazan con 401 cualquier request sin el
   secreto de webhook correcto.
6. Probado end-to-end con el usuario de desarrollo Jhon Quiceno (`user_id = 2`): importar un
   extracto real, y registrar al menos un gasto real por el bot de Telegram.

## Referencia de endpoints (Sprint 2)

```http
# Extractos bancarios
POST   /api/statement-imports/preview
POST   /api/statement-imports/confirm

# Integracion de Telegram (n8n)
POST   /api/integrations/telegram/link-code        # JWT normal
POST   /api/integrations/telegram/confirm-link      # secreto de webhook
POST   /api/integrations/telegram/expenses          # secreto de webhook
```

## Notas

- **El Nivel 3 (Open Finance/Belvo) y la integracion de correo (Gmail API + Pub/Sub) del
  roadmap unificado no son parte de este sprint.** La integracion de correo es
  probablemente demasiado grande para un solo sprint — se evalua partir en 2 al momento de
  detallarla (ver `docs/roadmap-saas-cuentas-reales.md`, seccion "Roadmap por Sprints",
  Sprint 3).
- El modelo generico `integration_credentials` (OAuth, tokens cifrados) que el roadmap
  bocetea para integraciones de terceros **no se construye en este sprint** — el vinculo de
  Telegram usa su propio mecanismo simple (decision de arquitectura 4), mas liviano que lo
  que Gmail va a necesitar en el Sprint 3.
- Los formatos de columnas exactos de los parsers de Bancolombia/Davivienda son un supuesto
  de este documento, no un hecho verificado — deben confirmarse contra un extracto real
  antes de darlos por terminados (decision de arquitectura 2).
