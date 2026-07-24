# Auditoria de base de datos — KoroFin (post Sprint 6)

Fecha original: 2026-07-05 (auditoria de cierre del Sprint 6 / MVP, que agrego `V12`). Motor: PostgreSQL, migrado con Flyway. El detalle de `V1` a `V12` que sigue en este documento refleja ese momento y no se reescribio.

**Actualizacion (estado actual, fase SaaS):** las migraciones llegan hoy hasta `V25` (`V1` a `V8`, `V10` a `V25`; sigue sin existir `V9` en el codigo fuente — ver seccion "Gap V9"). Las nuevas (`V13` a `V25`) acompanaron el modulo de tarjetas de credito (`credit_cards`, `card_movements`, `installment_plans`, `installments`), el historial de cargos de deuda (`debt_charges`), la telemetria de uso de IA (`ai_usage_events`) y la vinculacion con el bot de Telegram (`telegram_links`); el resto son columnas/enums agregados a tablas existentes (cuota de IA en `users`, "remember me" en `refresh_tokens`, nuevos tipos de notificacion/evento). Nada de esto contradice los hallazgos de `V1`-`V12` de mas abajo, que siguen vigentes tal cual se describieron.

## Migracion V12 (seguimiento post-Sprint-6)

`V12__add_debt_payment_id_to_expenses.sql` agrega una columna `debt_payment_id` (nullable) a `expenses`, con `FOREIGN KEY ... REFERENCES debt_payments (id) ON DELETE SET NULL` e indice, seguida de la misma logica exacta que `V5__add_recurring_payment_id_to_expenses.sql` uso para `recurring_payment_id`: registrar un abono a una deuda ahora genera un `Expense` vinculado, corrigiendo un bug real donde los abonos a deudas no aparecian como gasto del mes (a diferencia de los servicios recurrentes, que si generan un gasto vinculado desde el Sprint 3). Migracion de bajo riesgo: `ALTER TABLE ... ADD COLUMN` nullable sin default no bloquea con datos existentes en PostgreSQL moderno.

## Resumen ejecutivo

El esquema esta limpio, bien nombrado y cada tabla cumple una funcion clara sin solapamiento. No hay tablas huerfanas ni columnas sin uso evidente. Se encontro un indice redundante (`idx_recurring_payments_next_date`) que quedo cubierto por un indice compuesto agregado en un sprint posterior, y se confirmo que el riesgo historico de migraciones Flyway huerfanas en `target/` (de una funcionalidad BYOK removida) esta resuelto en el codigo fuente actual.

## Inventario de tablas (11)

| Tabla | Migracion | Proposito | FK / cascada |
|---|---|---|---|
| `users` | V1 | Cuentas de usuario, hash de contrasena, estado activo | — |
| `refresh_tokens` | V2 | Sesiones de refresh token (rotacion, revocacion) | `user_id` -> `users` (CASCADE) |
| `categories` | V3 | Categorias de ingreso/gasto por usuario | `user_id` -> `users` (CASCADE) |
| `incomes` | V3 | Movimientos de ingreso | `user_id` -> `users`, `category_id` -> `categories` |
| `expenses` | V3 | Movimientos de gasto | `user_id` -> `users`, `category_id` -> `categories`, `recurring_payment_id` -> `recurring_payments` (V5) |
| `debts` | V4 | Deudas del usuario (monto total/restante, tasa) | `user_id` -> `users` (CASCADE) |
| `debt_payments` | V4 | Pagos aplicados a una deuda | `debt_id` -> `debts` (CASCADE) |
| `recurring_payments` | V4 | Servicios/pagos recurrentes (mensual/semanal) | `user_id` -> `users` (CASCADE) |
| `financial_analysis` | V6 | Snapshot mensual de metricas financieras por usuario | `user_id` -> `users` |
| `notifications` / `notification_preferences` | V7 | Notificaciones in-app y preferencias por canal | `user_id` -> `users` |
| `ai_messages` | V8 | Historial de conversacion con el asistente IA | `user_id` -> `users` |

Cada tabla tiene una responsabilidad unica y no se encontro redundancia de datos entre ellas (por ejemplo, `financial_analysis` es un snapshot derivado, no una copia de `incomes`/`expenses`).

## Tablas agregadas despues del Sprint 6

Sin re-auditar cada una en el mismo detalle que las anteriores, estas tablas se sumaron en migraciones posteriores (`V13`-`V25`) para soportar tarjetas de credito, historial de cargos de deuda, telemetria de IA e integracion con Telegram, manteniendo el mismo patron de `user_id -> users` para el aislamiento entre usuarios:

| Tabla | Migracion | Proposito |
|---|---|---|
| `debt_charges` | V15 | Historial de cargos aplicados a una deuda (distinto de `debt_payments`, que son los abonos) |
| `ai_usage_events` | V16 | Telemetria de uso de los proveedores de IA (evento por llamada) |
| `credit_cards` | V17 | Tarjetas de credito del usuario |
| `card_movements` | V18 | Compras/pagos registrados sobre una tarjeta |
| `installment_plans` | V19 | Planes de cuotas asociados a una compra con tarjeta |
| `installments` | V20 | Cuotas individuales de un plan |
| `telegram_links` | V24 | Vinculo entre un usuario y su chat de Telegram (bot) |

