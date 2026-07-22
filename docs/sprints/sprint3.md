# Sprint 3 - Integración de Correo (Gmail API + Pub/Sub) (KoroFin SaaS)

Tercer sprint de la fase SaaS. Ataca la sección "Automatización con IA de correo/SMS/notificaciones"
del roadmap unificado (`docs/roadmap-saas-cuentas-reales.md`) — la pieza central de la
visión de largo plazo del producto: capturar gastos/ingresos automáticamente a partir de
las notificaciones que el banco ya le manda al usuario por correo, en vez de depender
100% de carga manual o del bot de Telegram.

**Estado: propuesto, sin empezar.** Este documento es un punto de partida para planificar
el sprint, no una implementación cerrada — el roadmap advierte explícitamente que el
alcance completo "probablemente es demasiado grande para un solo sprint" (ver Nota al
final). Las decisiones de arquitectura marcadas como abiertas deben resolverse antes de
escribir código.

## Antes de empezar

La rama de trabajo de este sprint se crea a partir de `develop`:

```bash
git checkout develop
git checkout -b feature/sprint-3-integracion-correo
```

Prerrequisito de producto: el bot de Telegram del Sprint 2 ya validó que el patrón
"n8n/webhook → backend → IA → registro real" funciona con datos reales — este sprint
reutiliza esa misma columna vertebral, cambiando la fuente de "mensaje de Telegram" a
"correo entrante".

## Objetivo

1. Conectar la cuenta de Gmail del usuario (OAuth2, scope mínimo `gmail.readonly`) y
   recibir notificaciones casi en tiempo real de correos nuevos vía Gmail API + Google Cloud
   Pub/Sub — **no polling**.
2. Extraer con IA los datos financieros relevantes de cada correo (monto, comercio/fuente,
   fecha, tipo: ingreso/gasto/servicio), con un score de confianza.
3. Bandeja de revisión: si la confianza es alta y el usuario habilitó auto-creación, crear
   el movimiento directo; si no, dejarlo en revisión pendiente para que el usuario confirme
   o edite antes de que se cree el `Income`/`Expense`/`RecurringPayment` real, siempre
   vinculado al mensaje de origen (trazabilidad y auditoría).
4. Dejar la base para que un futuro proveedor de correo no-Gmail (IMAP genérico con IDLE, o
   polling si el proveedor no soporta IDLE) pueda sumarse sin rediseñar el pipeline —
   aunque el alcance de este sprint es Gmail únicamente.

