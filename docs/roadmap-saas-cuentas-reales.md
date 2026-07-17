# Roadmap Estratégico — KoroFin hacia SaaS con cuentas/tarjetas reales y automatización de IA

> Unifica dos investigaciones: 6 agentes especializados sobre el salto a SaaS multi-tenant
> con automatización de IA (arquitectura backend, frontend/UX, base de datos,
> automatización/IA, mercado y modelo de negocio, app móvil — 2026-07-05), y el diseño de
> tres niveles incrementales para modelar cuentas/tarjetas reales (2026-07-10). Actualizado
> a 2026-07-14 con el estado real del proyecto tras el cierre del MVP.

## Resumen ejecutivo

KoroFin es hoy un MVP funcional y bien probado (286 tests backend, 91 tests frontend,
arquitectura por capas consistente, scoping por usuario correcto, refactor por dominios ya
en `main`). El MVP (6 sprints, ver `docs/finsmart_mvp_sprints.md` en el historial de git)
cerró formalmente el 2026-07-14 y este documento arranca la fase siguiente.

El salto a SaaS multi-tenant con automatización de IA **no requiere reescribir nada** —
requiere sumar capas nuevas (uso/cuotas, ingestión de mensajes, billing) sin romper lo que
ya funciona, y tomar decisiones honestas sobre qué es viable en web vs qué exige una app
móvil. En paralelo, KoroFin puede evolucionar del registro manual de movimientos hacia
funcionalidades conectadas con las finanzas reales de los usuarios en **tres niveles
incrementales** (cuentas/tarjetas modeladas en la app → importación de extractos → Open
Finance), cada uno con valor propio y sin depender de que el anterior esté "terminado" en
un sentido estricto.

Dos conclusiones deberían moldear el orden de todo el roadmap:

1. **La lectura de correo es viable hoy mismo desde el backend web actual** (Gmail API +
   Pub/Sub); la lectura de notificaciones de otras apps (bancos, billeteras) solo es
   posible en Android vía `NotificationListenerService`, y en iOS es estructuralmente
   imposible por diseño de Apple; leer SMS directamente no es viable en ninguna plataforma
   para publicar en las tiendas de apps. Esto define el orden natural: correo primero
   (funciona ya, en cualquier plataforma), automatización móvil Android después (cuando
   exista la app), SMS descartado como fuente.
2. **El modelo de cuentas/tarjetas reales (Nivel 1) es puro modelado de dominio, sin
   terceros ni regulación**, y resuelve un dolor real de hoy (la deuda de una tarjeta solo
   puede bajar en el modelo actual de `Debt`). Es la mejora de mayor impacto con menor
   riesgo de todo el roadmap, junto con conectar la IA de categorización que ya existe en
   el backend a los formularios reales.

---

## Nivel 1 — Cuentas y tarjetas reales (sin terceros)

**Qué es:** un nuevo dominio `cuentas/` donde el usuario registra sus productos
financieros (tarjetas de crédito, cuentas de ahorro) y asocia sus movimientos a ellos.

**Qué desbloquea para el usuario:**

- Tarjetas de crédito con **cupo, fecha de corte y fecha límite de pago**.
- Alertas del tipo "tu tarjeta corta en 3 días, llevas $X en este ciclo" (reutiliza los
  jobs y notificaciones existentes).
- Cupo disponible y porcentaje de utilización por tarjeta.
- La deuda de cada tarjeta alimenta el dominio `deudas/` y el motor de análisis.

**Costo:** cero terceros, cero regulación. Es puro modelado de dominio.

### 1.1 Rediseño de deudas: de préstamo fijo a crédito rotativo

> Diseño completo (modelo, reglas de negocio, fases y decisiones abiertas) en
> **[`docs/rediseno-deudas-tarjetas.md`](rediseno-deudas-tarjetas.md)**. Esta sección es un
> resumen — no lo dupliques, andá al archivo original para el detalle.

El modelo actual (`Debt` con `totalAmount`/`remainingAmount` que solo baja con
`DebtPayment`) representa un **préstamo**: nace con un monto y solo disminuye. Una tarjeta
de crédito es **crédito rotativo**: la deuda sube con cada compra, baja con cada pago y
genera intereses según cómo se difiera cada compra.

**Principio de diseño: la deuda no se edita, se deriva.** El saldo nunca es un campo que
alguien modifica a mano; es el resultado de un libro de movimientos (ledger). Trazabilidad
total, el número siempre cuadra, historial gratis.

Modelo propuesto (Fase B, detalle completo en el archivo de diseño):

