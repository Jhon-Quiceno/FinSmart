# Roadmap Estratégico — FinSmart hacia SaaS con Automatización de IA

> Investigación realizada con 6 agentes especializados (arquitectura backend, arquitectura frontend/UX, base de datos, automatización/IA, mercado y modelo de negocio, y app móvil) sobre el estado real del código tras el cierre del Sprint 6. Fecha: 2026-07-05.

## Resumen ejecutivo

FinSmart es hoy un MVP funcional y bien probado (286 tests backend, 91 tests frontend, arquitectura por capas consistente, scoping por usuario correcto). El salto a SaaS multi-tenant con automatización de IA no requiere reescribir nada — requiere **sumar capas nuevas** (uso/cuotas, ingestión de mensajes, billing) sin romper lo que ya funciona, y tomar decisiones honestas sobre qué es viable en web vs qué exige una app móvil.

La conclusión más importante de esta investigación, y la que más debería moldear el orden del roadmap: **la lectura de correo es viable hoy mismo desde el backend web actual (Gmail API + Pub/Sub); la lectura de notificaciones de otras apps (bancos, billeteras) solo es posible en Android vía `NotificationListenerService`, y en iOS es estructuralmente imposible por diseño de Apple; y leer SMS directamente no es viable en ninguna plataforma para publicar en las tiendas de apps.** Esto define el orden natural: correo primero (funciona ya, en cualquier plataforma), automatización móvil Android después (cuando exista la app), SMS descartado como fuente.

---

## 1. Mejoras a futuro — Backend

Fuente: auditoría de arquitectura backend (Spring Boot 4.0.7, 201 archivos Java, ~35 clases de test).

**Estado actual**: scoping por usuario consistente vía `SecurityUtils.getCurrentUserId()` en 16 servicios/controllers. Excepción a vigilar: `debt_payments` no tiene `user_id` propio — depende de que `DebtPaymentService` valide la propiedad del `Debt` padre antes de tocar el repositorio. Es un patrón intencional pero frágil ante un endpoint nuevo que lo saltee.

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

## 2. Mejoras a futuro — Frontend

Fuente: auditoría de arquitectura frontend (Next.js 16, hooks caseros con Map-cache + listeners).

**Estado actual**: el patrón de cache casero (`Map` + `Set<() => void>` + `invalidateXCache()`) se repite copiado en cada hook (`use-incomes.ts`, `use-ai.ts`, etc., ~140 líneas de boilerplate cada uno). Funciona hoy, pero cada dominio nuevo (facturación, automatizaciones, integraciones) implica copiar ese boilerplate de nuevo, y la invalidación es total (`.clear()` de todo el Map) en vez de quirúrgica por clave.

| Mejora | Prioridad | Esfuerzo |
|---|---|---|
| Migrar a TanStack Query (react-query) — no por rendimiento actual, sino porque cada módulo nuevo de la automatización va a heredar el mismo boilerplate si no se corta ahora. Puede convivir incrementalmente con los hooks legacy | Alta | Grande |
| Tests de componentes — hoy `package.json` no tiene `@testing-library/react` ni Playwright/Cypress; formularios críticos como `IncomeModal`/`ExpenseModal` no tienen cobertura | Alta | Mediano |
| Inputs custom en `asistente-ia/page.tsx` sin usar los primitivos de `components/ui/input.tsx`/`button.tsx` (inconsistencia de sistema de diseño y de accesibilidad) | Media | Chico |
| Botones "Google"/"GitHub" en login son decorativos (sin `onClick`) — o se implementan o se quitan antes de un SaaS real | Media | Chico |
| El `useEffect` + `setTimeout(0)` repetido en cada hook para evitar fetch en el render es un anti-patrón que react-query resuelve de raíz | Baja | (incluido en la migración de arriba) |

---

## 3. Implementaciones futuras — nuevos módulos

Fuente: auditoría de base de datos + arquitectura backend/frontend, cruzadas.

### Backend — módulos nuevos necesarios

