# Plan de app móvil — React Native (KoroFin)

> Plan de fases para construir la versión móvil de KoroFin sobre el backend existente,
> reutilizando lo que ya sirve y siendo honesto sobre lo que no aplica o no hace falta en
> móvil. Complementa (no reemplaza) la sección "App móvil" de
> [`roadmap-saas-cuentas-reales.md`](roadmap-saas-cuentas-reales.md) — ese documento define
> el *orden* dentro del roadmap general (la app móvil va al final, después de que la
> automatización de correo del Sprint 3 esté madura); este documento define el *cómo*.

---

## 0. Decisión de repositorio

**Decisión: mismo repo, carpeta nueva `smart-finance-mobile/` junto a `smart-finance-backend/`
y `smart-finance-frontend/`.** No un repo separado.

Justificación (evidencia, no preferencia):

- El backend ya es una API REST con auth **stateless por JWT Bearer** (`JwtService`,
  `JwtAuthenticationFilter`) pensada para ser consumida por cualquier cliente HTTP — no hay
  nada en el backend que ate la API a que el consumidor sea un navegador, salvo el mecanismo
  de refresh (ver §2, se ajusta con un endpoint nuevo, no con un rediseño).
- Lo que hoy se llama "monorepo" **no tiene tooling de monorepo real**: no hay
  `package.json`/`pnpm-workspace.yaml` en la raíz que conecte `smart-finance-backend/` y
  `smart-finance-frontend/`; son dos proyectos independientes que comparten `.git` y `docs/`.
  Agregar `smart-finance-mobile/` como tercera carpeta hermana no rompe nada porque no hay
  integración de build que romper.
- `docs/DESIGN.md` ya fue escrito pensando en portarse a móvil (tokens de color, motion,
  patrón responsive) — tenerlo en el mismo repo evita que ese documento se desactualice
  respecto a lo que la app móvil realmente usa.
- `lib/types/*.ts` y `lib/schemas/*.ts` del frontend son TypeScript puro (sin DOM, sin
  Next.js) — 100% portables a React Native tal cual. Si el contrato de la API cambia (nuevo
  campo, nuevo endpoint), un mismo PR puede tocar backend + tipos compartidos + los dos
  consumidores, evitando el drift de "el mobile no se enteró que el campo cambió de nombre"
  que sí es un riesgo real entre repos separados.
- El costo del monorepo (repo más pesado, CI que no debería dispararse cruzado) se resuelve
  con `paths:`/`paths-ignore:` en los workflows de GitHub Actions — no requiere separar
  repos. Si en algún momento el ritmo de release de la app justifica un repo propio (equipo
  más grande, CI de compilación nativa mucho más pesada que el resto), se puede extraer con
  `git filter-repo` conservando historia — separar después es barato; unir dos repos después
  no lo es.

**Qué NO implica esta decisión:** no significa configurar un workspace de build compartido
(Turborepo/Nx) desde el día uno. Alcanza con que `smart-finance-mobile/` sea una carpeta
Expo autocontenida, con su propio `package.json`. Si el dolor de repetir tipos/schemas se
vuelve real (Fase 1 en adelante), ahí se evalúa extraer un paquete `packages/shared/` — no
antes.

---

## 1. Stack recomendado

