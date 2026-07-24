# Modelo de Datos — KoroFin

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

### 2.1 Users (`V1`, + `V14`)

| Columna | Tipo | Restricciones |
|---------|------|--------------|
| `id` | BIGSERIAL | PK |
| `email` | VARCHAR(255) | UNIQUE, NOT NULL |
| `password_hash` | VARCHAR(255) | NOT NULL |
| `name` | VARCHAR(255) | NOT NULL |
| `ai_chat_used` | INT | NOT NULL, DEFAULT 0 (`V14`) — contador de mensajes de chat IA en el período actual |
| `ai_chat_period` | VARCHAR(7) | — (`V14`) — mes calendario UTC (`YYYY-MM`) al que pertenece `ai_chat_used`; al no coincidir con el mes actual, el contador se trata como reiniciado |
| `created_at` | TIMESTAMP | NOT NULL |
| `updated_at` | TIMESTAMP | NOT NULL |

`ai_chat_used`/`ai_chat_period` reemplazan un enfoque anterior que contaba filas `ai_messages` de tipo CHAT para la cuota — ese enfoque se rompía porque `UserService#login` purga esas filas en cada login, reseteando la cuota sin querer. Ser un contador dedicado también permite reservarlo con un único `UPDATE` atómico condicional, cerrando una condición de carrera check-then-act.

### 2.2 Refresh Tokens (`V2`, + `V13`)

| Columna | Tipo | Restricciones |
|---------|------|--------------|
| `id` | BIGSERIAL | PK |
| `user_id` | BIGINT | FK → users, NOT NULL |
| `token_hash` | VARCHAR(255) | UNIQUE, NOT NULL |
| `expires_at` | TIMESTAMP | NOT NULL |
| `revoked` | BOOLEAN | DEFAULT false |
| `remember_me` | BOOLEAN | NOT NULL (`V13`) — si el login se pidió con "recordarme"; controla si la cookie de refresh se emite con `Max-Age` (persiste tras cerrar el navegador) o como cookie de sesión |
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

### 2.5 Gastos (`V3`, + `V21`)

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
| `card_movement_id` | BIGINT | FK → card_movements (`V21`), ON DELETE SET NULL — vincula el gasto con el movimiento de tarjeta que lo originó (compra registrada en el ledger de `tarjetas`) |
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

### 2.13 Debt Charges (`V15`)

Espejo de Debt Payments: en vez de descontar `remaining_amount` de una deuda, lo incrementa (una compra con tarjeta asociada a la deuda, por ejemplo). Es el único punto del sistema donde `debts.remaining_amount` se incrementa — siempre a través de un `UPDATE` atómico condicional, nunca sobrescribiendo el valor directamente.

| Columna | Tipo | Restricciones |
|---------|------|--------------|
| `id` | BIGSERIAL | PK |
| `debt_id` | BIGINT | FK → debts, NOT NULL |
| `amount` | DECIMAL(15,2) | NOT NULL |
| `charge_date` | DATE | NOT NULL |
| `description` | VARCHAR(255) | — |
| `created_at` | TIMESTAMP | NOT NULL |

### 2.14 AI Usage Events (`V16`, + `V25`)

Registro granular de uso de proveedores de IA, distinto y complementario a la cuota de `users` (`V14`): mientras `ai_chat_used` es un tope mensual grueso, `ai_usage_events` guarda una fila por **intento** de llamada a un proveedor (incluyendo intentos que fallaron y se derivaron al siguiente en el failover), pensado para reporting y para un futuro metering de billing. Es inmutable — no tiene `updated_at`.

| Columna | Tipo | Restricciones |
|---------|------|--------------|
| `id` | BIGSERIAL | PK |
| `user_id` | BIGINT | FK → users, NOT NULL |
| `provider` | VARCHAR(60) | NOT NULL |
| `event_type` | VARCHAR(30) | NOT NULL — tipo de operación IA (chat, categorize, insight, statement_extract) |
| `tokens_used` | INT | NOT NULL |
| `cost_estimate` | DECIMAL(10,6) | — nullable si el proveedor/modelo no tiene precio conocido (ver `AiProviderPricing`) |
| `latency_ms` | INT | — (`V25`) nullable; solo poblado en filas por intento |
| `success` | BOOLEAN | NOT NULL, DEFAULT true (`V25`) — `false` marca un intento que falló y derivó al siguiente proveedor |
| `error_type` | VARCHAR(60) | — (`V25`) nombre simple de la excepción del intento fallido, `NULL` si tuvo éxito |
| `created_at` | TIMESTAMP | NOT NULL |

