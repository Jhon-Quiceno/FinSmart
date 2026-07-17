# 📚 Documentación de KoroFin

> Plataforma inteligente de gestión financiera personal.
> Stack: Java 21 + Spring Boot 4 · PostgreSQL · Next.js 16 · TypeScript · Tailwind CSS v4

---

## Estructura de la Documentación

```
docs/
├── README.md                          ← Este archivo — índice general
│
│  # Documentos vivos — fase SaaS actual
├── roadmap-saas-cuentas-reales.md     ← Roadmap estratégico: SaaS + cuentas/tarjetas reales
├── ideas-adicionales-producto.md      ← Funcionalidades investigadas fuera del roadmap actual
├── notifications-future.md            ← Canales de notificación futuros (Web Push, Telegram, WhatsApp)
├── sprints/                           ← Planificación detallada del sprint en curso
│
│  # Referencia técnica y de proceso
├── convenciones.md                    ← Commits, PRs, comentarios — obligatorio (citado en CLAUDE.md)
├── DESIGN.md                          ← Sistema de diseño visual (tokens, motion, dark mode)
├── runbook-produccion.md              ← Diagnóstico de fallas en producción
├── analisis-diseno/                   ← Análisis, arquitectura y diseño — ⚠️ desactualizado, ver nota abajo
│   ├── README.md                      ← Índice de análisis y diseño
│   ├── documentacion/
│   │   ├── 01-analisis-requisitos.md  ← Requisitos funcionales y no funcionales
│   │   ├── 02-arquitectura.md         ← Arquitectura general del sistema
│   │   ├── 03-modelo-datos.md         ← Modelo de datos y relaciones
│   │   ├── 04-api-rest.md             ← Diseño de la API REST
│   │   ├── 05-seguridad.md            ← Diseño de seguridad y autenticación
│   │   └── 06-ia-asistente.md         ← Diseño del asistente IA multi-proveedor
│   └── diagramas/
│       ├── puml/                      ← Diagramas fuente en PlantUML
│       └── renders/                   ← Diagramas renderizados (generados)
│
└── archivo/                           ← Histórico cerrado, no se toca (ver archivo/README.md)
    └── mvp/                           ← Evidencia, auditoría y demo del MVP (6 sprints, cerrado)
        ├── evidencias/
        ├── auditoria/
        ├── video-evidencias/
        └── FRONTEND_DOC.md
```

> ⚠️ **`analisis-diseno/` describe la arquitectura pre-refactor de dominios.**
> `02-arquitectura.md` todavía muestra paquetes planos (`config/, controller/, dto/...`); el
> backend real está organizado en 9 dominios (`common, usuario, ingresos, gastos, deudas,
> servicios, analisis, ia, reportes`) desde el refactor documentado en el roadmap. Sigue
> siendo el material más completo de requisitos/API/seguridad del repo, pero el diagrama de
> arquitectura y el modelo de datos (sin `DebtCharge`/`ai_usage_events`) necesitan una
> actualización antes de usarse como referencia de la estructura de paquetes actual.

---

## Propósito del Proyecto

**KoroFin** permite a cualquier persona tener control total de su dinero desde un solo lugar: ingresos, gastos, deudas, servicios recurrentes, reportes y un asistente IA que analiza hábitos financieros y ofrece recomendaciones personalizadas.

| Módulo | Descripción |
|--------|-------------|
| Dashboard | Balance general, gráficos, alertas y transacciones recientes |
| Ingresos | Registro y categorización de ingresos fijos y variables |
| Gastos | Registro detallado con filtros por categoría, método de pago y fecha |
| Deudas | Seguimiento de préstamos, intereses y abonos con historial |
| Servicios | Gestión de suscripciones y pagos recurrentes |
| Reportes | Análisis detallado, comparativas y exportación CSV |
| Asistente IA | Chat interactivo, insights, clasificación automática y predicciones |
| Configuración | Preferencias de notificación, perfil y estado de proveedores IA |

---

## Stack Tecnológico

