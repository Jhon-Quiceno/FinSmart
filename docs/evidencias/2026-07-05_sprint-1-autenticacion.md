# Evidencia Sprint 1 — Base del Sistema (JWT Real)

> **Fecha:** Julio 2026
> **Total de tareas:** 15/15 completadas
> **Migraciones:** V1 (users), V2 (refresh_tokens)

---

## 1. Objetivo

Establecer la base del sistema con autenticación JWT funcional end-to-end entre backend y frontend, incluyendo registro, inicio de sesión, cierre de sesión y renovación automática de tokens.

## 2. Alcance Implementado

### Backend (7 tareas)
- Setup del proyecto Spring Boot con estructura de capas (config/controller/service/repository/model/dto/mapper/exception)
- Entidad `User` + repositorio, servicio (registro y login)
- Endpoint `POST /api/users/register` con validaciones de email único y contraseña
- Endpoint `POST /api/users/login` con access token JWT + cookie HttpOnly de refresh token
- Endpoints `POST /api/users/refresh` y `POST /api/users/logout` con rotación/revocación
- Manejo global de excepciones (`GlobalExceptionHandler`) + DTOs de error estándar
- Configuración CORS y filtro JWT stateless

### Base de Datos (3 tareas)
- Configuración PostgreSQL + Spring Data JPA + Flyway
- Migración V1: tabla `users`
- Migración V2: tabla `refresh_tokens` con índices

### Frontend (5 tareas)
- Cliente HTTP (Axios) con access token en memoria y refresh automático
- AuthContext reemplazando mocks con llamadas reales al backend
- Persistencia de usuario en localStorage + cookie HttpOnly para refresh token
- Pantalla de Login conectada al backend
- Pantalla de Registro conectada al backend

## 3. Trazabilidad con Requisitos

| Requisito | Implementado en |
|-----------|-----------------|
| RF-01: Registro con email único | UserService + @Valid + UNIQUE constraint |
| RF-02: Autenticación JWT | JwtAuthenticationFilter + SecurityConfig |
| RF-03: Cierre de sesión con revocación | POST /logout → revoked = true |
| RF-04: Renovación de access token | POST /refresh + rotación automática |

## 4. Decisiones Técnicas

| Decisión | Justificación |
|----------|---------------|
| Access token en memoria (no localStorage) | Reduce riesgo de XSS; el token no persiste en almacenamiento accesible por JS |
| Refresh token en cookie HttpOnly | No accesible por JavaScript; protegido contra XSS |
| Rotación de refresh token | Si un refresh token es robado, su uso invalida el anterior (detección de robo) |
| Firma RSA256 (asimétrica) | La clave pública puede distribuirse sin comprometer la firma |
| Refresh tokens hasheados con bcrypt | Aún si la BD se filtra, los tokens no pueden reutilizarse |

## 5. Artefactos Desarrollados

### Backend
- `com.smartfinance.backend.config.SecurityConfig` — Configuración de Spring Security
- `com.smartfinance.backend.security.JwtAuthenticationFilter` — Filtro JWT
- `com.smartfinance.backend.controller.UserController` — Endpoints de auth
- `com.smartfinance.backend.service.UserService` — Lógica de negocio
- `com.smartfinance.backend.model.User` — Entidad JPA
- `com.smartfinance.backend.exception.GlobalExceptionHandler` — Manejo de errores
- Migraciones: `V1__create_users.sql`, `V2__create_refresh_tokens.sql`

### Frontend
- `lib/api-client.ts` — Axios con interceptor JWT
- `contexts/AuthContext.tsx` — Estado de autenticación
- `app/login/page.tsx` — Pantalla de login
- `app/registro/page.tsx` — Pantalla de registro

## 6. Evidencia Técnica Verificable

### Backend Tests
- Tests unitarios de servicios (UserService)
- Tests de integración de controladores (MockMvc)
- Validación de: registro exitoso, login exitoso, email duplicado (409), refresh exitoso, refresh con token revocado (401)

### Frontend Tests
- 86 tests (Vitest) cubriendo servicios y schemas

## 7. Flujo Funcional Resumido

```
1. Usuario se registra → POST /api/users/register → 201 Created
2. Usuario inicia sesión → POST /api/users/login → accessToken + cookie refreshToken
3. Cada request autenticado lleva Authorization: Bearer {accessToken}
4. Si expira → interceptor Axios llama POST /api/users/refresh automáticamente
5. Usuario cierra sesión → POST /api/users/logout → refresh token revocado
```

## 8. Limitaciones y Trabajo Pendiente

- No hay recuperación de contraseña (para futura iteración)
- No hay MFA (para versión premium)

## 9. Conclusión

El Sprint 1 sienta las bases de autenticación sólida con JWT real, refresh token rotado y manejo de sesiones seguro. Todo el flujo está verificado con tests y conectado end-to-end entre frontend y backend.
