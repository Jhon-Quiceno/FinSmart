# Sprint 4 - Motor Financiero y Dashboard Real (FinSmart)

Este sprint entrega el motor de analisis financiero (balance, ahorro, endeudamiento, top categorias y recomendaciones) y conecta el dashboard a datos reales, sin mocks.

## Antes de empezar

Crea la rama de trabajo de este sprint a partir de `develop` (asi se creo la rama del Sprint 3):

```bash
git checkout develop
git pull origin develop
git checkout -b feature/sprint-4-financial-engine-dashboard
```

## Objetivo

Entregar el motor financiero y el dashboard conectado a datos reales con:

1. Calculo de balance mensual (`totalIngresos - totalGastos`) por usuario y periodo, incluyendo los gastos generados por servicios recurrentes pagados (Sprint 3).
2. Calculo de porcentaje gasto/ingreso, con alerta cuando supera el 80%.
3. Calculo de nivel de endeudamiento (deudas activas frente a ingresos del periodo).
4. Calculo de ahorro mensual real y proyeccion de ahorro anual.
5. Ranking de top categorias de gasto por monto mensual.
6. Motor de recomendaciones por reglas (ej. gasto en una categoria supera cierto porcentaje del ingreso).
7. Dashboard conectado a datos reales del backend, sin mocks.

## Alcance del Sprint 4

### Backend

1. `FinancialAnalysisService.java` — balance mensual (`totalIngresos - totalGastos`) por `userId` y periodo, incluyendo gastos generados por servicios recurrentes pagados.
2. Calculo porcentaje gasto/ingreso — alertar si supera el 80%.
3. Calculo nivel de endeudamiento — suma deudas activas vs ingresos.
4. Calculo ahorro mensual real y proyeccion de ahorro anual.
5. Top categorias de gasto — ranking de categorias por monto mensual.
6. Endpoint `GET /api/analysis/summary` — balance, ahorro, % gasto, endeudamiento, top categorias.
7. Logica de recomendaciones por reglas — si `gasto_comida > 30%` -> recomendacion especifica.
8. Endpoint `GET /api/analysis/recommendations` — lista de alertas y sugerencias del motor.

### Base de datos

1. Migracion `V6` — tabla `financial_analysis`: snapshot mensual por usuario (`total_income`, `total_expense`, `savings`, `expense_ratio`, `debt_ratio`, `top_category_id`, `period_year`, `period_month`), con `UNIQUE(user_id, period_year, period_month)`.

### Frontend

1. Dashboard conectado a `GET /api/analysis/summary` — Balance Card con datos reales.
2. Stats Cards de ingresos, gastos, deudas y ahorros con datos reales del backend.
3. Grafico Ingresos vs Gastos (Recharts) alimentado por datos reales del periodo.
4. Grafico de distribucion por categoria con datos reales de top categorias.
5. Panel de alertas y recomendaciones conectado a `GET /api/analysis/recommendations`.
6. Transacciones recientes — ultimos 10 movimientos combinados (ingresos + gastos).

## Definicion de terminado (DoD)

1. `GET /api/analysis/summary` y `GET /api/analysis/recommendations` operativos end-to-end, filtrados siempre por `userId` (sin fuga entre usuarios).
2. El balance y el ahorro consideran los gastos generados por `PATCH /api/recurring/{id}/pay` (Sprint 3), no solo los gastos cargados manualmente.
3. El nivel de endeudamiento se calcula con `debts.remaining_amount` (no `total_amount`) frente a los ingresos del periodo.
4. `financial_analysis` (V6) persiste un snapshot por usuario/periodo respetando `UNIQUE(user_id, period_year, period_month)`, sin duplicados al recalcular.
5. Dashboard sin datos mock, con loading skeletons y manejo de estado vacio cuando el usuario no tiene movimientos registrados.
6. Tests de backend (unitarios + integracion) en verde para `FinancialAnalysisService` y los endpoints de analisis.
7. Migracion `V6` verificada contra Postgres real.

## Referencia de endpoints (Sprint 4)

```http
GET /api/analysis/summary
GET /api/analysis/recommendations
```
