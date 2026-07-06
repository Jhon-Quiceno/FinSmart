# Modelo de Datos — FinSmart

> **Propósito:** Describir el modelo entidad-relación, las migraciones Flyway y el esquema de base de datos.

---

## 1. Principios de Diseño

- **Tablas creadas incrementalmente**: cada tabla se crea en el sprint donde su funcionalidad se construye (no hay tablas vacías de features futuras).
- **Migraciones versionadas con Flyway**: formato `V{N}__{descripcion}.sql`, numeradas secuencialmente.
- **Integridad referencial**: claves foráneas donde corresponda, con `ON DELETE` explícito.
- **Índices de rendimiento**: migraciones dedicadas para índices (V10, V11).
- **Updates atómicos**: las operaciones críticas usan `UPDATE` condicional en SQL para evitar condiciones de carrera.

---

## 2. Entidades del Sistema

### 2.1 Users (`V1`)

| Columna | Tipo | Restricciones |
|---------|------|--------------|
| `id` | BIGSERIAL | PK |
| `email` | VARCHAR(255) | UNIQUE, NOT NULL |
| `password_hash` | VARCHAR(255) | NOT NULL |
| `name` | VARCHAR(255) | NOT NULL |
| `created_at` | TIMESTAMP | NOT NULL |
| `updated_at` | TIMESTAMP | NOT NULL |

### 2.2 Refresh Tokens (`V2`)

| Columna | Tipo | Restricciones |
|---------|------|--------------|
| `id` | BIGSERIAL | PK |
| `user_id` | BIGINT | FK → users, NOT NULL |
| `token_hash` | VARCHAR(255) | UNIQUE, NOT NULL |
| `expires_at` | TIMESTAMP | NOT NULL |
| `revoked` | BOOLEAN | DEFAULT false |
| `created_at` | TIMESTAMP | NOT NULL |

Índices: `(user_id)`, `(expires_at)`

### 2.3 Categorías (`V3`)

| Columna | Tipo | Restricciones |
|---------|------|--------------|
| `id` | BIGSERIAL | PK |
| `user_id` | BIGINT | FK → users, NOT NULL |
| `name` | VARCHAR(100) | NOT NULL |
| `type` | VARCHAR(10) | CHECK (INCOME / EXPENSE), NOT NULL |
| `color` | VARCHAR(7) | — |
| `icon` | VARCHAR(50) | — |
| `created_at` | TIMESTAMP | NOT NULL |

### 2.4 Ingresos (`V3`)

| Columna | Tipo | Restricciones |
|---------|------|--------------|
| `id` | BIGSERIAL | PK |
| `user_id` | BIGINT | FK → users, NOT NULL |
| `category_id` | BIGINT | FK → categories |
| `amount` | DECIMAL(12,2) | NOT NULL |
| `source` | VARCHAR(100) | — |
| `date` | DATE | NOT NULL |
| `description` | TEXT | — |
| `created_at` | TIMESTAMP | NOT NULL |
| `updated_at` | TIMESTAMP | NOT NULL |

### 2.5 Gastos (`V3`)

| Columna | Tipo | Restricciones |
|---------|------|--------------|
| `id` | BIGSERIAL | PK |
| `user_id` | BIGINT | FK → users, NOT NULL |
| `category_id` | BIGINT | FK → categories |
| `amount` | DECIMAL(12,2) | NOT NULL |
| `payment_method` | VARCHAR(20) | CHECK (CASH / DEBIT_CARD / CREDIT_CARD / TRANSFER / OTHER) |
| `date` | DATE | NOT NULL |
| `description` | TEXT | — |
| `recurring_payment_id` | BIGINT | FK → recurring_payments (V5), ON DELETE SET NULL |
| `debt_payment_id` | BIGINT | FK → debt_payments (V12), ON DELETE SET NULL |
| `created_at` | TIMESTAMP | NOT NULL |
| `updated_at` | TIMESTAMP | NOT NULL |

### 2.6 Deudas (`V4`)

| Columna | Tipo | Restricciones |
|---------|------|--------------|
| `id` | BIGSERIAL | PK |
| `user_id` | BIGINT | FK → users, NOT NULL |
| `name` | VARCHAR(255) | NOT NULL |
| `total_amount` | DECIMAL(12,2) | NOT NULL |
| `remaining_amount` | DECIMAL(12,2) | NOT NULL |
| `interest_rate` | DECIMAL(5,2) | — |
| `due_date` | DATE | NOT NULL |
| `created_at` | TIMESTAMP | NOT NULL |
| `updated_at` | TIMESTAMP | NOT NULL |

### 2.7 Debt Payments (`V4`)

| Columna | Tipo | Restricciones |
|---------|------|--------------|
| `id` | BIGSERIAL | PK |
| `debt_id` | BIGINT | FK → debts, NOT NULL |
| `amount` | DECIMAL(12,2) | NOT NULL |
| `payment_date` | DATE | NOT NULL |
| `notes` | TEXT | — |
| `created_at` | TIMESTAMP | NOT NULL |

### 2.8 Recurring Payments (`V4`)

