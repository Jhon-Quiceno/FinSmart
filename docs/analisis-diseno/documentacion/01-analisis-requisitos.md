# Análisis de Requisitos — FinSmart

> **Propósito:** Definir los requisitos funcionales y no funcionales que guían el desarrollo de FinSmart.

---

## 1. Visión del Producto

FinSmart es una plataforma web inteligente de gestión financiera personal que permite a los usuarios registrar, visualizar y analizar sus ingresos, gastos, deudas y servicios recurrentes. Integra un asistente IA multi-proveedor que ofrece recomendaciones personalizadas, predicciones y clasificación automática de gastos.

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
| RF-25 | El sistema debe calcular el nivel de endeudamiento (deudas activas vs ingresos) | Ata | 4 |
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
│           FinSmart System            │
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

| Sprint | Requisitos Cubiertos | Entregable Principal |
|--------|---------------------|---------------------|
| 1 | RF-01 al RF-04 | Autenticación JWT + registro/login |
| 2 | RF-07 al RF-15 | CRUD de categorías, ingresos y gastos |
| 3 | RF-16 al RF-22 | Deudas con abonos, servicios recurrentes |
| 4 | RF-23 al RF-28 | Motor financiero, dashboard y recomendaciones |
| 5 | RF-29 al RF-38 | Asistente IA, notificaciones y automatizaciones |
| 6 | RF-05, RF-06, RF-39 al RF-41 | Reportes, exportación y configuración |

---

*Documento de requisitos — FinSmart MVP*
