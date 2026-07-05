# 🏦 FinSmart — MVP Sprint Board (v3, sin n8n)

> **Stack:** Java + Spring Boot · PostgreSQL · Next.js
> **Total:** 6 Sprints · 105 tareas
> **Estados:** `[ ]` Pendiente · `[~]` En progreso · `[x]` Completado

---

## 📝 Notas de la revisión v3

1. **n8n se eliminó del stack.** Los 7 workflows del Sprint 5 se reemplazan por automatizaciones nativas del backend: jobs `@Scheduled`, eventos de aplicación (`ExpenseCreatedEvent`) y endpoints de IA multi-proveedor. Mismo resultado, menos infraestructura, y todo testeable con JUnit. Ver `docs/sprints/sprint5.md` para el detalle.
2. **La IA es multi-proveedor por diseño, configurada a nivel de aplicación.** Un único cliente OpenAI-compatible (`RestClient`) cubre NVIDIA NIM, OpenCode Zen y OpenRouter. Las API keys las carga el operador de la app vía variables de entorno (no hay carga de key ni selección de proveedor por parte del usuario final); `AiProviderRegistry` resuelve los proveedores habilitados y `AiChatOrchestrator` reintenta automáticamente con el siguiente proveedor configurado si el actual falla, de forma transparente para el usuario.
3. **El chat IA se movió de `/api/analysis/chat` a `/api/ai/*`**, porque la IA pasó a ser un dominio propio (chat, historial, insights, clasificación automática, estado de proveedores) y no un apéndice del motor de análisis.
4. **Email de notificaciones via Brevo SMTP** (gratuito, 300/día, sin dominio propio), opcional y degradable: sin credenciales, la app funciona solo con notificaciones in-app. Otros canales evaluados quedan en `docs/notifications-future.md`.
5. **Sprint 5 suma la migración `V10` (índices de salud del esquema)** sobre las ya previstas `V7` (notificaciones) y `V8` (mensajes de IA); no se crea tabla para los proveedores de IA (las API keys viven solo en variables de entorno del operador), por lo que el próximo número disponible para el Sprint 6 vuelve a ser `V11`.

---

## 📝 Notas de la revisión v2

Respecto al tablero original, se hicieron 4 cambios estructurales tras revisar la base de datos:

1. **Las tablas ya no se crean todas en Sprint 1.** Cada tabla se crea en el sprint donde su funcionalidad se construye, mediante una migración Flyway numerada (`V1`, `V2`, ...). Así cada sprint se puede probar con un esquema mínimo, sin arrastrar tablas vacías de features futuras.
2. **`payment_method` nace con `expenses`** en Sprint 2, no como columna agregada después. El formulario de gastos de Sprint 2 ya lo pide, así que la columna debe existir desde ahí.
3. **Nueva tabla `debt_payments`** (Sprint 3) — antes, abonar a una deuda sobrescribía `remaining_amount` sin dejar historial. Ahora cada abono queda registrado.
4. **`recurring_payments` y `expenses` quedan conectadas** mediante la columna `recurring_payment_id` (Sprint 3) y el endpoint `PATCH /api/recurring/{id}/pay`, que genera el gasto correspondiente al marcar un servicio como pagado. Antes eran dos formas de "gasto recurrente" sin relación entre sí.
5. **Nueva tabla `ai_messages`** (Sprint 5) — el frontend promete "historial de conversación" en el asistente IA, pero no existía dónde guardarlo.
6. **`notifications` se movió de Sprint 1 a Sprint 5**, porque ahí es donde se crea la entidad `Notification` y el panel del navbar que la consume.
7. **Sprint 2 sumó gestión completa de categorías en el frontend** (crear/editar/eliminar en `/categorias`, con alta rápida desde el propio selector de categoría en los formularios de ingreso/gasto), más allá del alcance original que solo pedía cargar categorías reales en los formularios. El backend ya lo soportaba desde el inicio (`/api/categories` con CRUD completo); solo faltaba la capa de UI, que fue verificada end-to-end (build, lint y tests en verde) antes de darse por completada.
8. **Sprint 3 — decisiones no explícitas en el alcance original:** `total_amount`/`remaining_amount` de una deuda son inmutables desde `PUT /api/debts/{id}` (solo cambian por creación o por `DebtPayment`), para no romper la trazabilidad que pide la migración `V4`. `PATCH /api/recurring/{id}/pay` devuelve `{recurringPayment, expenseId}` en vez de solo el servicio actualizado, para que el cliente pueda navegar al gasto generado sin una consulta extra. El descuento de `remaining_amount` y el avance de `next_payment_date` se implementaron como `UPDATE` atómico condicional en el repositorio (no lectura-validación-escritura), tras detectarse en revisión que dos pagos concurrentes sobre la misma deuda o el mismo servicio podían pisarse entre sí y perder o duplicar movimientos.