| Decisión | Elección | Por qué |
|---|---|---|
| Framework | **React Native vía Expo, SDK 54** (managed + Dev Client, no bare) | El roadmap ya definió React Native sobre Flutter (reutilización de conocimiento TypeScript/React del equipo — y para la Fase 2, ambos frameworks necesitan el mismo módulo nativo Kotlin, así que Flutter no tiene ventaja ahí). SDK fijado a 54, no al último, porque es el que soporta el Expo Go publicado en las stores (ver §5.1). Expo suma velocidad: OTA updates (`EAS Update`), `expo-notifications`, `expo-secure-store`, `expo-local-authentication` y `expo-camera` cubren gran parte de v1/v3 sin código nativo propio. |
| Módulo nativo Android (Fase 2) | **Expo Modules API** (config plugin + módulo Kotlin), compilado con **EAS Build** | Ya no hace falta "eyectar" de Expo para tener un módulo nativo (`NotificationListenerService`) — Expo Modules API permite escribir Kotlin/Swift y seguir usando EAS Build/Update para el resto de la app. |
| Estilos | **NativeWind** (Tailwind para RN) | Mismo mental model que el frontend web (`className`, tokens de `DESIGN.md`), reduce la curva de aprendizaje al portar componentes. |
| Data fetching / cache | **TanStack Query (react-query)** | Funciona igual en RN que en web; evita reimplementar a mano el patrón de cache casero (`Map` + `Set<() => void>`) que `roadmap-saas-cuentas-reales.md` ya marcó como deuda técnica a migrar en el frontend web (Sprint 5) — mejor no repetirlo en un stack nuevo. |
| Cliente HTTP | **axios** (mismo que el frontend web) | `lib/services/*.ts` se porta con cambios mínimos; solo cambia cómo se guarda el token (ver §2). |
| Almacenamiento seguro | **expo-secure-store** (Keychain en iOS / Keystore en Android) | Nunca `AsyncStorage` sin cifrar para tokens — es el equivalente móvil de no guardar el refresh token en `localStorage`. |
| Navegación | **Expo Router** (file-based, como el App Router de Next.js) | Mismo mental model de rutas que el equipo ya usa en `smart-finance-frontend/app/`. |
| Formularios | **React Hook Form + Zod** (mismos schemas de `lib/schemas/`) | Reutiliza `lib/schemas/*.ts` tal cual — son Zod puro, sin dependencia de DOM. |

---

## 2. Ajuste de backend necesario antes de la Fase 1

El backend **no necesita reescritura**, pero el mecanismo de refresh actual es
específicamente de navegador y no se traduce a RN sin ajuste:

- `UserController.login/register/refresh` devuelve el `accessToken` en el body (✅ portable
  tal cual) pero pone el `refreshToken` en una **cookie `HttpOnly`/`Secure`/`SameSite`**
  (`buildRefreshCookie`), y las mutaciones exigen un header `X-XSRF-TOKEN` obtenido de
  `GET /api/users/csrf` (protección CSRF, que solo tiene sentido contra un navegador con
  cookies ambientes — un cliente RN que solo manda `Authorization: Bearer` no está expuesto
  a CSRF).
- **Tarea de Fase 0:** agregar una variante de request para clientes no-browser (por
  ejemplo, un header `X-Client: mobile` en `POST /api/users/login|register|refresh`) que
  devuelva el `refreshToken` en el body de `AuthResponse` en vez de `Set-Cookie`, y quede
  exento del chequeo CSRF. La lógica de negocio (`UserService.login/register/refresh`,
  `RefreshTokenService`) no cambia — solo cambia cómo `UserController` transporta el
  refresh token según el tipo de cliente.
- En RN: `accessToken` en memoria (igual que hoy en web), `refreshToken` en
  `expo-secure-store` (nunca en `AsyncStorage` plano).

Sin este ajuste, técnicamente se podría usar un cookie jar en RN (`@react-native-cookies`),
pero es un anti-patrón: agrega complejidad para replicar un modelo de seguridad (CSRF)
pensado para un problema que un cliente nativo con Bearer token no tiene.

---

## 3. Qué se reutiliza del frontend web tal cual (sin reescribir lógica)

| Carpeta | Portabilidad | Nota |
|---|---|---|
| `lib/types/*.ts` | 100% — copiar o extraer a paquete compartido | TypeScript puro, sin DOM. |
| `lib/schemas/*.ts` (Zod) | 100% | Igual razón. |
| `lib/services/*.ts` | ~90% | Misma lógica de llamadas a la API; solo cambia el `apiClient` base (ver §2) y quitar lo específico de cookies/CSRF. |
| `lib/date.ts`, `lib/utils/period.ts` | 100% | Utilidades puras. |
| Reglas de negocio de UI (qué campos son obligatorios, defaults inteligentes del quick-add, sugerencia de categoría) | Conceptual, no el componente | El *concepto* se porta; el componente de React DOM no. |
| Tokens de diseño (`docs/DESIGN.md`) | Conceptual, con conversión | Colores OKLCH → HEX/RGB en build time (`culori`/`colorjs.io`); motion → Reanimated en vez de framer-motion; patrón "tabla → tarjetas" ya es literalmente el diseño recomendado para una lista en RN (`FlatList` de tarjetas). |