| Capa | Tecnología |
|------|-----------|
| **Frontend** | Next.js 16.2, React 19, TypeScript, Tailwind CSS v4, shadcn/ui, Recharts |
| **Backend** | Java 21, Spring Boot 4.0, Spring Security, Spring Data JPA, Flyway |
| **Base de datos** | PostgreSQL 16 |
| **IA** | Multi-proveedor: NVIDIA NIM, OpenCode Zen, OpenRouter (OpenAI-compatible) |
| **Notificaciones** | In-app + Email vía Resend SMTP |
| **DevOps** | Docker, docker-compose |

---

## Documentación Visual

- 🎬 **Video demo del MVP:** [`archivo/mvp/video-evidencias/finsmart-demo/finsmart-demo.mp4`](archivo/mvp/video-evidencias/finsmart-demo/finsmart-demo.mp4) — recorrido de ~30s por login, registro, dashboard y los 8 módulos, con capturas reales de la aplicación.
- 🖼️ **Capturas reales del MVP:** [`archivo/mvp/evidencias/capturas/`](archivo/mvp/evidencias/capturas/) — screenshots por fecha y feature, referenciados desde las evidencias de sprint.
- 📐 **Diagramas de arquitectura:** [`analisis-diseno/diagramas/`](analisis-diseno/diagramas/) — fuente PlantUML + renders SVG (⚠️ ver nota de desactualización arriba).

---

## Guía Rápida

| Si querés... | Andá a... |
|-------------|-----------|
| Entender la arquitectura | [`analisis-diseno/documentacion/02-arquitectura.md`](analisis-diseno/documentacion/02-arquitectura.md) |
| Ver el modelo de datos | [`analisis-diseno/documentacion/03-modelo-datos.md`](analisis-diseno/documentacion/03-modelo-datos.md) |
| Explorar los endpoints | [`analisis-diseno/documentacion/04-api-rest.md`](analisis-diseno/documentacion/04-api-rest.md) |
| Entender la seguridad | [`analisis-diseno/documentacion/05-seguridad.md`](analisis-diseno/documentacion/05-seguridad.md) |
| Ver el diseño del asistente IA | [`analisis-diseno/documentacion/06-ia-asistente.md`](analisis-diseno/documentacion/06-ia-asistente.md) |
| Ver diagramas de arquitectura | [`analisis-diseno/diagramas/`](analisis-diseno/diagramas/) |
| Ver un recorrido en video del MVP | [`archivo/mvp/video-evidencias/finsmart-demo/`](archivo/mvp/video-evidencias/finsmart-demo/) |
| Consultar el plan de sprints de la fase actual | [`sprints/`](sprints/) |
| Consultar la estrategia de producto (SaaS + cuentas reales) | [`roadmap-saas-cuentas-reales.md`](roadmap-saas-cuentas-reales.md) |
| Ver ideas de producto fuera del roadmap actual | [`ideas-adicionales-producto.md`](ideas-adicionales-producto.md) |
| Ver convenciones de commits/PRs (obligatorio) | [`convenciones.md`](convenciones.md) |
| Diagnosticar una falla en producción | [`runbook-produccion.md`](runbook-produccion.md) |
| Ver evidencia de un sprint del MVP | [`archivo/mvp/evidencias/`](archivo/mvp/evidencias/) |
| Revisar las auditorías del MVP | [`archivo/mvp/auditoria/`](archivo/mvp/auditoria/) |

---

## Convenciones de esta Documentación

- Los documentos de análisis y diseño priorizan **conceptos sobre implementación**.
- Los diagramas PlantUML son la fuente de verdad visual; los renders en `renders/` se generan a partir de ellos.
- Las evidencias siguen el formato `YYYY-MM-DD_nombre-descriptivo.md` y cubren qué se hizo, por qué, y cómo se verificó.
- Las capturas de pantalla reales van en `evidencias/capturas/YYYY-MM-DD_nombre-feature/`, nombradas `pagina-anchoxalto.png`.
- Los documentos técnicos (código, configuraciones, endpoints) están en español neutro.

---

*Documentación generada para el proyecto KoroFin — Julio 2026*
