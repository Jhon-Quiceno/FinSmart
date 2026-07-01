# 🏦 FinSmart — MVP Sprint Board (v2, corregido)

> **Stack:** Java + Spring Boot · PostgreSQL · Next.js · n8n
> **Total:** 6 Sprints · 96 tareas
> **Estados:** `[ ]` Pendiente · `[~]` En progreso · `[x]` Completado

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

---

## Leyenda de capas

| Etiqueta | Capa |
|----------|------|
| `[BE]` | Backend — Spring Boot |
| `[FE]` | Frontend — Next.js |
| `[N8]` | Automatización — n8n |
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
> Gestión de obligaciones, pagos periódicos, y trazabilidad de abonos y pagos generados · **18 tareas**

### Backend
- [ ] `[BE]` Entidad `Debt` + `DebtRepository` + `DebtService` — campos: nombre, monto total, restante, interés, vencimiento
- [ ] `[BE]` CRUD endpoints `/api/debts` — `GET`, `POST`, `PUT`, `DELETE` (sin lógica de pago embebida; eso vive en `DebtPayment`)
- [ ] `[BE]` Entidad `DebtPayment` + `DebtPaymentRepository` + `DebtPaymentService` — registra abonos individuales y actualiza `remaining_amount` de la deuda al crearse
- [ ] `[BE]` Endpoints `/api/debts/{id}/payments` — `POST` (crear abono) y `GET` (historial de abonos de esa deuda)
- [ ] `[BE]` Entidad `RecurringPayment` + Repository + Service — frecuencia `MONTHLY`/`WEEKLY`, `next_payment_date`, `is_active`
- [ ] `[BE]` CRUD endpoints `/api/recurring` — calcular automáticamente `next_payment_date` al crear/actualizar
- [ ] `[BE]` Lógica de activación/desactivación de servicios (`PATCH /api/recurring/{id}/toggle`)
- [ ] `[BE]` Endpoint `PATCH /api/recurring/{id}/pay` — crea un `Expense` vinculado (`recurring_payment_id`) con el monto del servicio, y recalcula `next_payment_date`

### Base de datos
- [ ] `[DB]` **Migración `V4`** — tablas `debts`, `debt_payments`, `recurring_payments`
- [ ] `[DB]` **Migración `V5`** — columna `recurring_payment_id` (FK nullable → `recurring_payments`, `ON DELETE SET NULL`) en `expenses`, para trazar qué gastos vinieron de un servicio recurrente

### Frontend
- [ ] `[FE]` Página `/deudas` conectada al backend — listado con progreso de pago (`remaining/total`)
- [ ] `[FE]` Formulario crear/editar deuda — campos de tasa de interés y fecha de vencimiento
- [ ] `[FE]` Panel de registro de abono a deuda — conectado a `POST /api/debts/{id}/payments`, refresca `remaining_amount` en tiempo real
- [ ] `[FE]` Historial de abonos por deuda — conectado a `GET /api/debts/{id}/payments`
- [ ] `[FE]` Página `/servicios` conectada al backend — tarjetas con próxima fecha de pago
- [ ] `[FE]` Formulario crear/editar servicio recurrente — selector de frecuencia y monto
- [ ] `[FE]` Toggle de activación/desactivación de servicios desde la UI con actualización inmediata
- [ ] `[FE]` Botón "Marcar como pagado" en tarjeta de servicio — conectado a `PATCH /api/recurring/{id}/pay`

---

## 🟥 Sprint 4 — Motor Financiero + Dashboard Real
> Cálculos automáticos y dashboard conectado · **15 tareas**

### Backend
- [ ] `[BE]` `FinancialAnalysisService.java` — balance mensual (`totalIngresos - totalGastos`) por `userId` y periodo, incluyendo gastos generados por servicios recurrentes pagados
- [ ] `[BE]` Cálculo porcentaje gasto/ingreso — alertar si supera el 80%
- [ ] `[BE]` Cálculo nivel de endeudamiento — suma deudas activas vs ingresos
- [ ] `[BE]` Cálculo ahorro mensual real y proyección de ahorro anual
- [ ] `[BE]` Top categorías de gasto — ranking de categorías por monto mensual
- [ ] `[BE]` Endpoint `GET /api/analysis/summary` — balance, ahorro, % gasto, endeudamiento, top categorías
- [ ] `[BE]` Lógica de recomendaciones por reglas — si `gasto_comida > 30%` → recomendación específica
- [ ] `[BE]` Endpoint `GET /api/analysis/recommendations` — lista de alertas y sugerencias del motor

### Base de datos
- [ ] `[DB]` **Migración `V6`** — tabla `financial_analysis`: snapshot mensual por usuario (`total_income`, `total_expense`, `savings`, `expense_ratio`, `debt_ratio`, `top_category_id`, `period_year`, `period_month`), `UNIQUE(user_id, period_year, period_month)`

### Frontend
- [ ] `[FE]` Dashboard conectado a `GET /api/analysis/summary` — Balance Card con datos reales
- [ ] `[FE]` Stats Cards de ingresos, gastos, deudas y ahorros con datos reales del backend
- [ ] `[FE]` Gráfico Ingresos vs Gastos (Recharts) alimentado por datos reales del período
- [ ] `[FE]` Gráfico de distribución por categoría con datos reales de top categorías
- [ ] `[FE]` Panel de alertas y recomendaciones conectado a `GET /api/analysis/recommendations`
- [ ] `[FE]` Transacciones recientes — últimos 10 movimientos combinados (ingresos + gastos)

---

## 🟩 Sprint 5 — IA + Automatizaciones n8n
> Notificaciones, historial de chat con IA, y workflows · **17 tareas**

