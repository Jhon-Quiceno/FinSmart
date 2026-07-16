# 📚 Documentación de KoroFin

> Plataforma inteligente de gestión financiera personal.
> Stack: Java 21 + Spring Boot 4 · PostgreSQL · Next.js 16 · TypeScript · Tailwind CSS v4

---

## Estructura de la Documentación

```
docs/
├── README.md                          ← Este archivo — índice general
├── analisis-diseno/                   ← Análisis, arquitectura y diseño del sistema
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
├── assets/                            ← Recursos estáticos compartidos (íconos, imágenes generales)
├── auditoria/                         ← Auditorías de arquitectura, BD y código
├── evidencias/                        ← Evidencias técnicas por sprint
│   ├── README.md                      ← Índice y guía de evidencias
│   └── capturas/                      ← Capturas de pantalla reales, por fecha y feature
├── video-evidencias/                  ← Video demo de la app (HyperFrames)
│   └── finsmart-demo/                 ← Composición + capturas reales + finsmart-demo.mp4
├── sprints/                           ← Planificación detallada por sprint (fase actual)
├── FRONTEND_DOC.md                    ← Documentación del frontend
├── notifications-future.md            ← Canales de notificación futuros
└── roadmap-saas-cuentas-reales.md     ← Roadmap estratégico: SaaS + cuentas/tarjetas reales
```

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

- 🎬 **Video demo:** [`video-evidencias/finsmart-demo/finsmart-demo.mp4`](video-evidencias/finsmart-demo/finsmart-demo.mp4) — recorrido de ~30s por login, registro, dashboard y los 8 módulos, con capturas reales de la aplicación.
- 🖼️ **Capturas reales:** [`evidencias/capturas/`](evidencias/capturas/) — screenshots por fecha y feature, referenciados desde las evidencias de sprint.
- 📐 **Diagramas de arquitectura:** [`analisis-diseno/diagramas/`](analisis-diseno/diagramas/) — fuente PlantUML + renders SVG.

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
| Ver un recorrido en video de la app | [`video-evidencias/finsmart-demo/`](video-evidencias/finsmart-demo/) |
| Consultar el plan de sprints de la fase actual | [`sprints/`](sprints/) |
| Consultar la estrategia de producto (SaaS + cuentas reales) | [`roadmap-saas-cuentas-reales.md`](roadmap-saas-cuentas-reales.md) |
| Ver evidencia de un sprint | [`evidencias/`](evidencias/) |
| Revisar las auditorías | [`auditoria/`](auditoria/) |

---

## Convenciones de esta Documentación

- Los documentos de análisis y diseño priorizan **conceptos sobre implementación**.
- Los diagramas PlantUML son la fuente de verdad visual; los renders en `renders/` se generan a partir de ellos.
- Las evidencias siguen el formato `YYYY-MM-DD_nombre-descriptivo.md` y cubren qué se hizo, por qué, y cómo se verificó.
- Las capturas de pantalla reales van en `evidencias/capturas/YYYY-MM-DD_nombre-feature/`, nombradas `pagina-anchoxalto.png`.
- Los documentos técnicos (código, configuraciones, endpoints) están en español neutro.

---

*Documentación generada para el proyecto KoroFin — Julio 2026*
