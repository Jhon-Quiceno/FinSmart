# Sprint 2 - Extractos Bancarios y Primer Flujo de n8n (KoroFin SaaS)

Segundo sprint de la fase SaaS. Ataca el Nivel 2 del roadmap unificado (`docs/roadmap-saas-cuentas-reales.md`,
seccion "Nivel 2 — Importar extractos bancarios"): datos reales del usuario sin pedir
credenciales bancarias, sin terceros ni carga regulatoria. En paralelo, construye el primer
flujo real de automatizacion sobre la infraestructura de n8n que ya esta corriendo en Docker
desde la fase de inicio de SaaS: un bot de Telegram para registrar gastos por chat.

**Estado: implementado y validado end-to-end con datos reales (2026-07-22), pendiente de
PR.** El alcance original de este documento contemplaba parsers por banco
(Bancolombia/Davivienda); durante la implementacion se aprobo un pivot de alcance —
extraccion generica con IA en lugar de parsers por banco — documentado en la seccion
"Decisiones de arquitectura" mas abajo. Este documento ya refleja el alcance final,
no el original.

La validacion end-to-end con el bot real de Telegram encontro y cerro varios bugs reales
que no aparecian en los 623 tests unitarios (todos mockeados) — ver "Validacion end-to-end
y hallazgos (2026-07-22)" mas abajo. El Frente 2 quedo bastante mas robusto de lo que
describia el diseno original: se sumo un proveedor de IA (Gemini), failover real de vision
(antes bypasseado a un solo proveedor), telemetria por intento, y prioridad de proveedores
configurable por tipo de tarea — trabajo que excede el alcance original del Frente 2 pero
que fue necesario para que el DoD real (bot funcionando con una foto real) se cumpliera.

## Antes de empezar

La rama de trabajo de este sprint se crea a partir de `develop`:

```bash
git checkout develop
git checkout -b feature/sprint-2-extractos-bancarios-telegram-bot
```

## Objetivo

1. Nivel 2 del roadmap unificado: el usuario sube el extracto de su banco (PDF con
   contrasena, CSV o XLSX, de cualquier banco), la app lo parsea con IA, deduplica contra lo
   ya registrado, sugiere categoria, y crea los ingresos/gastos reales solo despues de que el
   usuario confirma un preview — nunca se escribe nada a la base de datos sin confirmacion
   explicita.
2. Primer flujo real de n8n: un bot de Telegram vinculado a la cuenta de KoroFin del usuario,
   que permite registrar un gasto mandando un mensaje de texto libre (ej. "Uber 15000"),
   reutilizando el mismo parser de texto libre + `categorize()` que ya usa el quick-add web.
3. Limpieza operativa: retirar el test manual de humo de email que ya cumplio su proposito
   (verificar la migracion a Resend).

## Decisiones de arquitectura

1. **La importacion es un flujo preview-confirm de dos pasos, sin persistir nada en el primer
   paso.** `POST /api/statement-imports/preview` extrae el texto del archivo, lo manda a la
   IA para que identifique los movimientos, corre deduplicacion, y devuelve el resultado sin
   tocar la base de datos. Recien `POST /api/statement-imports/confirm` con las filas que el
   usuario aprobo (pudiendo destildar duplicados o corregir la categoria) crea los
   `Income`/`Expense` reales. Evita el peor escenario de un importador: escribir basura a la
   base de datos por una extraccion con errores. No hay tabla intermedia de "batch" en esta
   v1 — el preview vive solo en memoria del request, no se persiste ni se puede recuperar si
   el usuario cierra la pagina a mitad de camino (aceptable para v1; si se vuelve un problema
   real, la Fase 2 de esto seria persistir el batch).
