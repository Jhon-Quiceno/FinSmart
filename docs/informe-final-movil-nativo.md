# Informe final — Track móvil nativo (M1-M5)

> Ejecución de [`plan-sprints-movil-nativo.md`](plan-sprints-movil-nativo.md) del
> 2026-08-22. Este documento resume qué se implementó, qué quedó deliberadamente sin
> hacer (y por qué), cómo probar cada pieza nueva, y qué debería revisar un humano antes
> de dar por cerrado el track.

**Estado al cierre: M1 y M3 completos y mergeados a `develop`. M5 parcial (biometría +
modo offline) mergeado — widgets sin implementar. M2 en pausa por su propio gate. M4
bloqueado por una dependencia que no existe todavía.**

---

## 1. Qué se hizo

### M1 — Captura nativa de recibos (PR #98, + fix de logo en PR #97)

- **Backend**: `POST /api/receipts/scan` (JWT estándar, sin Telegram/n8n) expone
  `ReceiptExtractionService.extractFromImage` — el mismo servicio que ya usaba el bot de
  Telegram. No crea ningún `Expense`/`Income`, solo lee la imagen; la confirmación la hace
  la app llamando a los endpoints ya existentes (`POST /api/expenses`/`POST /api/incomes`).
  Entra al `RateLimitFilter` (mismo criterio IP+userId que `/api/ai/chat`, dispara una
  llamada de visión paga por request) y `ReceiptScanRequest` tiene un `@Size` acorde a una
  foto (~11MB crudos en base64).
- **Mobile**: pantalla `escanear-recibo` (`expo-camera`) — toma la foto, la manda al
  endpoint, y muestra una revisión editable (descripción, monto, categoría,
  gasto/ingreso) antes de confirmar. Botón de cámara junto al FAB de Movimientos.
- **De paso**: el logo de KoroFin no se veía en Expo Go. El SVG no tenía `viewBox`, así que
  `react-native-svg` clipeaba el contenido a una caja diminuta. Corregido con un `viewBox`
  calculado analíticamente sobre el bounding box real de los 120 paths del logo.

### M3 — Preferencias de usuario (PR #99)

- **Backend**: columnas `theme`/`currency`/`language` en `users` (migración `V27`, con
  `CHECK` constraints — mismo criterio que `ai_chat_used`/`ai_chat_period`).
  `GET`/`PATCH /api/users/preferences`. `UserResponse` (login/register/updateProfile)
  ahora incluye las tres preferencias.
- **Mobile**: `preferencias.tsx` dejó de tener moneda/idioma hardcodeados — se leen/escriben
  contra el backend. El tema se sigue aplicando al instante vía NativeWind +
  `expo-secure-store` (sin tocar esa pieza de la Fase 1), y además se sincroniza con el
  backend para sobrevivir a otro dispositivo/reinstalación.
- **Migración verificada contra Postgres real**: se levantó un contenedor Postgres 16
  descartable (mismo image que `docker-compose.yml`) y se corrió el backend apuntándole —
  Flyway aplicó las 26 migraciones limpio y Hibernate validó el mapeo de la entidad sin
  errores, no solo se corrió contra los `@WebMvcTest` slices.

### M5 — Fase 3, parcial: biometría + modo offline acotado (PR #100)

- **Biometría**: `BiometricLockGate` en la raíz de la app pide Face ID/huella al abrir en
  frío y al volver de background, si el usuario activó el toggle en Preferencias (solo
  visible con hardware+enrolamiento disponible). 100% local — el `refreshToken` sigue en
  `expo-secure-store` sin cambios.
- **Offline — lectura**: cache de `react-query` persistida en `AsyncStorage`
  (`@tanstack/react-query-persist-client`), así el último estado conocido (dashboard,
  movimientos, etc.) sigue disponible sin conexión, no solo mientras el proceso de JS
  sigue vivo en memoria.
- **Offline — escritura**: cola en SQLite (`expo-sqlite`) para altas de gasto/ingreso. Si
  la creación falla por conectividad (no por un error real del servidor — esos se propagan
  igual que siempre), se encola en vez de perderse, y se sincroniza sola al recuperar
  conexión (`NetInfo`) o al reabrir la app. Acotada a un `userId` por fila para no mezclar
  movimientos entre dos usuarios del mismo dispositivo.