---

## Leyenda de capas

| Etiqueta | Capa |
|----------|------|
| `[BE]` | Backend — Spring Boot |
| `[FE]` | Frontend — Next.js |
| `[DB]` | Base de datos — PostgreSQL |

---

## 🟦 Sprint 1 — Base del Sistema (JWT Real)
> Proyecto, autenticación JWT real y lo mínimo de base de datos para poder loguearse · **15 tareas**

### Backend
- [x] `[BE]` Setup proyecto Spring Boot + estructura de capas (`config/controller/service/repository/model/dto/mapper/exception`)
- [x] `[BE]` Entidad `User` + `UserRepository` + `UserService` (registro y login con seguridad)
- [x] `[BE]` Endpoint `POST /api/users/register` — validaciones de email único y contraseña
- [x] `[BE]` Endpoint `POST /api/users/login` — respuesta con `accessToken` JWT real + cookie HttpOnly de refresh token
- [x] `[BE]` Endpoints `POST /api/users/refresh` y `POST /api/users/logout` con rotación/revocación de refresh token
- [x] `[BE]` Manejo global de excepciones (`GlobalExceptionHandler`) + DTOs de error estándar
- [x] `[BE]` Configurar CORS y filtro JWT stateless para peticiones autenticadas desde frontend Next.js

### Base de datos
- [x] `[DB]` Configurar conexión PostgreSQL + Spring Data JPA + Flyway para migraciones
- [x] `[DB]` **Migración `V1`** — tabla `users` (lo único que necesita el login/registro)
- [x] `[DB]` **Migración `V2`** — tabla `refresh_tokens`, para persistencia y rotación segura de sesiones

### Frontend
- [x] `[FE]` Configurar cliente HTTP (axios) con access token en memoria y refresh automático vía `/api/users/refresh`
- [x] `[FE]` Reemplazar mock de `AuthContext` con llamadas reales a `POST /api/users/register` y `/login`
- [x] `[FE]` Persistir usuario en `localStorage`, usar cookie HttpOnly para refresh token y proteger rutas con `proxy.ts`
- [x] `[FE]` Pantalla Login conectada al backend — mostrar errores de validación del servidor
- [x] `[FE]` Pantalla Registro conectada al backend — feedback visual de éxito/error

---

## 🟧 Sprint 2 — Ingresos y Gastos
> CRUD completo de movimientos financieros, con método de pago desde el inicio · **17 tareas**

### Backend
- [x] `[BE]` Entidad `Category` + `CategoryRepository` + `CategoryService` — categorías por usuario (`INCOME`/`EXPENSE`)
- [x] `[BE]` CRUD endpoints `/api/categories` — `GET`, `POST`, `PUT`, `DELETE` filtradas por `userId`
- [x] `[BE]` Entidad `Income` + `IncomeRepository` + `IncomeService` con filtros por mes/año/fuente
- [x] `[BE]` CRUD endpoints `/api/incomes` — `GET` (con paginación), `POST`, `PUT`, `DELETE`
- [x] `[BE]` Entidad `Expense` (incluye `paymentMethod`) + `ExpenseRepository` + `ExpenseService` con filtros por categoría/fecha/método de pago
- [x] `[BE]` CRUD endpoints `/api/expenses` — `GET` (con paginación), `POST`, `PUT`, `DELETE`
- [x] `[BE]` Mappers `Income`/`Expense` Entity ↔ DTO para no exponer entidades directamente
- [x] `[BE]` Tests unitarios de servicios — `CategoryServiceTest`, `IncomeServiceTest`, `ExpenseServiceTest`
- [x] `[BE]` Tests de integración de controllers — `CategoryControllerTest`, `IncomeControllerTest`, `ExpenseControllerTest`

### Base de datos
- [x] `[DB]` **Migración `V3`** — tablas `categories`, `incomes`, `expenses` (con columna `payment_method` desde el inicio, `CHECK` en `CASH`/`DEBIT_CARD`/`CREDIT_CARD`/`TRANSFER`/`OTHER`)

