# Canales de notificacion futuros (KoroFin)

Investigacion realizada en Sprint 5 (julio 2026). En el MVP se implementaron **notificaciones in-app** (tabla `notifications` + campana del navbar) y **email via Brevo SMTP** (300 emails/dia gratis permanente, sin dominio propio, solo verificacion de remitente unico). Este documento deja el detalle de los canales evaluados y NO implementados, para retomarlos despues sin re-investigar.

## Comparativa general

| Canal | Esfuerzo | Gratis de verdad | Requisitos | Veredicto |
|-------|----------|------------------|------------|-----------|
| Web Push (VAPID) | M-L | Si (infra gratis) | HTTPS en prod, service worker, tabla de suscripciones | **Mejor candidato futuro** |
| Telegram Bot | S | Si (~1 msg/seg/chat) | Usuario debe iniciar el bot; capturar `chat_id` | **Segundo candidato** |
| Resend (email) | S-M | Limitado sin dominio (100/dia solo a tu propio inbox) | Dominio verificado (DNS) | Cuando KoroFin tenga dominio |
| Mailtrap (email) | S-M | Limitado sin dominio (150/dia) | Dominio con SPF/DKIM/DMARC | Cuando KoroFin tenga dominio |
| SendGrid | S | **No** — el plan gratis termino en 2025 | — | Descartado |
| Gmail SMTP directo | S | Funciona pero con riesgo | App password; ~500 destinatarios/24h | Riesgo ToS/entregabilidad; Brevo es mejor |
| WhatsApp Cloud API | M-L | **No** — cobra desde el mensaje #1 (jul-2025), ~$0.025-$0.12/msg | Verificacion de negocio en Meta | Solo si hay presupuesto |
| Twilio WhatsApp Sandbox | S | Solo para desarrollo | Destinatario debe mandar keyword a numero compartido | No viable en produccion |
| ntfy.sh / Gotify | S | Si | El usuario debe instalar una app y suscribirse | Solo para alertas de ops propias |
| Pushover | S | Envio gratis, pero el usuario paga $4.99/plataforma | — | Descartado (costo al usuario) |

## 1. Web Push (VAPID) — recomendado como proximo canal

Notificaciones del navegador aunque la pestana este cerrada. Infraestructura 100% gratis (los push services de Chrome/Firefox/Edge son parte de la plataforma web).

- **Libreria backend:** `nl.martijndwars:web-push:5.1.2` (github.com/web-push-libs/webpush-java, mantenida activamente).
- **Pasos:**
  1. Generar keypair VAPID (una vez) y guardar la privada como env var.
  2. Migracion: tabla `push_subscriptions` (`user_id`, `endpoint`, `p256dh`, `auth`, `created_at`, `UNIQUE(user_id, endpoint)`).
  3. Service worker en Next.js (`public/sw.js`) con handler de `push` + `notificationclick`.
  4. Frontend: pedir permiso, `registration.pushManager.subscribe(...)` con la clave publica VAPID, y POST de la suscripcion al backend.
  5. Backend: nuevo adaptador `WebPushAdapter` implementando el puerto `NotificationSender` existente (el fan-out por preferencias ya esta resuelto en Sprint 5).
- **Gotchas:** `localhost` cuenta como contexto seguro en desarrollo, pero produccion necesita HTTPS real. Manejar respuestas 404/410 del push service eliminando la suscripcion muerta. iOS Safari requiere que la app este instalada como PWA.

## 2. Telegram Bot — el mas barato de implementar

API gratuita, sin SDK: un POST a `https://api.telegram.org/bot{token}/sendMessage` con `chat_id` y `text`.

- **Pasos:**
  1. Crear bot con @BotFather → token (env var `TELEGRAM_BOT_TOKEN`).
  2. Columna `telegram_chat_id` en `notification_preferences` (nullable).
  3. Vinculacion: en `/configuracion`, mostrar link `t.me/{bot}?start={codigo-unico}`; un job o webhook procesa los updates del bot (`getUpdates` o webhook HTTPS) y asocia el `chat_id` al usuario por el codigo.
  4. Adaptador `TelegramAdapter` sobre el puerto `NotificationSender` (RestClient, sin dependencias nuevas).
- **Tradeoff UX:** el usuario tiene que tener Telegram y arrancar el bot; la vinculacion `chat_id` ↔ usuario es el 80% del trabajo.
- **Limites:** ~1 mensaje/segundo por chat, 30 msg/seg global — sobrado para KoroFin.

## 3. Email con dominio propio (Resend / Mailtrap)

Cuando KoroFin tenga dominio, Resend (100/dia gratis, DX excelente) o Mailtrap (150/dia) con DNS verificado (SPF/DKIM/DMARC) dan mejor entregabilidad y branding que Brevo con remitente unico. La migracion es trivial: cambiar host/credenciales SMTP en las mismas propiedades `spring.mail.*` — el `BrevoEmailAdapter` no cambia (renombrarlo a `SmtpEmailAdapter` si se generaliza).

## 4. WhatsApp — solo con presupuesto

Desde julio 2025 Meta cobra cada mensaje iniciado por el negocio (template): ~$0.025-$0.12 segun pais. Ademas requiere verificacion de negocio y un Business Solution Provider. El sandbox de Twilio sirve solo para demos (el destinatario debe mandar un keyword a un numero compartido cada 72h). **Conclusion:** documentado, pero fuera de un MVP zero-budget. Si algun dia se paga: Meta Cloud API directo (sin BSP intermediario) es lo mas barato.

## Arquitectura ya preparada

El Sprint 5 dejo el puerto `NotificationSender` con adaptadores in-app y email. Agregar cualquier canal de esta lista es: (1) nuevo adaptador que implementa el puerto, (2) toggle en `notification_preferences`, (3) datos de vinculacion del canal (suscripcion push / chat_id). El fan-out, la deduplicacion y las preferencias por tipo ya estan resueltos.
