# Sprint 1 — Base del Sistema (FinSmart)

Este documento define el trabajo del **Sprint 1** tomando como fuente `docs/finsmart_mvp_sprints.md`.

## Rama de trabajo (obligatorio)

Antes de iniciar cualquier tarea del sprint:

```bash
git checkout develop
git pull origin develop
 git checkout -b feature/auth-base-system-setup
```

> Todo el trabajo de este sprint debe salir desde `develop` y ejecutarse en una rama dedicada.

## Uso de skills (obligatorio)

Durante la ejecución del Sprint 1 se deben usar las skills disponibles en la raíz del proyecto:

- `.agents`
- `.claude`
- `.atl`

Estas skills deben guiar la implementación, revisión técnica y calidad de los entregables del sprint.

## Objetivo del Sprint 1

Dejar lista la base técnica del sistema: estructura inicial backend/frontend, autenticación básica y esquema base de base de datos.

## Alcance detallado (13 tareas)

### Backend (6)

1. Setup del proyecto Spring Boot con estructura por capas (`config/controller/service/repository/model/dto/mapper/exception`).
2. Crear entidad `User`, `UserRepository` y `UserService` (registro y login básico).
3. Implementar `POST /api/users/register` con validaciones de email único y contraseña.
4. Implementar `POST /api/users/login` con respuesta de token/sesión (stub JWT para fase futura).
5. Implementar manejo global de excepciones (`GlobalExceptionHandler`) y DTOs de error estándar.
6. Configurar CORS para permitir peticiones desde el frontend Next.js.

### Base de datos (2)

1. Configurar PostgreSQL + Spring Data JPA + Flyway/Liquibase para migraciones.
2. Crear schema inicial con tablas: `users`, `categories`, `incomes`, `expenses`, `debts`, `recurring_payments`, `notifications`.

### Frontend (5)

1. Configurar cliente HTTP (axios) con interceptores para token de autenticación.
2. Reemplazar mock de `AuthContext` por llamadas reales a `POST /api/users/register` y `/api/users/login`.
3. Persistir JWT en `localStorage` y proteger rutas con middleware de Next.js.
4. Conectar pantalla de Login al backend con visualización de errores de validación del servidor.
5. Conectar pantalla de Registro al backend con feedback visual de éxito/error.

## Definición de terminado (DoD) del Sprint 1

- Registro y login funcionales extremo a extremo (frontend ↔ backend).
- Backend con manejo de errores y CORS configurado.
- Base de datos inicial versionada con migraciones.
- Frontend autenticando contra API real sin mocks.
- Trabajo realizado en rama `feature/auth-base-system-setup` basada en `develop`.
- Uso explícito de skills desde `.agents`, `.claude` y `.atl` durante la ejecución.