2. **Pivot de alcance: extractor generico con IA en lugar de parsers por banco.** El diseno
   original de este documento proponia `BankStatementParser` + implementaciones concretas
   para Bancolombia y Davivienda. Se descarto durante la implementacion por tres motivos:
   - Los extractos reales del usuario llegan por correo como PDF protegido con contrasena
     (Bancolombia, Nu, Rappi son sus bancos reales); Nu en particular solo ofrece PDF, sin
     CSV (verificado). Un parser de columnas CSV no cubre ese caso.
   - Un parser por banco no escala al requisito real de "cualquier banco" — cada banco nuevo
     seria un parser nuevo que mantener.
   - El extractor generico resuelve extraccion y categorizacion **en una sola llamada a la
     IA** por extracto (`StatementAiExtractionService`, unico consumidor de
     `AiChatOrchestrator` en este flujo): el prompt de sistema pide un arreglo JSON estricto
     con `date`, `description`, `amount`, `movementType` (`INCOME`/`EXPENSE`) y
     `suggestedCategoryName` tomado de las categorias reales del usuario o `null`. La
     alternativa de categorizar fila por fila (`categorize()` por cada movimiento, como
     contemplaba el diseno original) hubiera costado una llamada de IA adicional por cada
     fila del extracto — 50 llamadas extra en un extracto de 50 movimientos.

     Implementacion (dominio `extractos/`):
     - `StatementTextExtractor` (interfaz de estrategia) con tres implementaciones:
       `PdfStatementTextExtractor` (PDFBox 3.0.5, acepta contrasena opcional provista por el
       usuario en cada request, nunca se guarda), `CsvStatementTextExtractor` (texto plano
       UTF-8) y `XlsxStatementTextExtractor` (Apache POI 5.4.1, `poi-ooxml`).
     - `StatementTextExtractionService` — despacha al extractor segun la extension del
       archivo; extension no soportada → 400 (`UnsupportedStatementFileException`); texto
       vacio tras la extraccion (ej. PDF escaneado como imagen) → 422
       (`EmptyStatementTextException`) — no hay OCR en esta v1.
     - `StatementAiExtractionService` — la llamada de IA descrita arriba. Parsing defensivo
       de la respuesta: tolera bloques de codigo markdown o texto adicional alrededor del
       JSON; respuesta no interpretable como JSON → 422
       (`StatementExtractionException`); filas individuales invalidas (fecha o monto no
       parseable) se descartan sin abortar el resto del extracto.
     - El uso se registra como un nuevo `AiUsageEventType.STATEMENT_EXTRACT` (migracion V23,
       ver seccion "Base de datos") — mide el costo de tokens de este flujo igual que ya se
       mide `CHAT`/`CATEGORIZE`/`INSIGHT`.

     Riesgos de este pivot y como se mitigan: fidelidad de la extraccion (la IA puede
     interpretar mal una fila) — mitigado por el preview-confirm de la decision 1, el usuario
     ve y corrige antes de confirmar; costo de tokens — metrado via `STATEMENT_EXTRACT`;
     privacidad de la contrasena del PDF — se declara en el frontend (nota de privacidad en
     `app/importar/page.tsx`) y nunca se persiste en el backend; extractos escaneados
     (imagen sin capa de texto) quedan fuera de alcance de la v1, sin OCR.
3. **Deduplicacion por fecha + monto + descripcion (match exacto en monto, similitud de texto
   en descripcion), ahora sobre las filas extraidas por IA en lugar de las filas de un parser
   de columnas** — el criterio no cambio con el pivot, solo la fuente de las filas de entrada.
   `DuplicateDetector` compara contra `expenses`/`incomes` existentes del usuario en una
   ventana de +-3 dias (el extracto y el registro manual pueden diferir en un dia por huso
   horario o fecha de proceso del banco), usando normalizacion de texto + contencion o
   similitud de tokens (Jaccard) ≥ 0.34 en `DescriptionSimilarity`. Una fila marcada como
   posible duplicado se muestra igual en el preview pero destildada por default — el usuario
   decide si de verdad es una fila nueva.