### Backend
- [ ] `[BE]` Entidad `Notification` + Repository — guardar notificaciones del sistema por usuario (tipo, mensaje, leído)
- [ ] `[BE]` Endpoint `GET /api/notifications` — listado de notificaciones, `PATCH` para marcar como leído
- [ ] `[BE]` Endpoint `POST /api/analysis/chat` — recibe pregunta, construye contexto financiero, llama a IA y **persiste** el mensaje del usuario y la respuesta en `ai_messages`
- [ ] `[BE]` Entidad `AiMessage` + `AiMessageRepository` — rol (`USER`/`ASSISTANT`), contenido, `userId`, timestamp
- [ ] `[BE]` Endpoint `GET /api/analysis/chat/history` — recupera el historial de conversación del usuario, paginado

### Base de datos
- [ ] `[DB]` **Migración `V7`** — tabla `notifications`
- [ ] `[DB]` **Migración `V8`** — tabla `ai_messages`

### n8n Workflows
- [ ] `[N8]` **Recordatorios de pagos:** Cron diario → consulta PostgreSQL → filtra pagos próximos (3-5 días) → envía email/WhatsApp
- [ ] `[N8]` **Alertas de sobregasto:** Trigger nuevo gasto → suma gastos del mes → si >80% ingresos → envía alerta
- [ ] `[N8]` **Resumen semanal:** Ingresos vs gastos, mayor categoría, ahorro del período, recomendación simple automática
- [ ] `[N8]` **Predicción fin de mes:** Promedio gasto diario × días restantes → proyección → alerta si saldo negativo
- [ ] `[N8]` **Motor de recomendaciones:** Contexto financiero del usuario → OpenAI/Claude API → recomendación personalizada
- [ ] `[N8]` **Clasificación automática de gastos:** Descripción del gasto → IA o reglas → asigna categoría automáticamente
- [ ] `[N8]` **Reactivación:** Si usuario sin actividad 3+ días → recordatorio de registro de gastos

### Frontend
- [ ] `[FE]` Página `/asistente-ia` conectada al backend — chat funcional, carga historial previo desde `GET /api/analysis/chat/history`
- [ ] `[FE]` Navbar: badge de notificaciones no leídas + panel desplegable conectado a `/api/notifications`
- [ ] `[FE]` Tarjeta de predicción fin de mes en dashboard — saldo proyectado y gasto máximo diario recomendado

---

## 🟪 Sprint 6 — Reportes, Pulido y Launch
> Reportes, UX final, índices de rendimiento, Docker y deploy del MVP · **14 tareas**

### Backend
- [ ] `[BE]` Endpoints de reportes por período — `GET /api/reports/monthly?month=X&year=Y` con breakdown completo
- [ ] `[BE]` Endpoint de exportación — `GET /api/reports/export` → CSV o JSON con todos los movimientos del período
- [ ] `[BE]` Dockerizar Spring Boot — `Dockerfile` + `docker-compose` con PostgreSQL y n8n
- [ ] `[BE]` Variables de entorno para producción — `application.properties` separado para `prod`/`dev`
- [ ] `[BE]` Validaciones exhaustivas en todos los endpoints — `@Valid`, mensajes de error claros en español
- [ ] `[BE]` Smoke testing de todos los endpoints con colección de Postman documentada

### Base de datos
- [ ] `[DB]` **Migración `V9`** — índices de cierre para consultas frecuentes: `user_id + date` en `expenses`/`incomes` (si no existen ya), `next_date` en `recurring_payments`, `debt_id + date` en `debt_payments`, `user_id + created_at` en `ai_messages`

### Frontend
- [ ] `[FE]` Página `/reportes` conectada al backend — selector de período + gráficos mensuales/anuales
- [ ] `[FE]` Funcionalidad de exportación desde `/reportes` — botón descargar CSV
- [ ] `[FE]` Manejo global de errores `401`/`403`/`500` — redirigir a login si token expirado
- [ ] `[FE]` Empty states para todas las páginas cuando no hay datos registrados
- [ ] `[FE]` Página `/configuracion` — cambio de contraseña, preferencias de notificación, datos de perfil
- [ ] `[FE]` Revisión de responsividad mobile en todas las páginas con datos reales
- [ ] `[FE]` Build de producción Next.js — variable `NEXT_PUBLIC_API_URL` configurada para prod

---

## 📊 Resumen del MVP (v2)

| Sprint | Título | Tareas | BE | FE | N8 | DB | Migraciones |
|--------|--------|--------|----|----|----|----|-------------|
| 1 | Base del Sistema (JWT Real) | 15/15 | 7 | 5 | 0 | 3 | `V1`, `V2` |
| 2 | Ingresos y Gastos | 17/17 | 9 | 7 | 0 | 1 | `V3` |
| 3 | Deudas y Servicios | 18 | 8 | 8 | 0 | 2 | `V4`, `V5` |
| 4 | Motor Financiero + Dashboard | 15 | 8 | 6 | 0 | 1 | `V6` |
| 5 | IA + n8n | 17 | 5 | 3 | 7 | 2 | `V7`, `V8` |
| 6 | Reportes y Launch | 14 | 6 | 7 | 0 | 1 | `V9` |
| **Total** | | **96** | **43** | **36** | **7** | **10** | **9 migraciones** |

---

## 🔗 Endpoints REST — Referencia rápida (v2)

```
# Usuarios
POST   /api/users/register
POST   /api/users/login
POST   /api/users/refresh
POST   /api/users/logout

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
POST   /api/analysis/chat
GET    /api/analysis/chat/history

# Notificaciones
GET    /api/notifications
PATCH  /api/notifications/{id}/read

# Reportes
GET    /api/reports/monthly?month=X&year=Y
GET    /api/reports/export
```

---

*FinSmart MVP Sprint Board v2 — tablas creadas de forma incremental, sprint por sprint*