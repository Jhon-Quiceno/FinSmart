# Sprint 6 - Reportes, Pulido y Launch (FinSmart)

Este sprint cierra el MVP: reportes por periodo con exportacion, pulido de UX (empty states, manejo global de errores, responsividad mobile), la pagina de `/configuracion` completa (perfil, contrasena, preferencias), validaciones exhaustivas en el backend, indices de cierre y el empaquetado para deploy (Docker + variables de entorno por perfil). Es el ultimo sprint del tablero (14 tareas).

## Antes de empezar

La rama de trabajo de este sprint se crea a partir de `develop` (el Sprint 5 ya esta integrado en `develop`, y este sprint depende de todo lo anterior: motor financiero, IA, notificaciones):

```bash
git checkout develop
git checkout -b feature/sprint-6-reports-launch
```

## Objetivo

1. Reportes por periodo: endpoint mensual con breakdown completo (ingresos, gastos, deudas, ahorro, top categorias) y exportacion a CSV/JSON de todos los movimientos del periodo.
2. Pagina `/reportes` en el frontend: selector de periodo, graficos mensuales/anuales y descarga de CSV.
3. Pulido de UX final: empty states en todas las paginas, manejo global de errores `401`/`403`/`500` (redirigir a login si el token expiro), y revision de responsividad mobile con datos reales.
4. `/configuracion` completa: cambio de contrasena, preferencias de notificacion (ya persistidas en Sprint 5) y datos de perfil.
5. Robustez de backend: validaciones exhaustivas (`@Valid`) en todos los endpoints con mensajes de error claros en espanol, y smoke testing documentado con una coleccion de Postman.
6. Salud del esquema: indices de cierre restantes.
7. Empaquetado para deploy: Dockerizar Spring Boot (`Dockerfile` + `docker-compose` con PostgreSQL), separar configuracion `prod`/`dev`, y build de produccion de Next.js con `NEXT_PUBLIC_API_URL` apuntando a prod.

## Decisiones de arquitectura

1. **Reportes sobre el motor existente, sin nuevas tablas.** `GET /api/reports/monthly` reutiliza los servicios de ingresos, gastos, deudas y el `FinancialAnalysisService` (Sprint 4) para armar el breakdown; no se persiste un snapshot nuevo aparte del que ya produce `financial_analysis`. La exportacion (`GET /api/reports/export`) es un stream de los movimientos crudos del periodo, no un reporte agregado.
2. **Exportacion CSV como formato por defecto, JSON opcional.** El endpoint de export negocia por query param (`format=csv|json`, default `csv`), devuelve `Content-Disposition: attachment` con un nombre de archivo por periodo, y escribe el CSV via streaming para no cargar todo el periodo en memoria.
3. **Manejo global de errores en el cliente, no por pantalla.** El interceptor de axios centraliza `401`/`403`/`500`: `401` con refresh fallido -> redireccion a login limpiando el estado de sesion; `403` -> mensaje de permiso; `500` -> toast generico. Ninguna pagina maneja estos codigos por su cuenta.
4. **Validaciones declarativas en los DTOs.** Las validaciones exhaustivas se hacen con Bean Validation (`@Valid` + `@NotNull`/`@Positive`/`@Size`/`@Pattern`) en los DTOs de entrada; los mensajes en espanol se resuelven y agregan en el `GlobalExceptionHandler` ya existente (Sprint 1), sin logica de validacion dispersa en los services.
5. **Configuracion por perfil de Spring.** `application.properties` base + `application-dev.properties` + `application-prod.properties`; los secretos (DB, JWT, API keys de IA, Brevo) solo llegan por variables de entorno en `prod`, nunca hardcodeados. `docker-compose` levanta backend + PostgreSQL para un entorno reproducible.
6. **Empty states y responsividad como parte del DoD, no un extra.** Cada pagina con datos del backend debe mostrar un estado vacio explicito cuando no hay registros (no una tabla en blanco), y verificarse en viewport mobile con datos reales del usuario de desarrollo.

## Alcance del Sprint 6

