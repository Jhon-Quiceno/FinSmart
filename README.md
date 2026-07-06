<div align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="./smart-finance-frontend/public/icon-dark-32x32.png">
    <img src="./smart-finance-frontend/public/icon-light-32x32.png" alt="FinSmart Logo" width="80" height="80">
  </picture>

  <h1 align="center" style="font-size: 2.5rem; margin-top: 0.5rem;">FinSmart</h1>

  <p align="center">
    <strong>Plataforma Inteligente de Gestión Financiera Personal</strong>
    <br />
    <em>Tu dinero, bajo control. Tu futuro, mejor planificado.</em>
  </p>

  <br />

  <!-- Badges -->
  <a href="./smart-finance-frontend/package.json"><img src="https://img.shields.io/badge/Next.js-16.2-black?style=flat&logo=next.js" alt="Next.js 16.2" /></a>
  <a href="./smart-finance-frontend/package.json"><img src="https://img.shields.io/badge/React-19.2-%2300D8FF?style=flat&logo=react" alt="React 19" /></a>
  <a href="./smart-finance-frontend/package.json"><img src="https://img.shields.io/badge/TypeScript-5.7-%233178C6?style=flat&logo=typescript" alt="TypeScript 5.7" /></a>
  <a href="./smart-finance-backend/pom.xml"><img src="https://img.shields.io/badge/Java-21-%23ED8B00?style=flat&logo=openjdk" alt="Java 21" /></a>
  <a href="./smart-finance-backend/pom.xml"><img src="https://img.shields.io/badge/Spring_Boot-4.0-%236DB33F?style=flat&logo=springboot" alt="Spring Boot 4.0" /></a>
  <a href="./docker-compose.yml"><img src="https://img.shields.io/badge/PostgreSQL-16-%234169E1?style=flat&logo=postgresql" alt="PostgreSQL 16" /></a>
  <br />
  <a href="./smart-finance-frontend/package.json"><img src="https://img.shields.io/badge/Tailwind_CSS-v4-%2306B6D4?style=flat&logo=tailwindcss" alt="Tailwind CSS v4" /></a>
  <img src="https://img.shields.io/badge/IA-Multi_Provider-%234A90E2?style=flat" alt="Multi-Provider AI" />
  <img src="https://img.shields.io/badge/JWT-Auth-%23000000?style=flat&logo=jsonwebtokens" alt="JWT Auth" />
  <img src="https://img.shields.io/badge/n8n-Automation-%23EA4AAA?style=flat" alt="n8n Automation" />
  <br />
  <img src="https://img.shields.io/badge/version-0.2.0-%2322c55e?style=flat" alt="Version 0.2.0" />
  <img src="https://img.shields.io/badge/status-active_development-%2322c55e?style=flat" alt="Status" />
</div>

<br />

---

## 🚀 ¿Qué es FinSmart?

**FinSmart** no es solo un registro de gastos. Es tu **asistente financiero inteligente 24/7** que analiza tus hábitos, anticipa problemas y te da recomendaciones personalizadas para mejorar tu salud económica.

> Registra ingresos y gastos, controla deudas, gestiona servicios recurrentes, recibe alertas predictivas y consulta a un asistente IA que conoce TUS finanzas reales.

<br />

## ✨ De un vistazo

<table>
  <tr>
    <td align="center"><strong>📊 Dashboard</strong><br />Balance, gráficos, alertas</td>
    <td align="center"><strong>💰 Ingresos & Gastos</strong><br />Registro y categorización</td>
    <td align="center"><strong>📋 Deudas</strong><br />Seguimiento con intereses</td>
  </tr>
  <tr>
    <td align="center"><strong>🔄 Servicios</strong><br />Pagos recurrentes</td>
    <td align="center"><strong>🤖 Asistente IA</strong><br />Consejos sobre tus datos reales</td>
    <td align="center"><strong>📈 Reportes</strong><br />Análisis y exportación</td>
  </tr>
  <tr>
    <td align="center"><strong>⚙️ Configuración</strong><br />Preferencias y categorías</td>
    <td align="center"><strong>🔔 Alertas</strong><br />Recordatorios inteligentes</td>
    <td align="center"><strong>🔮 Predicciones</strong><br />Proyección de fin de mes</td>
  </tr>
</table>

<br />

## 🧠 El problema que resolvemos

Millones de personas no saben en qué gastan su dinero, manejan múltiples medios de pago sin consolidación, olvidan fechas de vencimiento y no tienen herramientas que analicen su comportamiento financiero. **FinSmart resuelve todo eso en un solo lugar.**