Los agregados de uso mensual visibles al usuario (`GET /api/ai/usage`) filtran `success = true`, para que un intento fallido que derivó a otro proveedor no infle el conteo visible de "llamadas de IA este mes".

### 2.15 Credit Cards (`V17`)

Tarjeta de crédito revolvente de un usuario. `current_balance` es la suma cacheada y siempre consistente de los `card_movements` de la tarjeta — solo cambia vía `UPDATE` atómico (incrementar/decrementar) disparado por un movimiento concreto, nunca se sobrescribe directamente. El cupo disponible (`credit_limit - current_balance`) se deriva en lectura, no se almacena.

| Columna | Tipo | Restricciones |
|---------|------|--------------|
| `id` | BIGSERIAL | PK |
| `user_id` | BIGINT | FK → users, NOT NULL |
| `name` | VARCHAR(150) | NOT NULL |
| `bank` | VARCHAR(100) | — |
| `franchise` | VARCHAR(20) | NOT NULL — enum `CardFranchise` |
| `credit_limit` | DECIMAL(15,2) | NOT NULL |
| `monthly_rate` | DECIMAL(6,4) | NOT NULL — tasa mensual efectiva (ej. `0.0250` = 2.5% E.M.) |
| `cutoff_day` | INT | NOT NULL — día de corte del ciclo de facturación |
| `payment_due_day` | INT | NOT NULL — día límite de pago |
| `current_balance` | DECIMAL(15,2) | NOT NULL |
| `last_cutoff_date` | DATE | — última fecha de cierre de ciclo procesada (guarda a `CardCycleCloseJob` de cerrar el mismo ciclo dos veces) |
| `created_at` | TIMESTAMP | NOT NULL |
| `updated_at` | TIMESTAMP | NOT NULL |

### 2.16 Card Movements (`V18`, + `V21`)

Asiento inmutable del ledger de una tarjeta (compra, pago, interés materializado, etc.) — no existe endpoint de actualización para esta entidad. `amount` siempre se guarda positivo; el efecto sobre el saldo (sumar o restar) lo determina `type`, no el signo del monto.

| Columna | Tipo | Restricciones |
|---------|------|--------------|
| `id` | BIGSERIAL | PK |
| `card_id` | BIGINT | FK → credit_cards, NOT NULL |
| `type` | VARCHAR(25) | NOT NULL — enum `CardMovementType` (compra, pago, interés, compra a cuotas, ...) |
| `amount` | DECIMAL(15,2) | NOT NULL |
| `movement_date` | DATE | NOT NULL |
| `description` | VARCHAR(255) | — |
| `cycle_close_date` | DATE | — solo poblado en movimientos `INTEREST` agregados por `CardCycleCloseJob`; usado para idempotencia/auditoría del cierre de ciclo |
| `created_at` | TIMESTAMP | NOT NULL |

### 2.17 Installment Plans (`V19`)

Plan de amortización de capital fijo para una compra diferida (2+ cuotas), dueño del `CardMovement` que lo originó (tipo `INSTALLMENT_PURCHASE`) vía FK única.

| Columna | Tipo | Restricciones |
|---------|------|--------------|
| `id` | BIGSERIAL | PK |
| `movement_id` | BIGINT | FK → card_movements, UNIQUE, NOT NULL |
| `installment_count` | INT | NOT NULL |
| `rate_at_purchase` | DECIMAL(6,4) | NOT NULL — copia congelada de `credit_cards.monthly_rate` al momento de la compra; nunca se recalcula aunque la tasa de la tarjeta cambie después |
| `created_at` | TIMESTAMP | NOT NULL |

No existe un campo de capital restante cacheado: siempre se deriva como `SUM(installments.capital_amount WHERE status = PENDING)`, ya que `installments` es el ledger auditable por compra.

### 2.18 Installments (`V20`)