- **Tracking de uso/cuotas de IA**: no existe hoy ninguna tabla que registre tokens/costo por usuario. Es la base de todo el futuro modelo de cobro (ver sección 6).
- **Ingestión de correo/SMS/notificaciones**: módulo de ingesta + cola async + extracción con LLM (ver sección 5, es el corazón de la visión del producto).
- **Motor de reglas de automatización**: para que el usuario pueda afinar/override la clasificación automática de la IA (ej. "todo lo que venga de tal remitente, categorizarlo como tal cosa").
- **Credenciales de integraciones de terceros**: OAuth de Gmail, webhooks bancarios — deben vivir separadas de la config de proveedores de IA del operador, y **cifradas at-rest**, nunca en texto plano.

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

**Multi-tenancy**: para un B2C de finanzas personales (1 usuario = 1 tenant, sin cuentas compartidas todavía), `user_id` como aislamiento alcanza conceptualmente. No hace falta un `tenant_id` desacoplado ni particionamiento a esta escala (miles de usuarios, no millones de filas). Row-Level Security es la mejora pragmática recomendada — refuerza el filtro de la aplicación, no lo reemplaza. Si a futuro se suman cuentas compartidas (pareja, hogar, equipos), ahí sí introducir `accounts`/`account_members`, pero no antes de que exista ese requisito real.

**Privacidad y retención**: el contenido de correos/SMS es dato financiero sensible.
- Cifrar `encrypted_content` en reposo (AES-GCM con clave gestionada, no una constante en el código).
- **Truncar/purgar el raw payload después de procesarlo con éxito** — guardar solo los campos extraídos + referencia al movimiento generado, no el correo completo indefinidamente.
- Retención corta (30-90 días) para lo no procesado.
- Exponer exportación y borrado de datos del usuario, aunque no haya obligación legal local — genera confianza, que es justamente el activo más frágil de este tipo de producto.

**Deuda técnica a resolver antes de escalar**: timestamps inconsistentes entre migraciones tempranas (V1-V3, `created_at` sin default) y las posteriores (`DEFAULT now()` + `NOT NULL`); falta `plan_id`/`stripe_customer_id` en `users` antes de billing; sin soft-delete (`deleted_at`) en `users`/`subscriptions` para auditoría de facturación.

---

## 4. Automatizaciones para cargar ingresos/gastos más rápido (ya, sin esperar a la IA de correo)

Esto es lo que se puede construir **ahora mismo**, con el stack actual, sin depender de ninguna integración externa:

| Mejora | Prioridad | Esfuerzo |
|---|---|---|
| Quick-add flotante (FAB) global, accesible desde cualquier página, con atajo de teclado tipo `Ctrl+K` | Alta | Mediano |
| Modal ultra-mínimo (solo monto + descripción, resto colapsado/opcional) con defaults inteligentes: última categoría usada, fecha = hoy, método de pago más frecuente | Alta | Chico |
| Parser de texto libre tipo "Uber 15000" → autocompleta monto + descripción + categoría. **El backend ya expone esto**: `categorize()` en `lib/services/ai.service.ts` y `useCategorize()` en `hooks/use-ai.ts` existen pero no están conectados a los formularios reales de creación — es la mejora de menor esfuerzo con mayor impacto inmediato | Alta | Mediano |
| Plantillas de gastos frecuentes (chips tipo "Uber", "Supermercado" con monto/categoría precargados) | Media | Chico |
| Entrada por voz | Baja | Grande (bajo ROI hasta validar el parser de texto) |

**Nota importante**: ya existe `components/ui/command.tsx` (shadcn `cmdk`) importado pero sin usar en ninguna página — es el punto de partida natural para el command palette del quick-add.

---

## 5. La automatización con IA de correo/SMS/notificaciones — arquitectura y honestidad de plataforma

Esta es la pieza central de la visión de largo plazo. La investigación deja hechos de plataforma muy concretos que deberían moldear las expectativas del producto:

### Correo electrónico — viable HOY, 100% desde el backend web actual

- Flujo real: **Gmail API + Google Cloud Pub/Sub** (no polling). El usuario conecta su cuenta una vez vía OAuth2 (scope mínimo recomendado: `gmail.readonly`), el backend registra un `watch()` sobre la casilla, y Gmail empuja un webhook a un topic de Pub/Sub cada vez que llega un correo nuevo. El `watch` se renueva cada ~7 días (recomendado: cron diario).
- Para proveedores no-Gmail: IMAP genérico con IDLE (casi tiempo real) o polling si el proveedor no soporta IDLE.
- Esta es la parte más lista para producir de toda la visión — no depende de ninguna app móvil.

### SMS — prácticamente inviable en cualquier plataforma para un SaaS genérico

