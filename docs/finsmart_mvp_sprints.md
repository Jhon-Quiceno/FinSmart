# 🏦 FinSmart — MVP Sprint Board

> **Stack:** Java + Spring Boot · PostgreSQL · Next.js · n8n  
> **Total:** 6 Sprints · 80 tareas  
> **Estados:** `[ ]` Pendiente · `[~]` En progreso · `[x]` Completado

---

## Leyenda de capas

| Etiqueta | Capa |
|----------|------|
| `[BE]` | Backend — Spring Boot |
| `[FE]` | Frontend — Next.js |
| `[N8]` | Automatización — n8n |
| `[DB]` | Base de datos — PostgreSQL |

---

## 🟦 Sprint 1 — Base del Sistema
> Proyecto, autenticación y base de datos · **13 tareas**

### Backend
- [x] `[BE]` Setup proyecto Spring Boot + estructura de capas (`config/controller/service/repository/model/dto/mapper/exception`)
- [x] `[BE]` Entidad `User` + `UserRepository` + `UserService` (registro y login básico)
- [x] `[BE]` Endpoint `POST /api/users/register` — validaciones de email único y contraseña
- [x] `[BE]` Endpoint `POST /api/users/login` — respuesta con token/sesión (stub JWT para fase futura)
- [x] `[BE]` Manejo global de excepciones (`GlobalExceptionHandler`) + DTOs de error estándar
- [x] `[BE]` Configurar CORS para permitir peticiones desde el frontend Next.js

### Base de datos
- [x] `[DB]` Configurar conexión PostgreSQL + Spring Data JPA + Flyway/Liquibase para migraciones
- [x] `[DB]` Crear schema inicial: tablas `users`, `categories`, `incomes`, `expenses`, `debts`, `recurring_payments`, `notifications`

### Frontend
- [x] `[FE]` Configurar cliente HTTP (axios) con interceptores para token de autenticación
- [x] `[FE]` Reemplazar mock de `AuthContext` con llamadas reales a `POST /api/users/register` y `/login`
- [x] `[FE]` Persistir token JWT en `localStorage` y proteger rutas con middleware de Next.js
- [x] `[FE]` Pantalla Login conectada al backend — mostrar errores de validación del servidor
- [x] `[FE]` Pantalla Registro conectada al backend — feedback visual de éxito/error

---

## 🟧 Sprint 2 — Ingresos y Gastos
> CRUD completo de movimientos financieros · **14 tareas**

### Backend
- [ ] `[BE]` Entidad `Category` + `CategoryRepository` + `CategoryService` — categorías por usuario (`INCOME`/`EXPENSE`)
- [ ] `[BE]` CRUD endpoints `/api/categories` — `GET`, `POST`, `PUT`, `DELETE` filtradas por `userId`
- [ ] `[BE]` Entidad `Income` + `IncomeRepository` + `IncomeService` con filtros por mes/año/fuente
- [ ] `[BE]` CRUD endpoints `/api/incomes` — `GET` (con paginación), `POST`, `PUT`, `DELETE`
- [ ] `[BE]` Entidad `Expense` + `ExpenseRepository` + `ExpenseService` con filtros por categoría/fecha
- [ ] `[BE]` CRUD endpoints `/api/expenses` — `GET` (con paginación), `POST`, `PUT`, `DELETE`
- [ ] `[BE]` Mappers `Income`/`Expense` Entity ↔ DTO para no exponer entidades directamente

### Frontend
- [ ] `[FE]` Página `/ingresos` conectada al backend — lista con paginación y filtros por mes
- [ ] `[FE]` Modal/formulario crear y editar ingreso — select de fuente + monto + fecha
- [ ] `[FE]` Página `/gastos` conectada al backend — tabla con filtros por categoría y rango de fechas
- [ ] `[FE]` Modal/formulario crear y editar gasto — select de categoría real + método de pago
- [ ] `[FE]` Cargar categorías reales desde `/api/categories` en los formularios de gasto/ingreso
- [ ] `[FE]` Eliminación de registros con confirmación + feedback toast de éxito/error
- [ ] `[FE]` Loading skeletons mientras se cargan los datos del backend

---

## 🟨 Sprint 3 — Deudas y Servicios Recurrentes
> Gestión de obligaciones y pagos periódicos · **11 tareas**