### Frontend
- [x] `[FE]` Página `/ingresos` conectada al backend — lista con paginación y filtros por mes
- [x] `[FE]` Modal/formulario crear y editar ingreso — select de fuente + monto + fecha
- [x] `[FE]` Página `/gastos` conectada al backend — tabla con filtros por categoría y rango de fechas
- [x] `[FE]` Modal/formulario crear y editar gasto — select de categoría real + método de pago
- [x] `[FE]` Cargar categorías reales desde `/api/categories` en los formularios de gasto/ingreso
- [x] `[FE]` Eliminación de registros con confirmación + feedback toast de éxito/error
- [x] `[FE]` Loading skeletons mientras se cargan los datos del backend

---

## 🟨 Sprint 3 — Deudas y Servicios Recurrentes
> Gestión de obligaciones, pagos periódicos, y trazabilidad de abonos y pagos generados · **18/18 tareas**

### Backend
- [x] `[BE]` Entidad `Debt` + `DebtRepository` + `DebtService` — campos: nombre, monto total, restante, interés, vencimiento
- [x] `[BE]` CRUD endpoints `/api/debts` — `GET`, `POST`, `PUT`, `DELETE` (sin lógica de pago embebida; eso vive en `DebtPayment`)
- [x] `[BE]` Entidad `DebtPayment` + `DebtPaymentRepository` + `DebtPaymentService` — registra abonos individuales y actualiza `remaining_amount` de la deuda al crearse
- [x] `[BE]` Endpoints `/api/debts/{id}/payments` — `POST` (crear abono) y `GET` (historial de abonos de esa deuda)
- [x] `[BE]` Entidad `RecurringPayment` + Repository + Service — frecuencia `MONTHLY`/`WEEKLY`, `next_payment_date`, `is_active`
- [x] `[BE]` CRUD endpoints `/api/recurring` — calcular automáticamente `next_payment_date` al crear/actualizar
- [x] `[BE]` Lógica de activación/desactivación de servicios (`PATCH /api/recurring/{id}/toggle`)
- [x] `[BE]` Endpoint `PATCH /api/recurring/{id}/pay` — crea un `Expense` vinculado (`recurring_payment_id`) con el monto del servicio, y recalcula `next_payment_date`

### Base de datos
- [x] `[DB]` **Migración `V4`** — tablas `debts`, `debt_payments`, `recurring_payments`
- [x] `[DB]` **Migración `V5`** — columna `recurring_payment_id` (FK nullable → `recurring_payments`, `ON DELETE SET NULL`) en `expenses`, para trazar qué gastos vinieron de un servicio recurrente

### Frontend
- [x] `[FE]` Página `/deudas` conectada al backend — listado con progreso de pago (`remaining/total`)
- [x] `[FE]` Formulario crear/editar deuda — campos de tasa de interés y fecha de vencimiento
- [x] `[FE]` Panel de registro de abono a deuda — conectado a `POST /api/debts/{id}/payments`, refresca `remaining_amount` en tiempo real
- [x] `[FE]` Historial de abonos por deuda — conectado a `GET /api/debts/{id}/payments`
- [x] `[FE]` Página `/servicios` conectada al backend — tarjetas con próxima fecha de pago
- [x] `[FE]` Formulario crear/editar servicio recurrente — selector de frecuencia y monto
- [x] `[FE]` Toggle de activación/desactivación de servicios desde la UI con actualización inmediata
- [x] `[FE]` Botón "Marcar como pagado" en tarjeta de servicio — conectado a `PATCH /api/recurring/{id}/pay`

---

## 🟥 Sprint 4 — Motor Financiero + Dashboard Real
> Cálculos automáticos y dashboard conectado · **15/15 tareas**

### Backend
- [x] `[BE]` `FinancialAnalysisService.java` — balance mensual (`totalIngresos - totalGastos`) por `userId` y periodo, incluyendo gastos generados por servicios recurrentes pagados
- [x] `[BE]` Cálculo porcentaje gasto/ingreso — alertar si supera el 80%
- [x] `[BE]` Cálculo nivel de endeudamiento — suma deudas activas vs ingresos
- [x] `[BE]` Cálculo ahorro mensual real y proyección de ahorro anual
- [x] `[BE]` Top categorías de gasto — ranking de categorías por monto mensual
- [x] `[BE]` Endpoint `GET /api/analysis/summary` — balance, ahorro, % gasto, endeudamiento, top categorías
- [x] `[BE]` Lógica de recomendaciones por reglas — si `gasto_comida > 30%` → recomendación específica
- [x] `[BE]` Endpoint `GET /api/analysis/recommendations` — lista de alertas y sugerencias del motor

