# Sprint 2 - Ingresos y Gastos (FinSmart)

Este sprint entrega el CRUD completo de movimientos financieros (ingresos y gastos), con categorias por usuario y metodo de pago desde el inicio en gastos.

## Objetivo

Entregar gestion completa de ingresos y gastos con:

1. Categorias propias por usuario, separadas por tipo (`INCOME`/`EXPENSE`).
2. CRUD de ingresos con filtros por mes/ano/fuente.
3. CRUD de gastos con filtros por categoria/fecha/metodo de pago, incluyendo `paymentMethod` desde el modelo inicial (no como columna agregada despues).
4. Frontend conectado a datos reales, sin mocks, en `/ingresos` y `/gastos`.

## Alcance del Sprint 2

### Backend

1. Entidad `Category` + `CategoryRepository` + `CategoryService` — categorias por usuario (`INCOME`/`EXPENSE`).
2. CRUD endpoints `/api/categories` — `GET`, `POST`, `PUT`, `DELETE` filtradas por `userId`.
3. Entidad `Income` + `IncomeRepository` + `IncomeService` con filtros por mes/ano/fuente.
4. CRUD endpoints `/api/incomes` — `GET` (con paginacion), `POST`, `PUT`, `DELETE`.
5. Entidad `Expense` (incluye `paymentMethod`) + `ExpenseRepository` + `ExpenseService` con filtros por categoria/fecha/metodo de pago.
6. CRUD endpoints `/api/expenses` — `GET` (con paginacion), `POST`, `PUT`, `DELETE`.
7. Mappers `Income`/`Expense` Entity <-> DTO para no exponer entidades directamente.
8. Tests unitarios de servicios — `CategoryServiceTest`, `IncomeServiceTest`, `ExpenseServiceTest`.
9. Tests de integracion de controllers — `CategoryControllerTest`, `IncomeControllerTest`, `ExpenseControllerTest`.

### Base de datos

1. Migracion `V3` — tablas `categories`, `incomes`, `expenses` (con columna `payment_method` desde el inicio, `CHECK` en `CASH`/`DEBIT_CARD`/`CREDIT_CARD`/`TRANSFER`/`OTHER`).

### Frontend

1. Pagina `/ingresos` conectada al backend — lista con paginacion y filtros por mes.
2. Modal/formulario crear y editar ingreso — select de fuente + monto + fecha.
3. Pagina `/gastos` conectada al backend — tabla con filtros por categoria y rango de fechas.
4. Modal/formulario crear y editar gasto — select de categoria real + metodo de pago.
5. Cargar categorias reales desde `/api/categories` en los formularios de gasto/ingreso.
6. Eliminacion de registros con confirmacion + feedback toast de exito/error.
7. Loading skeletons mientras se cargan los datos del backend.

## Definicion de terminado (DoD)

1. `/api/categories`, `/api/incomes`, `/api/expenses` operativos end-to-end (CRUD + filtros + paginacion).
2. `expenses.payment_method` existe desde la migracion V3, con `CHECK` de valores validos.
3. Categorias, ingresos y gastos filtrados siempre por `userId` (sin fuga entre usuarios).
4. `/ingresos` y `/gastos` sin datos mock, con loading skeletons, confirmacion de borrado y toasts.
5. Tests de backend (unitarios + integracion) en verde para Category/Income/Expense.
6. Migracion V3 verificada contra Postgres real.

## Referencia de endpoints (Sprint 2)

```http
GET    /api/categories
POST   /api/categories
PUT    /api/categories/{id}
DELETE /api/categories/{id}

GET    /api/incomes
POST   /api/incomes
PUT    /api/incomes/{id}
DELETE /api/incomes/{id}

GET    /api/expenses
POST   /api/expenses
PUT    /api/expenses/{id}
DELETE /api/expenses/{id}
```