4. **El vinculo de Telegram usa un codigo de un solo uso, no OAuth.** El usuario genera un
   codigo desde `/configuracion` en la app (`POST /api/integrations/telegram/link-code`,
   autenticado con JWT normal), se lo manda al bot con `/start <codigo>`, y n8n llama a
   `POST /api/integrations/telegram/confirm-link` (autenticado con un secreto compartido de
   webhook, no JWT de usuario) para completar el vinculo `telegram_chat_id -> user_id`. Es
   deliberadamente mas simple que el modelo `integration_credentials` (OAuth, tokens
   cifrados) que el roadmap bocetea para Gmail — Telegram no entrega tokens de acceso a
   terceros, solo un `chat_id` estable. Ese modelo generico de credenciales queda para
   cuando el Sprint 3 (correo) lo necesite de verdad. `confirmLink` hace upsert: si el codigo
   corresponde a un `chat_id` ya vinculado a otra cuenta, re-vincula ese chat a la cuenta
   nueva en lugar de fallar.
5. **Los endpoints que llama n8n (`/api/integrations/telegram/*`) NO pasan por el
   `JwtAuthenticationFilter` normal** — son server-to-server, autenticados con un secreto
   compartido (`app.integrations.telegram.webhook-secret`, header
   `X-Telegram-Webhook-Secret`) verificado en `TelegramWebhookFilter` con comparacion en
   tiempo constante, y agregados a la lista `permitAll()` de `SecurityConfig` (el filtro
   propio rechaza con 401 si el secreto no matchea, antes de llegar al controller). Si el
   secreto configurado esta en blanco, la integracion queda deshabilitada por diseno: todas
   las requests devuelven 401. Mismo criterio de "server-to-server con secreto compartido"
   que ya se uso para los secrets de GitHub Actions, aplicado ahora a nivel de request HTTP.
6. **El mensaje de Telegram reutiliza el mismo parser de texto libre + `categorize()` que el
   quick-add web, no un parser nuevo.** `TelegramMessageParser` es un port a Java de la misma
   logica de extraccion de monto/descripcion que ya existe en `components/quick-add/` del
   frontend — la diferencia es que la creacion del `Expense` ocurre en el backend
   (`TelegramExpenseService`) a partir del webhook de n8n, no de una llamada autenticada del
   usuario via JWT. Si la categorizacion por IA falla (proveedor caido, mal configurado),
   `TelegramExpenseService` degrada de forma segura a "sin categoria" en lugar de perder el
   registro del gasto — un gasto real nunca se descarta por una falla de la IA.

## Alcance del Sprint 2

### Backend

**Frente 1 — Importacion de extractos bancarios (extraccion generica con IA)**

1. ✅ Dominio nuevo `extractos/`, con subpaquetes `service/extraction` (extraccion de texto),
   `service/ai` (extraccion de movimientos via IA), `service/dedup` (deduplicacion),
   `model`/`model/dto` y `exception`.
2. ✅ `StatementTextExtractor` (interfaz de estrategia) con `PdfStatementTextExtractor`
   (PDFBox 3.0.5, contrasena opcional), `CsvStatementTextExtractor` (UTF-8 plano) y
   `XlsxStatementTextExtractor` (Apache POI 5.4.1), despachadas por
   `StatementTextExtractionService` segun la extension del archivo.
3. ✅ `StatementAiExtractionService`: una sola llamada a `AiChatOrchestrator` por extracto,
   con prompt de sistema en espanol que exige un arreglo JSON estricto (`date`,
   `description`, `amount`, `movementType`, `suggestedCategoryName`); parsing defensivo
   (tolera fences de markdown y texto extra); filas invalidas se descartan sin abortar el
   resto. `ParsedTransaction` (record) es la forma normalizada de cada movimiento.
4. ✅ `DuplicateDetector` + `DescriptionSimilarity`: deduplicacion por monto exacto + ventana
   de fecha de +-3 dias + similitud de descripcion (normalizacion, contencion o Jaccard de
   tokens ≥ 0.34), contra `Expense`/`Income` existentes del usuario.