### Base de datos
- [x] `[DB]` **Migración `V6`** — tabla `financial_analysis`: snapshot mensual por usuario (`total_income`, `total_expense`, `savings`, `expense_ratio`, `debt_ratio`, `top_category_id`, `period_year`, `period_month`), `UNIQUE(user_id, period_year, period_month)`

### Frontend
- [x] `[FE]` Dashboard conectado a `GET /api/analysis/summary` — Balance Card con datos reales
- [x] `[FE]` Stats Cards de ingresos, gastos, deudas y ahorros con datos reales del backend
- [x] `[FE]` Gráfico Ingresos vs Gastos (Recharts) alimentado por datos reales del período
- [x] `[FE]` Gráfico de distribución por categoría con datos reales de top categorías
- [x] `[FE]` Panel de alertas y recomendaciones conectado a `GET /api/analysis/recommendations`
- [x] `[FE]` Transacciones recientes — últimos 10 movimientos combinados (ingresos + gastos)

**Sprint 4 — decisiones no explícitas en el alcance original:** `GET /api/analysis/summary` embebe la serie de 6 meses, el ranking de top categorías y las últimas 10 transacciones combinadas (ingresos + gastos) en una sola respuesta, para que el dashboard completo se alimente con una única llamada en vez de múltiples endpoints. El motor de recomendaciones implementa la regla "gasto en comida > 30% del ingreso" como una combinación de (a) una regla genérica "la categoría con mayor gasto supera el 30% del ingreso" y (b) un match case-insensitive por nombre contra términos de comida (`comida`, `alimentación`, `mercado`, `supermercado`, `restaurante`), ya que las categorías son por-usuario y de nombre libre — no existe un set semilla global contra el cual matchear por id. El snapshot de `financial_analysis` se persiste con un `INSERT ... ON CONFLICT (user_id, period_year, period_month) DO UPDATE` atómico (mismo patrón de updates atómicos del Sprint 3), para que recalcular un período ya snapshoteado nunca produzca una fila duplicada. `GET /api/analysis/recommendations` reutiliza `getSummary()` internamente (evita recalcular dos veces) — como efecto colateral, cada llamada a `/recommendations` también refresca el snapshot, lo cual es intencional/inofensivo porque el upsert es idempotente para el mismo período. El balance y el ahorro ya cuentan los gastos generados por `PATCH /api/recurring/{id}/pay` sin lógica adicional, porque esos gastos ya son filas reales de `expenses` (Sprint 3) y quedan incluidos al sumar los gastos del período. El nivel de endeudamiento usa `debts.remaining_amount` (no `total_amount`) frente a los ingresos del período, ya que `Debt` no tiene un flag `status`/`isActive` — una deuda con `remaining_amount = 0` ya contribuye `0` a la suma sin necesitar un filtro aparte. El frontend expone `TopCategoryResponse.totalAmount` y `MonthlySeriesPoint.totalIncome`/`totalExpense` con esos nombres exactos (no `amount`/`income`/`expense`) para calzar 1:1 con los DTOs reales del backend — se detectó y corrigió un desvío de nombres entre lo que el agente de frontend infirió del plan y el DTO real ya construido por el agente de backend. Datos de ejemplo para Jhon Quiceno (`user_id=2`) cargados vía script SQL de desarrollo (`smart-finance-backend/src/main/resources/db/dev-seed/seed_jhon_quiceno.sql`, fuera de Flyway) y verificados end-to-end contra Postgres real + dashboard en navegador; se detectó y corrigió una categoría de ingreso duplicada (`Salario` vs. una `Salario Mensual` preexistente de pruebas manuales) y un ingreso de julio redundante que dicha categoría duplicada arrastraba. La verificación visual en navegador encontró y corrigió dos bugs reales del frontend: el listado de transacciones recientes usaba `transaction.id` como `key` de React sin distinguir tipo, colisionando cuando un ingreso y un gasto comparten el mismo id (secuencias independientes); y la leyenda del gráfico de categorías mostraba siempre `0%` porque `percentage` llega del backend como fracción (`0.32`) y el componente no la multiplicaba por 100. Una revisión independiente (agente en contexto limpio) encontró y se corrigieron además: un N+1 en `buildRecentTransactions` (`findTop10ByUser_IdOrderByDateDescIdDesc` sin `JOIN FETCH category`, hasta 20 SELECTs extra por llamada a `/summary`; reemplazado por `findRecentByUserId(userId, Pageable)` con `LEFT JOIN FETCH`), y dos indicadores de tendencia incorrectos en `StatsCards`: la card de "Deudas Totales" siempre se pintaba en rojo (`debtsChange < 0 ? "up" : "down"` con `debtsChange` fijo en `0`, ya que el backend no expone una serie histórica de deuda) y la card de "Ingresos del Mes" tenía `trend="up"` fijo sin mirar el signo real del cambio.