Con estas 7 tablas nuevas, el total de entidades JPA en el proyecto es 19 (antes 12 al cierre del Sprint 6: los 11 renglones del inventario de arriba, contando `notifications` y `notification_preferences` como dos entidades).

## Gap V9

No existe `V9__*.sql` en el codigo fuente; la numeracion salta de `V8` a `V10`. Esto es **intencional y ya aceptado**: `V9` correspondia a una tabla `ai_provider_settings` (BYOK — claves de API de IA por usuario) de una funcionalidad que se removio antes de llegar a `develop`, reemplazada por proveedores de IA configurados por variables de entorno a nivel de operador de la app (Sprint 5). El gap en la numeracion es cosmetico y no representa una migracion faltante: Flyway no requiere continuidad estricta de version, solo orden creciente.

**Riesgo verificado y resuelto**: en una sesion anterior se encontraron artefactos compilados huerfanos (`V9__create_ai_provider_settings.sql`, `V11__add_unique_default_provider_index.sql` de la misma funcionalidad BYOK) unicamente en `target/classes/db/migration/`, nunca en `src`. Un `mvnw clean` los purgo. El `V11` actual (`add_debt_payments_index.sql`) es una migracion completamente distinta y correcta, sin relacion con el `V11` BYOK huerfano. Si algun entorno desplegado (no local) llego a aplicar el `V9`/`V11` BYOK antes de removerlos del codigo, su `flyway_schema_history` tendria una entrada que ya no corresponde a ningun archivo fuente — Flyway fallaria la validacion (`flyway validate`) contra ese entorno especifico. **Accion recomendada antes de desplegar a cualquier entorno que haya corrido versiones pre-Sprint-5 del backend**: correr `flyway info` contra ese entorno y, si aparecen esas dos migraciones aplicadas, resolver con `flyway repair` (elimina del historial las entradas sin archivo correspondiente) antes de aplicar `V10`/`V11` actuales.

## Indice redundante encontrado: `idx_recurring_payments_next_date`

`V4__create_debts_debt_payments_recurring_payments.sql` crea `idx_recurring_payments_next_date` (columna unica `next_payment_date`), pensado para el job de recordatorios de pago del Sprint 5. `V10__add_performance_indexes.sql` (tambien Sprint 5) agrego despues `idx_recurring_payments_active_next_date` (compuesto, `is_active, next_payment_date`).

Se verifico el unico consumidor de esta columna para *scans* (`RecurringPaymentRepository.findActiveByNextPaymentDateBetween`): siempre filtra `active = true AND nextPaymentDate BETWEEN :start AND :end`, es decir, **siempre** incluye `is_active` en el filtro. Por la regla de prefijo izquierdo de los indices B-tree, el indice compuesto de `V10` ya cubre completamente este patron de acceso; el indice de columna unica de `V4` quedo redundante desde que se agrego `V10`, exactamente el mismo caso que la propia `V10` documento para los indices de fecha de `expenses`/`incomes`.

**Recomendacion** (no aplicada en este sprint — es un cambio de esquema fuera del alcance ya definido para `V11`, y el Sprint 6 solo listaba el indice de `debt_payments` como pendiente): agregar una migracion `V12` que lo elimine, siguiendo el mismo patron que `V10`/`V11`:

```sql
-- idx_recurring_payments_next_date (V4) quedo redundante desde V10: el unico consumidor
-- (RecurringPaymentRepository.findActiveByNextPaymentDateBetween) siempre filtra por
-- is_active junto con next_payment_date, cubierto por idx_recurring_payments_active_next_date.
DROP INDEX IF EXISTS idx_recurring_payments_next_date;
```

Impacto de no aplicarlo: ninguno funcional; solo un costo marginal de mantenimiento de indice en cada escritura a `recurring_payments`.

## Migracion V11 (Sprint 6)

`V11__add_debt_payments_index.sql` reemplaza `idx_debt_payments_debt_id` (columna unica, de `V4`) por `idx_debt_payments_debt_date` (compuesto `debt_id, payment_date`), con la misma justificacion que `V10`: el reporte mensual y cualquier vista de historial de pagos por deuda siempre filtran por ambas columnas juntas. Migracion verificada: sintaxis correcta, sin bloqueos esperados (tabla pequena, `DROP INDEX IF EXISTS` + `CREATE INDEX` sin `CONCURRENTLY` es aceptable en este volumen).

## Seed de desarrollo

`smart-finance-backend/src/main/resources/db/dev-seed/seed_jhon_quiceno.sql` sigue apuntando a `user_id = 2`, consistente con el usuario de prueba usado en el DoD del Sprint 6 ("probado end-to-end con Jhon Quiceno, `user_id = 2`").

## Veredicto

El esquema esta sano: sin redundancia de tablas, cada una con una funcion clara, claves foraneas con `ON DELETE CASCADE` donde corresponde semanticamente (los hijos no tienen sentido sin el padre), y `CHECK` constraints protegiendo invariantes de negocio (montos positivos, frecuencias validas). El unico hallazgo de este sprint es el indice redundante de `recurring_payments`, de impacto minimo y con una migracion de una linea lista para aplicar cuando se decida. El gap de `V9` y el riesgo de `flyway_schema_history` en entornos viejos ya estan documentados y no bloquean el cierre del Sprint 6.
