# Sprint 2 — Ingresos, Gastos y Datos Reales (FinSmart)

Este sprint transforma el frontend de prototipo con datos falsos a una aplicación funcional conectada al backend, implementando el CRUD completo de Categorías, Ingresos y Gastos con seguridad end-to-end.

## Objetivo

1. CRUD funcional de Categorías, Ingresos y Gastos contra la base de datos PostgreSQL.
2. Frontend conectado a endpoints reales del backend (cero datos mock en las páginas de movimientos).
3. Arquitectura de servicios frontend organizada por dominio según estándar Next.js App Router.
4. Seguridad de rutas aplicada consistentemente en backend y frontend.

---

## Rama de Trabajo

- **Rama base:** `develop`
- **Nueva rama:** `feature/sprint-2-income-expense-crud`
- **Comando:** `git checkout develop && git pull && git checkout -b feature/sprint-2-income-expense-crud`
- **Destino del PR:** `develop`

---

## Skills Aplicados

Las siguientes skills del proyecto (`.agents/skills/` y `.claude/skills/`) guían las decisiones técnicas:

### Backend
| Skill | Aplicación en Sprint 2 |
|-------|----------------------|
| `java-springboot` | Estructura de capas, inyección de dependencias, configuración de beans |
| `java-coding-standards` | Nombres de clases/métodos, Optional, streams, manejo de excepciones |
| `java-docs` | Javadoc en entidades, servicios, controllers y mappers nuevos |

### Frontend
| Skill | Aplicación en Sprint 2 |
|-------|----------------------|
| `next-best-practices` | Organización `lib/` por dominio, Server/Client Components, data fetching |
| `react-hook-form` | Formularios de ingreso/gasto con `useForm` + `zod` resolver |
| `zod` | Schemas de validación client-side que espejen las constraints del backend |
| `shadcn` | Componentes UI para formularios (Select, DatePicker, Dialog) |
| `tailwind-v4-shadcn` | Variables CSS y tema consistente con las nuevas páginas |
| `tailwind-css-patterns` | Layouts responsive para tablas, modales y filtros |
| `typescript-advanced-types` | Tipos estrictos para DTOs, union types para `CategoryType` |
| `react-best-practices` | Composición de componentes, memoización de listas, hooks custom |
| `frontend-design` | Mantener estética pulida en las nuevas vistas CRUD |
| `accessibility` | Labels ARIA en formularios, navegación por teclado en modales |

---

## Alcance del Sprint 2

### 1. Backend — Entidades, Servicios y Endpoints

#### Dependencias en uso (pom.xml)
Todas las dependencias necesarias YA están configuradas:
- `spring-boot-starter-data-jpa` — JPA + Hibernate
- `spring-boot-starter-security` — Filtro JWT + SecurityContext
- `spring-boot-starter-validation` — `@Valid`, `@NotNull`, `@DecimalMin`, etc.
- `spring-boot-starter-webmvc` — Controllers REST
- `spring-boot-starter-flyway` + `flyway-database-postgresql` — Migraciones
- `springdoc-openapi-starter-webmvc-ui` (3.0.2) — Documentación Swagger
- `lombok` — Boilerplate reducido
- `mapstruct` (1.6.3) — Mappers Entity ↔ DTO
- `jjwt-api/impl/jackson` (0.12.6) — JWT ya implementado en Sprint 1
- `postgresql` — Driver BD
- `spring-boot-starter-actuator` — Health check

#### Tareas Backend

- [x] `[BE]` Entidad `Category` + `CategoryRepository` + `CategoryService`
  - Campos: `id`, `user_id` (FK → users), `name`, `type` (`INCOME`/`EXPENSE`), `icon`, `color`, `is_system`
  - Constraint `UNIQUE(user_id, name)` ya existe en BD (V1 migration)
  - Repository con métodos: `findAllAccessibleByUserId()`, `findAllAccessibleByUserIdAndType()`, `findAccessibleByIdAndUserId()`, `existsByUser_IdAndNameIgnoreCase()`
  - Service con inyección de `SecurityContext` para obtener `userId` del JWT
  - ✅ Implementado en `model/Category.java`, `repository/CategoryRepository.java`, `service/CategoryService.java`

