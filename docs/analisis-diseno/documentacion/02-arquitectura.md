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
│   │  Gemini → NVIDIA → OpenCode → OpenRouter →   │   │
│   │  Groq (sin key real, catalogado e inerte)    │   │
│   └──────────────────────────────────────────────┘   │
└──────────────────────┬───────────────────────────────┘
                       │
┌──────────────────────┴───────────────────────────────┐
│                 BASE DE DATOS                         │
│              PostgreSQL 16                            │
│   Migraciones versionadas con Flyway (V1 a V25)      │
└──────────────────────────────────────────────────────┘
```

> El bot de Telegram (orquestado por n8n) y la importación de extractos bancarios son clientes adicionales de esta misma API — ver dominios `integraciones` y `extractos` en el árbol de paquetes (sección 2.3) y el detalle de endpoints en `04-api-rest.md`.

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

El backend **no** se organiza en paquetes técnicos planos (`config/`, `controller/`, `dto/`, ...) a nivel raíz. Se organiza **por dominio de negocio**, y cada dominio contiene sus propias capas técnicas (`controller/`, `service/`, `repository/`, `model/{dto,entity}`, `mapper/`) más lo que le haga falta:

```
com.smartfinance.backend/
├── common/          → Transversal: config (CORS, Security, OpenAPI, Clock, Async, Scheduling),
│                       excepciones globales, filtros de seguridad compartidos
│                       (JwtAuthenticationFilter, TelegramWebhookFilter, RateLimitFilter)
├── usuario/         → Autenticación, registro, perfil, refresh tokens
│                       Entidades: User, RefreshToken
├── ingresos/        → Ingresos del usuario
│                       Entidad: Income
├── gastos/          → Gastos y categorías
│                       Entidades: Expense, Category
├── deudas/          → Deudas, abonos y cargos
│                       Entidades: Debt, DebtPayment, DebtCharge
├── servicios/       → Pagos recurrentes, notificaciones, preferencias, jobs (@Scheduled)
│                       Entidades: RecurringPayment, Notification, NotificationPreference
├── analisis/        → Motor financiero (balance, ratios, predicción, snapshots)
│                       Entidad: FinancialAnalysis
├── ia/              → Asistente IA multi-proveedor, orquestador, chat, insights, categorización,
│                       cuota y telemetría de uso
│                       Entidades: AiMessage, AiUsageEvent
├── reportes/        → Reportes mensuales, movimientos, exportación CSV
│                       (sin entidad propia — lee de otros dominios)
├── integraciones/   → Vínculo e ingesta desde el bot de Telegram (orquestado por n8n)
│                       Entidad: TelegramLink
├── tarjetas/        → Tarjetas de crédito, ledger de movimientos, compras a cuotas
│                       Entidades: CreditCard, CardMovement, InstallmentPlan, Installment
└── extractos/       → Importación de extractos bancarios (PDF/Excel) con extracción por IA
                        (sin entidad propia — crea Expense/CardMovement directamente vía servicio)
```

Cada uno de los 12 dominios sigue internamente el mismo patrón de capas descrito en la sección 2.1 (`controller/ → service/ → repository/`, con `model/dto` y `model/entity` como frontera y `mapper/` para la transformación), en vez de compartir paquetes técnicos con el resto del proyecto.

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

El `AiChatOrchestrator` implementa failover automático sobre 5 proveedores posibles (Gemini, NVIDIA, OpenCode, OpenRouter, Groq — ver `06-ia-asistente.md` para el detalle completo):

1. Intenta con el primer proveedor configurado (según prioridad global o, si la operación lo define, según `app.ai.task-priority.<tarea>`)
2. Si falla, reintenta con el siguiente (hasta agotar todos)
3. Si todos fallan, devuelve error al usuario (el mensaje no se persiste)
4. El usuario nunca ve el cambio de proveedor — es transparente

### 4.4 Notificaciones Degradables

El sistema de email (Resend, vía `spring-boot-starter-mail`) está diseñado para fallar sin afectar la experiencia:

- Envío async con `@Async`
- Si no hay credenciales SMTP, solo funcionan notificaciones in-app
- `EmailNotificationSender` captura excepciones y las registra sin propagarlas

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

Ver también el diagrama visual actualizado en `../../diagramas.md` (sección 1, vista de arquitectura general estilo C4-Contenedores).

---

*Documento de arquitectura — KoroFin*
