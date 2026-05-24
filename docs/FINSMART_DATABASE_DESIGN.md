# 🗄️ FinSmart — Diseño de Base de Datos (v2)

> Revisión completa del esquema PostgreSQL para la Plataforma Inteligente de Gestión Financiera Personal.  
> Fecha: Mayo 2026 | Stack: Java + Spring Boot + PostgreSQL + Flyway

---

## 📋 Resumen de cambios

| Tabla | Estado | Razón |
|---|---|---|
| `users` | ✅ Mejorada | Se agregan campos de seguridad y control |
| `categories` | ✅ Mejorada | Se agrega `description` |
| `incomes` | ❌ Eliminada | Unificada en `transactions` |
| `expenses` | ❌ Eliminada | Unificada en `transactions` |
| `transactions` | 🆕 Nueva | Reemplaza incomes + expenses |
| `accounts` | 🆕 Nueva | Crítica: origen del dinero |
| `budgets` | 🆕 Nueva | Base para alertas de sobregasto e IA |
| `financial_goals` | 🆕 Nueva | Metas de ahorro |
| `debts` | ✅ Mejorada | Se agregan tipo, acreedor y fecha inicio |
| `recurring_payments` | ✅ Mejorada | Se agrega fin, método de pago y días de alerta |
| `notifications` | ✅ Mejorada | Se agrega scheduling y entidad relacionada |
| `refresh_tokens` | ✅ Sin cambios | Está muy bien tal como está |

---

## ❌ Cambios críticos explicados

### Por qué eliminar `incomes` y `expenses` por separado

Un ingreso y un gasto son exactamente lo mismo: un **movimiento de dinero**. Solo cambia el tipo. Tener dos tablas separadas significa:

- Duplicar columnas idénticas (amount, description, date, category, timestamps)
- Duplicar lógica en el backend (dos services, dos repositories, dos controladores)
- Complicar los reportes: `SUM(incomes) - SUM(expenses)` vs un solo `SUM(CASE WHEN type = 'INCOME' THEN amount ELSE -amount END)`
- Bloquear la escalabilidad futura (transferencias, reembolsos, pagos de deuda)

**Solución: tabla `transactions` con campo `type`.**

---

### Por qué eliminar `is_recurring` de las transacciones

Tienes dos formas de representar recurrencia al mismo tiempo:

1. `is_recurring BOOLEAN` en `incomes`/`expenses`
2. La tabla `recurring_payments`

Eso genera inconsistencias: ¿cuál manda? ¿puede existir un gasto con `is_recurring = true` sin un registro en `recurring_payments`?

**Solución: eliminar `is_recurring` de transacciones. La recurrencia vive únicamente en `recurring_payments`. Cuando n8n ejecuta un pago recurrente, genera automáticamente una `transaction`.**

---

### Por qué eliminar `source` de `incomes`

El campo `source VARCHAR` contenía valores como `"Salario"`, `"Freelance"`, `"Otros"`. Eso es exactamente lo que hace la tabla `categories`. Estabas duplicando la categorización con un campo de texto libre, lo que complica filtros, reportes y consistencia.

**Solución: eliminado. Usar `category_id` correctamente.**

---

## 🗃️ Esquema completo

---

### 👤 `users`

```sql
CREATE TABLE public.users (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(120)            NOT NULL,
    email           VARCHAR(180)            NOT NULL UNIQUE,
    password_hash   VARCHAR(120)            NOT NULL,
    is_active       BOOLEAN                 NOT NULL DEFAULT true,   -- [+] soft delete
    last_login_at   TIMESTAMP,                                       -- [+] seguridad y análisis
    created_at      TIMESTAMP               NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP               NOT NULL DEFAULT now()
);
```

**Cambios:**
- `is_active` — en producción nunca borras usuarios realmente. El soft delete te protege de pérdida de datos y te permite reactivar cuentas.
- `last_login_at` — útil para el sistema de reactivación de n8n ("hace 7 días no usas la app") y para auditoría de seguridad.

---

### 🗂️ `categories`