5. ✅ `StatementImportService`:
   - `preview(MultipartFile file, String password)` — extrae el texto, llama a la IA para
     obtener los movimientos con categoria sugerida, corre deduplicacion, devuelve
     `StatementPreviewResponse` (lista de `ImportPreviewRow` + total + cantidad de
     duplicados). No persiste nada.
   - `confirm(StatementConfirmRequest request)` — crea `Income`/`Expense` reales solo para
     las filas que el usuario confirmo (`ImportConfirmRow`), con la categoria final que el
     usuario eligio (sugerida o corregida) y, para gastos, el metodo de pago elegido a nivel
     de importacion completa (default `OTHER`).
6. ✅ Endpoint `POST /api/statement-imports/preview` (multipart: `file` + `password`
   opcional) → `StatementPreviewResponse`.
7. ✅ Endpoint `POST /api/statement-imports/confirm` (`StatementConfirmRequest`) →
   `StatementImportResultResponse` con la cantidad de movimientos creados.
8. ✅ Validacion de entrada (tamano maximo de archivo 10 MB, extension soportada) y mensajes
   de error en espanol via `GlobalExceptionHandler` (400 para archivo/formato invalido, 422
   para texto vacio o extraccion no interpretable).

   Refactor de soporte (fuera del dominio `extractos/`, requerido para que `confirm()` pueda
   crear movimientos por cuenta del usuario dueno del extracto): `ExpenseService.createExpense`,
   `IncomeService.createIncome` y `AiCategorizationService.categorize` ganaron sobrecargas
   con `userId` explicito — los metodos existentes (que leen el usuario del
   `SecurityContext`) delegan en las nuevas. Necesario porque el flujo de Telegram (Frente 2)
   tambien crea gastos fuera de un request autenticado por JWT, sin `SecurityContext`
   disponible.

**Frente 2 — Vinculo e ingesta de Telegram (n8n)**

9. ✅ Migracion `V24__create_telegram_links.sql` — tabla `telegram_links` (`id`, `user_id` FK,
   `telegram_chat_id` UNIQUE, `linked_at`). (`V23` quedo tomada por el cambio de constraint
   de `ai_usage_events` del Frente 1 — ver seccion "Base de datos".)
10. ✅ `TelegramLinkService`: `generateLinkCode()` (delega en `TelegramLinkCodeStore`),
    `confirmLink(code, chatId)` (valida el codigo, crea o re-vincula la fila en
    `telegram_links` — upsert, ver decision de arquitectura 4).
11. ✅ `TelegramLinkCodeStore`: codigos de un solo uso en memoria (`ConcurrentHashMap`, sin
    persistir), alfabeto de 32 caracteres sin ambiguedad visual (`0/O`, `1/I` excluidos),
    longitud 8, TTL de 10 minutos, generados con `SecureRandom`, reloj inyectable
    (`Clock`) para tests deterministicos. `consume()` elimina la entrada al leerla: un
    codigo usado o expirado nunca se puede reutilizar.
12. ✅ `TelegramExpenseService`: `registerFromMessage(chatId, text)` — resuelve
    `telegram_links` → `userId`, reusa `TelegramMessageParser` (port a Java del parser de
    texto libre del quick-add) + `AiCategorizationService.categorize(userId, ...)` para
    extraer monto/descripcion/categoria, crea el `Expense`, devuelve un texto de
    confirmacion para que n8n se lo responda al usuario en Telegram. Si la categorizacion
    falla, degrada a "sin categoria" en lugar de perder el gasto (decision de arquitectura 6).
13. ✅ `TelegramWebhookFilter` — valida `X-Telegram-Webhook-Secret` contra
    `app.integrations.telegram.webhook-secret` con comparacion en tiempo constante antes de
    dejar pasar la request a los endpoints de `/api/integrations/telegram/*`. Secreto
    configurado en blanco = integracion deshabilitada, siempre 401. 401 con mensaje generico
    si no matchea (nunca confirmar si el secreto estaba "cerca" de ser correcto).
14. ✅ Endpoints (`TelegramIntegrationController`):
    - `POST /api/integrations/telegram/link-code` (JWT normal, usuario autenticado) → genera
      el codigo de vinculo.
    - `POST /api/integrations/telegram/confirm-link` (secreto de webhook) → `{ code,
      chatId }` → vincula.
    - `POST /api/integrations/telegram/expenses` (secreto de webhook) → `{ chatId, text }` →
      crea el gasto, devuelve el texto de confirmacion.