- [x] `[BE]` CRUD endpoints `/api/categories`
  - `GET /api/categories` — lista categorías del usuario autenticado, filtro opcional `?type=INCOME|EXPENSE`
  - `POST /api/categories` — crea categoría personalizada (validar nombre único por usuario)
  - `PUT /api/categories/{id}` — actualiza categoría (verificar ownership)
  - `DELETE /api/categories/{id}` — elimina categoría (verificar ownership, decidir comportamiento con movimientos asociados: `ON DELETE SET NULL` ya está en schema)
  - ✅ Implementado en `controller/CategoryController.java`

- [x] `[BE]` Entidad `Income` + `IncomeRepository` + `IncomeService`
  - Campos: `id`, `user_id`, `category_id` (FK nullable → categories), `amount`, `description`, `date`, `is_recurring`, `source`
  - Repository con filtros: `findAllByUser_Id()`, `findByUser_IdAndDateBetween()`, `findByUser_IdAndCategory_Id()`, `findAllByFilters()` (query personalizada)
  - Service con filtros por mes/año/categoría + paginación (`Pageable`)
  - ✅ Implementado en `model/Income.java`, `repository/IncomeRepository.java`, `service/IncomeService.java`

- [x] `[BE]` CRUD endpoints `/api/incomes`
  - `GET /api/incomes` — lista con paginación (`?page=0&size=20&month=3&year=2026`)
  - `POST /api/incomes` — crea ingreso vinculado al usuario autenticado
  - `PUT /api/incomes/{id}` — actualiza ingreso (verificar ownership)
  - `DELETE /api/incomes/{id}` — elimina ingreso (verificar ownership)
  - ✅ Implementado en `controller/IncomeController.java`

- [x] `[BE]` Entidad `Expense` + `ExpenseRepository` + `ExpenseService`
  - Campos: `id`, `user_id`, `category_id` (FK nullable → categories), `amount`, `description`, `date`, `is_recurring`, `payment_method`
  - Repository con filtros: `findAllByUser_Id()`, `findByUser_IdAndDateBetween()`, `findByUser_IdAndCategory_Id()`, `findAllByFilters()` (query personalizada)
  - Service con filtros por categoría/fecha/rango + paginación
  - ✅ Implementado en `model/Expense.java`, `repository/ExpenseRepository.java`, `service/ExpenseService.java`

- [x] `[BE]` CRUD endpoints `/api/expenses`
  - `GET /api/expenses` — lista con paginación (`?page=0&size=20&category=5&from=2026-03-01&to=2026-03-31`)
  - `POST /api/expenses` — crea gasto vinculado al usuario autenticado
  - `PUT /api/expenses/{id}` — actualiza gasto (verificar ownership)
  - `DELETE /api/expenses/{id}` — elimina gasto (verificar ownership)
  - ✅ Implementado en `controller/ExpenseController.java`

- [x] `[BE]` Mappers `CategoryMapper`, `IncomeMapper`, `ExpenseMapper`
  - Usar MapStruct (`@Mapper(componentModel = "spring")`)
  - Entity → DTO de respuesta (nunca exponer entidades directamente)
  - DTO de request → Entity (para crear/actualizar)
  - Incluir `userId` en el mapeo desde SecurityContext, no desde el DTO
  - ✅ Implementado en `mapper/CategoryMapper.java`, `mapper/IncomeMapper.java`, `mapper/ExpenseMapper.java`

- [x] `[BE]` DTOs de request/response
  - `CategoryRequest`, `CategoryResponse`
  - `IncomeRequest`, `IncomeResponse`
  - `ExpenseRequest`, `ExpenseResponse`
  - Validaciones: `@NotNull`, `@DecimalMin("0.01")` en amount, `@PastOrPresent` en date, `@NotBlank` en description
  - ✅ Implementado en `dto/category/`, `dto/income/`, `dto/expense/`

- [x] `[BE]` Seed de categorías por defecto (Flyway migration)
  - `V3__seed_default_categories.sql` — agrega columna `is_system`, permite `user_id = NULL`, inserta 9 categorías base del sistema
  - ✅ Implementado en `V3__seed_default_categories.sql`

### 2. Base de Datos — Migraciones