```
CreditCard        → nombre, banco, cupo, tasa E.M., día de corte, día límite de pago
CardMovement      → ledger con signo: PURCHASE (+), INSTALLMENT_PURCHASE (+),
                     PAYMENT (−), INTEREST (+), FEE (+)
InstallmentPlan   → plan de cuotas por compra diferida (cuotas, tasa congelada al momento
                     de la compra, amortización capital/interés)

saldo actual = Σ movimientos     (nunca un campo editable)
```

**Reglas de negocio clave (mercado colombiano):**

- 1 cuota no genera intereses; 2 o más sí. Cada compra diferida congela la tasa vigente al
  momento de la compra (si el banco la cambia después, las compras viejas no cambian).
- El ciclo de facturación agrupa movimientos entre fechas de corte y produce el "pago del
  mes" (compras a 1 cuota + cuotas que vencen + intereses + fees).
- Caso real de validación (tarjeta Rappi): deuda $1.500.000 → pago del mes −$225.000 →
  compra de TV +$700.000 a 3 cuotas ⇒ saldo = **$1.975.000**, más 3 cuotas de ~$233.333 de
  capital con interés sobre el saldo pendiente de esa compra específica.

**Implementación en dos fases:**

| Fase | Alcance | Esfuerzo |
|------|---------|----------|
| **A — cargos en deudas** | `DebtCharge` (espejo de `DebtPayment` con signo +): la deuda existente puede subir. No rompe nada del modelo ni la API actual. Resuelve el dolor de hoy. | Bajo |
| **B — dominio tarjetas** | `CreditCard` + ledger + plan de cuotas + ciclo de corte + intereses. La Fase A migra naturalmente: una `Debt` de tipo tarjeta pasa a derivar su saldo del ledger. Requiere **proposal, spec y design con SDD antes de una línea de código** — decisiones abiertas: amortización francesa vs. capital fijo (validar contra extractos reales), materialización de intereses al cierre de ciclo vs. cálculo on-the-fly, migración/convivencia de tipos de deuda, vínculo `CardMovement`↔`Expense`. | Medio/alto — SDD |

La Fase A es la primera tarea de `docs/sprints/sprint1.md`. La Fase B queda fuera de
sprint1 y arranca con `/sdd-new` cuando se decida priorizarla.

---

## Nivel 2 — Importar extractos bancarios (datos reales sin credenciales)

**Qué es:** el usuario descarga el extracto de su banco (CSV/Excel, todos los bancos
colombianos lo ofrecen) y lo sube a KoroFin. El backend lo parsea y crea las
transacciones, categorizadas por la IA que ya existe en la app.

**Por qué es valioso:** datos reales sin pedirle credenciales bancarias a nadie, sin
costos de terceros y sin carga regulatoria. Complementa el Nivel 1 (los movimientos
importados se asocian a la cuenta/tarjeta correspondiente).

**Consideraciones:** cada banco tiene su propio formato → empezar con 1-2 bancos (parser
por banco detrás de una interfaz común) y deduplicar contra lo ya registrado (fecha +
monto + descripción).

---

## Nivel 3 — Agregación automática vía Open Finance (terceros)

**Qué es:** conexión directa a los bancos con consentimiento del usuario, a través de un
agregador. **Nunca se almacenan credenciales bancarias propias** — el agregador entrega
tokens y datos normalizados.

**Opciones para Colombia/LatAm:**

- **Belvo** — el jugador fuerte de Open Finance en LatAm (Colombia, México, Brasil).
  Cuentas, saldos y movimientos. Tiene sandbox gratuito para desarrollo.
- **Prometeo** — alternativa con cobertura en varios países de la región.
- Plaid (el más conocido) casi no cubre Colombia: descartado.

**Realidad de costos:** el sandbox es gratis y es un excelente proyecto de
aprendizaje/portfolio, pero producción cobra por usuario/conexión y exige madurez de
compliance. Para KoroFin hoy: **explorar en sandbox, no prometer a usuarios.** Verificar
precios vigentes en belvo.com antes de comprometerse.

---

## Automatizaciones rápidas sin esperar la IA de correo

Esto se puede construir **ahora mismo**, con el stack actual, sin depender de ninguna
integración externa:

| Mejora | Prioridad | Esfuerzo |
|---|---|---|
| Quick-add flotante (FAB) global, accesible desde cualquier página, con atajo de teclado tipo `Ctrl+K` | Alta | Mediano |
| Modal ultra-mínimo (solo monto + descripción, resto colapsado/opcional) con defaults inteligentes: última categoría usada, fecha = hoy, método de pago más frecuente | Alta | Chico |
| Parser de texto libre tipo "Uber 15000" → autocompleta monto + descripción + categoría, conectado a `categorize()`/`useCategorize()` | Alta | Mediano |
| Plantillas de gastos frecuentes (chips tipo "Uber", "Supermercado" con monto/categoría precargados) | Media | Chico |
| Entrada por voz | Baja | Grande (bajo ROI hasta validar el parser de texto) |