## 4. Qué NO se traduce (no aplica o no hace falta en móvil)

| Cosa de la web | Por qué no aplica en RN |
|---|---|
| Web Push (VAPID) — `docs/notifications-future.md` lo marca como "mejor candidato futuro" para web | Es específico de Service Worker de navegador. En móvil el canal equivalente es **push nativo** (`expo-notifications` sobre FCM/APNs) — reemplaza a Web Push, no lo complementa. Requiere un adaptador backend nuevo (`ExpoPushAdapter` implementando el puerto hermano `PushNotificationSender`, no `NotificationSender` directamente — `NotificationSender` está atado 1:1 a `EmailRecipient` e inyectado como dependencia única, no lista, en `NotificationDispatcher`, así que reusarlo tal cual dispararía un `NoUniqueBeanDefinitionException`). |
| PWA / Service Worker propio (`public/sw.js` si se implementa) | No aplica — RN no corre en un navegador. |
| SSR/SEO de Next.js, metadata, rutas públicas indexables | Irrelevante en una app de instalación directa (no hay crawler indexando una app nativa). |
| Cookie `HttpOnly` + CSRF (`XSRF-TOKEN`) | Ver §2 — se reemplaza por Bearer + refresh en `expo-secure-store`. |
| Botones "Google"/"GitHub" decorativos en login (ya marcados como deuda en el roadmap, Sprint 5) | Si se implementan de verdad en la web antes, se heredan vía el mismo backend OAuth; si no, no hace falta duplicarlos en la primera versión móvil. |
| Sidebar colapsable de escritorio, drawer de hamburguesa | Se reemplaza por navegación nativa (tabs inferiores + stack), no por el mismo componente adaptado. |
| Exportación CSV vía descarga de archivo del navegador (`lib/download.ts`) | En RN el equivalente es compartir el archivo vía `expo-sharing`/`expo-file-system`, no un `<a download>`. |

---

## 5.1 Entorno de desarrollo — VS Code + Expo Go

**Decisión:** previsualizar en un **celular físico con la app Expo Go**, no un emulador
Android en la PC. Cero instalación pesada (sin Android Studio/SDK) y hot reload instantáneo
al guardar. Único límite: Expo Go solo corre el runtime "managed" estándar — en cuanto la
Fase 2 agregue el módulo nativo Kotlin (`NotificationListenerService`), esa fase pasa a
requerir un **Dev Client** propio (`npx expo run:android` o un build de EAS), porque un
módulo nativo custom no puede cargarse dentro de la app genérica Expo Go descargada de la
store. Para Fase 0/1, Expo Go alcanza.

Configuración de VS Code (carpeta `smart-finance-mobile/.vscode/`):

- Extensiones recomendadas (`extensions.json`): ESLint, Prettier, Tailwind CSS IntelliSense
  (para NativeWind), y el paquete de React Native/Expo Tools para autocompletado y depurar
  desde el editor.
- `settings.json` del proyecto: format-on-save con Prettier, ESLint como fuente de verdad
  para reglas, y las clases de NativeWind reconocidas por el IntelliSense de Tailwind.
- `tasks.json`: tarea para levantar `npx expo start` desde VS Code sin salir del editor.
- Flujo diario: `npx expo start` en el terminal integrado → escanear el QR con la cámara
  (iOS) o la app Expo Go (Android) → guardar un archivo → Fast Refresh actualiza el celular
  al instante.
- **Gotcha real encontrado al scaffoldear (2026-08-19):** con el `node-linker` estricto por
  defecto de pnpm, Metro no resuelve `react-native-css-interop/jsx-runtime` (dependencia
  interna de NativeWind) — el bundle de Android falla con "Unable to resolve module". Fix:
  `smart-finance-mobile/pnpm-workspace.yaml` fija `nodeLinker: hoisted` (equivalente al
  `node_modules` plano de npm/yarn clásico), que es lo que la comunidad de Metro/RN
  recomienda para pnpm. Ya está resuelto en el scaffold — no hace falta re-investigarlo.
