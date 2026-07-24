# Seguridad — KoroFin

> **Propósito:** Describir el diseño de seguridad, autenticación JWT y manejo de sesiones.

---

## 1. Modelo de Autenticación

KoroFin usa **JWT (JSON Web Tokens)** con un esquema de **doble token**:

| Token | Dónde vive | Duración | Propósito |
|-------|-----------|----------|-----------|
| **Access Token** | Memoria del cliente (variable JS) | 15 minutos | Autenticar requests a la API |
| **Refresh Token** | Cookie HttpOnly + BD (hasheado) | 7 días | Renovar access token sin re-login |

### Flujo

```
┌─────────┐          ┌──────────┐          ┌──────────┐
│ Cliente │          │ Frontend │          │ Backend  │
│ (Browser)│        │ (Next.js)│          │(Spring)  │
└────┬────┘          └────┬─────┘          └────┬─────┘
     │                    │                     │
     │  POST /login       │                     │
     │───────────────────>│                     │
     │                    │  POST /api/users/   │
     │                    │  login              │
     │                    │────────────────────>│
     │                    │                     │
     │                    │  { accessToken,     │
     │                    │    user }           │
     │                    │  + Set-Cookie:      │
     │                    │  refreshToken       │
     │                    │<────────────────────│
     │                    │                     │
     │  Guarda en         │                     │
     │  memoria (JS)      │                     │
     │<───────────────────│                     │
     │                    │                     │
     │  GET /api/gastos   │                     │
     │  Authorization:    │                     │
     │  Bearer eyJ...     │                     │
     │───────────────────>│                     │
     │                    │  GET /api/expenses  │
     │                    │  Authorization: ... │
     │                    │────────────────────>│
     │                    │                     │
     │                    │  Si 401             │
     │                    │  → POST /refresh    │
     │                    │────────────────────>│
     │                    │  nuevo accessToken  │
     │                    │<────────────────────│
     │                    │  → Reintenta request│
```

---

## 2. Implementación Técnica

### 2.1 Backend (Spring Security)

```
SecurityFilterChain
  ├── TelegramWebhookFilter (addFilterBefore JwtAuthenticationFilter)
  │    └── Protege las 3 rutas server-to-server de /api/integrations/telegram/*
  ├── JwtAuthenticationFilter (OncePerRequestFilter)
  │    ├── Extrae token del header Authorization
  │    ├── Valida firma RSA256
  │    ├── Verifica expiración
  │    ├── Carga UserDetails de BD
  │    └── Setea SecurityContextHolder
  └── RateLimitFilter (addFilterAfter JwtAuthenticationFilter)
       └── Limita /users/login, /users/register y /ai/chat
```

Ver sección 3 para el detalle de `TelegramWebhookFilter` y `RateLimitFilter`.

- **Algoritmo**: RSA256 (par de llaves pública/privada)
- **Claims**: `sub` (userId), `email`, `roles`, `iat`, `exp`
- **Refresh tokens**: se almacenan hasheados con bcrypt en tabla `refresh_tokens`
- **Rotación**: cada vez que se usa un refresh token, se rota (se revoca el anterior, se emite uno nuevo)

### 2.2 Frontend (Axios Interceptor)

```typescript
// lib/api-client.ts — Comportamiento del interceptor
1. Cada request agrega header Authorization: Bearer {accessToken}
2. Si response es 401 → llama POST /api/users/refresh con cookie
3. Si refresh funciona → actualiza accessToken en memoria → reintenta request original
4. Si refresh falla → limpia sesión → redirige a /login
```

---

## 3. Protecciones Adicionales

### CORS

Configurado para aceptar solo el origen del frontend (localhost:3000 en dev, dominio de producción en prod).

### CSRF

A pesar de ser stateless vía JWT, el backend **sí** protege contra CSRF con `CookieCsrfTokenRepository.withHttpOnlyFalse()`: el token viaja en la cookie `XSRF-TOKEN` (legible por JS, para que el cliente pueda leerla) y debe repetirse en el header `X-XSRF-TOKEN` en cada request mutante. El frontend obtiene el token inicial con `GET /users/csrf`.

Exentos de CSRF: `register`, `login`, `logout`, `api-docs`/`swagger`, `actuator/health`, y los 3 webhooks de Telegram (`/api/integrations/telegram/{confirm-link,expenses,receipts}` — protegidos en cambio por `TelegramWebhookFilter`, ver más abajo).