- [x] `[DB]` `V3__seed_default_categories.sql` — insertar categorías predefinidas del sistema (sin `user_id`, o con flag `is_system = true`)
- **Decisión implementada:** Se agregó columna `is_system BOOLEAN DEFAULT FALSE` en migración V3, y las categorías del sistema tienen `user_id = NULL` + `is_system = TRUE`. El service devuelve las del sistema + las del usuario.
- ✅ Implementado en `V3__seed_default_categories.sql`

- [x] `[DB]` Agregar columna `payment_method VARCHAR(30)` a tabla `expenses`
- El frontend actual ya maneja `paymentMethod` ("Tarjeta de Débito", "Tarjeta de Crédito", "Efectivo", "Transferencia")
- ✅ Implementado en `V4__add_payment_method_to_expenses.sql`

- [x] `[DB]` Agregar columna `source VARCHAR(50)` a tabla `incomes`
- El frontend actual maneja `source` ("Salario", "Freelance", "Inversiones", "Alquiler", "Bonos")
- ✅ Implementado en `V5__add_source_to_incomes.sql`

### 3. Seguridad de Rutas

#### Backend (`SecurityConfig.java`)
La configuración actual en Sprint 1 permite acceso público solo a:
```
/api/users/register, /api/users/login, /api/users/refresh, /api/users/logout
/v3/api-docs/**, /swagger-ui/**, /actuator/health
```
Todo lo demás requiere autenticación JWT (`anyRequest().authenticated()`).

- [x] `[BE]` Verificar que los nuevos endpoints (`/api/categories/**`, `/api/incomes/**`, `/api/expenses/**`) queden cubiertos por `anyRequest().authenticated()` — **ya lo están por defecto**, pero documentarlo.
  - ✅ Verificado en `config/SecurityConfig.java` — `anyRequest().authenticated()` cubre todos los endpoints nuevos

- [x] `[BE]` Implementar verificación de **ownership** en cada operación:
- En `CategoryService`, `IncomeService`, `ExpenseService`: antes de actualizar/eliminar, verificar que el `userId` del recurso coincida con el `userId` del JWT.
- Si no coincide → lanzar `AccessDeniedException` (403).
- ✅ Implementado en `security/SecurityUtils.java` (helper `getCurrentUserId()`) + ownership checks en cada Service

- [ ] `[BE]` Configurar `Method Security` con `@PreAuthorize` si se necesita granularidad adicional (opcional para este sprint, pero dejar la puerta abierta).

#### Frontend (protección de rutas)

- [x] `[FE]` Migrar `middleware.ts` → `proxy.ts` (Next 16 convención)
- ✅ Se migró de `middleware.ts` a `proxy.ts` — la función exportada `middleware` pasó a llamarse `proxy`, archivo renombrado a `proxy.ts`
- ✅ Next.js 16.2.0 depreca `middleware.ts` → `proxy.ts` con runtime `nodejs`. El proyecto ya usa Next 16.2.0, por lo que la migración es directa y no requiere edge runtime
- El matcher cubre todas las rutas excepto api/_next/static/favicon

- [x] `[FE]` Manejar errores 401/403 globalmente en el interceptor de `api-client.ts`:
- Si refresh falla → limpiar sesión y redirigir a `/login`
- Si 403 → mostrar toast de "No tenés permiso para realizar esta acción"
- ✅ Implementado en `api-client.ts` interceptors — `showForbiddenToast()` en 403, `clearClientSession()` + `redirectToLogin()` en refresh fallido

### 4. Frontend — Reorganización de Servicios

#### Estado actual del `lib/`
```
lib/
├── api-client.ts        ← Instancia axios + interceptores + funciones auth
├── api-client.test.ts   ← Test del api-client
└── utils.ts             ← cn() helper
```

#### Estructura objetivo (estándar Next.js App Router)

En Next.js App Router, `lib/` es el lugar canónico para utilidades, tipos y servicios internos. No se crea una carpeta `services/` suelta a nivel raíz — eso rompe la convención del framework.