Cuota individual de un `InstallmentPlan`, congelada al momento de la compra: `capital_amount` e `interest_amount` no cambian después. La suma de ambos sobre las cuotas en estado `PENDING` de un plan es el capital restante de esa compra.

| Columna | Tipo | Restricciones |
|---------|------|--------------|
| `id` | BIGSERIAL | PK |
| `plan_id` | BIGINT | FK → installment_plans, NOT NULL |
| `number` | INT | NOT NULL |
| `capital_amount` | DECIMAL(15,2) | NOT NULL |
| `interest_amount` | DECIMAL(15,2) | NOT NULL |
| `due_date` | DATE | NOT NULL |
| `status` | VARCHAR(15) | NOT NULL — enum `InstallmentStatus` (`PENDING` → `BILLED`) |
| `interest_movement_id` | BIGINT | FK → card_movements, nullable — se popula cuando el cierre de ciclo materializa el interés de esta cuota en un movimiento `INTEREST` |
| `created_at` | TIMESTAMP | NOT NULL |

### 2.19 Telegram Links (`V24`)

Vínculo entre un chat de Telegram y el usuario que lo confirmó, usado para resolver a qué usuario pertenece un mensaje entrante del bot (orquestado por n8n).

| Columna | Tipo | Restricciones |
|---------|------|--------------|
| `id` | BIGSERIAL | PK |
| `user_id` | BIGINT | FK → users, NOT NULL |
| `telegram_chat_id` | VARCHAR(50) | UNIQUE, NOT NULL |
| `linked_at` | TIMESTAMP | NOT NULL |

Re-vincular un chat ya vinculado actualiza el `user` de la fila existente en vez de crear una nueva.

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
| `V13` | Sprint 1 SaaS | Columna `remember_me` en `refresh_tokens` |
| `V14` | Sprint 1 SaaS | Columnas `ai_chat_used` / `ai_chat_period` en `users` (cuota mensual de chat IA) |
| `V15` | Sprint 1 SaaS | Tabla `debt_charges` |
| `V16` | Sprint 1 SaaS | Tabla `ai_usage_events` |
| `V17` | Fase B (tarjetas) | Tabla `credit_cards` |
| `V18` | Fase B (tarjetas) | Tabla `card_movements` |
| `V19` | Fase B (tarjetas) | Tabla `installment_plans` |
| `V20` | Fase B (tarjetas) | Tabla `installments` |
| `V21` | Fase B (tarjetas) | Columna `card_movement_id` en `expenses` |
| `V22` | Fase B (tarjetas) | Nuevo tipo de notificación `card_cycle_close` |
| `V23` | Sprint 2 SaaS | Nuevo tipo de evento IA `statement_extract` |
| `V24` | Sprint 2 SaaS | Tabla `telegram_links` |
| `V25` | Sprint 2 SaaS | Columnas `latency_ms` / `success` / `error_type` en `ai_usage_events` (telemetría por intento) |

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
users 1──N ai_usage_events
users 1──N financial_analysis
users 1──N credit_cards
users 1──N telegram_links (en la práctica 1──1, pero sin UNIQUE explícito del lado de users)

categories 1──N incomes
categories 1──N expenses
categories 1──N financial_analysis (top_category)

debts 1──N debt_payments
debts 1──N debt_charges
expenses N──1 recurring_payments (nullable)
expenses N──1 debt_payments (nullable)
expenses N──1 card_movements (nullable)

credit_cards 1──N card_movements
card_movements 1──1 installment_plans (nullable, solo si type = INSTALLMENT_PURCHASE)
installment_plans 1──N installments
installments N──1 card_movements (interest_movement, nullable)
```

---

## 5. Convenciones

- **Timestamps**: todas las tablas tienen `created_at`. Las que se modifican tienen `updated_at`.
- **Soft delete**: no se usa — las eliminaciones son físicas y con confirmación del usuario.
- **Precisión monetaria**: `DECIMAL(12,2)` para todos los montos (soporta hasta $99,999,999.99).
- **FKs**: todas las claves foráneas tienen índice implícito via JPA/Hibernate.
- **Nombres**: snake_case en BD, camelCase en Java.

---

Ver también el ERD visual en `../../diagramas.md` (sección 2).

---

*Documento de modelo de datos — KoroFin*
