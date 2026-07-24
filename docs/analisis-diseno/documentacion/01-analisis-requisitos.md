# Análisis de Requisitos — KoroFin

> **Propósito:** Definir los requisitos funcionales y no funcionales que guían el desarrollo de KoroFin.

---

## 1. Visión del Producto

KoroFin es una plataforma web inteligente de gestión financiera personal que permite a los usuarios registrar, visualizar y analizar sus ingresos, gastos, deudas y servicios recurrentes. Integra un asistente IA multi-proveedor que ofrece recomendaciones personalizadas, predicciones y clasificación automática de gastos.

---

## 2. Requisitos Funcionales

### 2.1 Autenticación y Usuarios

| ID | Requisito | Prioridad | Sprint |
|----|-----------|-----------|--------|
| RF-01 | El sistema debe permitir registro de usuarios con email único y contraseña segura | Alta | 1 |
| RF-02 | El sistema debe autenticar usuarios mediante JWT (access token + refresh token rotado) | Alta | 1 |
| RF-03 | El sistema debe permitir cierre de sesión con revocación de tokens | Alta | 1 |
| RF-04 | El sistema debe renovar access tokens automáticamente via refresh token | Alta | 1 |
| RF-05 | El usuario debe poder actualizar su perfil (nombre, email) | Media | 6 |
| RF-06 | El usuario debe poder cambiar su contraseña (verificando la actual) | Alta | 6 |

### 2.2 Gestión de Categorías

| ID | Requisito | Prioridad | Sprint |
|----|-----------|-----------|--------|
| RF-07 | El usuario debe poder crear categorías personalizadas (tipo INCOME / EXPENSE) | Alta | 2 |
| RF-08 | El usuario debe poder editar y eliminar sus categorías | Alta | 2 |
| RF-09 | Las categorías deben filtrarse por tipo al asociarlas a ingresos o gastos | Alta | 2 |

### 2.3 Ingresos

| ID | Requisito | Prioridad | Sprint |
|----|-----------|-----------|--------|
| RF-10 | El usuario debe poder registrar ingresos con monto, fecha, fuente y categoría | Alta | 2 |
| RF-11 | El usuario debe poder editar y eliminar ingresos existentes | Alta | 2 |
| RF-12 | El sistema debe listar ingresos con paginación y filtros por mes y fuente | Alta | 2 |

### 2.4 Gastos

| ID | Requisito | Prioridad | Sprint |
|----|-----------|-----------|--------|
| RF-13 | El usuario debe poder registrar gastos con monto, fecha, categoría, método de pago y descripción | Alta | 2 |
| RF-14 | El usuario debe poder editar y eliminar gastos | Alta | 2 |
| RF-15 | El sistema debe listar gastos con paginación y filtros por categoría, fecha y método de pago | Alta | 2 |

### 2.5 Deudas

| ID | Requisito | Prioridad | Sprint |
|----|-----------|-----------|--------|
| RF-16 | El usuario debe poder registrar deudas con nombre, monto total, tasa de interés y fecha de vencimiento | Alta | 3 |
| RF-17 | El usuario debe poder registrar abonos a una deuda, que actualicen el saldo restante automáticamente | Alta | 3 |
| RF-18 | El sistema debe registrar cada abono en un historial con fecha y monto | Alta | 3 |
| RF-19 | Al registrar un abono, el sistema debe generar un gasto vinculado automáticamente | Media | 3 |

### 2.6 Servicios Recurrentes

| ID | Requisito | Prioridad | Sprint |
|----|-----------|-----------|--------|
| RF-20 | El usuario debe poder registrar pagos recurrentes con frecuencia (mensual/semanal), monto y próxima fecha de pago | Alta | 3 |
| RF-21 | El usuario debe poder activar/desactivar un servicio recurrente | Alta | 3 |
| RF-22 | Al marcar un servicio como pagado, el sistema debe generar un gasto y recalcular la próxima fecha | Alta | 3 |

### 2.7 Motor Financiero

| ID | Requisito | Prioridad | Sprint |
|----|-----------|-----------|--------|
| RF-23 | El sistema debe calcular el balance mensual (ingresos - gastos) por usuario y período | Alta | 4 |
| RF-24 | El sistema debe calcular el porcentaje de gasto sobre ingreso, alertando si supera el 80% | Alta | 4 |
| RF-25 | El sistema debe calcular el nivel de endeudamiento (deudas activas vs ingresos) | Alta | 4 |
| RF-26 | El sistema debe calcular el ahorro mensual real y la proyección anual | Media | 4 |
| RF-27 | El sistema debe identificar el top de categorías de gasto por monto mensual | Alta | 4 |
| RF-28 | El sistema debe generar recomendaciones automáticas basadas en reglas de negocio | Alta | 4 |
| RF-29 | El sistema debe predecir el saldo proyectado a fin de mes | Alta | 5 |

