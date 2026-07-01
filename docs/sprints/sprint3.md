# Sprint 3 - Deudas y Servicios Recurrentes (FinSmart)

Este sprint entrega la gestion de deudas con historial de abonos, y de servicios recurrentes con calculo automatico de proxima fecha de pago y generacion de gasto al marcarlos como pagados.

## Antes de empezar

Crea la rama de trabajo de este sprint a partir de `develop` (asi se creo la rama del Sprint 2):

```bash
git checkout develop
git pull origin develop
git checkout -b feature/sprint-3-debts-recurring-services
```

## Objetivo

Entregar gestion completa de deudas y servicios recurrentes con:

1. CRUD de deudas, con seguimiento de pago (`remaining_amount`) e interes/vencimiento.
2. Historial de abonos por deuda, sin sobrescribir el monto restante directamente.
3. CRUD de servicios recurrentes, con calculo automatico de `next_payment_date` y activacion/desactivacion.
4. Generacion automatica de un `Expense` vinculado al marcar un servicio como pagado, trazable via `recurring_payment_id`.
5. Frontend conectado a datos reales, sin mocks, en `/deudas` y `/servicios`.

## Alcance del Sprint 3

### Backend

1. Entidad `Debt` + `DebtRepository` + `DebtService` — campos: nombre, monto total, restante, interes, vencimiento.
2. CRUD endpoints `/api/debts` — `GET`, `POST`, `PUT`, `DELETE` (sin logica de pago embebida; eso vive en `DebtPayment`).
3. Entidad `DebtPayment` + `DebtPaymentRepository` + `DebtPaymentService` — registra abonos individuales y actualiza `remaining_amount` de la deuda al crearse.
4. Endpoints `/api/debts/{id}/payments` — `POST` (crear abono) y `GET` (historial de abonos de esa deuda).
5. Entidad `RecurringPayment` + Repository + Service — frecuencia `MONTHLY`/`WEEKLY`, `next_payment_date`, `is_active`.
6. CRUD endpoints `/api/recurring` — calcular automaticamente `next_payment_date` al crear/actualizar.
7. Logica de activacion/desactivacion de servicios (`PATCH /api/recurring/{id}/toggle`).
8. Endpoint `PATCH /api/recurring/{id}/pay` — crea un `Expense` vinculado (`recurring_payment_id`) con el monto del servicio, y recalcula `next_payment_date`.

### Base de datos

1. Migracion `V4` — tablas `debts`, `debt_payments`, `recurring_payments`.
2. Migracion `V5` — columna `recurring_payment_id` (FK nullable -> `recurring_payments`, `ON DELETE SET NULL`) en `expenses`, para trazar que gastos vinieron de un servicio recurrente.

### Frontend

1. Pagina `/deudas` conectada al backend — listado con progreso de pago (`remaining/total`).
2. Formulario crear/editar deuda — campos de tasa de interes y fecha de vencimiento.
3. Panel de registro de abono a deuda — conectado a `POST /api/debts/{id}/payments`, refresca `remaining_amount` en tiempo real.
4. Historial de abonos por deuda — conectado a `GET /api/debts/{id}/payments`.
5. Pagina `/servicios` conectada al backend — tarjetas con proxima fecha de pago.
6. Formulario crear/editar servicio recurrente — selector de frecuencia y monto.
7. Toggle de activacion/desactivacion de servicios desde la UI con actualizacion inmediata.
8. Boton "Marcar como pagado" en tarjeta de servicio — conectado a `PATCH /api/recurring/{id}/pay`.

## Definicion de terminado (DoD)

1. `/api/debts`, `/api/debts/{id}/payments`, `/api/recurring` operativos end-to-end (CRUD + calculo automatico de `next_payment_date`).
2. `debts.remaining_amount` se actualiza correctamente al registrar un abono en `debt_payments`, con historial completo (nunca se sobrescribe sin dejar registro).
3. `expenses.recurring_payment_id` (V5) vincula correctamente el gasto generado por `PATCH /api/recurring/{id}/pay` con el servicio recurrente de origen.
4. Deudas, abonos y servicios recurrentes filtrados siempre por `userId` (sin fuga entre usuarios).
5. `/deudas` y `/servicios` sin datos mock, con loading skeletons, confirmacion de borrado y toasts.
6. Tests de backend (unitarios + integracion) en verde para `Debt`/`DebtPayment`/`RecurringPayment`.
7. Migraciones `V4` y `V5` verificadas contra Postgres real.

## Referencia de endpoints (Sprint 3)

```http
GET    /api/debts
POST   /api/debts
PUT    /api/debts/{id}
DELETE /api/debts/{id}
POST   /api/debts/{id}/payments
GET    /api/debts/{id}/payments

GET    /api/recurring
POST   /api/recurring
PUT    /api/recurring/{id}
DELETE /api/recurring/{id}
PATCH  /api/recurring/{id}/toggle
PATCH  /api/recurring/{id}/pay
```