- **SDK fijado a 54, no al último (2026-08-19):** `create-expo-app` instala por defecto el
  SDK más nuevo (57 al momento de escribir esto), pero el Expo Go que se descarga de la Play
  Store/App Store va bastante atrás — publica solo el SDK que Apple/Google ya aprobaron en
  review, no el último. Con el proyecto en SDK 57, Expo Go tira "Project is incompatible with
  this version of Expo Go" al escanear el QR. Fix: `expo install expo@^54.0.0 --fix` (alinea
  `react`/`react-native`/todos los `expo-*` a las versiones exactas que ese SDK espera) y
  regenerar el layout/pantalla de bienvenida del template, que usa APIs de Expo Router más
  nuevas (Native Tabs con subcomponentes `.Label`/`.Icon`) que no existen en el Expo Router de
  SDK 54 — no hace falta portarlas, esa pantalla es tutorial de Expo, se reemplaza en la
  Fase 1 igual. Antes de cada scaffold nuevo, chequear qué SDK soporta el Expo Go publicado
  (`https://expo.dev/changelog/expo-go-and-app-store-<mes-actual>`) y pinear ese, no el más
  nuevo — así se evita este ciclo la próxima vez.
- **Gotcha de entorno Windows, no de RN/Expo (2026-08-19):** `pnpm install` falló de forma
  intermitente y repetida con `EPERM`/`ENOENT` en paquetes distintos cada vez (`expo`,
  `react-native`, `react-native-gesture-handler`, `@expo/fingerprint`, `expo-device`...).
  Se descartaron OneDrive (la carpeta del proyecto no está sincronizada — se verificó con el
  registro, no es una suposición) y corrupción del store de pnpm (`pnpm install --force` no
  lo arregló). La causa real fue **Windows Defender con protección en tiempo real**: agregar
  una exclusión sobre `smart-finance-mobile/node_modules`
  (`Add-MpPreference -ExclusionPath ...`, requiere PowerShell como administrador) resolvió el
  problema de raíz — el install pasó los 978 paquetes sin un solo error después. Es fricción
  de Windows con cualquier gestor de paquetes de Node (no específico de pnpm, RN ni Expo);
  vale la pena dejar la exclusión de Defender configurada de entrada en cualquier máquina
  Windows nueva que clone este repo, antes de correr el primer install.

---

## 5. Fases

### Fase 0 — Preparación (antes de escribir pantallas)

- [x] Backend: endpoint de auth mobile-friendly (§2) — refresh token en body, exento de CSRF.
- [x] Backend: adaptador `ExpoPushAdapter` sobre el puerto hermano `PushNotificationSender`
      (no `NotificationSender` directamente — ver §4), tabla `push_tokens` (`user_id`,
      `expo_push_token`, `device_id`, `created_at`).
- [ ] Scaffolding `smart-finance-mobile/` con Expo + TypeScript + Expo Router + NativeWind.
- [ ] Configurar VS Code (`.vscode/extensions.json` + `settings.json` + `tasks.json`, ver §5.1)
      y verificar que `npx expo start` levanta y conecta con Expo Go en un celular físico.
- [ ] Portar tokens de `DESIGN.md` a un theme de NativeWind (conversión OKLCH → HEX).
- [ ] Decidir extracción de `lib/types/`/`lib/schemas/` a paquete compartido o copia directa
      (empezar con copia directa es válido; no bloquear la Fase 1 por esto).
- [ ] CI: workflow de GitHub Actions con `paths: smart-finance-mobile/**` para no disparar
      builds del backend/frontend en cada cambio móvil (y viceversa).

### Fase 1 (v1) — Paridad esencial con la web

Objetivo: un usuario puede hacer en el celular lo mismo que hace hoy en la web, sin las
funciones que dependen de terceros aún no maduros (correo, Open Finance).

