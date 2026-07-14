# Sprint 1 - Cargos de Deuda, Quick-Add con IA y Control de Uso (FinSmart SaaS)

Este sprint abre la fase SaaS del proyecto (post-MVP): resuelve el dolor real de que la deuda de una tarjeta hoy solo puede bajar, conecta la IA de categorizacion que ya existe en el backend a un flujo de carga rapida, y agrega la base de control de uso/rate limiting necesaria antes de abrir el producto a mas usuarios. Es el primer sprint del tablero de la fase SaaS (12 tareas).

## Antes de empezar

La rama de trabajo de este sprint se crea a partir de `develop`:

```bash
git checkout develop
git checkout -b feature/sprint-1-debt-charges-quick-add-ai-usage
```

## Objetivo

1. Fase A del rediseno de deudas (`docs/rediseno-deudas-tarjetas.md`, seccion 4): entidad `DebtCharge` como espejo de `DebtPayment` con signo positivo, para que una deuda pueda subir (compra con tarjeta) ademas de bajar (pago).
2. Quick-add conectado al parser de IA existente: extender el patron de sugerencia de categoria que ya funciona en `ExpenseModal` (Sprint 5) a `IncomeModal`, y sumar un modal ultra-minimo de carga rapida (monto + descripcion) con defaults inteligentes.
3. Tracking de uso de IA (`ai_usage_events`) y rate limiting basico en `/api/users/login`, `/api/users/register` y `/api/ai/chat`.

## Decisiones de arquitectura

1. **`DebtCharge` es un espejo exacto de `DebtPayment`, no una generalizacion.** Misma forma (`debt`, `amount`, `chargeDate`, `description`), pero suma en vez de restar sobre `remainingAmount`. No se introduce todavia el modelo de ledger (`CardMovement`) de la Fase B — eso queda para el diseño con SDD del dominio de tarjetas completo.
2. **La suma de `remainingAmount` en `DebtCharge` no puede superar ningun tope hoy** porque el modelo actual de `Debt` no tiene un campo de cupo (`creditLimit` es parte de la Fase B, dominio `CreditCard`). La validacion de esta fase se limita a que el monto sea positivo y la deuda exista y pertenezca al usuario autenticado.
3. **El quick-add de esta fase reutiliza el patron ya probado en `ExpenseModal`, no lo reinventa.** `ExpenseModal` ya tiene un boton "Sugerir categoria" conectado a `useCategorize()` desde el Sprint 5 (commit `b67b760`) — la investigacion original que decia que `categorize()` no estaba conectado a ningun formulario quedo desactualizada por ese commit. Este sprint corrige esa brecha real: `IncomeModal` no tiene la misma sugerencia. El modal de quick-add global (FAB) es un componente nuevo separado que reutiliza `categorize()` para resolver monto+descripcion+categoria en un solo campo de texto libre.
4. **Rate limiting en memoria (bucket por IP+usuario), no distribuido.** Alcanza para el volumen actual de usuarios (single-instance); si el backend escala a multiples instancias, este mecanismo debe migrar a un store compartido (Redis) — no es parte de este sprint, queda anotado como deuda tecnica futura en el roadmap unificado.
5. **`ai_usage_events` registra el evento en el mismo request que consume IA, no async.** El volumen actual no justifica una cola; se revisa si hace falta mover a async cuando el tracking muestre presion real sobre el tiempo de respuesta.
6. **`ai_usage_events` no reemplaza la cuota mensual ya existente (`V14`, `ai_chat_used`/`ai_chat_period` en `users`, usada por `AiChatService`).** Son mecanismos distintos y complementarios: `V14` es un tope duro de mensajes de chat por mes calendario (control de costo grueso); `ai_usage_events` es un registro granular por evento (categorizacion, chat, insights) con tokens y costo estimado, pensado para reporting y para el metering de billing futuro (roadmap seccion "SaaS multi-tenant"). El rate limiting de esta fase (item 10) es un tercer eje, independiente de ambos: throttling por ventana de tiempo para frenar abuso, no cuota de negocio.

## Alcance del Sprint 1

### Backend

**Frente 1 — Fase A rediseño de deudas**

1. Entidad `DebtCharge` (espejo de `DebtPayment`): `id`, `debt` (FK), `amount`, `chargeDate`, `description`, `createdAt`.
2. `DebtChargeRepository` — consultas por `debt.id` y por `debt.user.id` (mismo patron que `DebtPaymentRepository`).
3. `DebtChargeService` — valida que la deuda exista y pertenezca al usuario autenticado (mismo patron de `DebtPaymentService`: no hay `user_id` propio en la tabla, se valida contra el `Debt` padre); suma `amount` a `Debt.remainingAmount` al crear el cargo.
4. Endpoint `POST /api/debts/{id}/charges` — crea un cargo sobre una deuda existente, devuelve la deuda actualizada con el nuevo `remainingAmount`.
5. Endpoint `GET /api/debts/{id}/charges` — lista los cargos de una deuda (paginado, mismo patron que el listado de pagos).
6. `@Valid` en el DTO de entrada (`amount` positivo y obligatorio, `chargeDate` no futura), mensajes de error en espanol via `GlobalExceptionHandler`.