```
lib/
├── api-client.ts              ← Instancia base de axios con interceptores (se mantiene, pero SOLO config HTTP)
├── utils.ts                   ← cn() y helpers generales (se mantiene)
├── types/
│   ├── auth.ts                ← Tipos de auth (ApiUser, ApiAuthResponse) — migrados desde api-client.ts
│   ├── category.ts            ← Category, CategoryRequest, CategoryResponse
│   ├── income.ts              ← Income, IncomeRequest, IncomeResponse
│   └── expense.ts             ← Expense, ExpenseRequest, ExpenseResponse
├── schemas/
│   ├── category.schema.ts     ← Zod schemas para validación de categorías
│   ├── income.schema.ts       ← Zod schemas para validación de ingresos
│   └── expense.schema.ts      ← Zod schemas para validación de gastos
└── services/
    ├── auth.service.ts        ← registerRequest, loginRequest, refreshRequest, logoutRequest (migrados desde api-client.ts)
    ├── category.service.ts    ← getCategories, createCategory, updateCategory, deleteCategory
    ├── income.service.ts      ← getIncomes, createIncome, updateIncome, deleteIncome
    └── expense.service.ts     ← getExpenses, createExpense, updateExpense, deleteExpense
```

- [x] `[FE]` Extraer tipos desde `api-client.ts` a `lib/types/auth.ts`
  - ✅ Implementado en `lib/types/auth.ts` — `ApiUser`, `ApiAuthResponse`

- [x] `[FE]` Mover funciones de auth desde `api-client.ts` a `lib/services/auth.service.ts`
  - ✅ Implementado en `lib/services/auth.service.ts` — `registerRequest`, `loginRequest`, `refreshRequest`, `logoutRequest`

- [x] `[FE]` Limpiar `api-client.ts` — dejar SOLO la instancia de axios + interceptores + `setAccessToken`/`clearAccessToken`/`getApiErrorMessage`
  - ✅ Implementado en `lib/api-client.ts` — solo config HTTP, interceptores, refresh logic, helpers

- [x] `[FE]` Crear `lib/types/category.ts`, `lib/types/income.ts`, `lib/types/expense.ts`
  - ✅ Implementado — tipos con `Category`, `CategoryType`, `Income`, `IncomeFilters`, `PaginatedResponse`, `Expense`, `ExpenseFilters`, etc.

- [x] `[FE]` Crear `lib/schemas/` con Zod schemas que espejen las validaciones del backend
  - ✅ Implementado — `category.schema.ts`, `income.schema.ts`, `expense.schema.ts`

- [x] `[FE]` Crear `lib/services/category.service.ts`
  - ✅ Implementado — `getCategories`, `createCategory`, `updateCategory`, `deleteCategory`

- [x] `[FE]` Crear `lib/services/income.service.ts`
  - ✅ Implementado — `getIncomes`, `createIncome`, `updateIncome`, `deleteIncome` con normalización de respuesta

- [x] `[FE]` Crear `lib/services/expense.service.ts`
  - ✅ Implementado — `getExpenses`, `createExpense`, `updateExpense`, `deleteExpense` con normalización de respuesta

- [x] `[FE]` Actualizar imports en `auth-context.tsx` y demás componentes que usen `api-client.ts`
  - ✅ Implementado — todos los componentes usan `@/lib/services/` y `@/lib/types/`

### 5. Frontend — Eliminar Datos Falsos y Conectar a Backend

#### Páginas con mock data que deben migrarse

| Página | Archivo | Mock data actual | Cambio requerido |
|--------|---------|------------------|------------------|
| Dashboard | `app/page.tsx` | `balance={45750}`, `monthlyIncome={32000}`, etc. | Crear `lib/services/dashboard.service.ts` con `GET /api/analysis/summary` (si existe) o calcular desde incomes/expenses |
| Ingresos | `app/ingresos/page.tsx` | `initialIncomes` array hardcodeado (8 items) | Fetch desde `GET /api/incomes` con filtros de mes |
| Gastos | `app/gastos/page.tsx` | `initialExpenses` array hardcodeado (10 items) | Fetch desde `GET /api/expenses` con filtros de categoría y fecha |