| Módulo | Alcance |
|---|---|
| Auth | Login, registro, logout, refresh transparente (interceptor axios equivalente al de `api-client.ts`), cambio de contraseña. |
| Dashboard | Balance general, gráficos (Victory Native o `react-native-gifted-charts` en vez de Recharts), alertas, transacciones recientes. |
| Ingresos / Gastos | CRUD completo, quick-add, sugerencia de categoría por IA (reutiliza `useCategorize()`/endpoint existente). |
| Deudas / Tarjetas | Listado, cupo/utilización, registro de cargos y pagos — dominio `tarjetas/`/`deudas/` ya completo en backend (Fase B del roadmap), consumo directo. |
| Servicios | Suscripciones y pagos recurrentes. |
| Reportes | Vista in-app de comparativas; exportar/compartir CSV vía `expo-sharing` en vez de descarga de navegador. |
| Asistente IA | Chat, igual que la web (mismo endpoint `/api/ai/chat`). |
| Vínculo con Telegram | Pantalla de configuración que genera el código de un solo uso (`TelegramLinkService.generateLinkCode`) — reutiliza el endpoint tal cual, es el mismo flujo que ya existe en la web. |
| Notificaciones push | `expo-notifications` + `ExpoPushAdapter` de Fase 0 — primera vez que KoroFin tiene push real (la web solo tiene in-app + email). |
| Configuración | Perfil, preferencias de notificación, estado de proveedores de IA (solo lectura). |

**Fuera de alcance de v1** (dependen de piezas que el roadmap general aún no cerró):
extractos bancarios (Nivel 2, pendiente de validar con datos reales), Open Finance (Nivel 3,
sandbox), billing/planes (Sprint 7).

**Progreso de diseño (UI mock, sin integración de backend real)** — pantallas navegables con
datos de ejemplo, construidas sobre `(tabs)` + rutas sueltas en Expo Router; cada submit/acción
real queda marcado con `TODO(Fase 0 backend)` en el código:

- [x] Auth — login (con acceso demo temporal a `(tabs)`) y registro; falta el endpoint mobile-friendly (§2) para que dejen de ser un stub.
- [x] Navegación — 4 tabs (Inicio, Movimientos, Deudas, Asistente) + burbuja de perfil en el header de cada una (menú: Ver perfil / Configuraciones / Notificaciones); botón "Agregar" como FAB flotante en vez de ícono fijo en el header. Se eliminó el tab "Más" — su contenido se redistribuyó según el modelo de datos real (ver ítems siguientes).
- [x] Dashboard — balance, categorías principales, alertas/insights, transacciones recientes (gráfico simple con barras, sin librería de charts nueva).
- [x] Movimientos — ahora es un hub con 3 secciones (Movimientos / Categorías / Servicios) porque están acopladas en el modelo de datos real (`Expense.category`/`Income.category` son FK directas, y `RecurringPaymentService.payRecurringPayment` crea un `Expense` enlazado al pagar un servicio):
  - Movimientos: lista con filtro ingreso/gasto y quick-add visual; sugerencia de categoría por IA queda como nota, no implementada.
  - Categorías: listado filtrable por Ingreso/Gasto; el CRUD ya existe en el backend real, falta el cliente HTTP mobile-friendly.
  - Servicios: listado de pagos recurrentes con próxima fecha de cobro.
- [x] Deudas / Tarjetas — Deudas y Tarjetas separadas con toggle (mismo patrón que el filtro de Ingresos/Gastos); registrar cargo/pago son botones sin acción real todavía.
- [x] Reportes — resumen del mes + categorías + movimientos; exportar CSV queda deshabilitado (pendiente `expo-sharing`).
- [x] Asistente IA — pantalla de chat con mensajes de ejemplo; el input no envía nada todavía.
- [x] Vínculo con Telegram — genera un código mock local; falta conectar con `TelegramLinkService.generateLinkCode`.
- [x] Preferencias — pantalla de solo lectura (tema/moneda/idioma), accesible desde Configuración; no existe todavía un endpoint de preferencias en el backend.
- [x] Notificaciones push — pantalla de opt-in con toggles; falta integrar `expo-notifications` + `ExpoPushAdapter`.
- [x] Configuración — perfil (solo lectura), preferencias de notificación y proveedores de IA (solo lectura).

### Fase 2 (v2) — Automatización Android (la pieza diferencial)

> El seguimiento en forma de milestone de esta fase (y de todo lo que viene después de la
> Fase 1) vive en [`plan-sprints-movil-nativo.md`](plan-sprints-movil-nativo.md) — ahí es
> M4.