**Estado real verificado hoy (2026-07-14) — corrige la investigación original:** el backend
ya expone `categorize()` (`lib/services/ai.service.ts`) y el hook `useCategorize()`
(`hooks/use-ai.ts`), y **`components/expenses/expense-modal.tsx` ya tiene un botón
"Sugerir categoría" conectado** (agregado en el Sprint 5, commit `b67b760`, 2026-07-04 —
antes incluso de que se escribiera la investigación original que decía que no estaba
conectado a ningún formulario). Lo que falta realmente:

- **`components/income/income-modal.tsx` no tiene esa misma sugerencia** — es la brecha
  real a cerrar, replicando el patrón ya probado en gastos.
- El **quick-add global** (FAB + `Ctrl+K` + modal ultra-mínimo) y el **parser de texto
  libre** ("Uber 15000" → monto + descripción + categoría en un solo campo) son
  funcionalidad nueva, no existen hoy en ninguna forma.
- Ya existe `components/ui/command.tsx` (shadcn `cmdk`) importado pero sin usar en
  ninguna página — es el punto de partida natural para el command palette del quick-add.

---

## Automatización con IA de correo/SMS/notificaciones — arquitectura y honestidad de plataforma

Esta es la pieza central de la visión de largo plazo. La investigación deja hechos de
plataforma muy concretos que deberían moldear las expectativas del producto.

### Correo electrónico — viable HOY, 100% desde el backend web actual

- Flujo real: **Gmail API + Google Cloud Pub/Sub** (no polling). El usuario conecta su
  cuenta una vez vía OAuth2 (scope mínimo recomendado: `gmail.readonly`), el backend
  registra un `watch()` sobre la casilla, y Gmail empuja un webhook a un topic de Pub/Sub
  cada vez que llega un correo nuevo. El `watch` se renueva cada ~7 días (recomendado: cron
  diario).
- Para proveedores no-Gmail: IMAP genérico con IDLE (casi tiempo real) o polling si el
  proveedor no soporta IDLE.
- Esta es la parte más lista para producir de toda la visión — no depende de ninguna app
  móvil.

### SMS — prácticamente inviable en cualquier plataforma para un SaaS genérico

- **Android**: Google Play restringe `READ_SMS` a apps que sean el **manejador por
  defecto de SMS, Teléfono o Asistente**. Una app de finanzas personales no calificaría —
  Google la rechazaría en revisión de la tienda.
- **iOS**: no existe ninguna API pública para que terceros lean SMS/iMessage de otras
  apps. Sandboxing absoluto, sin excepción real para este caso.
- **Conclusión**: descartar SMS como fuente de ingesta. No es una limitación de KoroFin,
  es una limitación de las plataformas.

### Notificaciones push de otras apps (bancos, billeteras) — solo Android, y solo con app móvil

- **Android**: `NotificationListenerService` sigue vigente en 2026, es una API pública
  documentada, no exige ser manejador por defecto de nada — el usuario la activa
  manualmente por app en Ajustes → Acceso a notificaciones. Es la vía más prometedora para
  capturar gastos con tarjeta y notificaciones de servicios, **pero requiere una app
  nativa Android** (no funciona desde web).
- **iOS**: Apple no ofrece equivalente para terceros. Lo único nuevo en 2026 (iOS 26.3,
  exclusivo UE por el Digital Markets Act) es reenvío de notificaciones a **accesorios
  físicos emparejados** (relojes) — no aplica a apps de terceros.

### Pipeline propuesto (a alto nivel)

```
Ingesta (correo / notificación Android)
   → extracción con LLM (monto, comercio/fuente, fecha, tipo: ingreso/gasto/servicio)
   → score de confianza
   → si confianza alta Y el usuario habilitó auto-creación → crea el movimiento directo
   → si no → bandeja de "revisión pendiente" → usuario confirma/edita
   → al confirmar → se crea el Income/Expense/RecurringPayment real,
     vinculado al mensaje origen (trazabilidad y auditoría)
```

La bandeja de revisión **no es opcional al principio** — es el mecanismo de control de
calidad contra falsos positivos de categorización, que en dinero real son mucho más graves
que en un chatbot cualquiera.

### Qué es viable hoy vs qué depende de la futura app móvil