```sql
CREATE TABLE public.categories (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT REFERENCES public.users(id) ON DELETE CASCADE,  -- NULL = categoría del sistema
    name        VARCHAR(100)    NOT NULL,
    type        VARCHAR(20)     NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    icon        VARCHAR(50),
    color       VARCHAR(7),
    description VARCHAR(255),                                            -- [+] descripción opcional
    is_system   BOOLEAN         NOT NULL DEFAULT false,
    created_at  TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP       NOT NULL DEFAULT now(),
    UNIQUE (user_id, name)
);

CREATE INDEX idx_categories_user_id  ON public.categories(user_id);
CREATE INDEX idx_categories_type     ON public.categories(type);
CREATE INDEX idx_categories_is_system ON public.categories(is_system);
```

**Cambios:**
- `description` — campo opcional para que el usuario explique su categoría personalizada. Útil en el UI.

> ℹ️ Las categorías del sistema tienen `user_id = NULL` e `is_system = true`. Ese patrón está perfecto, no cambia.

---

### 💳 `accounts` *(NUEVA — CRÍTICA)*

Esta es la tabla más importante que faltaba. Sin ella no sabes **de dónde sale ni a dónde va el dinero**.

```sql
CREATE TYPE account_type AS ENUM (
    'CASH',           -- Efectivo
    'SAVINGS',        -- Cuenta de ahorros
    'CHECKING',       -- Cuenta corriente
    'CREDIT_CARD',    -- Tarjeta de crédito
    'DIGITAL_WALLET', -- Nequi, Daviplata, etc.
    'OTHER'
);

CREATE TABLE public.accounts (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT          NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    name        VARCHAR(100)    NOT NULL,         -- Ej: "Bancolombia Ahorros", "Nequi", "Efectivo"
    type        account_type    NOT NULL,
    balance     NUMERIC(15,2)   NOT NULL DEFAULT 0.00,
    currency    VARCHAR(3)      NOT NULL DEFAULT 'COP',
    color       VARCHAR(7),                       -- Para diferenciarlo en UI
    is_active   BOOLEAN         NOT NULL DEFAULT true,
    created_at  TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE INDEX idx_accounts_user_id ON public.accounts(user_id);
```

**¿Para qué sirve?**
- El dashboard puede mostrar el balance real por cuenta
- Los gastos saben si se pagaron con efectivo, tarjeta o Nequi
- Futuro: transferencias entre cuentas
- El motor financiero puede detectar qué cuenta está más comprometida

---

### 💰 `transactions` *(NUEVA — reemplaza incomes + expenses)*

```sql
CREATE TYPE transaction_type AS ENUM (
    'INCOME',    -- Ingreso
    'EXPENSE',   -- Gasto
    'TRANSFER'   -- Futuro: transferencia entre cuentas
);

CREATE TYPE payment_method_type AS ENUM (
    'CASH',          -- Efectivo
    'DEBIT_CARD',    -- Tarjeta débito
    'CREDIT_CARD',   -- Tarjeta crédito
    'TRANSFER',      -- Transferencia bancaria
    'DIGITAL_WALLET' -- Nequi, Daviplata, etc.
);

CREATE TABLE public.transactions (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT                  NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    account_id      BIGINT                  REFERENCES public.accounts(id) ON DELETE SET NULL,
    category_id     BIGINT                  REFERENCES public.categories(id) ON DELETE SET NULL,
    type            transaction_type        NOT NULL,
    amount          NUMERIC(15,2)           NOT NULL CHECK (amount > 0),
    description     VARCHAR(255),
    transaction_date DATE                   NOT NULL,
    payment_method  payment_method_type,
    notes           TEXT,                           -- [+] notas adicionales opcionales
    created_at      TIMESTAMP               NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP               NOT NULL DEFAULT now()
);

CREATE INDEX idx_transactions_user_id          ON public.transactions(user_id);
CREATE INDEX idx_transactions_type             ON public.transactions(type);
CREATE INDEX idx_transactions_transaction_date ON public.transactions(transaction_date);
CREATE INDEX idx_transactions_category_id      ON public.transactions(category_id);
CREATE INDEX idx_transactions_account_id       ON public.transactions(account_id);
```

**Qué se eliminó y por qué:**
- `is_recurring` — la recurrencia la maneja `recurring_payments` exclusivamente
- `source` — lo maneja `category_id`

