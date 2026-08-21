# Plan de Sprints — Track Móvil Nativo (KoroFin)

> Este documento complementa a [`plan-app-movil-react-native.md`](plan-app-movil-react-native.md)
> de la misma forma en que `docs/sprints/*.md` complementa a
> [`roadmap-saas-cuentas-reales.md`](roadmap-saas-cuentas-reales.md): el plan de app móvil
> define el marco de fases (Fase 0-3, stack, decisiones de arquitectura de fondo); este
> documento lo convierte en milestones concretos con alcance, DoD y endpoints, para todo lo
> que viene **después** de que la Fase 0 y la Fase 1 (paridad esencial) ya están mergeadas.
>
> **Por qué no es `docs/sprints/sprintN.md`:** la numeración de `sprints/` ya reserva el 4
> (seguridad backend), 5 (frontend), 6 (Open Finance) y 7 (billing) — todos propuestos pero
> sin empezar (ver `roadmap-saas-cuentas-reales.md`, sección "Índice por sprint"). Meter el
> trabajo móvil ahí colisionaría con esas reservas. Por eso este track usa su propia
> etiqueta de milestone (**M0-M5**, no "Sprint N") y su propio documento, referenciado desde
> el roadmap pero fuera de su numeración de sprints.

**Estado: propuesto, sin empezar excepto M0.**

---

## M0 — Fase 0 y Fase 1 (ya entregado)

