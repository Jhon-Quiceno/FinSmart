# Arquitectura del Sistema — KoroFin

> **Propósito:** Describir la arquitectura general del sistema, las capas, los patrones utilizados y las decisiones técnicas fundamentales.

---

## 1. Visión General

KoroFin sigue una arquitectura **monolítica modular** con frontend y backend separados, comunicados via REST API. El backend implementa **Clean Architecture** con capas bien definidas, y el frontend usa **Next.js App Router** con componentes server-side y client-side según corresponda.

```
┌──────────────────────────────────────────────────────┐
│                   CLIENTE WEB                         │
│              Next.js 16 + TypeScript                  │
│   ┌──────────┐ ┌──────────┐ ┌────────────────────┐   │
│   │  Páginas  │ │Componente│ │  Contextos (Auth,  │   │
│   │  (App     │ │  shadcn  │ │  Notificaciones)   │   │
│   │  Router)  │ │  /Radix  │ │                    │   │
│   └──────────┘ └──────────┘ └────────────────────┘   │
│                      │ Axios (api-client)             │
│                      │ JWT en memoria + refresh auto  │
└──────────────────────┼───────────────────────────────┘
                       │ HTTPS / REST
┌──────────────────────┼───────────────────────────────┐
│                   BACKEND                             │
│           Spring Boot 4.0 + Java 21                   │
│                                                       │
│   ┌──────────┐  ┌──────────┐  ┌──────────────────┐   │
│   │ Controller│→│  Service  │→│   Repository     │   │
│   │ (REST)    │  │(Negocio) │  │ (Spring Data JPA)│   │
│   └──────────┘  └──────────┘  └──────────────────┘   │
│       │              │               │                │
│   ┌───┴───┐   ┌──────┴──────┐  ┌────┴────┐          │
│   │ DTOs  │   │   Mappers  │  │  Model  │           │
│   │       │   │  (MapStruct)│  │ (JPA    │           │
│   └───────┘   └─────────────┘  │ Entities)│          │
│                                └─────────┘           │
│   ┌──────────┐  ┌──────────┐  ┌──────────────────┐   │
│   │ Security │  │  Events  │  │       Jobs       │   │
│   │ (JWT)    │  │(Listener)│  │  (@Scheduled)    │   │
│   └──────────┘  └──────────┘  └──────────────────┘   │
│                                                       │
│   ┌──────────────────────────────────────────────┐   │
│   │  AI Chat Orchestrator (multi-proveedor)      │   │
│   │  ┌─────────┐ ┌──────────┐ ┌──────────────┐  │   │
│   │  │NVIDIA   │ │OpenCode  │ │ OpenRouter   │  │   │
│   │  │ NIM     │ │ Zen      │ │              │  │   │
│   │  └─────────┘ └──────────┘ └──────────────┘  │   │
│   └──────────────────────────────────────────────┘   │
└──────────────────────┬───────────────────────────────┘
                       │
┌──────────────────────┴───────────────────────────────┐
│                 BASE DE DATOS                         │
│              PostgreSQL 16                            │
│   Migraciones versionadas con Flyway (V1 a V12)      │
└──────────────────────────────────────────────────────┘
```

---

## 2. Arquitectura del Backend

### 2.1 Capas

| Capa | Responsabilidad | Tecnología |
|------|----------------|------------|
| **Controller** | Endpoints REST, validación de entrada, respuesta HTTP | `@RestController`, `@Valid` |
| **Service** | Lógica de negocio, orquestación, transacciones | `@Service`, `@Transactional` |
| **Repository** | Acceso a datos, consultas personalizadas | `JpaRepository`, `Specification` |
| **Model** | Entidades JPA que mapean a tablas | `@Entity`, relaciones |
| **DTO** | Objetos de transferencia para requests/responses | Records de Java 21 |
| **Mapper** | Transformación Entity ↔ DTO | MapStruct |
| **Exception** | Manejo global de errores con `@RestControllerAdvice` | Clases de error + DTOs estandarizados |

### 2.2 Principios Aplicados

- **Clean Architecture**: dependencias hacia adentro (Controller → Service → Repository)
- **DTOs como frontera**: las entidades JPA nunca se exponen directamente en la API
- **Transacciones declarativas**: `@Transactional` en servicios, no en controladores
- **Manejo centralizado de errores**: `GlobalExceptionHandler` con respuestas consistentes

### 2.3 Paquetes

```
com.smartfinance.backend/
├── config/          → Configuración (CORS, Security, OpenAPI)
├── controller/      → Controladores REST
├── dto/             → Records DTO por dominio
│   ├── ai/
│   ├── analysis/
│   ├── auth/
│   ├── category/
│   ├── debt/
│   ├── error/
│   ├── expense/
│   ├── income/
│   ├── notification/
│   ├── recurring/
│   └── report/
├── event/           → Eventos de dominio (ExpenseCreatedEvent)
├── exception/       → Jerarquía de excepciones + handler global
├── mapper/          → MapStruct mappers
├── model/           → Entidades JPA
├── repository/      → Repositorios + Specifications
├── security/        → Filtro JWT, implementación de seguridad
└── service/         → Servicios de negocio
    ├── ai/          → Lógica de IA multi-proveedor
    ├── jobs/        → Tareas programadas (@Scheduled)
    └── notification/→ Notificaciones in-app + email
```