### Backend
- [ ] `[BE]` Entidad `Debt` + `DebtRepository` + `DebtService` — campos: nombre, monto total, restante, interés, vencimiento
- [ ] `[BE]` CRUD endpoints `/api/debts` — incluir lógica de actualización de `remaining_amount` al registrar pagos
- [ ] `[BE]` Entidad `RecurringPayment` + Repository + Service — frecuencia `MONTHLY`/`WEEKLY`, `next_payment_date`, `is_active`
- [ ] `[BE]` CRUD endpoints `/api/recurring` — calcular automáticamente `next_payment_date` al crear/actualizar
- [ ] `[BE]` Lógica de activación/desactivación de servicios (`PATCH /api/recurring/{id}/toggle`)

### Frontend
- [ ] `[FE]` Página `/deudas` conectada al backend — listado con progreso de pago (`remaining/total`)
- [ ] `[FE]` Formulario crear/editar deuda — campos de tasa de interés y fecha de vencimiento
- [ ] `[FE]` Panel de registro de abono a deuda — actualizar `remaining_amount` en tiempo real
- [ ] `[FE]` Página `/servicios` conectada al backend — tarjetas con próxima fecha de pago
- [ ] `[FE]` Formulario crear/editar servicio recurrente — selector de frecuencia y monto
- [ ] `[FE]` Toggle de activación/desactivación de servicios desde la UI con actualización inmediata

---

## 🟥 Sprint 4 — Motor Financiero + Dashboard Real
> Cálculos automáticos y dashboard conectado · **15 tareas**

### Backend
- [ ] `[BE]` `FinancialAnalysisService.java` — balance mensual (`totalIngresos - totalGastos`) por `userId` y periodo
- [ ] `[BE]` Cálculo porcentaje gasto/ingreso — alertar si supera el 80%
- [ ] `[BE]` Cálculo nivel de endeudamiento — suma deudas activas vs ingresos
- [ ] `[BE]` Cálculo ahorro mensual real y proyección de ahorro anual
- [ ] `[BE]` Top categorías de gasto — ranking de categorías por monto mensual
- [ ] `[BE]` Endpoint `GET /api/analysis/summary` — balance, ahorro, % gasto, endeudamiento, top categorías
- [ ] `[BE]` Lógica de recomendaciones por reglas — si `gasto_comida > 30%` → recomendación específica
- [ ] `[BE]` Endpoint `GET /api/analysis/recommendations` — lista de alertas y sugerencias del motor

### Base de datos
- [ ] `[DB]` Tabla `financial_analysis` — snapshot mensual por usuario (`income`, `expense`, `savings`, `month`)

### Frontend
- [ ] `[FE]` Dashboard conectado a `GET /api/analysis/summary` — Balance Card con datos reales
- [ ] `[FE]` Stats Cards de ingresos, gastos, deudas y ahorros con datos reales del backend
- [ ] `[FE]` Gráfico Ingresos vs Gastos (Recharts) alimentado por datos reales del período
- [ ] `[FE]` Gráfico de distribución por categoría con datos reales de top categorías
- [ ] `[FE]` Panel de alertas y recomendaciones conectado a `GET /api/analysis/recommendations`
- [ ] `[FE]` Transacciones recientes — últimos 10 movimientos combinados (ingresos + gastos)

---

## 🟩 Sprint 5 — IA + Automatizaciones n8n
> Notificaciones inteligentes, asistente IA y workflows · **13 tareas**

### Backend
- [ ] `[BE]` Entidad `Notification` + Repository — guardar notificaciones del sistema por usuario (tipo, mensaje, leído)
- [ ] `[BE]` Endpoint `GET /api/notifications` — listado de notificaciones, `PATCH` para marcar como leído
- [ ] `[BE]` Endpoint `POST /api/analysis/chat` — recibe pregunta, construye contexto financiero y llama a IA