El refresh token, aparte, usa cookie `SameSite=Lax` + `HttpOnly`, y solo se envía en la ruta `/api/users/refresh`.

### Validaciones

- Contraseñas con `@Valid`: mínimo 8 caracteres, al menos una mayúscula, un número y un carácter especial
- Emails únicos controlados con constraint `UNIQUE` en BD + catch de `DataIntegrityViolationException`
- `PUT /users/password` requiere contraseña actual para autorizar el cambio
- Cambiar contraseña revoca **todos** los refresh tokens del usuario

### Manejo de Sesiones

- El endpoint `/users/password` está excluido del interceptor de renovación automática de sesión (el `401` de "contraseña incorrecta" no debe tratarse como sesión expirada)
- `/users/logout` revoca el refresh token en BD y limpia la cookie
- El refresh token tiene una bandera `remember_me`: si el login se pidió con "recordarme", la cookie de refresh se emite con `Max-Age` (sobrevive a cerrar el navegador); si no, es una cookie de sesión

### TelegramWebhookFilter

Autentica las 3 rutas server-to-server que llama n8n en nombre del bot de Telegram (`confirm-link`, `expenses`, `receipts`) con un secreto compartido en el header `X-Telegram-Webhook-Secret`, en lugar de JWT/CSRF — son rutas sin sesión de usuario, marcadas `permitAll()` en `SecurityConfig` pero interceptadas por este filtro propio antes de `JwtAuthenticationFilter`. La comparación del secreto usa `MessageDigest.isEqual` (tiempo constante) para no filtrar por temporización cuán cerca estuvo un secreto incorrecto del real. Un secreto vacío (`TELEGRAM_WEBHOOK_SECRET` sin configurar) deshabilita la integración completa: nunca se acepta un secreto vacío como válido.

### RateLimitFilter

Rate limiting básico en memoria (bucket por clave, sin store distribuido) sobre las tres rutas más expuestas a abuso:

| Ruta | Clave | Default |
|------|-------|---------|
| `/users/login` | IP | 5 requests / 60s |
| `/users/register` | IP | 3 requests / 300s |
| `/ai/chat` | IP + userId (si está autenticado) | 10 requests / 60s |

Se registra después de `JwtAuthenticationFilter` a propósito: para `/ai/chat` necesita que `SecurityContextHolder` ya tenga el usuario autenticado, así dos usuarios distintos detrás de la misma IP (oficina/NAT) no comparten un mismo balde de rate limit. Si el backend escala a múltiples instancias, este mecanismo en memoria debería migrar a un store compartido (Redis) — queda anotado como deuda técnica en el roadmap.

---

## 4. Seguridad por Capas

| Capa | Mecanismo |
|------|-----------|
| **Transporte** | HTTPS obligatorio en producción |
| **API** | JWT Access Token en header |
| **Cookie** | Refresh Token HttpOnly + Secure + SameSite=Lax |
| **BD** | Refresh tokens hasheados con bcrypt |
| **IA** | API keys solo en variables de entorno del operador |
| **Exportación** | CSV escapado contra inyección de fórmulas (=, +, -, @) |

---

## 5. Variables de Entorno para Producción

```properties
# JWT
JWT_PRIVATE_KEY=-----BEGIN PRIVATE KEY-----\n...
JWT_PUBLIC_KEY=-----BEGIN PUBLIC KEY-----\n...
JWT_ISSUER=https://api.korofin.app
JWT_ACCESS_EXPIRATION=900
JWT_REFRESH_EXPIRATION=604800

# IA (opcionales — al menos una debe estar configurada para que el asistente funcione)
GEMINI_API_KEY=...
NVIDIA_API_KEY=nvapi-...
OPENCODE_API_KEY=oc-...
OPENROUTER_API_KEY=sk-or-...
GROQ_API_KEY=...

# Telegram (integración vía n8n)
TELEGRAM_WEBHOOK_SECRET=...
```

> Nota: `JWT_ISSUER` de arriba es un ejemplo genérico. La infraestructura real desplegada (Cloud Run, Vercel) todavía usa el nombre técnico "finsmart" en sus dominios reales — ver `docs/runbook-produccion.md`. `GROQ_API_KEY` está catalogado en el código pero sin key real configurada todavía en ningún ambiente.

---

*Documento de seguridad — KoroFin*