### 2.8 Asistente IA

| ID | Requisito | Prioridad | Sprint |
|----|-----------|-----------|--------|
| RF-30 | El sistema debe integrar un chat con IA que conozca el contexto financiero del usuario | Alta | 5 |
| RF-31 | El sistema debe soportar múltiples proveedores de IA con failover automático | Alta | 5 |
| RF-32 | El sistema debe generar insights financieros personalizados vía IA | Alta | 5 |
| RF-33 | El sistema debe clasificar gastos automáticamente por IA según su descripción | Media | 5 |
| RF-34 | El sistema debe exponer el estado de los proveedores IA (sin filtrar API keys) | Media | 5 |

### 2.9 Notificaciones

| ID | Requisito | Prioridad | Sprint |
|----|-----------|-----------|--------|
| RF-35 | El sistema debe notificar al usuario sobre vencimientos próximos (3-5 días antes) | Alta | 5 |
| RF-36 | El sistema debe alertar si el usuario supera el 80% de gasto sobre ingreso en un período | Alta | 5 |
| RF-37 | El sistema debe enviar resúmenes semanales de actividad financiera | Media | 5 |
| RF-38 | El usuario debe poder configurar preferencias de notificación (in-app y email) | Media | 5 |

### 2.10 Reportes

| ID | Requisito | Prioridad | Sprint |
|----|-----------|-----------|--------|
| RF-39 | El sistema debe generar reportes mensuales con breakdown completo de ingresos y gastos | Alta | 6 |
| RF-40 | El sistema debe permitir exportar movimientos a CSV | Alta | 6 |
| RF-41 | El sistema debe exponer la lista de movimientos del período para visualización en pantalla | Alta | 6 |

### 2.11 Tarjetas de Crédito y Cuotas (Fase SaaS — Fase B)

| ID | Requisito | Prioridad | Sprint |
|----|-----------|-----------|--------|
| RF-42 | El usuario debe poder registrar tarjetas de crédito con cupo, tasa mensual efectiva, día de corte y día de pago | Alta | Fase B (SaaS) |
| RF-43 | El sistema debe registrar movimientos de tarjeta (compras, pagos) como asientos inmutables que actualicen el saldo de forma atómica | Alta | Fase B (SaaS) |
| RF-44 | El usuario debe poder diferir una compra a cuotas, con la tasa congelada al momento de la compra aunque la tasa de la tarjeta cambie después | Alta | Fase B (SaaS) |
| RF-45 | El sistema debe cerrar el ciclo de facturación de cada tarjeta automáticamente, materializando los intereses de las cuotas pendientes como un movimiento agregado | Alta | Fase B (SaaS) |

### 2.12 Cargos de Deuda, Cuota y Telemetría de IA (Sprint 1 SaaS)

| ID | Requisito | Prioridad | Sprint |
|----|-----------|-----------|--------|
| RF-46 | El sistema debe permitir registrar cargos sobre una deuda (compras) que incrementen el saldo restante, como espejo de los abonos | Media | Sprint 1 (SaaS) |
| RF-47 | El sistema debe limitar los mensajes de chat IA por usuario a un tope mensual configurable, reiniciado por período calendario | Alta | Sprint 1 (SaaS) |
| RF-48 | El sistema debe registrar telemetría de cada intento de llamada a un proveedor de IA (éxito/fallo, tokens, costo estimado, latencia) para reporting y futuro billing | Media | Sprint 1 (SaaS) |

### 2.13 Extractos Bancarios e Integración con Telegram (Sprint 2 SaaS)

| ID | Requisito | Prioridad | Sprint |
|----|-----------|-----------|--------|
| RF-49 | El usuario debe poder importar un extracto bancario (PDF o Excel, con contraseña opcional) y previsualizar los movimientos detectados por IA antes de confirmarlos | Alta | Sprint 2 (SaaS) |
| RF-50 | El sistema debe permitir vincular una cuenta de Telegram al usuario mediante un código de un solo uso | Alta | Sprint 2 (SaaS) |
| RF-51 | El usuario debe poder registrar gastos por mensaje de texto o foto de recibo desde el bot de Telegram, con clasificación y extracción automática por IA | Alta | Sprint 2 (SaaS) |

---

## 3. Requisitos No Funcionales