Retrospectiva, no propuesta — este trabajo ya está mergeado. Aterrizó en la rama
`feat/app-movil-react-native` y en las ramas sucesoras `feat/app-movil-diseno-pantallas` y
`feat/backend-auth-mobile-friendly` (PRs #91-#94), más el trabajo de integración final de
datos reales documentado en la rama `feat/mobile-integracion-backend`. Este apartado existe
para que el documento no arranque de la premisa falsa de "nada existe todavía".

Lo entregado:

- Backend: fallback de refresh token en el body de la respuesta para clientes
  `X-Client: mobile` (antes solo viajaba por cookie `HttpOnly`, ver §2 del plan de app
  móvil), y la excepción de CSRF ampliada a todas las mutaciones que llegan con ese mismo
  header.
- Backend: endpoint de baja de push token (`DELETE /api/notifications/push-token/{deviceId}`),
  sobre la tabla `push_tokens` (migración `V26__create_push_tokens.sql`) y el adaptador
  `ExpoPushAdapter` (puerto `PushNotificationSender`) de la Fase 0.
- Mobile: capa de datos completa — cliente HTTP (`axios` + interceptor de refresh),
  almacenamiento de sesión en `expo-secure-store`, y las pantallas de la Fase 1 (auth,
  dashboard, movimientos, deudas/tarjetas, reportes, asistente IA, vínculo con Telegram,
  preferencias de solo lectura, notificaciones push, configuración) conectadas a los
  endpoints reales del backend en vez de a los mocks con los que se habían construido.

## M1 — Captura nativa de recibos

### Antes de empezar

```bash
git checkout develop
git checkout -b feature/movil-m1-captura-recibos
```

### Objetivo

1. Exponer `ReceiptExtractionService.extractFromImage` (hoy solo alcanzable vía el bot de
   Telegram, ver Decisiones) detrás de un endpoint nuevo autenticado por JWT, sin ninguna
   dependencia de Telegram.
2. Conectar `expo-camera` en la app móvil para capturar la foto de un recibo y enviarla a ese
   endpoint nuevo.
3. Cerrar el ítem "Captura de recibo por cámara" que la Fase 3 original del plan de app móvil
   dejaba para más adelante — se adelanta a este milestone porque el trabajo real (un
   controlador delgado sobre un servicio ya existente) es mínimo y no depende de ninguna de
   las otras capacidades nativas de esa fase (biometría, offline, widgets).

### Decisiones de arquitectura

**Este milestone es aditivo y de bajo riesgo, porque la capa de servicio ya es agnóstica de
cliente.** `ReceiptExtractionService.extractFromImage(userId, imageUrl)` (paquete
`com.smartfinance.backend.ia.service`) toma un `userId` plano y una URL/data URI de imagen,
y devuelve un `ReceiptExtraction` — cero acoplamiento a Telegram. Hoy el único camino hacia
ese servicio es `POST /api/integrations/telegram/receipts`, que exige un `chatId` vinculado
y el header de secreto compartido `X-Telegram-Webhook-Secret`
(`TelegramWebhookFilter`) — inutilizable desde un cliente JWT normal. Falta exclusivamente un
controlador nuevo que llame al mismo servicio con autenticación JWT estándar.

**El body es JSON con un data URI, no `multipart/form-data`.** Es el mismo patrón que ya usa
`TelegramReceiptRequest.imageUrl` — el proveedor de IA de visión acepta indistintamente una
URL `https://` o un data URI `data:image/...;base64,...` en su campo `image_url.url` (ver
Javadoc de `ReceiptExtractionService`). `expo-camera` puede producir directamente un data URI
en base64 (`takePictureAsync({ base64: true })`), así que este formato evita manejar
multipart o un paso intermedio de almacenamiento de archivo en el backend — el mismo ahorro
de complejidad que ya le sirvió al flujo de Telegram.

**No se toca nada relacionado con Telegram en este milestone.** M1 y M2 (retiro de Telegram)
están secuenciados a propósito: primero se prueba el camino nativo con fotos reales de
usuarios reales, y solo después de esa validación se retira el camino viejo — ver el gate de
M2.

**Endpoint propuesto:**

```
POST /api/receipts/scan
Authorization: Bearer <accessToken>
Content-Type: application/json

{ "imageDataUri": "data:image/jpeg;base64,..." }
```

Nombre y forma exacta a confirmar en el diseño final; el criterio detrás de la propuesta es
el ya explicado (JSON + data URI, JWT, sin nada de Telegram).

### Alcance

**Backend**

- [ ] DTO `ReceiptScanRequest(String imageDataUri)` — `@NotBlank`, mismo patrón de
      validación que `TelegramReceiptRequest`.
- [ ] Controlador nuevo `POST /api/receipts/scan` (JWT, sin relación con Telegram) que
      resuelve el `userId` autenticado y llama directo a
      `ReceiptExtractionService.extractFromImage(userId, imageDataUri)`.
- [ ] Definir en el diseño si el controlador vive junto a `ReceiptExtractionService` en el
      dominio `ia/` o en un paquete nuevo `recibos/` — decisión menor, no bloquea el resto
      del milestone.
- [ ] Tests de contrato del endpoint: 200 con recibo válido, respuesta "no es un recibo" con
      una imagen sin relación, 401 sin JWT — mismo nivel de cobertura que
      `TelegramIntegrationController`/`ReceiptExtractionServiceTest`.

**Frontend (mobile)**

- [ ] Integrar `expo-camera` (captura directa; evaluar si también permite elegir de galería)
      en una pantalla nueva o en el flujo de quick-add existente.
- [ ] Cliente HTTP nuevo (equivalente mobile de `lib/services/`) que llama a
      `POST /api/receipts/scan` con el data URI capturado.
- [ ] Pantalla de revisión: mostrar monto/comercio/categoría extraídos y exigir confirmación
      antes de crear el `Expense`/`Income` real — mismo principio de "no auto-crear sin
      confirmar" que ya aplica en el flujo de Telegram.

### Definición de terminado (DoD)

1. [ ] Una foto real de un recibo, tomada con `expo-camera` en un celular físico, produce una
   extracción correcta (monto, comercio, categoría sugerida) de punta a punta, sin pasar por
   Telegram ni n8n.
2. [ ] Confirmar la extracción crea el `Expense`/`Income` real en el backend.
3. [ ] Una foto sin relación con un recibo resulta en "no es un recibo", sin crear ningún
   movimiento.
4. [ ] `./mvnw.cmd test` en verde con la cobertura nueva del controlador; validado con al
   menos una foto real de un recibo real desde el celular — no solo con datos sintéticos.

### Referencia de endpoints

```http
POST /api/receipts/scan
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "imageDataUri": "data:image/jpeg;base64,..."
}
```

### Notas

- No se elimina, deprecia ni modifica ningún archivo del dominio `integraciones/` (Telegram)
  en este milestone — el inventario completo a retirar vive en M2.
- El modelo de visión que usa `ReceiptExtractionService`
  (`nvidia/nemotron-nano-12b-v2-vl`) ya fue validado empíricamente con un recibo real (ver el
  Javadoc de la clase, Sprint 2) — este milestone no repite esa validación de modelo, solo la
  de transporte (cliente móvil → endpoint nuevo).

## M2 — Retiro de Telegram

### Antes de empezar

```bash
git checkout develop
git checkout -b feature/movil-m2-retiro-telegram
```

### Objetivo

1. Retirar el bot de Telegram como canal de captura de gastos/recibos, ahora que M1 provee un
   camino nativo equivalente.
2. Dejar el dominio `integraciones/` limpio de código Telegram-específico, sin deuda muerta.
3. Marcar como pendiente (fuera de este repo) la baja del workflow de n8n asociado.

**Gate explícito: este milestone no arranca hasta que M1 esté validado end-to-end con fotos
reales de usuarios reales, no solo con datos sintéticos** — el mismo criterio no negociable
que `docs/sprints/sprint2.md` ya aplicó al validar el bot de Telegram en producción (2026-07-22)
antes de darlo por cerrado. Retirar el camino viejo antes de confirmar que el reemplazo
funciona en producción dejaría sin camino de captura por foto a cualquier usuario que hoy
dependa del bot.

### Decisiones de arquitectura

**Se elimina, no se deprecia en paralelo.** Mantener dos caminos redundantes (Telegram +
cámara nativa) indefinidamente es deuda técnica, no valor de producto — una vez validado M1,
no hay razón de negocio para sostener el bot.

**El n8n workflow no se toca desde este repo.** El polling/descarga de fotos contra la Bot
API de Telegram vive en la instancia de n8n, fuera de este repositorio — su
desprovisionamiento es un paso operativo aparte, no un ítem de checklist de código (ver
Notas).

### Alcance

**Backend**

- [ ] Eliminar `integraciones/controller/TelegramIntegrationController.java`.
- [ ] Eliminar `integraciones/service/{TelegramLinkService,TelegramLinkCodeStore,
      TelegramExpenseService,TelegramMessageParser,TelegramIntentDetector}.java`.
- [ ] Eliminar `integraciones/model/entity/TelegramLink.java`.
- [ ] Eliminar `integraciones/repository/TelegramLinkRepository.java`.
- [ ] Eliminar los 6 DTOs de `integraciones/model/dto/*.java`.
- [ ] Eliminar las 6 excepciones de `integraciones/exception/*.java`.
- [ ] Eliminar `common/security/TelegramWebhookFilter.java` y su registro en la cadena de
      filtros de `SecurityConfig`.
- [ ] Eliminar los 8 archivos de test bajo `src/test/java/.../integraciones/`.
      `ReceiptExtractionServiceTest` (bajo `ia/service/`) **no se elimina** — el servicio
      sigue vivo, usado por M1 — solo se revisa que no le queden asunciones específicas de
      Telegram.
- [ ] Migración de limpieza nueva: `DROP TABLE telegram_links` (creada en
      `V24__create_telegram_links.sql`). Verificar el último `V` disponible en el momento de
      implementar este milestone, no numerarlo de antemano acá — mismo criterio que ya usa
      `docs/sprints/sprint1.md` en su sección de base de datos.

**Frontend / Mobile**

- [ ] Quitar la pantalla/flujo de vínculo con Telegram de la app móvil (`telegram.tsx`) y de
      `/configuracion` en el frontend web.

### Definición de terminado (DoD)

1. [ ] No queda ningún archivo con "Telegram" en el nombre ni referencia al dominio
   `integraciones/telegram` en backend, frontend ni mobile.
2. [ ] `./mvnw.cmd test` en verde tras la eliminación.
3. [ ] Migración de limpieza verificada contra Postgres real.
4. [ ] Confirmado que ningún usuario que dependía del bot pierde la capacidad de capturar un
   recibo por foto — M1 ya cubre ese caso de uso de forma validada antes de cerrar este
   milestone.

### Referencia de endpoints

```http
# Eliminados en este milestone (dejan de existir tras M2)
POST   /api/integrations/telegram/link-code
GET    /api/integrations/telegram/status
POST   /api/integrations/telegram/confirm-link
POST   /api/integrations/telegram/expenses
POST   /api/integrations/telegram/receipts
```

### Notas

- El workflow de n8n que hace polling/descarga de fotos contra la Bot API de Telegram vive
  fuera de este repositorio (en la instancia de n8n) — necesita desprovisionarse aparte,
  ningún checklist de este documento lo cubre.
- `common/security/TelegramWebhookFilter.java` es el único archivo de `common/` tocado por
  este milestone; el resto del dominio no cambia.

## M3 — Huecos de paridad

### Antes de empezar

```bash
git checkout develop
git checkout -b feature/movil-m3-huecos-paridad
```

### Objetivo

1. Endpoint real de preferencias de usuario (tema, moneda, idioma) — hoy no existe: cero
   columnas de tema/moneda/idioma en las 26 migraciones del backend, `UserResponse` es solo
   `{id, name, email}`, y la pantalla `preferencias.tsx` de mobile es de solo lectura con
   literales hardcodeados.
2. Dejar planteado (sin cerrar el diseño) dónde entraría la importación de extractos
   bancarios en mobile — hoy es exclusivamente web, explícitamente fuera de alcance de la
   Fase 1 del plan de app móvil.

### Decisiones de arquitectura

**Las preferencias van como columnas nuevas en `users`, no como tabla aparte.** Es una
relación 1:1 escalar por usuario, mismo tipo de dato que `ai_chat_used`/`ai_chat_period`
(agregados directo a `users` en `V14__add_ai_quota_to_users.sql`) — no hay necesidad de una
tabla `user_preferences` separada para tres columnas simples sin historial propio.

**El alcance de importación de extractos en mobile queda deliberadamente abierto.** No se
sobre-especifica acá: la pantalla de upload, si usa `expo-file-system`/`expo-document-picker`,
y cómo se relaciona con el flujo web existente son decisiones reales que dependen de cómo
termine de validarse el Nivel 2 del roadmap general (extractos bancarios, aún pendiente de
validar con datos reales) — este milestone solo anota que este es el lugar natural para
retomarlo.

### Alcance

**Backend**

- [ ] Columnas nuevas en `users`: `theme`, `currency`, `language` (nombres exactos a
      confirmar en el diseño) vía migración nueva.
- [ ] Endpoint de preferencias — extender `UserController` o agregar uno nuevo; `UserResponse`
      pasa de `{id, name, email}` a incluir las tres preferencias.

**Frontend / Mobile**

- [ ] Conectar `preferencias.tsx` (hoy de solo lectura) al endpoint nuevo.
- [ ] Reflejar el tema elegido en NativeWind (claro/oscuro) de forma real, no solo visual.

**Base de datos**

- [ ] Migración nueva — verificar el último `V` disponible en el momento de implementar,
      mismo criterio de M2.

### Definición de terminado (DoD)

1. [ ] El usuario cambia tema, moneda o idioma desde la app móvil y el cambio persiste en el
   backend (no solo en estado local).
2. [ ] `preferencias.tsx` deja de tener literales hardcodeados.
3. [ ] `./mvnw.cmd test` en verde; validado con un usuario real cambiando cada una de las tres
   preferencias, no solo con datos sintéticos.

### Referencia de endpoints

```http
GET   /api/users/preferences
PATCH /api/users/preferences
```

### Notas

- La importación de extractos bancarios en mobile queda anotada acá como el lugar natural
  para retomarla más adelante, genuinamente sin diseño cerrado todavía — ver Decisiones.

## M4 — Fase 2 (Android)

### Antes de empezar

```bash
git checkout develop
git checkout -b feature/movil-m4-fase-2-android
```

### Objetivo

Mismo alcance que la Fase 2 (v2) de `plan-app-movil-react-native.md` §5, llevado a formato
de milestone:

1. Módulo nativo Kotlin (`NotificationListenerService`) vía Expo Modules API, que el usuario
   activa manualmente en Ajustes → Acceso a notificaciones.
2. El módulo captura el texto de la notificación (banco/billetera) y lo manda al mismo
   pipeline de extracción con IA + bandeja de revisión que procesa correo.
3. Declaración de permiso sensible en Play Console, documentando el uso exacto para no
   arriesgar el rechazo en review.
4. Solo Android — en iOS no existe API equivalente (sandboxing de Apple sin excepción real
   para terceros); comunicar como "auto-categorización inteligente en Android", nunca como
   paridad de plataforma.

### Decisiones de arquitectura

**Bloqueado por el Sprint 3 del roadmap general (ingestión de correo).** No tiene sentido
construir este milestone antes: reutiliza el mismo pipeline de extracción/confianza/bandeja
de revisión que ese sprint deja armado para correo — construirlo antes significaría
duplicar esa pieza o dejarla sin backend real que la sostenga.

**Requiere salir de Expo Go por completo.** Un módulo nativo Kotlin custom no puede cargarse
dentro de la app genérica Expo Go descargada de la store — este milestone pasa a necesitar un
**Dev Client** propio (`npx expo run:android`) o un build de **EAS Build**. No basta con lo
que alcanzó para la Fase 0/1. Esta misma limitación de tooling ya afecta hoy al push remoto
que entregó la Fase 0 (`ExpoPushAdapter`): tampoco se puede verificar en Expo Go sobre SDK
54/Android — ambas piezas comparten la necesidad de moverse a un Dev Client antes de poder
probarse de verdad, así que conviene resolver esa migración de tooling una sola vez para las
dos.

**Declaración de permiso sensible en Play Console.** Requisito de store, no técnico — debe
documentarse el uso exacto del acceso a notificaciones para no arriesgar el rechazo en
review.

### Alcance

- [ ] Spike chico: módulo Kotlin mínimo que solo loguea notificaciones, para validar la
      madurez de Expo Modules API antes de comprometer el alcance completo (riesgo ya
      anotado en `plan-app-movil-react-native.md` §6).
- [ ] Módulo nativo Kotlin (`NotificationListenerService`) vía Expo Modules API.
- [ ] Conexión del texto capturado al pipeline de extracción con IA + bandeja de revisión del
      Sprint 3 (correo).
- [ ] Migración a Dev Client/EAS Build para el proyecto mobile completo (no solo para este
      módulo).
- [ ] Declaración de permiso sensible en Play Console.
- [ ] Mensaje de producto: comunicar como función Android-only desde el día uno.

### Definición de terminado (DoD)

1. [ ] Spike Kotlin mínimo validado en un dispositivo real.
2. [ ] La app corre sobre Dev Client/EAS Build (no Expo Go) de forma estable.
3. [ ] Una notificación bancaria real capturada por el listener produce una extracción tan
   correcta como el pipeline de correo del Sprint 3 — validado con datos reales, no solo
   sintéticos.
4. [ ] Declaración de permiso sensible aprobada en la review de Play Console.

### Referencia de endpoints

```http
# Sin endpoints propios nuevos: reutiliza el pipeline de bandeja de revisión que expone
# el Sprint 3 del roadmap general (ingested_messages + endpoints de revisión), todavía
# propuesto — ver docs/sprints/sprint3.md.
```

### Notas

- No iniciar antes de que el Sprint 3 (correo) del roadmap general esté maduro — bloqueo
  explícito, no una preferencia de orden.
- Comparte con la Fase 0 (push remoto) la necesidad de moverse a Dev Client/EAS Build —
  ninguna de las dos piezas se puede verificar hoy en Expo Go sobre SDK 54/Android.

## M5 — Fase 3

### Antes de empezar

```bash
git checkout develop
git checkout -b feature/movil-m5-fase-3
```

### Objetivo

Mismo alcance que la Fase 3 (v3) de `plan-app-movil-react-native.md` §5, llevado a formato de
milestone — sin inventar alcance nuevo sobre lo que ese documento ya define:

1. Biometría vía `expo-local-authentication`.
2. Modo offline acotado.
3. Widgets de pantalla de inicio.

**La captura de recibo por cámara que la Fase 3 original listaba en este nivel ya no vive
acá** — se adelantó a M1 de este documento, porque el trabajo real resultó mínimo (exponer un
servicio ya agnóstico de cliente) y no dependía de ninguna de las otras tres capacidades
nativas de esta fase.

### Decisiones de arquitectura

Mismas decisiones que ya fija el plan de app móvil §5 — este milestone no las reabre:

- **Biometría solo gatea el acceso local**; el `refreshToken` sigue viviendo en
  `expo-secure-store`, la biometría no reemplaza ese mecanismo.
- **Offline empieza acotado**: solo lectura del último estado conocido + cola de escritura
  para gasto/ingreso, no todo el dominio.
- **Widgets**: Android e iOS tienen APIs de widget distintas — evaluar
  `react-native-android-widget` / WidgetKit vía config plugin en el diseño de este milestone.

### Alcance

- [ ] Biometría (`expo-local-authentication`): desbloqueo de la app y/o confirmación de
      acciones sensibles.
- [ ] Modo offline: cache local (`expo-sqlite` o WatermelonDB) + cola de sincronización
      acotada a altas de gasto/ingreso.
- [ ] Widgets: balance del mes / próximo vencimiento de tarjeta.

### Definición de terminado (DoD)

1. [ ] Face ID/huella desbloquea la app en un dispositivo real con biometría habilitada.
2. [ ] Un gasto/ingreso creado sin conexión se sincroniza correctamente al recuperar
   conexión, validado en un dispositivo real.
3. [ ] El widget de balance refleja el dato real del backend en al menos una plataforma
   (Android o iOS).

### Referencia de endpoints

```http
# Sin endpoints propios nuevos: offline y widgets reutilizan los endpoints ya existentes
# de dashboard/tarjetas/movimientos; biometría es 100% local (no llama al backend).
```

### Notas

- Nivel de detalle igual al que ya tenía la Fase 3 original
  (`plan-app-movil-react-native.md` §5) — este milestone no agrega alcance nuevo, solo lo
  lleva al formato de milestone de este documento.

---

## Fuentes

- [`plan-app-movil-react-native.md`](plan-app-movil-react-native.md) — marco de fases (Fase
  0-3), stack y decisiones de arquitectura de fondo que este documento convierte en
  milestones concretos.
- [`roadmap-saas-cuentas-reales.md`](roadmap-saas-cuentas-reales.md) — roadmap general del
  que depende M4 (Sprint 3, ingestión de correo) y contra el que este track se referencia
  desde la sección "App móvil" y el "Índice por sprint".