### Backend

1. Endpoints de reportes por periodo — `GET /api/reports/monthly?month=X&year=Y` con breakdown completo (ingresos, gastos, deudas, ahorro, % gasto, top categorias del periodo).
2. Endpoint de exportacion — `GET /api/reports/export` -> CSV o JSON con todos los movimientos del periodo (ingresos + gastos), via streaming.
3. Dockerizar Spring Boot — `Dockerfile` (build multi-stage con Maven) + `docker-compose` con PostgreSQL.
4. Variables de entorno para produccion — `application.properties` separado para `prod`/`dev`, secretos solo por entorno.
5. Validaciones exhaustivas en todos los endpoints — `@Valid` en los DTOs de entrada, mensajes de error claros en espanol via `GlobalExceptionHandler`.
6. Smoke testing de todos los endpoints con una coleccion de Postman documentada.

### Base de datos

1. **Migracion `V11`** — indices de cierre restantes: `(debt_id, payment_date)` en `debt_payments`. Los indices de `expenses`/`incomes`/`recurring_payments`/`ai_messages` ya quedaron cubiertos por `V8` y `V10` en el Sprint 5.

### Frontend

1. Pagina `/reportes` conectada al backend — selector de periodo + graficos mensuales/anuales (Recharts) alimentados por `GET /api/reports/monthly`.
2. Funcionalidad de exportacion desde `/reportes` — boton descargar CSV contra `GET /api/reports/export`.
3. Manejo global de errores `401`/`403`/`500` — interceptor que redirige a login si el token expiro y muestra mensajes claros para el resto.
4. Empty states para todas las paginas cuando no hay datos registrados.
5. Pagina `/configuracion` — cambio de contrasena, preferencias de notificacion (persistidas en Sprint 5) y datos de perfil.
6. Revision de responsividad mobile en todas las paginas con datos reales.
7. Build de produccion Next.js — variable `NEXT_PUBLIC_API_URL` configurada para prod.

## Definicion de terminado (DoD)

1. `GET /api/reports/monthly` devuelve el breakdown completo del periodo pedido, filtrado siempre por `userId`, consistente con lo que muestra el dashboard para ese mes.
2. `GET /api/reports/export` descarga un CSV (o JSON con `format=json`) con todos los movimientos del periodo, con `Content-Disposition` de attachment y nombre de archivo por periodo.
3. La pagina `/reportes` permite elegir periodo, ver los graficos con datos reales y descargar el CSV desde el navegador.
4. Un token expirado o invalido redirige a login limpiando la sesion; `403` y `500` muestran mensajes claros en espanol, no stack traces.
5. Todas las paginas con datos del backend muestran un empty state explicito cuando no hay registros.
6. `/configuracion` permite cambiar la contrasena, editar datos de perfil y ver/editar las preferencias de notificacion persistidas.
7. Todos los endpoints de entrada validan con `@Valid` y devuelven mensajes claros en espanol ante datos invalidos.
8. La coleccion de Postman corre de punta a punta (smoke) contra el backend dockerizado sin errores.
9. `docker-compose up` levanta backend + PostgreSQL y la app queda operativa con la configuracion de `dev`; la build de `prod` toma los secretos solo por variables de entorno.
10. Build de produccion de Next.js en verde con `NEXT_PUBLIC_API_URL` de prod; responsividad mobile verificada en todas las paginas.
11. Migracion `V11` verificada contra Postgres real; `mvnw test` y suite frontend (test/lint/build) en verde.
12. Probado end-to-end con el usuario de desarrollo Jhon Quiceno (`user_id = 2`).

## Referencia de endpoints (Sprint 6)

```http
# Reportes
GET    /api/reports/monthly?month=X&year=Y
GET    /api/reports/export?month=X&year=Y&format=csv
```

## Variables de entorno nuevas

```properties
# Perfil activo de Spring (dev por defecto en local, prod en deploy)
SPRING_PROFILES_ACTIVE=dev

# Frontend: URL del backend en produccion
NEXT_PUBLIC_API_URL=
```
