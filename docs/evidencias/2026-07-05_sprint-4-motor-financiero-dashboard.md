# Evidencia Sprint 4 — Motor Financiero + Dashboard Real

> **Fecha:** Julio 2026
> **Total de tareas:** 15/15 completadas
> **Migraciones:** V6 (financial_analysis)

---

## 1. Objetivo

Implementar el motor de análisis financiero con cálculos automáticos y un dashboard conectado a datos reales, reemplazando los datos mock.

## 2. Alcance Implementado

### Backend (8 tareas)
- `FinancialAnalysisService` — balance mensual (ingresos - gastos) por usuario y período
- Cálculo de porcentaje gasto/ingreso con alerta si supera el 80%
- Cálculo de nivel de endeudamiento (deudas activas vs ingresos)
- Cálculo de ahorro mensual real y proyección anual
- Top categorías de gasto (ranking por monto mensual)
- Endpoint `GET /api/analysis/summary` — resumen completo en una llamada
- Lógica de recomendaciones por reglas (si gasto en categoría X > 30% → recomendación)
- Endpoint `GET /api/analysis/recommendations`

### Base de Datos (1 tarea)
- Migración V6: tabla `financial_analysis` con UNIQUE(user_id, period_year, period_month)

### Frontend (6 tareas)
- Dashboard conectado a `GET /api/analysis/summary`
- Stats Cards con datos reales (ingresos, gastos, deudas, ahorros)
- Gráfico Ingresos vs Gastos (Recharts) con datos reales del período
- Gráfico de distribución por categoría
- Panel de alertas y recomendaciones conectado a `/recommendations`
- Transacciones recientes (últimos 10 movimientos combinados)

## 3. Decisiones Técnicas

| Decisión | Justificación |
|----------|---------------|
| Summary embebe serie 6 meses + top categorías + transacciones recientes | El dashboard completo se alimenta con UNA llamada |
| `INSERT ... ON CONFLICT DO UPDATE` atómico | Recalcular un período nunca duplica filas |
| `recommendations` reusa `getSummary()` internamente | Evita recalcular el análisis dos veces |
| Endeudamiento usa `remaining_amount` (no `total_amount`) | Una deuda pagada (remaining = 0) ya contribuye 0 |
| Regla de comida match case-insensitive por nombre | Categorías son por-usuario y de nombre libre |

## 4. Performance

El endpoint `/analysis/summary` ejecuta:
1. Suma de ingresos del período (1 query)
2. Suma de gastos del período (1 query)
3. Serie de 6 meses (1 query por mes, o agrupación)
4. Top categorías (1 query con GROUP BY + ORDER BY)
5. Últimas 10 transacciones (1 query combinada)

Todo ejecutado dentro del mismo `@Transactional(readOnly = true)`.

## 5. Conclusión

El Sprint 4 convierte a KoroFin de un simple registrador de datos a una plataforma con inteligencia analítica. El dashboard ofrece una visión completa de la salud financiera del usuario con datos en tiempo real, alertas y recomendaciones prácticas.