**Qué se agregó:**
- `account_id` — ahora sabes exactamente de qué cuenta salió el dinero
- `notes` — campo libre para anotaciones del usuario (distinto a description)
- `CHECK (amount > 0)` — el tipo ya indica si es ingreso o gasto, el monto siempre positivo

---

### 💳 `debts`

```sql
CREATE TYPE debt_type AS ENUM (
    'CREDIT_CARD',   -- Tarjeta de crédito
    'PERSONAL_LOAN', -- Préstamo personal
    'STUDENT_LOAN',  -- Préstamo educativo
    'MORTGAGE',      -- Hipoteca
    'FAMILY',        -- Deuda con familiar/amigo
    'OTHER'
);

CREATE TABLE public.debts (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT          NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    name             VARCHAR(150)    NOT NULL,
    debt_type        debt_type       NOT NULL DEFAULT 'OTHER',   -- [+]
    creditor_name    VARCHAR(150),                                -- [+] Ej: "Bancolombia", "Mamá"
    total_amount     NUMERIC(15,2)   NOT NULL,
    remaining_amount NUMERIC(15,2)   NOT NULL,
    interest_rate    NUMERIC(5,2),
    minimum_payment  NUMERIC(15,2),
    start_date       DATE,                                        -- [+] fecha de inicio
    due_date         DATE,                                        -- fecha de vencimiento
    status           VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE'
                     CHECK (status IN ('ACTIVE', 'PAID', 'DEFAULTED')),
    created_at       TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE INDEX idx_debts_user_id ON public.debts(user_id);
CREATE INDEX idx_debts_status  ON public.debts(status);
```

**Cambios:**
- `debt_type` — permite al motor financiero priorizar deudas correctamente (mayor interés primero)
- `creditor_name` — identifica al acreedor (banco, persona, etc.)
- `start_date` — permite calcular cuánto tiempo llevas con la deuda y proyectar el costo total de intereses

---

### 🔁 `recurring_payments`

```sql
CREATE TABLE public.recurring_payments (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT          NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    account_id          BIGINT          REFERENCES public.accounts(id) ON DELETE SET NULL, -- [+]
    category_id         BIGINT          REFERENCES public.categories(id) ON DELETE SET NULL,
    name                VARCHAR(150)    NOT NULL,
    amount              NUMERIC(15,2)   NOT NULL,
    frequency           VARCHAR(20)     NOT NULL
                        CHECK (frequency IN ('DAILY', 'WEEKLY', 'BIWEEKLY', 'MONTHLY', 'YEARLY')),
    payment_method      payment_method_type,                -- [+] método de pago
    next_date           DATE            NOT NULL,
    end_date            DATE,                               -- [+] NULL = indefinido (Netflix, etc.)
    reminder_days_before INT            NOT NULL DEFAULT 3, -- [+] cuántos días antes alertar (para n8n)
    is_active           BOOLEAN         NOT NULL DEFAULT true,
    created_at          TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE INDEX idx_recurring_payments_user_id   ON public.recurring_payments(user_id);
CREATE INDEX idx_recurring_payments_next_date ON public.recurring_payments(next_date);
CREATE INDEX idx_recurring_payments_is_active ON public.recurring_payments(is_active);
```

**Cambios:**
- `account_id` — desde qué cuenta se cobra
- `payment_method` — forma de pago del servicio
- `end_date` — para suscripciones temporales (un curso de 3 meses, etc.)
- `reminder_days_before` — n8n consulta este campo para saber cuándo disparar la alerta. Diferente por servicio: el usuario puede querer 1 día para Netflix y 5 para el arriendo

---

### 🔔 `notifications`