---

## 🟩 Sprint 5 — Asistente IA Multi-Proveedor + Notificaciones + Automatizaciones Nativas
> IA conectada a los datos del usuario, notificaciones in-app + email, y jobs nativos que reemplazan n8n · **26/26 tareas** · Detalle en `docs/sprints/sprint5.md`

### Backend
- [x] `[BE]` Entidad `Notification` + Repository + Service — tipo, título, mensaje, `is_read`/`read_at` por usuario
- [x] `[BE]` Endpoints `GET /api/notifications` (paginado), `GET /api/notifications/unread-count`, `PATCH /{id}/read`, `PATCH /read-all`
- [x] `[BE]` Entidad `NotificationPreference` + `GET/PUT /api/notifications/preferences` — toggles por tipo + email
- [x] `[BE]` `NotificationSender` (puerto) + adaptador in-app + `BrevoEmailAdapter` (`spring-boot-starter-mail`, `@Async`, degradable sin credenciales)
- [x] `[BE]` `SupportedAiProvider` (catálogo) + `AiProviderProperties` (bindeo de variables de entorno) + `AiProviderRegistry` (proveedores habilitados + orden de prioridad) + `AiChatOrchestrator` (failover automático) + `GET /api/ai/providers/status` — solo lectura, nunca expone una key
- [x] `[BE]` `AiChatClient` (`RestClient` → `{baseUrl}/chat/completions`, OpenAI-compatible) + jerarquía `AiProviderException` mapeada con mensajes claros en español
- [x] `[BE]` `FinancialContextBuilder` — system prompt en español con resumen del motor financiero + movimientos recientes + deudas + servicios
- [x] `[BE]` Entidad `AiMessage` (rol, `kind` `CHAT`/`INSIGHT`, proveedor, modelo) + `POST /api/ai/chat` (persiste pregunta y respuesta) + `GET /api/ai/chat/history`
- [x] `[BE]` `GET /api/ai/insights` + `POST /api/ai/insights/generate` — recomendaciones personalizadas por IA con contexto del usuario
- [x] `[BE]` `POST /api/ai/categorize` — clasificación automática: la IA sugiere categoría existente según la descripción del gasto
- [x] `[BE]` `@EnableScheduling` + `PaymentReminderJob` — diario: servicios y deudas que vencen en 3-5 días → notificación + email
- [x] `[BE]` Alerta de sobregasto por evento — `ExpenseCreatedEvent` al crear gasto: si gasto del mes > 80% del ingreso → alerta (una por período)
- [x] `[BE]` `WeeklySummaryJob` (resumen semanal → notificación + email) + `InactivityReminderJob` (sin movimientos 3+ días, una por racha)
- [x] `[BE]` `GET /api/analysis/prediction` — predicción fin de mes: saldo proyectado + gasto máximo diario recomendado; el job diario alerta si es negativa
- [x] `[BE]` Tests unitarios + integración de servicios, controllers y jobs (clock/fixtures controlados)

### Base de datos
- [x] `[DB]` **Migración `V7`** — tablas `notifications` (índices `(user_id, is_read)`, `(user_id, created_at)`) y `notification_preferences` (`UNIQUE(user_id)`)
- [x] `[DB]` **Migración `V8`** — tabla `ai_messages` (rol, kind, proveedor, modelo, índice `(user_id, created_at)`)
- [x] `[DB]` **Migración `V10`** — índices de salud: `(user_id, date)` en `expenses`/`incomes`, `(is_active, next_payment_date)` en `recurring_payments`, `(user_id, due_date)` en `debts`