### Proceso seguido en las 3 (M1, M3, M5)

Cada milestone: rama → implementación con tests → PR → **revisión independiente con
`opencode`** (corriendo en este mismo repo, sin ver mi razonamiento) → aplicar los
hallazgos reales que encontró → merge a `develop`. `opencode` encontró y se corrigieron
7 problemas reales en total (fecha UTC vs. local, falta de rate-limit, stomp de defaults
en un error-path, fuga de datos entre cuentas en la cola offline, una cola que podía
trabarse para siempre, y una carrera en el gate de biometría) — el detalle de cada uno
está en los comentarios de los PRs #98, #99 y #100.

---

## 2. Qué NO se hizo (y por qué)

### M2 — Retiro de Telegram: **en pausa, no por decisión mía**

El plan tiene un gate explícito: *"este milestone no arranca hasta que M1 esté validado
end-to-end con fotos reales de usuarios reales, no solo con datos sintéticos"*. Yo no
puedo tomar una foto con un celular físico — así que ese gate no está satisfecho todavía.
Retirar Telegram ahora, sin esa validación, dejaría sin camino de captura por foto a
cualquier usuario que hoy dependa del bot si el flujo nuevo tuviera un bug no detectado.

**Qué falta para desbloquearlo**: probar `escanear-recibo` con una foto real en un celular
(ver sección 3), confirmar que la extracción y la creación del movimiento funcionan de
punta a punta, y recién ahí ejecutar M2 (es un milestone chico — básicamente borrar los
~20 archivos de `integraciones/telegram*` y una migración de limpieza — no debería tomar
mucho una vez que el gate esté satisfecho).

### M4 — Notificaciones de Android: **bloqueado, no evaluado como "no prioritario"**

Depende del Sprint 3 del roadmap general (ingestión de correo), que todavía no arrancó —
el plan mismo lo marca como bloqueo explícito, no como preferencia de orden. No hay
backend de bandeja de revisión contra el cual conectar el listener de notificaciones, así
que no hay nada real que construir todavía.

### Widgets de pantalla de inicio (parte de M5): **no implementado, con evidencia de por qué**

Los widgets (`react-native-android-widget` / WidgetKit) son módulos nativos que requieren
salir de Expo Go a un **Dev Client o EAS Build** — no hay forma de escribirlos y
verificar siquiera que compilan sin ese toolchain (Android Studio/Gradle para Android,
Xcode para iOS). Este entorno tiene el Android SDK parcialmente instalado (`adb`,
`platform-tools`) pero **no un emulador ni un dispositivo físico conectado** — se verificó
explícitamente antes de decidir esto. Se prefirió dejarlos sin implementar, documentados
acá, en vez de entregar código nativo que nadie pudo confirmar que compila.

### Otras cosas deliberadamente fuera de alcance (documentadas en el código)

- **i18n real**: elegir "English" en Preferencias guarda la preferencia pero no traduce
  ningún texto — la app no tiene una capa de internacionalización todavía.
- **`formatCurrency` dinámico**: el formato de moneda en el resto de la app (dashboard,
  reportes, etc.) no lee la preferencia de moneda guardada. Cablear eso en cada pantalla
  es un refactor más amplio que "huecos de paridad" — se guarda la preferencia, pero no
  se propaga a la UI todavía.
- **Importación de extractos bancarios en mobile**: el plan mismo la deja sin diseñar
  ("genuinamente sin diseño cerrado todavía") — no se tocó.
- **Web export roto a propósito**: `expo-sqlite` en web necesita un build wasm + headers
  `COOP`/`COEP` de servidor — una configuración aparte, fuera de alcance para una app que
  nunca se pensó para desplegarse a web (`web` es un target secundario de conveniencia).
  `expo export -p ios` y `-p android` sí se verificaron y compilan limpio.
- **Duplicación en la cola offline**: si un `POST` de creación de gasto/ingreso llega al
  servidor pero la respuesta se pierde por timeout/red inestable, el cliente lo clasifica
  como falla de conectividad y lo reencola — puede terminar duplicado. Arreglarlo de
  verdad necesita una idempotency-key con soporte del backend; documentado como límite
  conocido del alcance "acotado" que pide el plan.