Depende de que el Sprint 3 del roadmap general (ingestión de correo, `ingested_messages`,
motor de extracción con IA, bandeja de revisión) esté maduro — **no tiene sentido construir
esto antes**, porque reutiliza el mismo pipeline de extracción/confianza/revisión que ese
sprint deja armado.

- [ ] Módulo nativo Kotlin (`NotificationListenerService`) vía Expo Modules API — el usuario
      lo activa manualmente en Ajustes → Acceso a notificaciones (no requiere ser manejador
      por defecto de nada, a diferencia de `READ_SMS` que sí está descartado en
      `roadmap-saas-cuentas-reales.md`).
- [ ] El módulo captura el texto de la notificación (banco/billetera) y lo manda al mismo
      pipeline de extracción con IA + bandeja de revisión que procesa correo.
- [ ] Declaración de permiso sensible en Play Console (requisito de store, documentar el uso
      exacto para no arriesgar el rechazo en review).
- [ ] **Solo Android** — en iOS no existe API equivalente (confirmado en la investigación del
      roadmap: sandboxing de Apple sin excepción real para terceros). Comunicar como
      "auto-categorización inteligente en Android", nunca como paridad de plataforma.

### Fase 3 (v3) — Capacidades nativas adicionales

> Seguimiento como milestone en [`plan-sprints-movil-nativo.md`](plan-sprints-movil-nativo.md)
> (M5). La captura de recibo por cámara listada más abajo se adelantó a M1 de ese documento
> (no depende de biometría/offline/widgets y el trabajo real resultó mínimo — ver M1 para el
> detalle).

| Ítem | Detalle |
|---|---|
| Biometría | `expo-local-authentication` (Face ID / huella) para desbloquear la app o confirmar acciones sensibles; el `refreshToken` sigue en `expo-secure-store`, la biometría solo gatea el acceso local. |
| Modo offline | Cache local (SQLite vía `expo-sqlite` o WatermelonDB) + cola de sincronización para altas de gasto/ingreso hechas sin conexión. Empezar acotado: solo lectura del último estado + cola de escritura para gasto/ingreso, no todo el dominio. |
| Widgets de pantalla de inicio | Balance del mes / próximo vencimiento de tarjeta (Android e iOS tienen APIs de widget distintas — evaluar `react-native-android-widget` / WidgetKit vía config plugin). |
| Captura de recibo por cámara | El bot de Telegram ya resuelve "foto de recibo → gasto" en el backend (Sprint 2, validado end-to-end) — en móvil es exponer ese mismo flujo con `expo-camera` en vez de depender de enviarle la foto al bot. |

---

## 6. Riesgos y decisiones abiertas

- **Madurez de Expo Modules API para el módulo de Fase 2**: validar con un spike chico
  (Kotlin mínimo que solo loguea notificaciones) antes de comprometer el alcance completo de
  v2 — si resulta insuficiente, la alternativa es un módulo nativo bare solo para ese
  paquete, sin abandonar Expo para el resto de la app.
- **Extracción a paquete compartido de `lib/types/`/`lib/schemas/`**: no bloquea la Fase 1;
  evaluar cuando el drift entre copias empiece a doler de verdad (regla general del repo:
  no introducir abstracción antes de que el dolor sea real).
- **Mensaje de producto para iOS**: ya documentado en `roadmap-saas-cuentas-reales.md`
  ("Implicancia de producto para iOS") — v1 y v3 son multiplataforma, v2 es Android-only y
  debe comunicarse así desde el día uno, no como un roadmap de paridad futura.

---

## Fuentes

- [`roadmap-saas-cuentas-reales.md`](roadmap-saas-cuentas-reales.md) — sección "App móvil"
  (framework, fases v1/v2/v3 a alto nivel, honestidad de plataforma SMS/notificaciones).
- [`DESIGN.md`](DESIGN.md) — sistema de diseño, ya escrito pensando en portarse a móvil.
- [`notifications-future.md`](notifications-future.md) — canales de notificación evaluados
  (Web Push queda reemplazado por push nativo en móvil, no complementado).
- Exploración directa de `JwtService`, `JwtAuthenticationFilter`, `UserController`,
  `UserService`, `TelegramLinkService` (vía CodeGraph) para confirmar el mecanismo real de
  auth (Bearer + refresh en cookie HttpOnly con CSRF) — no un dato asumido.
