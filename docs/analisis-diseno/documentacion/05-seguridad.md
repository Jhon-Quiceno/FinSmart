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
     │  localStorage      │                     │
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
  └── JwtAuthenticationFilter (OncePerRequestFilter)
       ├── Extrae token del header Authorization
       ├── Valida firma RSA256
       ├── Verifica expiración
       ├── Carga UserDetails de BD
       └── Setea SecurityContextHolder
```

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

No aplica — el backend es stateless (JWT). El frontend usa cookie `SameSite=Lax` + `HttpOnly` para el refresh token, que solo se envía en la ruta `/api/users/refresh`.

### Validaciones

- Contraseñas con `@Valid`: mínimo 8 caracteres, al menos una mayúscula, un número y un carácter especial
- Emails únicos controlados con constraint `UNIQUE` en BD + catch de `DataIntegrityViolationException`
- `PUT /users/password` requiere contraseña actual para autorizar el cambio
- Cambiar contraseña revoca **todos** los refresh tokens del usuario

### Manejo de Sesiones

- El endpoint `/users/password` está excluido del interceptor de renovación automática de sesión (el `401` de "contraseña incorrecta" no debe tratarse como sesión expirada)
- `/users/logout` revoca el refresh token en BD y limpia la cookie

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
JWT_ISSUER=https://api.finsmart.app
JWT_ACCESS_EXPIRATION=900
JWT_REFRESH_EXPIRATION=604800

# IA (opcionales — al menos una)
NVIDIA_API_KEY=nvapi-...
OPENCODE_API_KEY=oc-...
OPENROUTER_API_KEY=sk-or-...
```

---

*Documento de seguridad — KoroFin MVP*