```sql
CREATE TABLE public.notifications (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT          NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    title               VARCHAR(150)    NOT NULL,
    message             TEXT,
    type                VARCHAR(30)     NOT NULL
                        CHECK (type IN ('INFO', 'WARNING', 'ALERT', 'RECOMMENDATION')), -- [+] RECOMMENDATION
    related_entity_type VARCHAR(50),    -- [+] Ej: 'DEBT', 'RECURRING_PAYMENT', 'BUDGET'
    related_entity_id   BIGINT,         -- [+] ID de la entidad relacionada
    scheduled_for       TIMESTAMP,      -- [+] Para notificaciones futuras (n8n las crea con fecha)
    is_read             BOOLEAN         NOT NULL DEFAULT false,
    created_at          TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_user_id      ON public.notifications(user_id);
CREATE INDEX idx_notifications_is_read      ON public.notifications(is_read);
CREATE INDEX idx_notifications_type         ON public.notifications(type);
CREATE INDEX idx_notifications_scheduled_for ON public.notifications(scheduled_for); -- [+]
```

**Cambios:**
- `RECOMMENDATION` en el tipo — para las sugerencias del motor de IA
- `related_entity_type` + `related_entity_id` — la notificación puede decir "Esta alerta pertenece a la deuda #5", así el frontend puede hacer deep link directo al recurso
- `scheduled_for` — n8n puede crear notificaciones a futuro. Ejemplo: "crear notificación para el 28 de mayo sobre el vencimiento del arriendo"

---

### 📊 `budgets` *(NUEVA)*

Sin presupuestos no puedes detectar sobregasto, que es una de las funciones más importantes de n8n.

```sql
CREATE TABLE public.budgets (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT          NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    category_id BIGINT          REFERENCES public.categories(id) ON DELETE SET NULL,
    name        VARCHAR(100)    NOT NULL,        -- Ej: "Comida mayo", "Entretenimiento"
    amount      NUMERIC(15,2)   NOT NULL,        -- Límite de gasto
    period      VARCHAR(20)     NOT NULL DEFAULT 'MONTHLY'
                CHECK (period IN ('WEEKLY', 'MONTHLY', 'YEARLY')),
    start_date  DATE            NOT NULL,
    end_date    DATE,                            -- NULL = presupuesto recurrente
    is_active   BOOLEAN         NOT NULL DEFAULT true,
    created_at  TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE INDEX idx_budgets_user_id     ON public.budgets(user_id);
CREATE INDEX idx_budgets_category_id ON public.budgets(category_id);
```

**¿Cómo se usa con n8n?**

n8n consulta `budgets` y `transactions` del período actual, calcula el porcentaje ejecutado y dispara la alerta cuando supera el umbral (ej: 80%).

---

### 🎯 `financial_goals` *(NUEVA)*

Para el módulo de metas de ahorro del asistente de IA.

```sql
CREATE TABLE public.financial_goals (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT          NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    name            VARCHAR(150)    NOT NULL,        -- Ej: "Moto", "Viaje a Cartagena"
    target_amount   NUMERIC(15,2)   NOT NULL,        -- Cuánto quiero ahorrar
    current_amount  NUMERIC(15,2)   NOT NULL DEFAULT 0.00,  -- Cuánto llevo
    target_date     DATE,                            -- NULL = sin fecha límite
    status          VARCHAR(20)     NOT NULL DEFAULT 'IN_PROGRESS'
                    CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    created_at      TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE INDEX idx_financial_goals_user_id ON public.financial_goals(user_id);
CREATE INDEX idx_financial_goals_status  ON public.financial_goals(status);
```

---

### 🔑 `refresh_tokens` *(sin cambios)*

Esta tabla está excelente. El hashing del token, el `token_id` como UUID separado, el `revoked_at` para invalidación explícita y los índices están bien pensados. No se toca.

```sql
-- Sin cambios respecto a V2
CREATE TABLE public.refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    token_id    UUID        NOT NULL UNIQUE,
    user_id     BIGINT      NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(120) NOT NULL UNIQUE,
    expires_at  TIMESTAMP   NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT now(),
    revoked_at  TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_user_id    ON public.refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON public.refresh_tokens(expires_at);
```

---

## 🗺️ Diagrama de relaciones

```
users
  ├── accounts              (1:N) — cuentas del usuario
  ├── categories            (1:N) — categorías personalizadas
  ├── transactions          (1:N) → account_id, category_id
  ├── debts                 (1:N)
  ├── recurring_payments    (1:N) → account_id, category_id
  ├── budgets               (1:N) → category_id
  ├── financial_goals       (1:N)
  ├── notifications         (1:N)
  └── refresh_tokens        (1:N)

recurring_payments → genera → transactions  (via n8n)
budgets            → vigila → transactions  (via n8n)
```