15. ✅ `SecurityConfig`: agregadas `/api/integrations/telegram/confirm-link` y
    `/api/integrations/telegram/expenses` a `permitAll()` y a la lista de rutas exentas de
    CSRF (protegidas por `TelegramWebhookFilter`, no por JWT); `/link-code` sigue detras del
    JWT normal (ya cubierto por `anyRequest().authenticated()`); `TelegramWebhookFilter`
    agregado a la cadena de filtros antes de `JwtAuthenticationFilter`.

**Frente 3 — Limpieza operativa**

16. ✅ `EmailSmokeManualTest.java` ya no existe en el arbol de trabajo — hecho de facto, el
    archivo nunca estuvo trackeado por git. Solo quedan artefactos stale de surefire bajo
    `target/`, que no requieren accion.

### Base de datos

1. ✅ `V23__add_statement_extract_event_type.sql` — recrea el CHECK constraint
   `ck_ai_usage_events_event_type` de `ai_usage_events` para incluir el nuevo valor
   `STATEMENT_EXTRACT` (ver `AiUsageEventType`, Frente 1). Es la migracion que quedo con el
   numero `V23` (siguiente disponible tras `V22__add_card_cycle_close_notification_type.sql`
   de la Fase B de tarjetas).
2. ✅ `V24__create_telegram_links.sql` — tabla `telegram_links`: `id BIGINT GENERATED ALWAYS
   AS IDENTITY PRIMARY KEY`, `user_id BIGINT NOT NULL` FK a `users(id) ON DELETE CASCADE`,
   `telegram_chat_id VARCHAR(50) NOT NULL UNIQUE`, `linked_at TIMESTAMPTZ NOT NULL DEFAULT
   now()`. Sin indice adicional — el UNIQUE de `telegram_chat_id` ya lo cubre.
3. Sin migraciones nuevas para el Frente 1 mas alla de la V23 — el preview no persiste nada,
   y `confirm()` crea `Income`/`Expense` reales usando las tablas ya existentes, sin columnas
   nuevas.

### Frontend

1. ✅ `app/importar/page.tsx` — subida de archivo (PDF/CSV/XLSX), campo de contrasena para
   PDF protegido, nota de privacidad sobre el manejo de la contrasena, tabla de preview
   (`components/statement-import/import-preview-table.tsx`: seleccion global y por fila,
   duplicados destildados por default, categoria editable por fila via `Select`), selector de
   metodo de pago a nivel de importacion completa, y boton de confirmar con el conteo de
   filas seleccionadas.
2. ✅ `lib/services/statement-import.service.ts` — primer uso de `FormData`/multipart en la
   app (subida de archivo); expone la llamada a preview y a confirm. Con test unitario
   colocado (`statement-import.service.test.ts`).
3. ✅ `lib/types/statement-import.ts` — tipos del preview y la confirmacion.
4. ✅ Botones outline "Importar extracto" en los headers de `/gastos` y `/ingresos`, en lugar
   de un item nuevo de sidebar — evita forzar una seccion completa para un flujo de uso
   ocasional.
5. ✅ `components/settings/integrations-card.tsx`, montado en `/configuracion` — seccion
   "Integraciones" con boton "Vincular Telegram" que muestra el codigo de un solo uso y las
   instrucciones (`/start <codigo>` al bot).
6. ✅ `lib/services/telegram-integration.service.ts` — `generateLinkCode()`, con test
   unitario colocado (`telegram-integration.service.test.ts`) y
   `lib/types/telegram-integration.ts`.

Backend: 500 tests en verde (54 nuevos del Frente 1, 31 nuevos del Frente 2). Frontend: 127
tests en verde, lint limpio, build de produccion exitoso.

## Definicion de terminado (DoD)

