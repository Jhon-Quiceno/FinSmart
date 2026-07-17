# Evidencia Sprint 2 — Ingresos, Gastos y Categorías

> **Fecha:** Julio 2026
> **Total de tareas:** 17/17 completadas
> **Migraciones:** V3 (categories, incomes, expenses)

---

## 1. Objetivo

Implementar el CRUD completo de categorías, ingresos y gastos con filtros, paginación y validaciones, tanto en backend como frontend.

## 2. Alcance Implementado

### Backend (9 tareas)
- Entidad `Category` + CRUD endpoints (`/api/categories`)
- Entidad `Income` + CRUD endpoints con filtros por mes, año, fuente
- Entidad `Expense` (incluye `paymentMethod`) + CRUD endpoints con filtros
- Mappers Entity ↔ DTO con MapStruct
- Tests unitarios de servicios (`CategoryServiceTest`, `IncomeServiceTest`, `ExpenseServiceTest`)
- Tests de integración de controladores

### Base de Datos (1 tarea)
- Migración V3: tablas `categories`, `incomes`, `expenses` con columna `payment_method` y CHECK

### Frontend (7 tareas)
- Página `/ingresos` con lista paginada y filtros por mes
- Modal/formulario crear y editar ingreso
- Página `/gastos` con tabla y filtros por categoría y rango de fechas
- Modal/formulario crear y editar gasto con categoría real y método de pago
- Categorías reales desde `/api/categories` en formularios
- Eliminación con confirmación + toast de éxito/error
- Loading skeletons durante carga de datos

## 3. Trazabilidad con Requisitos

| Requisito | Implementado en |
|-----------|-----------------|
| RF-07: Categorías personalizables | CategoryController + UI de categorías |
| RF-10: Registrar ingresos | IncomeController + formulario frontend |
| RF-13: Registrar gastos | ExpenseController + formulario frontend |
| RF-15: Filtrar gastos | ExpenseService con Specifications |

## 4. Decisiones Técnicas

| Decisión | Justificación |
|----------|---------------|
| CHECK constraint en payment_method | Validación a nivel BD, no solo en aplicación |
| `payment_method` desde V3 | El formulario de Sprint 2 ya lo requiere; no tiene sentido agregarlo después |
| Mappers con MapStruct | Evita exponer entidades JPA directamente en la API |
| Paginación nativa de Spring Data | Rendimiento predecible, evita cargar todo en memoria |

## 5. Artefactos Desarrollados

### Backend
- `CategoryController`, `IncomeController`, `ExpenseController`
- `CategoryService`, `IncomeService`, `ExpenseService`
- `Category`, `Income`, `Expense` (entidades JPA)
- `IncomeMapper`, `ExpenseMapper`, `CategoryMapper` (MapStruct)
- Migración: `V3__create_categories_incomes_expenses.sql`

### Frontend
- `app/ingresos/page.tsx` — Lista de ingresos + modal CRUD
- `app/gastos/page.tsx` — Lista de gastos + modal CRUD
- `app/categorias/page.tsx` — CRUD de categorías
- Componentes: `IncomeForm`, `ExpenseForm`, `CategoryForm`, `CategorySelect`

## 6. Evidencia Técnica Verificable

### Tests
- `CategoryServiceTest`, `IncomeServiceTest`, `ExpenseServiceTest`
- `CategoryControllerTest`, `IncomeControllerTest`, `ExpenseControllerTest` (MockMvc)
- Frontend: tests con Vitest

### Validaciones
- Monto positivo, fecha válida, categoría existente
- Email único en usuarios, categoría única por usuario + tipo

## 7. Conclusión

Sprint 2 entrega la funcionalidad central de registro financiero con una API robusta, validaciones en todos los niveles y una UI funcional con filtros, paginación y feedback visual.