---

## 3. Arquitectura del Frontend

### 3.1 Estructura

| Capa | Descripción |
|------|-------------|
| **Páginas (App Router)** | Rutas de Next.js, Server Components por defecto, Client Components solo con interactividad |
| **Componentes** | Componentes React por dominio (dashboard, expenses, debts, etc.) + shadcn/ui base |
| **Contextos** | AuthContext (manejo de sesión), NotificationContext (campana en navbar) |
| **Hooks** | Custom hooks para lógica reutilizable (API calls, caching) |
| **lib/** | Cliente HTTP (axios), utilidades |

### 3.2 Flujo de Autenticación en Frontend

```
1. Usuario ingresa credenciales → POST /api/users/login
2. Backend devuelve { accessToken, user } + cookie HttpOnly con refresh token
3. Frontend guarda accessToken EN MEMORIA (no localStorage)
4. Cada request usa axios interceptor para adjuntar accessToken
5. Si el accessToken expira → axios interceptor llama POST /api/users/refresh automáticamente
6. Si el refresh falla → se redirige a login
```

### 3.3 Características de UX

- Sidebar colapsable en desktop, menú hamburguesa en mobile
- Modo oscuro/claro vía `next-themes` + Tailwind CSS v4
- Gráficos interactivos con Recharts
- Toasts de notificación con sonner
- Loading skeletons mientras se cargan datos
- Estado vacío por página cuando no hay datos

---

## 4. Patrones y Decisiones Técnicas

### 4.1 Actualizaciones Atómicas

Para evitar condiciones de carrera en operaciones concurrentes, las actualizaciones críticas se implementan como `UPDATE` atómico condicional en el repositorio (no lectura-validación-escritura):

- **Deudas**: descontar `remaining_amount` al crear un `DebtPayment`
- **Servicios**: avanzar `next_payment_date` al pagar
- **Snapshots financieros**: `INSERT ... ON CONFLICT DO UPDATE`

### 4.2 Eventos de Aplicación

Los eventos de dominio se publican con `ApplicationEventPublisher` y se consumen con `@EventListener`:

- `ExpenseCreatedEvent` → verificar si hay sobregasto → crear notificación
- Los listeners de notificaciones usan `@TransactionalEventListener(AFTER_COMMIT)` + `REQUIRES_NEW` para evitar transacciones abortadas

### 4.3 Failover de IA

El `AiChatOrchestrator` implementa failover automático:

1. Intenta con el primer proveedor configurado (según prioridad)
2. Si falla, reintenta con el siguiente (hasta agotar todos)
3. Si todos fallan, devuelve error al usuario (el mensaje no se persiste)
4. El usuario nunca ve el cambio de proveedor — es transparente

### 4.4 Notificaciones Degradables

El sistema de email (Resend SMTP) está diseñado para fallar sin afectar la experiencia:

- Envío async con `@Async`
- Si no hay credenciales SMTP, solo funcionan notificaciones in-app
- El `BrevoEmailAdapter` captura excepciones y las registra sin propagarlas

---

## 5. Diagrama de Despliegue

```
┌─────────────────┐     ┌───────────────────────────────────┐
│   Usuario       │────→│   Frontend (Next.js)              │
│   (Navegador)   │     │   Puerto 3000                     │
└─────────────────┘     └──────────────┬────────────────────┘
                                       │ /api/* proxy
                                       │
┌──────────────────────────────────────┴────────────────────┐
│                   Docker Compose                           │
│  ┌─────────────────────┐  ┌────────────────────────────┐  │
│  │ Backend (Spring Boot)│  │ PostgreSQL 16              │  │
│  │ Puerto 8080          │  │ Puerto 5432                │  │
│  │ Perfiles: dev/prod   │  │ Volumen persistente        │  │
│  └─────────────────────┘  └────────────────────────────┘  │
└───────────────────────────────────────────────────────────┘
```

---

## 6. Stack Completo

| Componente | Tecnología | Versión |
|-----------|-----------|---------|
| Lenguaje Backend | Java | 21 |
| Framework Backend | Spring Boot | 4.0.7 |
| ORM | Spring Data JPA / Hibernate | — |
| Migraciones | Flyway | — |
| API Docs | SpringDoc OpenAPI | 3.0.2 |
| Mapper | MapStruct | 1.6.3 |
| Base de datos | PostgreSQL | 16 |
| Framework Frontend | Next.js | 16.2.10 |
| UI | React | 19.2.7 |
| Estilos | Tailwind CSS | 4.3.2 |
| Componentes | shadcn/ui (Radix UI) | — |
| Gráficos | Recharts | 2.15.0 |
| Formularios | React Hook Form + Zod | — |
| Cliente HTTP | Axios | 1.18.1 |
| Contenedores | Docker + docker-compose | — |

---

*Documento de arquitectura — KoroFin MVP*