### Frontend
- [x] `[FE]` Página `/asistente-ia` conectada — chat real (`POST /api/ai/chat`), historial, selector de proveedor/modelo, errores en español
- [x] `[FE]` Panel de insights financieros del asistente (`GET /api/ai/insights` + regenerar)
- [x] `[FE]` Navbar: badge de no leídas real + panel conectado a `/api/notifications` + marcar leída/todas
- [x] `[FE]` Tarjeta de predicción fin de mes en dashboard (`GET /api/analysis/prediction`)
- [x] `[FE]` Sección de insights IA en dashboard (último insight + link al asistente)
- [x] `[FE]` `/configuracion`: tarjeta de solo lectura con el estado de los proveedores IA (`GET /api/ai/providers/status`) + preferencias de notificación persistidas
- [x] `[FE]` Botón "Sugerir categoría" (IA) en formulario de gasto (`POST /api/ai/categorize`)
- [x] `[FE]` Tests de servicios/schemas nuevos (vitest)

**Sprint 5 — decisiones no explícitas en el alcance original:** La revisión adversarial en contexto limpio encontró y se corrigieron bugs críticos invisibles para los tests con mocks: (1) el dedupe de notificaciones (`saveAndFlush` + catch de `DataIntegrityViolationException`) dentro de la misma transacción del gasto dejaba la transacción de Postgres abortada — el cliente recibía 200 OK pero el gasto no persistía; se corrigió moviendo el listener de sobregasto a `@TransactionalEventListener(AFTER_COMMIT)` + `REQUIRES_NEW` y aislando `createNotification` en su propia transacción; (2) el dispatcher pasaba un proxy lazy de Hibernate a través de la frontera `@Async` del email (`LazyInitializationException` silenciosa en el thread de mail) — ahora cruza un record `EmailRecipient` con valores planos. Además: la predicción de fin de mes ya no proyecta un día fantasma el último día del mes, y `MonthEndPredictionJob` pre-filtra usuarios con gastos en el mes en curso en lugar de escanear todos los usuarios activos. Decisión de producto pendiente registrada: si todos los proveedores de IA configurados fallan, el mensaje del usuario NO se persiste en `ai_messages` (la transacción del turno se revierte completa).

**Segundo rediseño del asistente IA (post-implementación del Batch 1 BYOK):** el diseño inicial de este sprint implementó un modelo BYOK (Bring Your Own Key) — cada usuario cargaba su propia API key desde la UI de `/configuracion`, cifrada (AES-256-GCM) en la entidad `AiProviderSetting` (tabla `ai_provider_settings`, con un índice único parcial para garantizar un solo proveedor "default" por usuario). Antes de integrar la rama, el dueño de la app pidió cambiar el modelo de fondo: las API keys pasan a configurarse a nivel de aplicación, vía variables de entorno (`NVIDIA_API_KEY`, `OPENCODE_API_KEY`, `OPENROUTER_API_KEY`), resueltas por `AiProviderRegistry` y probadas en cadena por `AiChatOrchestrator` con failover automático y transparente entre proveedores. La razón es resiliencia: el usuario final nunca debería ver una caída del asistente si hay al menos un proveedor de respaldo configurado, y ya no tiene sentido pedirle que administre sus propias keys cuando el operador puede cubrir el costo con múltiples free tiers. Como nada de esto llegó a commitearse ni desplegarse, se eliminó el stack BYOK completo (entidad, cifrado, CRUD, migraciones `V9`/`V11`) en vez de mantenerlo como código muerto o migración histórica. El flujo BYOK previo al rediseño sí fue verificado end-to-end contra Postgres real con el usuario de desarrollo Jhon Quiceno (`user_id=2`) antes de descartarse (backend 267/267, frontend 86/86, lint y build limpios); el flujo con `AiProviderRegistry`/`AiChatOrchestrator` requiere su propia verificación focalizada (migraciones `V7`, `V8` y `V10` limpias, failover real entre proveedores, `GET /api/ai/providers/status` sin fuga de keys) antes de dar el sprint por cerrado.

---

## 🟪 Sprint 6 — Reportes, Pulido y Launch
> Reportes, UX final, índices de rendimiento, Docker y deploy del MVP · **12/14 tareas completas, 2 parciales** (ver detalle)