<br />

## 🏗️ Arquitectura

```
┌──────────────────────────────────────────────────────────┐
│                    FRONTEND (Next.js 16)                  │
│    TypeScript · Tailwind CSS v4 · shadcn/ui · Recharts    │
│    App Router · Server Components · React 19              │
└────────────────────────┬─────────────────────────────────┘
                         │  HTTP REST (Axios)
┌────────────────────────▼─────────────────────────────────┐
│                  BACKEND (Spring Boot 4.0)                 │
│       Java 21 · Clean Architecture · Spring Data JPA       │
│    ┌──────────┐  ┌──────────┐  ┌──────────┐              │
│    │Controller│→ │ Service  │→ │Repository│  + Mappers    │
│    └──────────┘  └──────────┘  └──────────┘              │
└──────────┬──────────────────────────────┬─────────────────┘
           │                              │
┌──────────▼──────┐             ┌─────────▼──────────────┐
│   PostgreSQL 16  │             │    Multi-Provider AI    │
│  + Flyway Migs   │             │  NVIDIA · OpenCode · OR │
└─────────────────┘             └────────────────────────┘
           │                              │
           │              ┌───────────────▼───────────────┐
           │              │     Automatización (n8n)       │
           │              │  Recordatorios · Alertas ·     │
           └──────────────►  Resúmenes · Predicciones     │
                          └───────────────────────────────┘
```

<br />

## 💎 Stack Tecnológico

