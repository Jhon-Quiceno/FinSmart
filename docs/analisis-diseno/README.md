# 🏗️ Análisis y Diseño — KoroFin

> Documentación de arquitectura, modelo de datos, API, seguridad y diseño del asistente IA.

> **Actualizado 2026-07-23** al estado real del backend: 12 dominios, 19 entidades JPA, 5 proveedores de IA.

---

## Estructura

```
analisis-diseno/
├── README.md
├── documentacion/
│   ├── 01-analisis-requisitos.md   ← Requisitos funcionales y no funcionales
│   ├── 02-arquitectura.md          ← Arquitectura general del sistema
│   ├── 03-modelo-datos.md          ← Modelo de datos y relaciones
│   ├── 04-api-rest.md              ← Diseño de la API REST
│   ├── 05-seguridad.md             ← Diseño de seguridad y autenticación
│   └── 06-ia-asistente.md          ← Diseño del asistente IA multi-proveedor
└── diagramas/
    ├── puml/                        ← Diagramas fuente en PlantUML
    └── renders/                     ← Diagramas renderizados (PNG/SVG)
```

---

## Documentos

| Documento | Propósito |
|-----------|-----------|
| [01-analisis-requisitos.md](documentacion/01-analisis-requisitos.md) | Requisitos funcionales, no funcionales, historias de usuario y casos de uso |
| [02-arquitectura.md](documentacion/02-arquitectura.md) | Arquitectura general, patrones, capas y decisiones técnicas |
| [03-modelo-datos.md](documentacion/03-modelo-datos.md) | Modelo entidad-relación, migraciones Flyway y esquema de BD |
| [04-api-rest.md](documentacion/04-api-rest.md) | Diseño de endpoints, contratos y ejemplos de uso |
| [05-seguridad.md](documentacion/05-seguridad.md) | Autenticación JWT, autorización, CORS y manejo de sesiones |
| [06-ia-asistente.md](documentacion/06-ia-asistente.md) | Arquitectura multi-proveedor, orquestación y failover |

> Los dominios de extractos bancarios (`extractos`), integración con Telegram (`integraciones`) y tarjetas de crédito (`tarjetas`) están cubiertos dentro de los documentos anteriores (arquitectura, modelo de datos y API), no como documentos independientes.

---

## Diagramas

| Diagrama | Archivo PLANTUML |
|----------|------------------|
| Arquitectura general del sistema | [`diagramas/puml/arquitectura-general.puml`](diagramas/puml/arquitectura-general.puml) |
| Modelo de datos (ER) | [`diagramas/puml/modelo-datos.puml`](diagramas/puml/modelo-datos.puml) |
| Flujo de autenticación | [`diagramas/puml/flujo-autenticacion.puml`](diagramas/puml/flujo-autenticacion.puml) |
| Flujo IA multi-proveedor | [`diagramas/puml/flujo-ia-multiproveedor.puml`](diagramas/puml/flujo-ia-multiproveedor.puml) |

> Los diagramas se renderizan con PlantUML. Para regenerar las imágenes: `plantuml puml/*.puml -o ../renders/`