### Backend
- [x] `[BE]` Endpoints de reportes por período — `GET /api/reports/monthly?month=X&year=Y` con breakdown completo
- [x] `[BE]` Endpoint de exportación — `GET /api/reports/export` → CSV o JSON con todos los movimientos del período
- [x] `[BE]` Dockerizar Spring Boot — `Dockerfile` + `docker-compose` con PostgreSQL
- [x] `[BE]` Variables de entorno para producción — `application.properties` separado para `prod`/`dev`
- [x] `[BE]` Validaciones exhaustivas en todos los endpoints — `@Valid`, mensajes de error claros en español
- [~] `[BE]` Smoke testing de todos los endpoints con colección de Postman documentada — smoke testing cubierto por la suite automatizada (MockMvc); la colección de Postman exportable queda pendiente, ver `docs/auditoria/pendientes-fuera-de-sprint6.md`

### Base de datos
- [x] `[DB]` **Migración `V11`** — índices de cierre restantes: `debt_id + payment_date` en `debt_payments` (los de `expenses`/`incomes`/`recurring_payments`/`ai_messages` ya quedaron cubiertos por `V8` y `V10` en Sprint 5)

### Frontend
- [x] `[FE]` Página `/reportes` conectada al backend — selector de período + gráficos mensuales/anuales
- [x] `[FE]` Funcionalidad de exportación desde `/reportes` — botón descargar CSV
- [x] `[FE]` Manejo global de errores `401`/`403`/`500` — redirigir a login si token expirado
- [x] `[FE]` Empty states para todas las páginas cuando no hay datos registrados
- [x] `[FE]` Página `/configuracion` — cambio de contraseña, preferencias de notificación, datos de perfil
- [~] `[FE]` Revisión de responsividad mobile en todas las páginas con datos reales — pendiente de verificación manual en navegador por el usuario, no automatizable
- [x] `[FE]` Build de producción Next.js — variable `NEXT_PUBLIC_API_URL` configurada para prod

**Sprint 6 — decisiones no explícitas en el alcance original:** el sprint pedía `/configuracion` con cambio de contraseña y datos de perfil, pero el alcance de backend original no listaba los endpoints correspondientes — se agregaron `PUT /api/users/profile` (nombre/email, rechaza con `409` si el email ya está en uso por otro usuario) y `PUT /api/users/password` (verifica la contraseña actual antes de aplicar la nueva). El reporte mensual (`GET /api/reports/monthly`) no persiste un snapshot nuevo: reutiliza `FinancialAnalysisService.getSummary()` (Sprint 4) para el breakdown y agrega el desglose de movimientos crudos vía `ReportService`, siguiendo la decisión de arquitectura ya documentada en `docs/sprints/sprint6.md`. Se agregaron 3 agentes revisores en contexto limpio (backend, frontend, tests+arquitectura) que auditaron el diff completo del sprint y encontraron, entre otros: inyección de fórmulas en el CSV exportado (corregido escapando `=`/`+`/`-`/`@` al inicio de un campo), que cambiar la contraseña no invalidaba sesiones existentes (se agregó revocación de todos los refresh tokens del usuario), una condición de carrera en la validación de email único que podía degradar a un `500` en vez de un `409` (corregida con `try/catch` sobre `DataIntegrityViolationException`), que el `401` de "contraseña actual incorrecta" era tratado por el interceptor global como sesión expirada (se excluyó `/api/users/password` de la recuperación automática de sesión), y que el nuevo manejo global de `500` duplicaba el toast de error en páginas que ya mostraban su propio mensaje (se introdujo `toastApiError`, que omite el toast local en 5xx). Se encontró y eliminó código muerto real (los primitivos de toast de shadcn/ui — `toast.tsx`/`toaster.tsx`/`use-toast.ts` — completamente reemplazados por `sonner` y sin ninguna referencia en el proyecto), sin podar el resto de la librería shadcn/ui por decisión explícita del usuario. El detalle completo de estas revisiones y lo que queda pendiente fuera de este sprint está en `docs/auditoria/` (`arquitectura.md`, `base-de-datos.md`, `codigo-muerto.md`, `pendientes-fuera-de-sprint6.md`). Verificado con la suite completa: 281 tests backend y 88 tests frontend en verde, lint y build de producción limpios.