**Explícitamente fuera de alcance de este sprint** (ver roadmap, sección "Automatización
con IA de correo/SMS/notificaciones"):

- **SMS** — inviable en Android (requiere ser manejador por defecto de SMS/Teléfono, Google
  rechazaría la app en revisión) e inviable en iOS (sandboxing sin excepción). Descartado
  como fuente, no es una limitación de KoroFin sino de las plataformas.
- **Notificaciones push de apps bancarias** (`NotificationListenerService`) — solo viable en
  Android y **requiere la futura app móvil nativa**, no se puede hacer desde la web. Queda
  para la fase "App móvil" del roadmap.
- **Motor de reglas de automatización** (`automation_rules`, que el usuario pueda anular la
  clasificación de la IA con reglas tipo "todo lo de tal remitente va a tal categoría") —
  el roadmap lo agrupa con este sprint por ser la misma pieza de dominio, pero es
  razonable partirlo a un Sprint 3b si el 3a (conexión + ingesta) ya resulta grande por sí
  solo — ver Nota final.

## Decisiones de arquitectura (a resolver antes de implementar)

Estas son las preguntas abiertas que el roadmap deja boceteadas pero sin cerrar — conviene
responderlas explícitamente en la fase de diseño de este sprint, no descubrirlas a mitad de
implementación:

1. **¿Cómo se corta el sprint si resulta demasiado grande?** Candidato natural: **3a**
   (conexión Gmail + ingesta + persistencia de `ingested_messages`, sin extracción con IA
   todavía) y **3b** (extracción con IA + score de confianza + bandeja de revisión +
   creación del movimiento real). El roadmap ya sugiere esta partición.
2. **Modelo de `integration_credentials`** — el roadmap bocetea una tabla genérica
   (`provider`, `encrypted_access_token`, `encrypted_refresh_token`, `token_expires_at`,
   `scopes`, `status`, `last_synced_at`) pensada para reusarse con futuros proveedores
   (bancos vía webhook, etc.), separada de la config de proveedores de IA del operador. El
   vínculo de Telegram del Sprint 2 usó su propio mecanismo simple (código de un solo uso,
   sin OAuth) — **no** reusar ese patrón acá, este sprint sí necesita el modelo genérico con
   cifrado at-rest real (AES-GCM con clave gestionada, nunca una constante en el código).
3. **Renovación del `watch()` de Gmail** — Gmail requiere renovar el `watch()` cada ~7 días.
   Definir el mecanismo (cron diario recomendado por la documentación de Gmail) y qué pasa
   si la renovación falla silenciosamente (¿alerta al usuario? ¿reintento con backoff?).
4. **Forma de `ingested_messages`** y su ciclo de vida — el roadmap bocetea columnas
   (`source`, `external_ref`, `encrypted_content`, `processing_status`, `matched_expense_id`,
   `matched_income_id`, `ai_confidence`, `error_detail`). Definir en la fase de diseño: el
   `encrypted_content` debe truncarse/purgarse después de procesar con éxito (ver
   "Privacidad y retención" en el roadmap) — solo guardar los campos extraídos + referencia
   al movimiento generado, no el correo completo indefinidamente. Retención corta (30-90
   días) para lo no procesado.
5. **Umbral de confianza para auto-creación vs. bandeja de revisión** — la bandeja de
   revisión **no es opcional al principio**, es el control de calidad contra falsos
   positivos en dinero real. Definir el umbral inicial (conservador) y si es configurable
   por el usuario desde el arranque o se agrega después.
6. **Qué proveedor de IA usa este flujo** — reusar `AiChatOrchestrator` (failover
   multi-proveedor ya existente, con Gemini/NVIDIA/OpenCode/OpenRouter/Groq desde el Sprint
   2) es la opción obvia; definir qué `AiUsageEventType` nuevo corresponde (o si conviene
   uno especifico tipo `EMAIL_EXTRACT`, en la misma línea que `STATEMENT_EXTRACT`), y si este
   flujo necesita su propia prioridad de tarea vía `app.ai.task-priority.*` (mecanismo ya
   construido en el Sprint 2, ver `AiProviderRegistry#enabledInPriorityOrder(AiUsageEventType)`).

## Alcance propuesto

### Backend — Frente 1: Conexión Gmail e ingesta (candidato a Sprint 3a)

- [ ] Dominio nuevo `integraciones/` (o extender el existente de Sprint 2) con el modelo
  genérico `integration_credentials` (decisión 2), cifrado at-rest.
- [ ] Flujo OAuth2 completo: autorización, intercambio de código, refresh token, revocación.
- [ ] Registro de `watch()` sobre la casilla del usuario tras conectar la cuenta.
- [ ] Endpoint webhook que recibe el push de Pub/Sub, valida la notificación, y encola el
  procesamiento (no procesar sincrónicamente dentro del webhook).
- [ ] Job de renovación del `watch()` (decisión 3).
- [ ] Tabla `ingested_messages` (decisión 4) y su persistencia inicial (`status = PENDING`).

### Backend — Frente 2: Extracción con IA y bandeja de revisión (candidato a Sprint 3b)

- [ ] Extracción con IA del correo ingerido: monto, comercio/fuente, fecha, tipo de
  movimiento, score de confianza — vía `AiChatOrchestrator` (decisión 6).
- [ ] Lógica de auto-creación vs. bandeja de revisión según el umbral de confianza (decisión
  5) y la preferencia del usuario.
- [ ] Endpoints de la bandeja de revisión: listar pendientes, confirmar (crea el movimiento
  real, vinculado al `ingested_messages` de origen), editar antes de confirmar, descartar.
- [ ] Purga/truncado del `encrypted_content` tras procesar con éxito (decisión 4).

### Frontend

- [ ] Pantalla de conexión de Gmail (`/configuracion` → Integraciones, mismo lugar que
  Telegram del Sprint 2) — iniciar OAuth, mostrar estado de conexión, desconectar.
- [ ] Bandeja de revisión: lista de movimientos pendientes de confirmar, con la info
  extraída y la posibilidad de editar/confirmar/descartar antes de crear el movimiento real.
- [ ] Indicador de confianza por movimiento pendiente (visual simple, no necesita ser
  elaborado en la v1).

### Base de datos (boceto, ver decisión 4 antes de convertir a DDL final)

```sql
-- Sprint 3a
integration_credentials (id, user_id, provider[GMAIL], encrypted_access_token,
                         encrypted_refresh_token, token_expires_at, scopes, status,
                         last_synced_at, created_at, updated_at)

ingested_messages     (id, user_id, source[EMAIL], external_ref, encrypted_content,
                       received_at, processing_status[PENDING|PROCESSED|FAILED|IGNORED],
                       processed_at, matched_expense_id, matched_income_id, ai_confidence,
                       error_detail, created_at)

-- Sprint 3b (o Sprint 3, fuera de alcance del núcleo si se corta)
automation_rules      (id, user_id, name, match_type[sender|regex|keyword], match_value,
                       target_category_id, is_active, priority, created_at, updated_at)
```

## Definición de terminado (DoD) — propuesto

1. [ ] El usuario conecta su cuenta de Gmail desde `/configuracion` vía OAuth2, y KoroFin
   recibe notificaciones de correos nuevos casi en tiempo real (sin polling).
2. [ ] Un correo real de notificación bancaria (ej. "compra aprobada por $X en Y comercio")
   se extrae correctamente (monto, comercio, tipo) y aparece en la bandeja de revisión, o se
   auto-crea si supera el umbral de confianza configurado.
3. [ ] Confirmar un pendiente de la bandeja de revisión crea el `Income`/`Expense` real,
   vinculado al mensaje de origen.
4. [ ] El `watch()` se renueva automáticamente antes de expirar (verificar con al menos un
   ciclo completo de ~7 días, o simular el vencimiento en un entorno de prueba).
5. [ ] El contenido crudo del correo se purga/trunca después de procesar con éxito — no
   queda el correo completo persistido indefinidamente.
6. [ ] `./mvnw.cmd test` en verde con la cobertura nueva; validación con al menos una cuenta
   de Gmail real y un correo real de notificación bancaria (no solo con datos sintéticos).

## Notas

- **Este sprint es candidato fuerte a partirse en dos** (3a: conexión + ingesta, 3b:
  extracción + revisión) — evaluarlo en el kickoff, no a mitad de implementación. El
  roadmap ya lo advierte explícitamente.
- El motor de reglas de automatización (`automation_rules`) puede quedar fuera del alcance
  inicial sin bloquear el valor central del sprint (bandeja de revisión + auto-creación) —
  es una mejora sobre la clasificación de la IA, no un requisito para que el flujo básico
  funcione.
- Este sprint depende conceptualmente de que el patrón "ingesta externa → IA → revisión →
  movimiento real" ya esté probado con datos reales — el Sprint 2 lo validó con el bot de
  Telegram (ver `docs/sprints/sprint2.md`, sección "Validación end-to-end"). La lección más
  importante de esa validación: **los bugs reales aparecen en la integración con el
  proveedor externo real, no en la lógica de negocio propia** — presupuestar tiempo de
  debugging real con una cuenta de Gmail real antes de dar este sprint por cerrado.
- El Nivel 3 (Open Finance/Belvo, Sprint 6) sigue sin depender de este sprint — son fuentes
  de datos independientes.
