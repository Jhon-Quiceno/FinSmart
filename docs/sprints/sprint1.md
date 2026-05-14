# Sprint 1 - Autenticacion JWT end-to-end (FinSmart)

Este sprint redefine la base del sistema para dejar **registro/login/logout/refresh funcionales** entre backend y frontend, sin tokens stub.

## Objetivo

Entregar autenticacion completa con:

1. Access token JWT en memoria del cliente.
2. Refresh token en cookie `HttpOnly`.
3. Rotacion y revocacion de refresh token.
4. Frontend conectado a endpoints reales de auth.

## Alcance del Sprint 1

### Backend

1. Mantener `POST /api/users/register` y `POST /api/users/login`, devolviendo `accessToken`, `tokenType`, `expiresIn` y `user`.
2. Agregar `POST /api/users/refresh` para renovar access token y rotar refresh token.
3. Agregar `POST /api/users/logout` para revocar refresh token y limpiar cookie.
4. Implementar emision y validacion JWT real (access/refresh) en Spring Security.
5. Persistir refresh tokens en base de datos (hash + expiracion + revocado).
6. Configurar propiedades JWT en `application.properties` (issuer, expiraciones, cookie).
7. Mantener validaciones con anotaciones y manejo global de errores consistente.
8. Usar mapper para transformar entidad `User` a DTO de salida.

### Base de datos

1. Agregar migracion para tabla `refresh_tokens`.
2. Incluir indices por `user_id` y `expires_at`.

### Frontend

1. Ajustar `api-client` al nuevo contrato JWT.
2. Guardar solo usuario en `localStorage` y manejar access token en memoria.
3. Usar refresh automatico ante `401` con endpoint `/api/users/refresh`.
4. Ejecutar logout real contra `/api/users/logout`.
5. Mantener pantallas de login/registro conectadas a backend real.
6. Proteger rutas usando cookie de refresh en `proxy.ts`.

## Definicion de terminado (DoD)

1. Register/login/logout/refresh operativos con JWT real.
2. Refresh token guardado en cookie `HttpOnly`, sin exponerlo en JavaScript.
3. Access token renovable automaticamente desde frontend.
4. Seguridad stateless con filtro JWT para endpoints protegidos.
5. Migraciones y configuracion listas para correr en entornos dev/prod.
6. Pruebas de backend/frontend de autenticacion actualizadas.

## Referencia de endpoints de auth (Sprint 1)

```http
POST /api/users/register
POST /api/users/login
POST /api/users/refresh
POST /api/users/logout
```