**Sprint 6 — seguimiento posterior (post-cierre, fuera del alcance original):** tras cerrar el sprint, se detectaron y corrigieron 3 items adicionales de pulido. (1) **Bug real de datos**: registrar un abono a una deuda (`POST /api/debts/{id}/payments`) no generaba un gasto, a diferencia de marcar un servicio recurrente como pagado (`PATCH /api/recurring/{id}/pay`), que sí lo hace desde el Sprint 3 — un abono a deuda es plata que sale de tu bolsillo igual que cualquier gasto, y no aparecía en `/gastos` ni en el dashboard del mes. Se corrigió replicando exactamente el patrón ya existente: **migración `V12`** agrega `debt_payment_id` (FK nullable, `ON DELETE SET NULL`) a `expenses`; `DebtPaymentService.createPayment` ahora crea un `Expense` vinculado (`"Abono a deuda: {nombre}"`, método de pago `OTHER` por no capturarse uno específico en el abono, sin categoría) en la misma transacción que descuenta el saldo restante; `DebtPaymentResponse` expone el nuevo `expenseId`; el frontend invalida la caché de gastos (`invalidateExpensesCache()`) tras registrar un abono, igual que ya hacía al pagar un servicio. (2) **Diferenciación `/reportes` vs. dashboard**: el dashboard es un resumen ejecutivo del mes en curso; `/reportes` ahora agrega una tabla de movimientos detallados del período seleccionado (fecha, tipo, categoría, descripción, monto, método de pago — la misma data que ya se exportaba a CSV, pero visible en pantalla), vía un nuevo endpoint de solo lectura `GET /api/reports/movements` (misma forma que `/export?format=json` pero sin semántica de descarga de archivo, para no forzar un `Content-Disposition` en un fetch que solo alimenta una tabla). (3) **CSV más legible**: el archivo exportado ahora traduce tipo (`Ingreso`/`Gasto`) y método de pago a español (mismas etiquetas que ya usa el frontend en `/gastos`) y formatea el monto con 2 decimales fijos — el contrato JSON (`/export?format=json` y el nuevo `/movements`) se mantiene sin traducir, ya que el frontend ya tiene su propia capa de etiquetas en pantalla. Verificado: 286 tests backend y 92 tests frontend en verde, lint y build limpios.

---

## 📊 Resumen del MVP (v3)

| Sprint | Título | Tareas | BE | FE | DB | Migraciones |
|--------|--------|--------|----|----|----|-------------|
| 1 | Base del Sistema (JWT Real) | 15/15 | 7 | 5 | 3 | `V1`, `V2` |
| 2 | Ingresos y Gastos | 17/17 | 9 | 7 | 1 | `V3` |
| 3 | Deudas y Servicios | 18/18 | 8 | 8 | 2 | `V4`, `V5` |
| 4 | Motor Financiero + Dashboard | 15/15 | 8 | 6 | 1 | `V6` |
| 5 | IA Multi-Proveedor + Notificaciones | 26/26 | 15 | 8 | 3 | `V7`, `V8`, `V10` |
| 6 | Reportes y Launch | 12/14 (2 parciales) | 6 | 7 | 1 (+`V12` post-cierre) | `V11`, `V12` |
| **Total** | | **103/105** | **53** | **41** | **11** | **11 migraciones** |

---

## 🔗 Endpoints REST — Referencia rápida (v2)

```
# Usuarios
POST   /api/users/register
POST   /api/users/login
POST   /api/users/refresh
POST   /api/users/logout
PUT    /api/users/profile
PUT    /api/users/password

# Categorías
GET    /api/categories
POST   /api/categories
PUT    /api/categories/{id}
DELETE /api/categories/{id}

# Ingresos
GET    /api/incomes
POST   /api/incomes
PUT    /api/incomes/{id}
DELETE /api/incomes/{id}

# Gastos
GET    /api/expenses
POST   /api/expenses
PUT    /api/expenses/{id}
DELETE /api/expenses/{id}

# Deudas
GET    /api/debts
POST   /api/debts
PUT    /api/debts/{id}
DELETE /api/debts/{id}
POST   /api/debts/{id}/payments
GET    /api/debts/{id}/payments

# Servicios recurrentes
GET    /api/recurring
POST   /api/recurring
PUT    /api/recurring/{id}
DELETE /api/recurring/{id}
PATCH  /api/recurring/{id}/toggle
PATCH  /api/recurring/{id}/pay

# Motor financiero
GET    /api/analysis/summary
GET    /api/analysis/recommendations
GET    /api/analysis/prediction

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

# Reportes
GET    /api/reports/monthly?month=X&year=Y
GET    /api/reports/movements?month=X&year=Y
GET    /api/reports/export
```

---

*FinSmart MVP Sprint Board v3 — tablas creadas de forma incremental, sprint por sprint, sin n8n*