| ID | Requisito | Categoría |
|----|-----------|-----------|
| RNF-01 | El sistema debe responder las APIs REST en menos de 500ms (p95) para operaciones CRUD simples | Rendimiento |
| RNF-02 | El sistema debe soportar autenticación stateless via JWT | Seguridad |
| RNF-03 | Los refresh tokens deben almacenarse hasheados en BD y rotarse en cada uso | Seguridad |
| RNF-04 | El sistema debe usar Flyway para migraciones de base de datos versionadas | Mantenibilidad |
| RNF-05 | El frontend debe ser responsivo (desktop y mobile) | UX |
| RNF-06 | El sistema debe soportar modo oscuro y claro | UX |
| RNF-07 | Las API keys de IA deben configurarse solo vía variables de entorno del operador | Seguridad |
| RNF-08 | El failover entre proveedores IA debe ser transparente para el usuario final | Resiliencia |
| RNF-09 | Las notificaciones por email deben ser degradables (fallan sin afectar la app) | Resiliencia |
| RNF-10 | El código backend debe seguir Clean Architecture (Controller → Service → Repository) | Mantenibilidad |
| RNF-11 | Los endpoints deben tener validación con `@Valid` y mensajes de error en español | UX/Mantenibilidad |

---

## 4. Historias de Usuario (Ejemplos Representativos)

> Como **usuario financiero**, quiero **registrar mis gastos diarios** para **tener control de a dónde se va mi dinero**.

> Como **usuario financiero**, quiero **que el sistema me alerte antes de que venza un pago** para **evitar intereses por mora**.

> Como **usuario financiero**, quiero **preguntarle al asistente IA sobre mis finanzas** para **obtener recomendaciones personalizadas sin hacer cálculos manuales**.

> Como **usuario financiero**, quiero **ver un dashboard con mi situación financiera actual** para **tomar decisiones informadas rápidamente**.

---

## 5. Casos de Uso Principales

```
┌──────────────────────────────────────┐
│           KoroFin System              │
│  ┌─────────────────────────────────┐ │
│  │ Actor: Usuario (Autenticado)    │ │
│  └─────────────────────────────────┘ │
│                                      │
│  ┌─────────────────────────────────┐ │
│  │ UC-01: Registrarse en la plata- │ │
│  │        forma                    │ │
│  ├─────────────────────────────────┤ │
│  │ UC-02: Iniciar sesión           │ │
│  ├─────────────────────────────────┤ │
│  │ UC-03: Gestionar ingresos       │ │
│  ├─────────────────────────────────┤ │
│  │ UC-04: Gestionar gastos         │ │
│  ├─────────────────────────────────┤ │
│  │ UC-05: Gestionar deudas         │ │
│  ├─────────────────────────────────┤ │
│  │ UC-06: Gestionar servicios      │ │
│  │         recurrentes             │ │
│  ├─────────────────────────────────┤ │
│  │ UC-07: Ver dashboard financiero │ │
│  ├─────────────────────────────────┤ │
│  │ UC-08: Consultar asistente IA   │ │
│  ├─────────────────────────────────┤ │
│  │ UC-09: Configurar notificaciones│ │
│  ├─────────────────────────────────┤ │
│  │ UC-10: Generar reportes         │ │
│  └─────────────────────────────────┘ │
└──────────────────────────────────────┘
```

---

## 6. Matriz de Trazabilidad

La matriz cubre dos fases distintas del proyecto: el **MVP** (sprints 1 a 6, cerrado) y la **fase SaaS** post-MVP, que corre con su propia numeración de sprints (ver `docs/sprints/`) y su propio roadmap (`docs/roadmap-saas-cuentas-reales.md`).

### MVP (cerrado)

| Sprint | Requisitos Cubiertos | Entregable Principal |
|--------|---------------------|---------------------|
| 1 | RF-01 al RF-04 | Autenticación JWT + registro/login |
| 2 | RF-07 al RF-15 | CRUD de categorías, ingresos y gastos |
| 3 | RF-16 al RF-22 | Deudas con abonos, servicios recurrentes |
| 4 | RF-23 al RF-28 | Motor financiero, dashboard y recomendaciones |
| 5 | RF-29 al RF-38 | Asistente IA, notificaciones y automatizaciones |
| 6 | RF-05, RF-06, RF-39 al RF-41 | Reportes, exportación y configuración |

### Fase SaaS (post-MVP)

| Sprint | Requisitos Cubiertos | Entregable Principal | Estado |
|--------|---------------------|---------------------|--------|
| Fase B (tarjetas de crédito) | RF-42 al RF-45 | Dominio `tarjetas` completo: ledger de movimientos, cuotas con tasa congelada, cierre de ciclo | Implementado |
| Sprint 1 SaaS | RF-46 al RF-48 | Cargos de deuda (`DebtCharge`), cuota mensual de IA por usuario, telemetría de uso de IA | Implementado |
| Sprint 2 SaaS | RF-49 al RF-51 | Importación de extractos bancarios + bot de Telegram (n8n) para registrar gastos por chat/foto | Implementado y validado end-to-end (ver `docs/sprints/sprint2.md`) |
| Sprint 3 SaaS | — (propuesto, sin RF asignado todavía) | Integración de correo (Gmail API + Pub/Sub) para captura automática de movimientos bancarios | Propuesto, sin empezar (ver `docs/sprints/sprint3.md`) |

---

*Documento de requisitos — KoroFin*