1. ✅ `POST /api/statement-imports/preview` extrae texto de PDF (con contrasena)/CSV/XLSX,
   llama a la IA para identificar los movimientos, marca correctamente los duplicados contra
   datos ya cargados, y sugiere categoria — cubierto por los tests del Frente 1 con archivos
   sinteticos.
2. ✅ `POST /api/statement-imports/confirm` crea unicamente los `Income`/`Expense` de las
   filas que el usuario selecciono, con la categoria final que el usuario eligio (sugerida o
   corregida).
3. ✅ Migraciones `V23`/`V24` aplicadas contra Postgres real en desarrollo; `mvnw test` en
   verde (500 tests).
4. ✅ Los endpoints de `/api/integrations/telegram/*` rechazan con 401 cualquier request sin
   el secreto de webhook correcto (incluye el caso de secreto en blanco = integracion
   deshabilitada) — cubierto por `TelegramWebhookFilterTest`.
5. ✅ **Validado por el usuario (2026-07-22)** — un mensaje de texto libre enviado al bot
   real de Telegram crea un `Expense` real en la cuenta vinculada, con
   monto/descripcion/categoria resueltos igual que el quick-add web. Confirmado
   funcionando desde el primer intento (a diferencia del flujo de foto, que necesito varias
   rondas de arreglos — ver seccion siguiente).
6. ✅ **Validado por el usuario (2026-07-22), parcialmente** — registrar un gasto real por
   el bot de Telegram con una foto de un recibo real (`extractos-reales/Foto-recibo.jpg`,
   carpeta local `.gitignore`d) quedo confirmado funcionando de punta a punta, con el
   usuario de desarrollo Jhon Quiceno: `✅ Gasto registrado desde la foto: D1 — $15.950
   (Comida)`. Esto costo varias rondas de debugging real — ver "Validacion end-to-end y
   hallazgos" mas abajo. **Sigue pendiente**: importar al menos un extracto bancario real
   (PDF protegido con contrasena — Bancolombia, Nu, Rappi) desde `extractos-reales/` no se
   probo en esta sesion de validacion; la calidad de la extraccion de `StatementAiExtractionService`
   contra un extracto real todavia no esta verificada.

## Validacion end-to-end y hallazgos (2026-07-22)

Probar el flujo de foto contra un recibo real (no sintetico) encontro varios bugs reales
que ningun test unitario (todos mockeados) podia atrapar. Quedan documentados porque son
el tipo de problema que va a volver a aparecer si se toca este flujo de nuevo:

1. **Bug de infraestructura n8n — credencial "fantasma"**: el workflow exportado
   (`n8n/workflows/telegram-expense-bot.json`) trae un ID de credencial placeholder
   (`REPLACE_WITH_YOUR_TELEGRAM_CREDENTIAL_ID`) en los 3 nodos que hablan con la API de
   Telegram. Asignar la credencial real a mano en la UI de n8n no bastaba: el workflow
   activo tenia ademas una copia **duplicada** (mismo nombre, quedo de un reimport previo) y
   la instancia activa de n8n seguia sirviendo la version vieja en memoria para las
   ejecuciones reales (webhook), aunque los tests manuales dentro del editor si reflejaban
   el arreglo. Solucion definitiva: borrar el workflow duplicado y reimportar limpio,
   asignando la credencial una sola vez en el que queda activo.
2. **Bug de n8n — modo de binarios "filesystem" rompe la conversion a base64**: el nodo
   Code "Armar data URI" leia `binary.data.data` esperando el base64 real, pero esta version
   de n8n (2.30.4) usa `filesystem` como modo de almacenamiento de binarios por defecto (sin
   que este seteado en ningun lado) combinado con el task runner — combinacion con un bug
   conocido de la comunidad de n8n donde `.data` devuelve el marcador interno
   `"filesystem-v2"` en vez del contenido real, y `this.helpers.getBinaryDataBuffer()`
   tampoco funciona ahi. Los 3 proveedores de IA rechazaban la imagen con errores de
   "base64 invalido" que parecian (por el mensaje) un problema de calidad de imagen, pero
   eran puramente de transporte. Arreglado agregando `N8N_DEFAULT_BINARY_DATA_MODE=default`
   (memoria) a la config de n8n en `docker-compose.yml`, evitando la combinacion rota por
   completo.