---

## 3. Cómo probar la app

### Backend

```bash
cd smart-finance-backend
./mvnw.cmd test        # 665 tests, deben pasar todos
```

Para levantar contra Postgres real: `docker-compose up db` desde la raíz del repo (usa
las variables de `.env`), después `./mvnw.cmd spring-boot:run` con
`SPRING_PROFILES_ACTIVE=dev`.

### Mobile

```bash
cd smart-finance-mobile
pnpm install
pnpm test               # 178 tests, deben pasar todos
pnpm exec tsc --noEmit  # debe salir limpio
pnpm run lint           # debe salir limpio (solo warnings preexistentes)
pnpm start              # Expo Go, apuntando al backend por IP LAN (ver .env.example)
```

### Validación manual pendiente (necesita un dispositivo/emulador real — no la pude ejecutar yo)

Estos son los puntos que un humano tiene que probar en un celular antes de confiar en
que M1/M3/M5 funcionan de punta a punta, no solo en tests automatizados:

1. **Logo**: abrir login/register en Expo Go y confirmar que el logo de KoroFin se ve
   completo (el fix de M1 corrige exactamente este síntoma reportado).
2. **Escanear recibo** (M1, y gate de M2): desde Movimientos, tocar el botón de cámara,
   sacarle una foto a un recibo/factura real, confirmar que la extracción (monto,
   comercio, categoría sugerida) es razonable, y que "Confirmar" crea el movimiento real.
   Probar también con una foto sin relación (ej. una pared) y confirmar el mensaje de
   "no es un recibo" sin crear nada.
3. **Preferencias** (M3): cambiar tema, moneda e idioma desde Preferencias, cerrar la app
   por completo, volver a abrirla, y confirmar que los tres valores persistieron (no solo
   en memoria). Probar también con el WiFi apagado al entrar a la pantalla, para ver el
   estado de error con "Reintentar".
4. **Biometría** (M5): activar "Bloqueo con biometría" en Preferencias en un dispositivo
   con Face ID/huella configurada, cerrar la app (o mandarla a background y volver), y
   confirmar que pide biometría antes de mostrar el contenido.
5. **Offline** (M5): con la app abierta y sesión iniciada, activar modo avión, crear un
   gasto o ingreso (o confirmar uno escaneado), y confirmar que no tira error. Desactivar
   modo avión y, en menos de un minuto, confirmar que el movimiento aparece en
   Movimientos (se sincronizó solo). Repetir también matando la app en modo avión y
   reabriéndola ya con conexión, para probar el drenado al inicio (no solo por el listener
   de reconexión).

---

## 4. Cosas a tener en cuenta

- **PRs de esta ejecución**: #97 (fix logo), #98 (M1), #99 (M3), #100 (M5 parcial) — todos
  mergeados a `develop`, ninguno a `main`.
- **Antes de retirar Telegram (M2)**: confirmar el punto 2 de la sección 3 en un
  dispositivo real primero — es un gate del plan, no una sugerencia.
- **Antes de M4**: no tiene sentido evaluarlo hasta que el Sprint 3 del roadmap general
  (ingestión de correo) tenga backend real.
- **Antes de widgets**: van a necesitar migrar el proyecto mobile completo a Dev
  Client/EAS Build — el plan ya lo anota como compartido con el push remoto de la Fase 0,
  que tampoco se puede verificar hoy en Expo Go.
- **Rate limits nuevos**: `/api/receipts/scan` ahora comparte el mecanismo de
  `RateLimitFilter` (10 req/min por defecto, configurable vía
  `RATE_LIMIT_RECEIPT_SCAN_*`) — si en producción se ve gente golpeando ese límite
  legítimamente (ej. escaneando muchos recibos de un viaje), ese es el valor a ajustar.
- **Monedas soportadas**: el `CHECK` de la migración V27 solo permite
  `COP/USD/MXN/ARS/EUR`. Agregar una nueva moneda necesita una migración (no es un enum de
  Java, así que no rompe el build — solo hace falta el `ALTER TABLE ... DROP/ADD
  CONSTRAINT`).