| Fuente | Viable ya (web) | Requiere app móvil |
|---|---|---|
| Correo (Gmail API/Pub/Sub, IMAP) | ✅ | — |
| Notificaciones de apps bancarias/billeteras | — | ✅ (solo Android) |
| SMS | ❌ (inviable en ambas tiendas) | ❌ |

---

## Módulos backend nuevos + tablas de BD propuestas (boceto)

Fuente: auditoría de base de datos + arquitectura backend/frontend, cruzadas.

### Backend — módulos nuevos necesarios

- **Tracking de uso/cuotas de IA**: no existe hoy ninguna tabla que registre tokens/costo
  por usuario. Es la base de todo el futuro modelo de cobro.
- **Ingestión de correo/SMS/notificaciones**: módulo de ingesta + cola async + extracción
  con LLM — es el corazón de la visión del producto.
- **Motor de reglas de automatización**: para que el usuario pueda afinar/override la
  clasificación automática de la IA (ej. "todo lo que venga de tal remitente,
  categorizarlo como tal cosa").
- **Credenciales de integraciones de terceros**: OAuth de Gmail, webhooks bancarios —
  deben vivir separadas de la config de proveedores de IA del operador, y **cifradas
  at-rest**, nunca en texto plano.

### Base de datos — tablas nuevas propuestas (boceto, no DDL final)

```
ingested_messages     (id, user_id, source[EMAIL|SMS|PUSH], external_ref, encrypted_content,
                       received_at, processing_status[PENDING|PROCESSED|FAILED|IGNORED],
                       processed_at, matched_expense_id, matched_income_id, ai_confidence,
                       error_detail, created_at)

automation_rules      (id, user_id, name, match_type[sender|regex|keyword], match_value,
                       target_category_id, is_active, priority, created_at, updated_at)

integration_credentials (id, user_id, provider[GMAIL|BANK_WEBHOOK], encrypted_access_token,
                         encrypted_refresh_token, token_expires_at, scopes, status,
                         last_synced_at, created_at, updated_at)

ai_usage_events       (id, user_id, period_year, period_month, event_type, tokens_used,
                       cost_estimate, created_at)

plans                 (id, name, price, limits jsonb)
subscriptions         (id, user_id, plan_id, status, current_period_start, current_period_end,
                       external_billing_id)
```

**Multi-tenancy**: para un B2C de finanzas personales (1 usuario = 1 tenant, sin cuentas
compartidas todavía), `user_id` como aislamiento alcanza conceptualmente. No hace falta un
`tenant_id` desacoplado ni particionamiento a esta escala (miles de usuarios, no millones
de filas). Row-Level Security es la mejora pragmática recomendada — refuerza el filtro de
la aplicación, no lo reemplaza. Si a futuro se suman cuentas compartidas (pareja, hogar,
equipos), ahí sí introducir `accounts`/`account_members`, pero no antes de que exista ese
requisito real.

**Privacidad y retención**: el contenido de correos/SMS es dato financiero sensible.

- Cifrar `encrypted_content` en reposo (AES-GCM con clave gestionada, no una constante en
  el código).
- **Truncar/purgar el raw payload después de procesarlo con éxito** — guardar solo los
  campos extraídos + referencia al movimiento generado, no el correo completo
  indefinidamente.
- Retención corta (30-90 días) para lo no procesado.
- Exponer exportación y borrado de datos del usuario, aunque no haya obligación legal
  local — genera confianza, que es justamente el activo más frágil de este tipo de
  producto.

**Deuda técnica a resolver antes de escalar**: timestamps inconsistentes entre
migraciones tempranas (V1-V3, `created_at` sin default) y las posteriores (`DEFAULT now()`
+ `NOT NULL`); falta `plan_id`/`stripe_customer_id` en `users` antes de billing; sin
soft-delete (`deleted_at`) en `users`/`subscriptions` para auditoría de facturación.

---

## Mejoras técnicas pendientes — backend y frontend

### Backend

Fuente: auditoría de arquitectura backend (Spring Boot 4.0.7, 201 archivos Java, ~35
clases de test).

**Estado actual**: scoping por usuario consistente vía `SecurityUtils.getCurrentUserId()`
en 16 servicios/controllers. Excepción a vigilar: `debt_payments` no tiene `user_id`
propio — depende de que `DebtPaymentService` valide la propiedad del `Debt` padre antes de
tocar el repositorio. Es un patrón intencional pero frágil ante un endpoint nuevo que lo
saltee.

| Mejora | Prioridad | Esfuerzo |
|---|---|---|
| Agregar `user_id` desnormalizado (o test de contrato) en `debt_payments` para blindar contra un futuro IDOR | Alta | Chico |
| Row-Level Security de Postgres como defensa en profundidad (el aislamiento hoy depende 100% de que cada query en el código filtre por usuario) | Alta | Mediano |
| Rate limiting en `/api/users/login`, `/api/users/register` y `/api/ai/chat` (hoy sin protección de fuerza bruta ni de abuso a proveedores de IA de pago) | Alta | Mediano |
| `@Scheduled` in-process (`PaymentReminderJob`, `WeeklySummaryJob`) duplica envíos si en algún momento hay 2+ instancias del backend corriendo | Alta | Mediano |
| Configurar HikariCP (`maximum-pool-size`, `connection-timeout`) — hoy usa defaults de Spring Boot | Media | Chico |
| Caching corto (Caffeine/Redis) para reportes y análisis mensual, que hoy recalculan agregados en cada request | Media | Mediano |
| `AsyncConfig` solo cubre el envío de mails; cualquier otro trabajo pesado (categorización IA, insights) corre en el hilo de request | Media | Chico |
| Testcontainers/Postgres real en CI (hoy los tests son unit/MockMvc puros) | Media | Mediano |
| Logging estructurado (JSON) + correlación de request-id, necesario para depurar un SaaS multi-tenant en producción | Media | Mediano |
| Análisis estático (Checkstyle/SpotBugs) y `dependency-check` OWASP sobre Maven en CI (hoy solo hay Trivy sobre la imagen Docker) | Baja | Chico |

### Frontend

Fuente: auditoría de arquitectura frontend (Next.js 16, hooks caseros con Map-cache +
listeners).

**Estado actual**: el patrón de cache casero (`Map` + `Set<() => void>` +
`invalidateXCache()`) se repite copiado en cada hook (`use-incomes.ts`, `use-ai.ts`, etc.,
~140 líneas de boilerplate cada uno). Funciona hoy, pero cada dominio nuevo (facturación,
automatizaciones, integraciones) implica copiar ese boilerplate de nuevo, y la
invalidación es total (`.clear()` de todo el Map) en vez de quirúrgica por clave.

| Mejora | Prioridad | Esfuerzo |
|---|---|---|
| Migrar a TanStack Query (react-query) — no por rendimiento actual, sino porque cada módulo nuevo de la automatización va a heredar el mismo boilerplate si no se corta ahora. Puede convivir incrementalmente con los hooks legacy | Alta | Grande |
| Tests de componentes — hoy `package.json` no tiene `@testing-library/react` ni Playwright/Cypress; formularios críticos como `IncomeModal`/`ExpenseModal` no tienen cobertura | Alta | Mediano |
| Inputs custom en `asistente-ia/page.tsx` sin usar los primitivos de `components/ui/input.tsx`/`button.tsx` (inconsistencia de sistema de diseño y de accesibilidad) | Media | Chico |
| Botones "Google"/"GitHub" en login son decorativos (sin `onClick`) — o se implementan o se quitan antes de un SaaS real | Media | Chico |
| El `useEffect` + `setTimeout(0)` repetido en cada hook para evitar fetch en el render es un anti-patrón que react-query resuelve de raíz | Baja | (incluido en la migración de arriba) |

---

## SaaS multi-tenant y modelo de negocio

Fuente: investigación de mercado y estrategia (competidores activos en 2026).

### Panorama competitivo

- **YNAB**, **Monarch Money**, **Copilot Money** (solo iOS/Mac, categorización con IA
  "genuinamente impresiva" según reviews), **Mint** (discontinuado por Intuit en 2024,
  migración a Credit Karma perdió funciones — confirma que hay usuarios huérfanos buscando
  alternativa), **Fintonic** (España, modelo cuestionado por monetizar recomendando
  seguros/préstamos con los datos del usuario), **PocketGuard**.
- **Ya existen productos que leen SMS/notificaciones con IA**: FinArt, Moneyview,
  PennyWise AI (esta última con IA on-device, 100% privada — resuelve el problema de
  confianza de raíz). Este patrón es común en mercados donde predomina el SMS bancario
  (India, Latam), menos donde domina la agregación bancaria tipo Plaid (EE.UU./Europa).

### ¿Es un diferencial real?

Honestamente: **no es un diferencial único**, pero sí de posicionamiento. El ángulo
defendible de KoroFin no es "somos los únicos que leen correos" — es la combinación de:
multiplataforma + motor de análisis financiero propio + IA multi-proveedor con fallback
(reduce el riesgo de costo/disponibilidad propio) + foco en mercados donde la agregación
bancaria tipo Plaid es débil o cara (Latam).

### Modelo de negocio recomendado

- **Freemium**: plan gratis con carga manual ilimitada + cuota baja de IA (ej. 20
  categorizaciones/mes); plan pago con automatización completa vía correo/notificaciones +
  cuota alta de IA con overage medido.
- **Metering por tokens reales** consumidos por usuario y por proveedor (no por
  "features") — crítico porque el fallback entre NVIDIA/OpenRouter/etc puede caer en el
  proveedor más caro sin aviso, y ahí se pierde margen si no se mide.
- Stripe Billing + Metronome (Stripe adquirió Metronome específicamente para metered
  billing de IA) es la integración más directa para suscripciones + overage.
- Soft-cap que degrada a plan free al agotar cuota, no corte abrupto del servicio.

### Riesgos de negocio

- **Regulatorio**: leer correos es dato personal sensible bajo GDPR — requiere DPIA antes
  de lanzar en Europa, consentimiento explícito como base legal (no "interés legítimo").
- **Confianza**: un error de categorización automática en dinero real (duplicar un gasto,
  marcar un ingreso como gasto) es mucho más grave que un error de chatbot — el flujo de
  revisión de la sección de correo/notificaciones no es negociable.
- **Costo**: el fallback entre proveedores de IA puede disparar el costo por usuario sin
  aviso si no se monitorea con el metering de arriba.

---

## App móvil

Fuente: investigación de framework y viabilidad de plataforma.

### Framework: React Native

Gana sobre Flutter y nativo puro para este caso puntual: el equipo ya conoce
React/TypeScript (reutilización directa de conocimiento), y tanto
`NotificationListenerService` como el acceso a SMS requieren un módulo nativo en Kotlin de
todas formas en cualquier framework — ahí Flutter no tiene ventaja. RN además tiene mejor
velocidad de soporte para APIs nuevas de Android/iOS con equipos chicos.

### Fases sugeridas

| Fase | Alcance | Esfuerzo |
|------|---------|----------|
| v1 | Paridad con la web: dashboard, carga manual, push del backend | Mediano |
| v2 | Android only: `NotificationListenerService` + módulo nativo Kotlin, matching con IA | Grande |
| v3 | Widgets, biometría, modo offline | Chico-Mediano |

### Por qué al final del roadmap

La app móvil es la fase más cara en esfuerzo relativo (nuevo stack, módulo nativo,
políticas de tienda), y su valor diferencial (v2) depende de que el motor de
categorización por IA ya esté maduro y probado con datos reales de correo. Empezar por la
automatización de correo primero permite validar todo el pipeline de IA (extracción,
confianza, bandeja de revisión) sin la complejidad adicional de permisos nativos ni
fricción de Play Store — y recién ahí construir sobre una base ya probada.

### Implicancia de producto para iOS

Lanzar la auto-captura por notificaciones **solo en Android**; en iOS ofrecer carga manual
+ la integración de correo (que funciona igual en cualquier plataforma). Comunicarlo como
"auto-categorización inteligente en Android", sin prometer paridad en iOS para esa función
puntual — es una limitación de plataforma real, no un compromiso técnico evitable.

---

## Orden recomendado de todo el roadmap

Fusiona el orden de 6 pasos de la investigación SaaS con los 3 niveles de cuentas reales
en una sola secuencia priorizada:

1. **`docs/sprints/sprint1.md`, bajo esfuerzo y alto impacto — HECHO (2026-07-16,
   `feature/sprint-1-debt-charges-quick-add-ai-usage`, pendiente de merge a `develop`):**
   1. ✅ Fase A del rediseño de deudas (`DebtCharge`) — resuelve el dolor real de hoy.
   2. ✅ Quick-add + parser de texto conectado a `categorize()`/`useCategorize()`, y cerrada
      la brecha de que `income-modal.tsx` no tenía la sugerencia de categoría que
      `expense-modal.tsx` ya tenía.
   3. ✅ Tracking de uso de IA (`ai_usage_events`) y rate limiting — necesarios antes de
      abrir el producto a más usuarios.
2. **Corto plazo, en paralelo (ítems operativos ya en curso, no bloquean lo anterior):**
   dominio de GitHub Students → activación de Resend (DKIM/SPF sobre `korofin.jhonqui.dev`,
   `MAIL_FROM`) → subir los 17 secrets a GitHub Actions → PR `develop` → `main` → primer
   flujo real de n8n (bot de Telegram para registrar gastos, sobre la infraestructura
   Docker ya levantada hoy).
3. **Mediano plazo:**
   - Nivel 2: importar extractos bancarios (CSV/Excel) — datos reales sin terceros ni
     regulación, complementa el Nivel 1.
   - Integración de correo (Gmail API + Pub/Sub) con bandeja de revisión — el corazón de
     la visión de automatización por IA, no depende de nada más.
4. **Mediano-largo plazo:**
   - Nivel 3: Open Finance (Belvo/Prometeo) en sandbox, sin prometer a usuarios todavía.
   - Planes/suscripciones + Stripe/Metronome, una vez que el consumo de IA esté medido y
     la automatización de correo esté validada con usuarios reales.
5. **Largo plazo, en paralelo (no bloquean nada de lo anterior):** Row-Level Security,
   migración de cache a react-query, tests de componentes, y el resto del backlog técnico
   de las tablas de mejoras pendientes.
6. **Al final:**
   - Fase B del dominio de tarjetas (`CreditCard` + ledger + ciclos) — diseño con SDD.
   - App móvil React Native, empezando por paridad (v1) y recién en v2 la automatización
     por notificaciones en Android.

---

## Estado actual del proyecto

### Hecho y mergeado

| Qué | Dónde |
|-----|-------|
| MVP completo: 6 sprints (`docs/finsmart_mvp_sprints.md`, tablero y `docs/sprints/` de la fase MVP eliminados del repo hoy — el detalle queda en el historial de git) | `main` (producción) |
| Refactor por dominios: backend en 9 dominios (`common`, `usuario`, `ingresos`, `gastos`, `deudas`, `servicios`, `analisis`, `ia`, `reportes`) + frontend pulido | `main` |
| Extracción de componentes de `asistente-ia` (310→137 líneas) y `reportes` (269→119) | `main` |
| Optimización de queries del análisis: summary ~19→9 consultas, recommendations ~19→8, log SQL sin duplicar (verificado en runtime) | `develop` |
| Convenciones del repo en español (`docs/convenciones.md` + `CLAUDE.md`) | `main` |
| Emails Brevo: diagnóstico completo (IP autorizada → remitente verificado → cuenta requiere activación manual; Brevo pidió dominio propio); Brevo terminó bloqueando la cuenta y nunca la activó. **Decisión (2026-07-16): abandonar Brevo, migrar a Resend.** Dominio `korofin.jhonqui.dev` agregado en Resend, registros DNS (DKIM + SPF) cargados en Name.com, verificación en curso. | Configuración |
| Decisión: mantener módulo de IA custom, no migrar a Spring AI (revisar si llega streaming/tools/RAG) | Documentada |
| Decisión: n8n solo para canales e integraciones (patrón backend→webhook→n8n); la lógica de negocio queda en el backend | Documentada |
| **n8n integrado en Docker local (hoy, 2026-07-14)**: servicio `n8n-db` (Postgres dedicado, separado del `db` del backend, sin puerto publicado al host — solo accesible dentro de la red de `docker-compose`) para persistencia propia de n8n. n8n corriendo en `http://localhost:5678`. | `chore/inicio-fase-saas` |

### Decisiones tomadas hoy (2026-07-14)

- **No implementar la Fase A del rediseño de deudas (`DebtCharge`) en esta misma rama** —
  queda como la primera tarea de `docs/sprints/sprint1.md`, para no mezclar código de
  feature con trabajo de infraestructura/documentación en la misma PR.
- Rama de trabajo activa: `chore/inicio-fase-saas`.

### Pendiente (en orden)

1. **Dominio `korofin.jhonqui.dev` en Resend** (registros DNS cargados, verificación en
   curso) → confirmar estado `verified` → cambiar `MAIL_FROM` a una dirección del dominio
   → prueba final de entrega → eliminar el test manual `EmailSmokeManualTest.java` (está
   sin trackear, a propósito).
2. **Subir secrets a GitHub Actions** (17 en total, valores nuevos del `.env`; el deploy a
   Cloud Run los necesita — reemplazar `BREVO_SMTP_LOGIN`/`BREVO_SMTP_KEY` por
   `RESEND_API_KEY`).
3. **PR `develop` → `main`** (en pausa hasta completar 1 y 2) + limpieza de ramas.
4. **Primer flujo real de n8n** (bot de Telegram para registrar gastos) — la
   infraestructura Docker local ya está lista, falta construir el flujo.
5. ✅ **`docs/sprints/sprint1.md`** — HECHO (2026-07-16): Fase A del rediseño de deudas,
   quick-add conectado a la IA existente, y tracking de uso de IA/rate limiting. Falta
   mergear `feature/sprint-1-debt-charges-quick-add-ai-usage` a `develop`.
6. **Nivel 1 completo de este documento**: dominio de cuentas/tarjetas — Fase B (tarjeta
   rotativa con SDD), una vez validada la Fase A (ya validada por el usuario en sprint1).

### Backlog técnico (sin urgencia)

- Cache de datos en el frontend (SWR/react-query) — el mayor impacto de perf restante: hoy
  cada cambio de módulo re-fetchea todo.
- `JwtAuthenticationFilter` hace `existsById` (1 consulta) en cada request — cachear o
  confiar en el token para operaciones no sensibles.
- Mover el upsert de `financial_analysis` fuera del `GET /api/analysis/summary`.
- `EmailNotificationSender` se traga las excepciones (`log.warn`) — endurecer cuando las
  notificaciones sean críticas.
- Warning de serialización de `PageImpl` en los logs — migrar a `PagedModel`
  (`@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)`).

---

## Fuentes citadas

- [Configure push notifications in Gmail API](https://developers.google.com/workspace/gmail/api/guides/push)
- [Method: users.watch | Gmail](https://developers.google.com/workspace/gmail/api/reference/rest/v1/users/watch)
- [Use of SMS or Call Log permission groups - Play Console Help](https://support.google.com/googleplay/android-developer/answer/10208820?hl=en)
- [Permissions used only in default handlers | Android Developers](https://developer.android.com/guide/topics/permissions/default-handlers)
- [NotificationListenerService | Android Developers](https://developer.android.com/reference/android/service/notification/NotificationListenerService)
- [Notification access and notification listener policy | AOSP](https://source.android.com/docs/automotive/hmi/notifications/notification-access)
- [Security of runtime process in iOS - Apple Support](https://support.apple.com/guide/security/sandboxing-sec15bfe098e/web)
- [Apple introduces privacy rules for third-party access to notifications - 9to5Mac](https://9to5mac.com/2026/03/30/apple-introduces-privacy-rules-for-third-party-access-to-notifications-and-live-activities/)
- [iOS 26.3 Brings AirPods-Like Pairing to Third-Party Devices in EU Under DMA - MacRumors](https://www.macrumors.com/2025/12/22/ios-26-3-dma-airpods-pairing/)
- [React Native vs Flutter 2026: Benchmarks & Performance Guide](https://adevs.com/blog/react-native-vs-flutter/)
- [Era vs. Monarch vs. Copilot vs. YNAB: 2026 comparison](https://era.app/articles/era-vs-monarch-vs-copilot-vs-ynab/)
- [FinArt: Automatic Expense Tracker](https://finart.app/)
- [SMS Expense Tracking Apps 2026 - Finny Blog](https://getfinny.app/blog/sms-expense-tracking-app)
- [PennyWise AI - F-Droid](https://f-droid.org/en/packages/com.pennywiseai.tracker/)
- [Las 8 mejores alternativas a Fintonic en 2026](https://banktrack.com/blog/alternativas-fintonic)
- [What Happened to Mint? - WalletHub](https://wallethub.com/edu/b/what-happened-to-mint/151868)
- [AI SaaS Pricing Models in 2026 - Fungies.io](https://fungies.io/ai-saas-pricing-models-2026/)
- [Usage-based billing software for AI - Stripe/Metronome](https://stripe.com/billing/usage-based-billing)
- [GDPR Compliance for Email Tracking Tools](https://www.warmforge.ai/blog/gdpr-compliance-for-email-tracking-tools)

## Anexo — dependencias del frontend actualizadas en la rama del cierre del MVP

Se sincronizaron y actualizaron las dependencias sin saltos de versión mayor (patch/minor
únicamente): paquetes `@radix-ui/*`, `next` (16.2.0 → 16.2.10), `react`/`react-dom`
(19.2.4 → 19.2.7), `axios`, `date-fns`, `react-hook-form`, `tailwindcss`, `postcss`,
`autoprefixer`, `@types/react`, `eslint-config-next`, `tw-animate-css`, `tailwind-merge`.
Verificado: lint limpio, build de producción exitoso, 91/91 tests en verde.

**Actualizaciones mayores pendientes, requieren una rama y pruebas dedicadas por ser
cambios con breaking changes reales**: `zod` (3→4), `recharts` (2→3), `typescript` (5→6),
`vitest` (2→4), `eslint` (9→10), `sonner` (1→2), `lucide-react` (0.x→1.x),
`react-day-picker` (9→10), `@hookform/resolvers` (3→5), `react-resizable-panels` (2→4),
`@vercel/analytics` (1→2), `@types/node` (22→26). No se tocaron en esta sesión para no
arriesgar el build sin una migración dedicada a cada una.
