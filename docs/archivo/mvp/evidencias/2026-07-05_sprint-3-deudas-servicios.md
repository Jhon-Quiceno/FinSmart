# Evidencia Sprint 3 — Deudas y Servicios Recurrentes

> **Fecha:** Julio 2026
> **Total de tareas:** 18/18 completadas
> **Migraciones:** V4 (debts, debt_payments, recurring_payments), V5 (recurring_payment_id en expenses)

---

## 1. Objetivo

Implementar la gestión de deudas con abonos historizados y servicios recurrentes con generación automática de gastos al pagar.

## 2. Alcance Implementado

### Backend (8 tareas)
- Entidad `Debt` + CRUD endpoints (`/api/debts`)
- Entidad `DebtPayment` + endpoints de abono e historial
- Al crear un abono, se descuenta `remaining_amount` de la deuda atómicamente
- Entidad `RecurringPayment` + CRUD endpoints con cálculo automático de `next_payment_date`
- Toggle de activación/desactivación (`PATCH /api/recurring/{id}/toggle`)
- Endpoint `PATCH /api/recurring/{id}/pay` que crea Expense vinculado

### Base de Datos (2 tareas)
- Migración V4: tablas `debts`, `debt_payments`, `recurring_payments`
- Migración V5: columna `recurring_payment_id` (FK) en `expenses`

### Frontend (8 tareas)
- Página `/deudas` con progreso de pago (`remaining/total`)
- Formulario crear/editar deuda con tasa de interés y vencimiento
- Panel de registro de abono con actualización en tiempo real
- Historial de abonos por deuda
- Página `/servicios` con tarjetas y próxima fecha de pago
- Formulario crear/editar servicio (frecuencia y monto)
- Toggle de activación/desactivación con actualización inmediata
- Botón "Marcar como pagado" conectado al endpoint

## 3. Decisiones Técnicas

| Decisión | Justificación |
|----------|---------------|
| Updates atómicos en repositorio | Evita condiciones de carrera: dos pagos concurrentes no se pisan |
| Immutable `total_amount`/`remaining_amount` en PUT | La trazabilidad de abonos se rompe si se modifica el saldo directamente |
| `DebtPayment` genera Expense vinculado (V12) | Un abono es plata que sale del bolsillo; debe aparecer en gastos y dashboard |
| `PATCH /recurring/{id}/pay` devuelve expenseId | El frontend navega al gasto generado sin consulta extra |

## 4. Trazabilidad con Requisitos

| Requisito | Implementado en |
|-----------|-----------------|
| RF-16: Registrar deudas | DebtController + DebtService |
| RF-17: Registrar abonos | DebtPaymentController con UPDATE atómico |
| RF-18: Historial de abonos | GET /debts/{id}/payments |
| RF-19: Gasto vinculado al abono | V12 + creación de Expense en DebtPaymentService |
| RF-22: Generar gasto al pagar servicio | PATCH /recurring/{id}/pay |

## 5. Conclusión

Sprint 3 implementa un sistema completo de obligaciones financieras con trazabilidad total: cada abono queda registrado, actualiza el saldo atómicamente y genera un gasto visible en el dashboard. Los servicios recurrentes permiten gestionar suscripciones y pagos periódicos con un solo clic.