### n8n Workflows
- [ ] `[N8]` **Recordatorios de pagos:** Cron diario → consulta PostgreSQL → filtra pagos próximos (3-5 días) → envía email/WhatsApp
- [ ] `[N8]` **Alertas de sobregasto:** Trigger nuevo gasto → suma gastos del mes → si >80% ingresos → envía alerta
- [ ] `[N8]` **Resumen semanal:** Ingresos vs gastos, mayor categoría, ahorro del período, recomendación simple automática
- [ ] `[N8]` **Predicción fin de mes:** Promedio gasto diario × días restantes → proyección → alerta si saldo negativo
- [ ] `[N8]` **Motor de recomendaciones:** Contexto financiero del usuario → OpenAI/Claude API → recomendación personalizada
- [ ] `[N8]` **Clasificación automática de gastos:** Descripción del gasto → IA o reglas → asigna categoría automáticamente
- [ ] `[N8]` **Reactivación:** Si usuario sin actividad 3+ días → recordatorio de registro de gastos

### Frontend
- [ ] `[FE]` Página `/asistente-ia` conectada al backend — chat funcional con historial de conversación
- [ ] `[FE]` Navbar: badge de notificaciones no leídas + panel desplegable conectado a `/api/notifications`
- [ ] `[FE]` Tarjeta de predicción fin de mes en dashboard — saldo proyectado y gasto máximo diario recomendado

---

## 🟪 Sprint 6 — Reportes, Pulido y Launch
> Reportes, UX final, Docker y deploy del MVP · **14 tareas**

### Backend
- [ ] `[BE]` Endpoints de reportes por período — `GET /api/reports/monthly?month=X&year=Y` con breakdown completo
- [ ] `[BE]` Endpoint de exportación — `GET /api/reports/export` → CSV o JSON con todos los movimientos del período
- [ ] `[BE]` Dockerizar Spring Boot — `Dockerfile` + `docker-compose` con PostgreSQL y n8n
- [ ] `[BE]` Variables de entorno para producción — `application.properties` separado para `prod`/`dev`
- [ ] `[BE]` Validaciones exhaustivas en todos los endpoints — `@Valid`, mensajes de error claros en español
- [ ] `[BE]` Smoke testing de todos los endpoints con colección de Postman documentada

### Base de datos
- [ ] `[DB]` Índices en BD para consultas frecuentes: `user_id + date` en `expenses`/`incomes`, `next_payment_date` en `recurring_payments`

### Frontend
- [ ] `[FE]` Página `/reportes` conectada al backend — selector de período + gráficos mensuales/anuales
- [ ] `[FE]` Funcionalidad de exportación desde `/reportes` — botón descargar CSV
- [ ] `[FE]` Manejo global de errores `401`/`403`/`500` — redirigir a login si token expirado
- [ ] `[FE]` Empty states para todas las páginas cuando no hay datos registrados
- [ ] `[FE]` Página `/configuracion` — cambio de contraseña, preferencias de notificación, datos de perfil
- [ ] `[FE]` Revisión de responsividad mobile en todas las páginas con datos reales
- [ ] `[FE]` Build de producción Next.js — variable `NEXT_PUBLIC_API_URL` configurada para prod

---

## 📊 Resumen del MVP

| Sprint | Título | Tareas | BE | FE | N8 | DB |
|--------|--------|--------|----|----|----|----|
| 1 | Base del Sistema | 13/13 | 6 | 5 | 0 | 2 |
| 2 | Ingresos y Gastos | 14 | 7 | 7 | 0 | 0 |
| 3 | Deudas y Servicios | 11 | 5 | 6 | 0 | 0 |
| 4 | Motor Financiero + Dashboard | 15 | 8 | 6 | 0 | 1 |
| 5 | IA + n8n | 13 | 3 | 3 | 7 | 0 |
| 6 | Reportes y Launch | 14 | 6 | 7 | 0 | 1 |
| **Total** | | **80** | **35** | **34** | **7** | **4** |

---

## 🔗 Endpoints REST — Referencia rápida

```
# Usuarios
POST   /api/users/register
POST   /api/users/login

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

# Servicios recurrentes
GET    /api/recurring
POST   /api/recurring
PUT    /api/recurring/{id}
DELETE /api/recurring/{id}
PATCH  /api/recurring/{id}/toggle

# Motor financiero
GET    /api/analysis/summary
GET    /api/analysis/recommendations
POST   /api/analysis/chat

# Notificaciones
GET    /api/notifications
PATCH  /api/notifications/{id}/read

# Reportes
GET    /api/reports/monthly?month=X&year=Y
GET    /api/reports/export
```

---

*FinSmart MVP Sprint Board — generado para el equipo de desarrollo*