**Frente 2 — Quick-add e IA**

7. Verificar que `POST /api/ai/categorize` soporte el mismo contrato para ingresos que para gastos (hoy solo se prueba desde `ExpenseModal`); ajustar el DTO de respuesta si falta distinguir el tipo de movimiento sugerido.

**Frente 3 — Tracking de uso de IA y rate limiting**

8. Tabla y entidad `AiUsageEvent`: `id`, `user`, `provider`, `eventType`, `tokensUsed`, `costEstimate`, `createdAt`. Se registra un evento por cada llamada real a un proveedor de IA (categorizacion, chat, insights).
9. `AiUsageEventService` — registra el evento al final de cada llamada exitosa a IA (integrado en los puntos existentes donde el modulo de IA custom llama al proveedor), expone un resumen de uso por usuario/periodo.
10. Rate limiting basico (bucket en memoria, por IP y por usuario donde aplique) en `/api/users/login`, `/api/users/register` y `/api/ai/chat` — respuesta `429 Too Many Requests` con mensaje claro en espanol al superar el limite.

### Base de datos

1. **Migracion `V15`** — tabla `debt_charges` (mismas columnas que `debt_payments`, mas `description`). Verificar contra el ultimo `V` usado en el repo antes de aplicar (a la fecha de este sprint, el ultimo es `V14__add_ai_quota_to_users.sql`).
2. **Migracion `V16`** — tabla `ai_usage_events` (`id`, `user_id` FK, `provider`, `event_type`, `tokens_used`, `cost_estimate`, `created_at`), con indice por `(user_id, created_at)` para las consultas de resumen por periodo.

### Frontend

1. Servicio `debtCharges` en `lib/services/` — `createDebtCharge()`, `getDebtCharges()` contra los endpoints nuevos.
2. Hook `useDebtCharges()` en `hooks/` — mismo patron de cache casero (`Map` + listeners) que `use-debts.ts`, invalida el cache de deudas al crear un cargo.
3. UI minima para registrar un cargo: boton "Registrar cargo" en la vista de detalle de una deuda existente + modal con monto, fecha y descripcion; muestra el `remainingAmount` actualizado tras guardar.
4. Sugerencia de categoria en `IncomeModal` — replicar el boton "Sugerir categoria" + `useCategorize()` que ya existe en `ExpenseModal`, adaptado al contrato de ingresos.
5. Modal de quick-add global (FAB) — componente nuevo, accesible desde cualquier pagina autenticada, con atajo de teclado `Ctrl+K` (usar `components/ui/command.tsx`, ya presente en el proyecto pero sin uso).
6. Campo unico de texto libre en el quick-add (ej. "Uber 15000") que llama a `categorize()`/`useCategorize()` para autocompletar monto + descripcion + categoria antes de confirmar.
7. Defaults inteligentes en el quick-add: fecha = hoy, metodo de pago mas frecuente del usuario, ultima categoria usada como fallback si la IA no sugiere ninguna.

## Definicion de terminado (DoD)

1. `POST /api/debts/{id}/charges` crea el cargo, suma el monto a `remainingAmount` de la deuda, y valida que la deuda pertenezca al usuario autenticado (404/403 si no).
2. `GET /api/debts/{id}/charges` devuelve el historial de cargos de una deuda, paginado.
3. Migraciones `V15` y `V16` verificadas contra Postgres real; `mvnw test` en verde.
4. `IncomeModal` sugiere categoria con el mismo comportamiento ya verificado en `ExpenseModal` (boton, estado de carga, toast de resultado).
5. El quick-add global es accesible desde cualquier pagina autenticada via FAB y `Ctrl+K`, y crea un ingreso o gasto real a partir de un texto libre confirmado por el usuario.
6. `ai_usage_events` registra un evento por cada llamada real a un proveedor de IA (categorizacion, chat, insights), consultable por usuario y periodo.
7. `/api/users/login`, `/api/users/register` y `/api/ai/chat` devuelven `429` con mensaje claro en espanol al superar el limite de requests configurado.
8. Probado end-to-end con el usuario de desarrollo Jhon Quiceno (`user_id = 2`).

## Referencia de endpoints (Sprint 1)

```http
# Cargos de deuda
POST   /api/debts/{id}/charges
GET    /api/debts/{id}/charges
```

## Notas

- **La Fase B del dominio de tarjetas (`CreditCard` + `CardMovement`/ledger + planes de cuotas + ciclos de facturacion, ver `docs/rediseno-deudas-tarjetas.md` seccion 3-4 y `docs/roadmap-saas-cuentas-reales.md` seccion "Nivel 1") queda fuera de este sprint.** Es un cambio con reglas de negocio de verdad — arranca con `/sdd-new` cuando se decida priorizarla, no como checklist de tareas directas.
- El Nivel 2 (importar extractos bancarios) y Nivel 3 (Open Finance/Belvo) del roadmap unificado tampoco son parte de este sprint — son pasos posteriores segun el orden recomendado en `docs/roadmap-saas-cuentas-reales.md`.