- [x] `[FE]` Página `/ingresos` conectada al backend
 - Reemplazar `useState<Income[]>(initialIncomes)` por fetch a `GET /api/incomes`
 - Implementar paginación con `useState` + effects
 - Filtros por mes/año (select de mes, selector de año)
 - Modal/formulario de crear y editar ingreso — select de categoría real desde `/api/categories?type=INCOME` + campo source + monto + fecha
 - Usar `react-hook-form` + `zod` para validación client-side
 - ✅ Implementado en `app/ingresos/page.tsx`, `components/income/income-modal.tsx`, `components/income/income-table.tsx`

- [x] `[FE]` Página `/gastos` conectada al backend
 - Reemplazar `useState<Expense[]>(initialExpenses)` por fetch a `GET /api/expenses`
 - Filtros por categoría real desde `/api/categories?type=EXPENSE` y rango de fechas
 - Tabla con paginación
 - Modal/formulario de crear y editar gasto — select de categoría real + método de pago + monto + fecha
 - Usar `react-hook-form` + `zod` para validación client-side
 - ✅ Implementado en `app/gastos/page.tsx`, `components/expenses/expense-modal.tsx`, `components/expenses/expenses-table.tsx`, `components/expenses/expenses-filters.tsx`

- [x] `[FE]` Cargar categorías reales en todos los formularios
 - Componente reutilizable `<CategorySelect type="INCOME|EXPENSE" />`
 - Fetch desde `GET /api/categories?type=INCOME` o `?type=EXPENSE`
 - Incluir categorías del sistema + personalizadas del usuario
 - ✅ Implementado en `components/shared/category-select.tsx` — usa `useCategories(type)` hook

- [x] `[FE]` Eliminación de registros con confirmación
 - `AlertDialog` de shadcn antes de eliminar
 - Toast de `sonner` para feedback de éxito/error
 - Invalidar cache/re-fetch después de eliminar
 - ✅ Implementado en ambas páginas (`ingresos/page.tsx`, `gastos/page.tsx`) con `AlertDialog` + `toast`

- [x] `[FE]` Loading skeletons en todas las páginas de datos
 - `Skeleton` de shadcn mientras se cargan datos del backend
 - Estados de carga en tablas, cards y formularios
 - ✅ Implementado — `Skeleton` en cards de resumen de ingresos, en total de gastos, en dashboard

- [x] `[FE]` Dashboard — datos básicos reales (no es el dashboard completo del Sprint 4)
 - Obtener total ingresos del mes desde `GET /api/incomes` (resumen simple)
 - Obtener total gastos del mes desde `GET /api/expenses` (resumen simple)
 - Reemplazar los valores hardcodeados en `BalanceCard` y `StatsCards`
 - Los gráficos y recomendaciones se quedan para Sprint 4
 - ✅ Implementado en `app/page.tsx` + `lib/services/dashboard.service.ts` — `getMonthlyTotals()` con datos reales

### 6. Frontend — Hooks Personalizados

- [x] `[FE]` Crear `hooks/use-categories.ts`
 - `useCategories(type?: "INCOME" | "EXPENSE")` — fetch con cache, revalidación
 - Retorna `{ categories, isLoading, error, refetch }`
 - ✅ Implementado en `hooks/use-categories.ts` — cache manual con `Map` + invalidación

- [x] `[FE]` Crear `hooks/use-incomes.ts`
 - `useIncomes(filters)` — lista paginada con filtros
 - `useCreateIncome()`, `useUpdateIncome()`, `useDeleteIncome()` — mutaciones con invalidación de cache
 - ✅ Implementado en `hooks/use-incomes.ts`

- [x] `[FE]` Crear `hooks/use-expenses.ts`
 - `useExpenses(filters)` — lista paginada con filtros
 - `useCreateExpense()`, `useUpdateExpense()`, `useDeleteExpense()` — mutaciones con invalidación de cache
 - ✅ Implementado en `hooks/use-expenses.ts`

### 7. Tests

- [x] `[BE]` Tests unitarios de `CategoryService`, `IncomeService`, `ExpenseService`
 - JUnit 5 + Mockito
 - Casos: CRUD exitoso, validaciones, ownership violado (403), categoría duplicada
 - ✅ Implementado en `service/CategoryServiceTest.java`, `service/IncomeServiceTest.java`, `service/ExpenseServiceTest.java`