---

## 🚀 Plan de migración con Flyway

Si ya tienes datos en `incomes` y `expenses`, la migración debe hacerse en orden:

```
V6__create_accounts_table.sql
V7__create_transactions_table.sql
V8__migrate_incomes_to_transactions.sql       ← INSERT INTO transactions SELECT ...
V9__migrate_expenses_to_transactions.sql      ← INSERT INTO transactions SELECT ...
V10__drop_incomes_table.sql
V11__drop_expenses_table.sql
V12__create_budgets_table.sql
V13__create_financial_goals_table.sql
V14__improve_debts_table.sql
V15__improve_recurring_payments_table.sql
V16__improve_notifications_table.sql
V17__improve_users_table.sql
V18__seed_default_accounts.sql               ← Opcional: cuenta "Efectivo" por defecto
```

> ⚠️ **Importante:** ejecutar las migraciones de datos (V8, V9) antes de eliminar las tablas originales (V10, V11). Verifica los datos migrados antes de continuar.

---

## 📊 Queries clave del motor financiero

Con el nuevo esquema, las consultas más importantes quedan así:

### Balance mensual
```sql
SELECT
    SUM(CASE WHEN type = 'INCOME'  THEN amount ELSE 0 END) AS total_income,
    SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END) AS total_expense,
    SUM(CASE WHEN type = 'INCOME'  THEN amount ELSE -amount END) AS balance
FROM transactions
WHERE user_id = :userId
  AND DATE_TRUNC('month', transaction_date) = DATE_TRUNC('month', CURRENT_DATE);
```

### Gasto por categoría (para detección de sobreconsumo)
```sql
SELECT
    c.name,
    SUM(t.amount) AS total,
    b.amount AS budget_limit,
    ROUND(SUM(t.amount) / b.amount * 100, 2) AS percentage_used
FROM transactions t
JOIN categories c ON t.category_id = c.id
LEFT JOIN budgets b ON b.category_id = c.id AND b.is_active = true
WHERE t.user_id = :userId
  AND t.type = 'EXPENSE'
  AND DATE_TRUNC('month', t.transaction_date) = DATE_TRUNC('month', CURRENT_DATE)
GROUP BY c.name, b.amount;
```

### Predicción fin de mes (para n8n)
```sql
SELECT
    SUM(amount) / EXTRACT(DAY FROM CURRENT_DATE) AS avg_daily_expense,
    (EXTRACT(DAY FROM DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month - 1 day') 
     - EXTRACT(DAY FROM CURRENT_DATE)) AS days_remaining
FROM transactions
WHERE user_id = :userId
  AND type = 'EXPENSE'
  AND DATE_TRUNC('month', transaction_date) = DATE_TRUNC('month', CURRENT_DATE);
```

---

## ✅ Checklist de implementación

### Alta prioridad (hacer ya)
- [ ] Crear tabla `accounts`
- [ ] Crear tabla `transactions`
- [ ] Migrar datos de `incomes` → `transactions` con `type = 'INCOME'`
- [ ] Migrar datos de `expenses` → `transactions` con `type = 'EXPENSE'`
- [ ] Eliminar tablas `incomes` y `expenses`
- [ ] Actualizar entities, repositories y services en Spring Boot

### Media prioridad (siguiente sprint)
- [ ] Crear tabla `budgets`
- [ ] Crear tabla `financial_goals`
- [ ] Mejorar tabla `debts` (agregar `debt_type`, `creditor_name`, `start_date`)
- [ ] Mejorar tabla `recurring_payments` (agregar `end_date`, `reminder_days_before`)
- [ ] Mejorar tabla `notifications` (agregar campos de scheduling y entidad)
- [ ] Mejorar tabla `users` (agregar `is_active`, `last_login_at`)

### Baja prioridad (cuando tengas IA activa)
- [ ] Crear cuenta "Efectivo" por defecto al registrar usuario
- [ ] Agregar tabla `transaction_tags` para análisis semántico
- [ ] Tabla `financial_analysis` para histórico de IA

---

*Documento generado para el proyecto FinSmart — v2.0*