| Columna | Tipo | Restricciones |
|---------|------|--------------|
| `id` | BIGSERIAL | PK |
| `user_id` | BIGINT | FK → users, NOT NULL |
| `name` | VARCHAR(255) | NOT NULL |
| `amount` | DECIMAL(12,2) | NOT NULL |
| `frequency` | VARCHAR(10) | CHECK (MONTHLY / WEEKLY), NOT NULL |
| `next_payment_date` | DATE | NOT NULL |
| `is_active` | BOOLEAN | DEFAULT true |
| `created_at` | TIMESTAMP | NOT NULL |
| `updated_at` | TIMESTAMP | NOT NULL |

### 2.9 Financial Analysis (`V6`)

| Columna | Tipo | Restricciones |
|---------|------|--------------|
| `id` | BIGSERIAL | PK |
| `user_id` | BIGINT | FK → users, NOT NULL |
| `period_year` | INT | NOT NULL |
| `period_month` | INT | NOT NULL |
| `total_income` | DECIMAL(12,2) | NOT NULL |
| `total_expense` | DECIMAL(12,2) | NOT NULL |
| `savings` | DECIMAL(12,2) | NOT NULL |
| `expense_ratio` | DECIMAL(5,2) | NOT NULL |
| `debt_ratio` | DECIMAL(5,2) | NOT NULL |
| `top_category_id` | BIGINT | FK → categories |
| `created_at` | TIMESTAMP | NOT NULL |

Restricción: `UNIQUE(user_id, period_year, period_month)`

### 2.10 Notificaciones (`V7`)

| Columna | Tipo | Restricciones |
|---------|------|--------------|
| `id` | BIGSERIAL | PK |
| `user_id` | BIGINT | FK → users, NOT NULL |
| `type` | VARCHAR(50) | NOT NULL |
| `title` | VARCHAR(255) | NOT NULL |
| `message` | TEXT | NOT NULL |
| `is_read` | BOOLEAN | DEFAULT false |
| `read_at` | TIMESTAMP | — |
| `created_at` | TIMESTAMP | NOT NULL |

Índices: `(user_id, is_read)`, `(user_id, created_at)`

### 2.11 Notification Preferences (`V7`)

| Columna | Tipo | Restricciones |
|---------|------|--------------|
| `id` | BIGSERIAL | PK |
| `user_id` | BIGINT | FK → users, UNIQUE, NOT NULL |
| `payment_reminder` | BOOLEAN | DEFAULT true |
| `overspend_alert` | BOOLEAN | DEFAULT true |
| `weekly_summary` | BOOLEAN | DEFAULT true |
| `email_enabled` | BOOLEAN | DEFAULT false |

### 2.12 AI Messages (`V8`)

| Columna | Tipo | Restricciones |
|---------|------|--------------|
| `id` | BIGSERIAL | PK |
| `user_id` | BIGINT | FK → users, NOT NULL |
| `role` | VARCHAR(10) | CHECK (user / assistant), NOT NULL |
| `kind` | VARCHAR(10) | CHECK (CHAT / INSIGHT), NOT NULL |
| `content` | TEXT | NOT NULL |
| `provider` | VARCHAR(50) | — |
| `model` | VARCHAR(100) | — |
| `created_at` | TIMESTAMP | NOT NULL |

Índice: `(user_id, created_at)`

---

## 3. Migraciones Flyway

| Migración | Sprint | Descripción |
|-----------|--------|-------------|
| `V1` | 1 | Tabla `users` |
| `V2` | 1 | Tabla `refresh_tokens` + índices |
| `V3` | 2 | Tablas `categories`, `incomes`, `expenses` |
| `V4` | 3 | Tablas `debts`, `debt_payments`, `recurring_payments` |
| `V5` | 3 | Columna `recurring_payment_id` en `expenses` |
| `V6` | 4 | Tabla `financial_analysis` |
| `V7` | 5 | Tablas `notifications`, `notification_preferences` |
| `V8` | 5 | Tabla `ai_messages` |
| `V10` | 5 | Índices de salud (`expenses.date`, `incomes.date`, etc.) |
| `V11` | 6 | Índices de cierre (`debt_payments`) |
| `V12` | 6 (post) | Columna `debt_payment_id` en `expenses` |

> Nota: No existe V9 — fue reservada para el diseño BYOK que se descartó.

---

## 4. Relaciones Clave

```
users 1──N refresh_tokens
users 1──N categories
users 1──N incomes
users 1──N expenses
users 1──N debts
users 1──N recurring_payments
users 1──N notifications
users 1──1 notification_preferences
users 1──N ai_messages
users 1──N financial_analysis

categories 1──N incomes
categories 1──N expenses
categories 1──N financial_analysis (top_category)

debts 1──N debt_payments
expenses N──1 recurring_payments (nullable)
expenses N──1 debt_payments (nullable)
```

---

## 5. Convenciones

- **Timestamps**: todas las tablas tienen `created_at`. Las que se modifican tienen `updated_at`.
- **Soft delete**: no se usa — las eliminaciones son físicas y con confirmación del usuario.
- **Precisión monetaria**: `DECIMAL(12,2)` para todos los montos (soporta hasta $99,999,999.99).
- **FKs**: todas las claves foráneas tienen índice implícito via JPA/Hibernate.
- **Nombres**: snake_case en BD, camelCase en Java.

---

*Documento de modelo de datos — FinSmart MVP*