### Frontend
| Tecnología | Versión | Para qué |
|---|---|---|
| [Next.js](https://nextjs.org/) | 16.2 | App Router, Server Components, React 19 |
| [TypeScript](https://www.typescriptlang.org/) | 5.7 | Tipado estático robusto |
| [Tailwind CSS](https://tailwindcss.com/) | v4 | Diseño utilitario moderno con OKLCH |
| [shadcn/ui](https://ui.shadcn.com/) | latest | Componentes accesibles sobre Radix UI |
| [Recharts](https://recharts.org/) | 2.15 | Visualización de datos financieros |
| [React Hook Form](https://react-hook-form.com/) + [Zod](https://zod.dev/) | latest | Formularios con validación tipada |
| [Lucide](https://lucide.dev/) | latest | Iconografía consistente |
| [date-fns](https://date-fns.org/) | 4.4 | Manipulación de fechas |
| [Sonner](https://sonner.emilkowal.ski/) | latest | Notificaciones toast |

### Backend
| Tecnología | Versión | Para qué |
|---|---|---|
| [Java](https://openjdk.org/) | 21 | LTS moderno con records, pattern matching |
| [Spring Boot](https://spring.io/) | 4.0.7 | Web MVC, Security, Data JPA, Validation, Mail |
| [SpringDoc OpenAPI](https://springdoc.org/) | 3.0.2 | Documentación interactiva de API |
| [Flyway](https://flywaydb.org/) | latest | Migraciones de base de datos versionadas |
| [MapStruct](https://mapstruct.org/) | 1.6.3 | Mapeo DTO ↔ Entidad en compile-time |
| [Lombok](https://projectlombok.org/) | latest | Reducción de boilerplate |
| [JJWT](https://github.com/jwtk/jjwt) | 0.12.6 | Autenticación con tokens JWT |

### Base de Datos
| Tecnología | Versión |
|---|---|
| [PostgreSQL](https://www.postgresql.org/) | 16 (Alpine) |
| Driver | 42.7.11 |

### IA & Automatización
| Tecnología | Uso |
|---|---|
| NVIDIA NIM | Proveedor principal de IA (Llama 3.1 70B) |
| OpenCode API | Fallback automático |
| OpenRouter | Fallback secundario (DeepSeek R1) |
| n8n | Automatización de workflows |
| Brevo SMTP | Envío de correos transaccionales |

### Infraestructura
| Tecnología | Uso |
|---|---|
| Docker Compose | Orquestación local (PostgreSQL + App) |
| Vercel Analytics | Analítica del frontend |

<br />

## 📦 Módulos del Sistema

### 🖥️ Frontend (9 páginas + layout)
| Ruta | Módulo | Funcionalidad |
|---|---|---|
| `/` | **Dashboard** | Balance general, gráficos ingreso/gasto, categorías, alertas, recomendaciones IA, predicción fin de mes, transacciones recientes |
| `/ingresos` | **Ingresos** | CRUD con categorización, fijos y variables |
| `/gastos` | **Gastos** | Registro detallado con filtros por categoría, método de pago y fecha |
| `/deudas` | **Deudas** | Seguimiento con interés, saldo pendiente, historial de pagos |
| `/servicios` | **Servicios** | Gestión de suscripciones y pagos recurrentes con acción "pagar ahora" |
| `/reportes` | **Reportes** | Análisis detallado, comparativas mensuales |
| `/asistente-ia` | **Asistente IA** | Chat contextual con los datos financieros reales del usuario |
| `/configuracion` | **Configuración** | Perfil, cambio de contraseña, categorías personalizadas, preferencias de notificación |
| `/login` · `/registro` | **Auth** | Autenticación y registro de usuarios |

### ⚙️ Backend (20+ controladores y servicios)

#### Módulo de Autenticación
- `UserController` → Registro, login, refresh token, perfil, cambio de contraseña
- `JwtService` → Access + Refresh tokens con cookies httpOnly
- `SecurityConfig` → Spring Security con JWT filter chain

#### Módulo Financiero
- `IncomeController` / `ExpenseController` → CRUD con especificaciones JPA
- `DebtController` / `DebtPaymentController` → Ciclo completo de deudas con abonos
- `RecurringPaymentController` → Servicios recurrentes con control de fechas

#### Módulo de Análisis
- `AnalysisController` → Resumen mensual, serie histórica, top categorías, predicción
- `ReportController` → Reportes con movimientos detallados
- `FinancialAnalysisService` → Cálculo de métricas y semáforo de riesgo

#### Módulo de IA
- `AiChatController` → Chat contextual con contexto financiero real
- `AiInsightController` → Insights y recomendaciones automáticas
- `AiCategorizationController` → Clasificación inteligente de gastos
- `AiChatOrchestrator` → Multi-provider router con failover automático
- `FinancialContextBuilder` → Construye el contexto financiero del usuario para la IA

#### Módulo de Notificaciones
- `NotificationController` → Historial y preferencias
- `PaymentReminderJob` → Recordatorios de pago (cron diario)
- `OverspendAlertListener` → Alerta inmediata al registrar gastos
- `WeeklySummaryJob` → Resumen semanal automático
- `InactivityReminderJob` → Reactivación tras inactividad
- `MonthEndPredictionJob` → Predicción de cierre de mes
- `EmailNotificationSender` → Envío de emails vía Brevo SMTP

### 💾 Base de Datos (12 tablas + migraciones Flyway)
```
users, incomes, expenses, categories, debts, debt_payments,
recurring_payments, notifications, notification_preferences,
financial_analysis, ai_messages, refresh_tokens
```

<br />

## 🤖 Multi-Provider AI — El Cerebro

El sistema de IA tiene **failover automático** entre proveedores para máxima disponibilidad:

```
                    ┌──────────────────┐
                    │  AI Provider     │
                    │  Registry        │
                    └────────┬─────────┘
                             │
              ┌──────────────┼──────────────┐
              ▼              ▼              ▼
      ┌────────────┐ ┌────────────┐ ┌────────────┐
      │  NVIDIA    │→│  OpenCode  │→│  OpenRouter │
      │  (primary) │ │  (fallback)│ │  (fallback) │
      └────────────┘ └────────────┘ └────────────┘
              │              │              │
              └──────────────┼──────────────┘
                             ▼
                    ┌──────────────────┐
                    │  Financial       │
                    │  Context Builder │
                    └──────────────────┘
```

Cada mensaje al asistente incluye **datos financieros reales del usuario** (balance, gastos recientes, deudas próximas), lo que permite respuestas contextualizadas y precisas.

<br />

## 🔧 Cómo empezar

### Prerrequisitos

- **Node.js** ≥ 20 + **pnpm**
- **JDK 21** + **Maven**
- **Docker Desktop** (para PostgreSQL)
- Opcional: claves de API para los proveedores de IA

### 1. Clonar e instalar

```bash
# Frontend
cd smart-finance-frontend
pnpm install

# Backend
cd ../smart-finance-backend
./mvnw clean install -DskipTests
```

### 2. Configurar variables de entorno

```bash
cp .env.example .env
# Editar .env con tus valores (DB, JWT, APIs de IA)
```

### 3. Arrancar base de datos

```bash
docker compose up db -d
```

### 4. Ejecutar

```bash
# Terminal 1 — Backend
cd smart-finance-backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Terminal 2 — Frontend
cd smart-finance-frontend
pnpm dev
```

La aplicación estará disponible en `http://localhost:3000` y la API en `http://localhost:8080`.

### Documentación de la API

Con el backend corriendo, abre: [`http://localhost:8080/swagger-ui.html`](http://localhost:8080/swagger-ui.html)

<br />

## 🧪 Testing

```bash
# Frontend (Vitest)
cd smart-finance-frontend && pnpm test

# Backend (Spring Boot test slices)
cd smart-finance-backend && ./mvnw test
```

<br />

## 🗺️ Roadmap

- [x] **Sprint 1-3**: Core (auth, income, expenses, debts)
- [x] **Sprint 4**: Dashboard, gráficos, servicios recurrentes, UI completa
- [x] **Sprint 5**: Asistente IA multi-provider, notificaciones, jobs programados
- [x] **Sprint 6**: Reportes, configuración de perfil, pulido general
- [ ] **Sprint 7**: Integración bancaria (Open Banking)
- [ ] **Sprint 8**: App móvil nativa (iOS / Android)
- [ ] **Sprint 9**: Versión PYMES y equipos
- [ ] **Futuro**: Coach financiero con sesiones personalizadas, recomendaciones de inversión

<br />

## 📁 Estructura del Proyecto

```
FinSmart/
├── smart-finance-frontend/      ← Next.js 16 App Router
│   ├── app/                     ← Páginas (dashboard, ingresos, gastos, etc.)
│   ├── components/              ← Componentes React (ui, dashboard, layout, etc.)
│   ├── contexts/                ← AuthContext
│   ├── hooks/                   ← Custom hooks (use-analysis, use-ai, use-expenses...)
│   ├── lib/                     ← Utilidades, schemas Zod, servicios API
│   │   ├── schemas/             ← Validación Zod
│   │   ├── services/            ← Clientes HTTP con tests
│   │   └── types/               ← Tipos TypeScript
│   └── public/                  ← Assets estáticos, iconos SVG
│
├── smart-finance-backend/       ← Spring Boot 4.0 + Java 21
│   ├── src/main/java/.../backend/
│   │   ├── config/              ← Security, JWT, OpenAPI, Async, Scheduling
│   │   ├── controller/          ← 13 controladores REST
│   │   ├── dto/                 ← Request/Response objects
│   │   ├── exception/           ← Manejo global + excepciones de IA
│   │   ├── mapper/              ← MapStruct mappers
│   │   ├── model/               ← Entidades JPA
│   │   ├── repository/          ← Spring Data JPA repositories
│   │   ├── security/            ← SecurityUtils
│   │   └── service/             ← Lógica de negocio + IA + Jobs + Notificaciones
│   ├── src/main/resources/
│   │   ├── db/migration/        ← Flyway migrations
│   │   └── application.yml      ← Configuración multi-entorno
│   └── Dockerfile               ← Imagen Docker del backend
│
├── docker-compose.yml           ← Orquestación PostgreSQL + Backend
├── .env / .env.example          ← Variables de entorno
└── docs/                        ← Documentación técnica y sprints
```

<br />

## 📊 Estado del Desarrollo

| Componente | Estado |
|---|---|
| Frontend — UI/UX completa | ✅ 100% |
| Frontend — Lógica de negocio | 🔄 ~90% |
| Backend — API REST | ✅ 100% |
| Backend — Autenticación JWT | ✅ 100% |
| Backend — Motor financiero | ✅ 100% |
| Backend — IA multi-provider | ✅ 100% |
| Backend — Notificaciones y jobs | ✅ 100% |
| Base de datos — Migraciones Flyway | ✅ 100% |
| Automatización n8n | 📋 Planificado |
| Open Banking | 📋 Planificado |
| App móvil | 📋 Planificado |
| Tests automatizados | 🔄 En progreso |

<br />

## 💰 Modelo de Negocio

**Freemium** escalable:

| Plan Gratuito | Plan Premium ($5–10/mes) |
|---|---|
| Hasta 50 movimientos/mes | Movimientos ilimitados |
| Alertas básicas | IA financiera avanzada |
| Dashboard mensual | Reportes detallados + exportación |
| Resumen semanal | Notificaciones WhatsApp |
| | Metas de ahorro automáticas |
| | Análisis profundo de hábitos |

<br />

---

<div align="center">
  <sub>
    Construido con ❤️ y mucha 🧠 para transformar la manera en que las personas gestionan su dinero.
    <br />
    <strong>FinSmart</strong> — <em>No es solo una app de finanzas. Es tu coach financiero personal.</em>
  </sub>
</div>