- [x] `[BE]` Tests de integración de endpoints
  - `CategoryControllerTest` (9 tests), `IncomeControllerTest` (11 tests), `ExpenseControllerTest` (10 tests)
  - Verifican paginación, filtros, errores de validación, ownership (403), auth (401/403)
  - ✅ Completados y estabilizados — tests ejecutados localmente y pasando. Se añadieron mocks/ajustes en tests (JpaMetamodelMappingContext mocked, ObjectMapper configured para JavaTime) para que el slice de controllers sea reproducible en CI.

- [x] `[FE]` Actualizar tests de `api-client.test.ts` después de la refactorización
 - ✅ Implementado en `lib/api-client.test.ts` — usa `auth.service.ts` con `MockAdapter`

- [x] `[FE]` Tests de schemas Zod (validación client-side coincide con backend)
 - ✅ Implementado en `lib/schemas/schemas.test.ts` — tests para `categorySchema`, `incomeSchema`, `expenseSchema`

---

## Referencia de Endpoints (Sprint 2)

```http
# Categorías
GET    /api/categories?type=INCOME|EXPENSE
POST   /api/categories
PUT    /api/categories/{id}
DELETE /api/categories/{id}

# Ingresos
GET    /api/incomes?page=0&size=20&month=3&year=2026
POST   /api/incomes
PUT    /api/incomes/{id}
DELETE /api/incomes/{id}

# Gastos
GET    /api/expenses?page=0&size=20&category=5&from=2026-03-01&to=2026-03-31
POST   /api/expenses
PUT    /api/expenses/{id}
DELETE /api/expenses/{id}
```

Todos los endpoints requieren header `Authorization: Bearer <access_token>`.

---

## Definición de Terminado (DoD)

1. **Datos reales:** Las páginas `/ingresos` y `/gastos` muestran datos de PostgreSQL. Cero mock data en las páginas de movimientos.
2. **Ownership:** Un usuario NO puede ver, modificar ni eliminar movimientos de otro usuario. Verificado en backend (403 si intenta).
3. **Arquitectura frontend:** `lib/services/` contiene archivos separados por dominio. `lib/types/` tiene tipos TypeScript centralizados. `api-client.ts` es solo config HTTP.
4. **Validación:** Formularios usan `react-hook-form` + `zod`. Las reglas client-side coinciden con las del backend.
5. **UX:** Loading skeletons mientras cargan datos. Toast de sonner para éxito/error. Confirmación antes de eliminar.
6. **Seguridad:** `proxy.ts` protege rutas (Next 16 convención). Interceptor maneja 401/403 globalmente. Backend verifica ownership.
7. **Migraciones:** V3, V4, V5 aplicadas sin errores. Categorías del sistema disponibles.
8. **Código:** Backend sigue `java-coding-standards` y `java-docs`. Frontend sigue `next-best-practices` y `typescript-advanced-types`.
9. **Tests:** Services backend con JUnit 5 (10 tests). Controllers con `@WebMvcTest` (30 tests). Schemas Zod testeados (6 tests). API client tests actualizados (7 tests). Total: 53 tests.

---

## Riesgos y Consideraciones

- **Categorías del sistema vs. usuario:** ✅ RESUELTO — Se agregó `is_system BOOLEAN` + `user_id NULLABLE` en V3. El service usa `findAllAccessibleByUserId()` que combina system + user categories.
- **Paginación backend:** ✅ RESUELTO — Spring Data `Pageable` retorna formato `Page<T>`. El frontend normaliza la respuesta con `PaginatedResponse<T>`.
- **Dashboard parcial:** ✅ RESUELTO — Se conectaron los totales básicos via `dashboard.service.ts`. Los gráficos y recomendaciones son del Sprint 4.
- **`payment_method` y `source`:** ✅ RESUELTO — Migraciones V4 y V5 aplicadas. Entidades y DTOs incluyen ambos campos.
- **✅ `middleware.ts` → `proxy.ts` (Next 16):** RESUELTO — Se migró a `proxy.ts` con función `proxy()` exportada. Next 16.2.0 depreca `middleware.ts`. Runtime `nodejs` (no edge). No se requiere codemod porque la lógica es simple.

---

*FinSmart — Sprint 2 — Plan de Ingeniería — COMPLETADO*