- **Android**: Google Play restringe `READ_SMS` a apps que sean el **manejador por defecto de SMS, Teléfono o Asistente**. Una app de finanzas personales no calificaría — Google la rechazaría en revisión de la tienda. Confirmado también desde el ángulo de la app móvil: la política prohíbe declarar el permiso en el manifest sin ese rol.
- **iOS**: no existe ninguna API pública para que terceros lean SMS/iMessage de otras apps. Sandboxing absoluto, sin excepción real para este caso.
- **Conclusión**: descartar SMS como fuente de ingesta. No es una limitación de FinSmart, es una limitación de las plataformas.

### Notificaciones push de otras apps (bancos, billeteras) — solo Android, y solo con app móvil

- **Android**: `NotificationListenerService` sigue vigente en 2026, es una API pública documentada, no exige ser manejador por defecto de nada — el usuario la activa manualmente por app en Ajustes → Acceso a notificaciones. Es la vía más prometedora para capturar gastos con tarjeta y notificaciones de servicios, **pero requiere una app nativa Android** (no funciona desde web).
- **iOS**: Apple no ofrece equivalente para terceros. Lo único nuevo en 2026 (iOS 26.3, exclusivo UE por el Digital Markets Act) es reenvío de notificaciones a **accesorios físicos emparejados** (relojes) — no aplica a apps de terceros.

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

La bandeja de revisión **no es opcional al principio** — es el mecanismo de control de calidad contra falsos positivos de categorización, que en dinero real son mucho más graves que en un chatbot cualquiera.

### Qué es viable hoy vs qué depende de la futura app móvil

| Fuente | Viable ya (web) | Requiere app móvil |
|---|---|---|
| Correo (Gmail API/Pub/Sub, IMAP) | ✅ | — |
| Notificaciones de apps bancarias/billeteras | — | ✅ (solo Android) |
| SMS | ❌ (inviable en ambas tiendas) | ❌ |

---

## 6. SaaS multi-tenant y modelo de negocio

Fuente: investigación de mercado y estrategia (competidores activos en 2026).

### Panorama competitivo

- **YNAB**, **Monarch Money**, **Copilot Money** (solo iOS/Mac, categorización con IA "genuinamente impresiva" según reviews), **Mint** (discontinuado por Intuit en 2024, migración a Credit Karma perdió funciones — confirma que hay usuarios huérfanos buscando alternativa), **Fintonic** (España, modelo cuestionado por monetizar recomendando seguros/préstamos con los datos del usuario), **PocketGuard**.
- **Ya existen productos que leen SMS/notificaciones con IA**: FinArt, Moneyview, PennyWise AI (esta última con IA on-device, 100% privada — resuelve el problema de confianza de raíz). Este patrón es común en mercados donde predomina el SMS bancario (India, Latam), menos donde domina la agregación bancaria tipo Plaid (EE.UU./Europa).

### ¿Es un diferencial real?

Honestamente: **no es un diferencial único**, pero sí de posicionamiento. El ángulo defendible de FinSmart no es "somos los únicos que leen correos" — es la combinación de: multiplataforma + motor de análisis financiero propio + IA multi-proveedor con fallback (reduce el riesgo de costo/disponibilidad propio) + foco en mercados donde la agregación bancaria tipo Plaid es débil o cara (Latam).

### Modelo de negocio recomendado

- **Freemium**: plan gratis con carga manual ilimitada + cuota baja de IA (ej. 20 categorizaciones/mes); plan pago con automatización completa vía correo/notificaciones + cuota alta de IA con overage medido.
- **Metering por tokens reales** consumidos por usuario y por proveedor (no por "features") — crítico porque el fallback entre NVIDIA/OpenRouter/etc puede caer en el proveedor más caro sin aviso, y ahí se pierde margen si no se mide.
- Stripe Billing + Metronome (Stripe adquirió Metronome específicamente para metered billing de IA) es la integración más directa para suscripciones + overage.
- Soft-cap que degrada a plan free al agotar cuota, no corte abrupto del servicio.

### Riesgos de negocio

- **Regulatorio**: leer correos es dato personal sensible bajo GDPR — requiere DPIA antes de lanzar en Europa, consentimiento explícito como base legal (no "interés legítimo").
- **Confianza**: un error de categorización automática en dinero real (duplicar un gasto, marcar un ingreso como gasto) es mucho más grave que un error de chatbot — el flujo de revisión de la sección 5 no es negociable.
- **Costo**: el fallback entre proveedores de IA puede disparar el costo por usuario sin aviso si no se monitorea con el metering de arriba.