3. **Confiabilidad real de vision, no solo teorica**: una vez resuelto el problema de
   transporte, la extraccion funcionaba pero era inconsistente segun que proveedor
   respondiera — NVIDIA (`nemotron-nano-12b-v2-vl`) devuelve HTTP 200 con JSON valido aunque
   el contenido este mal (agarra texto legal/tributario del recibo como si fuera el nombre
   del comercio), mientras que Gemini fue mas consistente en la misma foto probada varias
   veces. Como un JSON bien formado no es distinguible de uno correcto a nivel de
   HTTP/parsing, el failover no detecta esto solo — se reordeno la prioridad de proveedores
   para que Gemini vaya primero (ver `AiProviderRegistry.DEFAULT_PRIORITY`).
4. **Mejora de prompt**: se le agrego a `ReceiptExtractionService` la instruccion explicita
   de ignorar texto legal/tributario (Gran Contribuyente, resoluciones DIAN, NIT, telefonos)
   al identificar el nombre del comercio, y usar una descripcion generica si no es legible
   con certeza — antes el modelo (sobre todo el proveedor de respaldo) confundia ese texto
   con el nombre del negocio.

Trabajo adicional de robustez de IA hecho durante esta validacion (excede el Frente 2
original pero vive en los mismos archivos): catalogo de proveedores ampliado con **Gemini**
(`gemini-3.5-flash`, multimodal) y **Groq** (catalogado, sin key real todavia); failover de
`AiChatOrchestrator#completeVision` reescrito para iterar todos los proveedores con modelo
de vision configurado (antes bypasseaba a NVIDIA unicamente); telemetria por intento
(latencia, exito/fallo, tipo de error, costo estimado) via `AiUsageEvent` + migracion
`V25__add_ai_usage_event_telemetry.sql`; prioridad de proveedores configurable **por tipo de
tarea** (`app.ai.task-priority.*`) ademas de la global; y ampliacion del bot conversacional
(consultas de deudas, desglose de ingresos por categoria, periodos `LAST_MONTH`/`YEAR`,
fallback amigable de "no entendi").

## Referencia de endpoints (Sprint 2)

```http
# Extractos bancarios
POST   /api/statement-imports/preview               # multipart: file + password (opcional)
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
- **El Frente 2 (bot de Telegram) esta validado end-to-end con datos reales, incluyendo
  fotos de recibos.** El Frente 1 (extraccion de extractos) sigue con la extraccion por IA
  **sin validar contra un extracto real** — reemplaza a la advertencia original sobre el
  formato de columnas de Bancolombia/Davivienda, que quedo obsoleta con el pivot a
  extraccion generica (decision de arquitectura 2). Antes de dar el Frente 1 por cerrado en
  la practica, hay que correr `preview` contra al menos un extracto real de cada banco del
  usuario (Bancolombia, Nu, Rappi) y revisar si la IA identifica bien fecha/monto/tipo de
  movimiento. Dos gaps conocidos de ese flujo, documentados y sin resolver por decision
  explicita de alcance (no forman parte de este sprint): no hay quality gate para detectar
  texto extraido corrupto/vacio mas alla del caso binario vacio-o-no, y no hay verificacion
  cruzada de los montos extraidos contra un total declarado en el extracto.
- **El workflow de n8n ya esta en el repo**: `n8n/workflows/telegram-expense-bot.json`
  (Telegram Trigger → deteccion de `/start <codigo>` vs texto libre/foto → llamadas a
  `/api/integrations/telegram/*` con el header `X-Telegram-Webhook-Secret` → respuesta al
  chat). El setup del bot desde cero (BotFather, secreto, importacion, prueba) esta
  documentado en `n8n/README.md` — incluye la tabla de solucion de problemas actualizada con
  los hallazgos de la seccion "Validacion end-to-end" de arriba.