---

## 7. App móvil — la última fase, y por qué en ese orden

Fuente: investigación de framework y viabilidad de plataforma.

### Framework: React Native

Gana sobre Flutter y nativo puro para este caso puntual: el equipo ya conoce React/TypeScript (reutilización directa de conocimiento), y tanto `NotificationListenerService` como el acceso a SMS requieren un módulo nativo en Kotlin de todas formas en cualquier framework — ahí Flutter no tiene ventaja. RN además tiene mejor velocidad de soporte para APIs nuevas de Android/iOS con equipos chicos.

### Fases sugeridas

| Fase | Alcance | Esfuerzo |
|---|---|---|
| v1 | Paridad con la web: dashboard, carga manual, push del backend | Mediano |
| v2 | Android only: `NotificationListenerService` + módulo nativo Kotlin, matching con IA | Grande |
| v3 | Widgets, biometría, modo offline | Chico-Mediano |

### Por qué al final del roadmap

La app móvil es la fase más cara en esfuerzo relativo (nuevo stack, módulo nativo, políticas de tienda), y su valor diferencial (v2) depende de que el motor de categorización por IA ya esté maduro y probado con datos reales de correo. Empezar por la automatización de correo primero permite validar todo el pipeline de IA (extracción, confianza, bandeja de revisión) sin la complejidad adicional de permisos nativos ni fricción de Play Store — y recién ahí construir sobre una base ya probada.

### Implicancia de producto para iOS

Lanzar la auto-captura por notificaciones **solo en Android**; en iOS ofrecer carga manual + la integración de correo (que funciona igual en cualquier plataforma). Comunicarlo como "auto-categorización inteligente en Android", sin prometer paridad en iOS para esa función puntual — es una limitación de plataforma real, no un compromiso técnico evitable.

---

## 8. Orden recomendado de todo el roadmap

1. **Ahora, bajo esfuerzo, alto impacto**: quick-add + parser de texto conectado a `categorize()` (sección 4) — ya hay infraestructura de backend sin usar.
2. **Corto plazo**: tracking de uso de IA (`ai_usage_events`) y rate limiting — necesarios antes de abrir el producto a más usuarios, independientemente del resto.
3. **Mediano plazo**: integración de correo (Gmail API + Pub/Sub) con bandeja de revisión — es el corazón de la visión y no depende de nada más.
4. **Mediano-largo plazo**: planes/suscripciones + Stripe/Metronome, una vez que el consumo de IA esté medido y la automatización de correo esté validada con usuarios reales.
5. **Largo plazo**: Row-Level Security, migración de cache a react-query, tests de componentes — mejoras de fondo que conviene ir haciendo en paralelo a medida que el producto crece, no bloquean nada de lo anterior.
6. **Al final**: app móvil React Native, empezando por paridad (v1) y recién en v2 la automatización por notificaciones en Android.

---

## Fuentes citadas por los agentes de investigación

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

## Anexo — dependencias del frontend actualizadas en esta misma rama

Se sincronizaron y actualizaron las dependencias sin saltos de versión mayor (patch/minor únicamente): paquetes `@radix-ui/*`, `next` (16.2.0 → 16.2.10), `react`/`react-dom` (19.2.4 → 19.2.7), `axios`, `date-fns`, `react-hook-form`, `tailwindcss`, `postcss`, `autoprefixer`, `@types/react`, `eslint-config-next`, `tw-animate-css`, `tailwind-merge`. Verificado: lint limpio, build de producción exitoso, 91/91 tests en verde.

**Actualizaciones mayores pendientes, requieren una rama y pruebas dedicadas por ser cambios con breaking changes reales**: `zod` (3→4), `recharts` (2→3), `typescript` (5→6), `vitest` (2→4), `eslint` (9→10), `sonner` (1→2), `lucide-react` (0.x→1.x), `react-day-picker` (9→10), `@hookform/resolvers` (3→5), `react-resizable-panels` (2→4), `@vercel/analytics` (1→2), `@types/node` (22→26). No se tocaron en esta sesión para no arriesgar el build sin una migración dedicada a cada una.
