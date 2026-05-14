# 💚 FinSmart — Plataforma Inteligente de Gestión Financiera Personal

> *Tu dinero, bajo control. Tu futuro, mejor planificado.*

---

## 📌 ¿Qué es FinSmart?

**FinSmart** es una plataforma web inteligente de gestión financiera personal diseñada para que cualquier persona tenga control total de su dinero desde un solo lugar. Integra ingresos, gastos diarios, pagos con tarjetas, deudas y servicios recurrentes en un ecosistema unificado.

Pero FinSmart no es solo un registro de datos.

Es un **asistente financiero inteligente** que analiza los hábitos del usuario, anticipa problemas financieros y ofrece recomendaciones personalizadas para mejorar su salud económica de forma proactiva.

---

## ⚠️ El Problema que Resolvemos

Millones de personas enfrentan estos problemas a diario:

- No saben con claridad en qué gastan su dinero
- Manejan múltiples medios de pago (efectivo, tarjetas, transferencias) sin un consolidado
- Olvidan fechas de pago de servicios, suscripciones y deudas
- No cuentan con herramientas que analicen su comportamiento financiero

**Las consecuencias son reales:**

| Problema | Consecuencia |
|----------|-------------|
| Gastos descontrolados | Dinero que "desaparece" sin justificación |
| Pagos olvidados | Intereses por mora y recargos innecesarios |
| Sin análisis de hábitos | Mala toma de decisiones financieras |
| Sin planificación | Imposibilidad de ahorrar o invertir |

---

## 💡 Nuestra Solución

FinSmart permite:

- **Registrar** ingresos, gastos, deudas y pagos recurrentes de manera simple
- **Llevar control** diario, mensual y anual del dinero en tiempo real
- **Gestionar servicios** como agua, luz, internet o suscripciones digitales
- **Recibir alertas inteligentes** antes de vencimientos de pagos
- **Analizar patrones de gasto** automáticamente mediante IA
- **Obtener recomendaciones personalizadas** para mejorar la salud financiera

---

## 🚀 Nuestro Diferenciador

La mayoría de aplicaciones financieras solo registran datos. **FinSmart va mucho más allá:**

| Otras apps | FinSmart |
|-----------|---------|
| Solo registra datos | Los registra **y los interpreta** |
| Solo alerta sobre vencimientos | **Anticipa problemas** antes de que ocurran |
| Solo muestra gastos | **Te dice cómo mejorar** |
| Reactiva | **Proactiva e inteligente** |

### Ejemplos de inteligencia en acción:

> *"Estás gastando el 35% de tu ingreso en comida. Podrías reducir un 10% sin afectar tu calidad de vida."*

> *"Si mantienes este ritmo de gasto, terminarás el mes con −$80. Te recomiendo reducir gastos en ocio."*

> *"Prioriza pagar la deuda X — tiene la tasa de interés más alta y te está costando más cada día."*

> *"Esta semana gastaste $350. Tu mayor categoría fue comida (40%). Podrías ahorrar ~$50 fácilmente."*

---

## 🧠 Componentes del Sistema

### 1️⃣ Registro Financiero Completo

El núcleo de la plataforma permite registrar y categorizar toda la actividad económica del usuario:

- **Ingresos:** fijos (salario) y variables (freelance, bonos, etc.)
- **Gastos:** clasificados por categorías personalizables
- **Deudas:** con seguimiento de monto total, saldo pendiente y tasa de interés
- **Pagos recurrentes:** servicios del hogar, suscripciones digitales, cuotas

---

### 2️⃣ Sistema de Alertas Inteligentes

Un motor de notificaciones que trabaja de forma continua y automatizada:

- **Recordatorios anticipados:** 3–5 días antes del vencimiento de cualquier pago
- **Alertas el mismo día** de vencimiento con información del impacto si no se paga
- **Notificaciones por exceso de gasto:** cuando se supera el presupuesto de una categoría
- **Alertas predictivas:** cuando el sistema detecta que el mes cerrará en negativo

*Canales de notificación (actuales y futuros):*
- 📧 Email
- 💬 WhatsApp (integración vía API)
- 🔔 Push notifications

---

### 3️⃣ Motor Financiero (El Cerebro del Sistema)

Un motor de cálculo automático que corre en tiempo real:

| Métrica | Descripción |
|---------|-------------|
| **Balance actual** | Ingresos totales menos gastos del período |
| **Ahorro mensual** | Proyección del ahorro disponible al cierre del mes |
| **% Gasto/Ingreso** | Porcentaje del ingreso destinado a gastos |
| **Nivel de endeudamiento** | Relación deuda/ingreso con semáforo de riesgo |
| **Categorías críticas** | Las categorías donde más se gasta vs. el promedio |
| **Predicción de fin de mes** | Estimación del saldo final basada en el ritmo actual |

**Lógica de alertas automáticas:**
```
Si gasto > 80% del ingreso   → ALERTA ROJA de sobregasto
Si deuda próxima a vencer    → NOTIFICACIÓN de recordatorio
Si gasto en categoría X > 30% → RECOMENDACIÓN de reducción
Si promedio diario es alto   → PREDICCIÓN de saldo negativo
```

---

### 4️⃣ IA Financiera — El Valor Principal 🤖

El asistente de inteligencia artificial integrado en FinSmart puede:

- **Detectar sobreconsumo** en categorías específicas comparando con históricos
- **Recomendar estrategias de ahorro** personalizadas según el perfil del usuario
- **Priorizar el pago de deudas** según tasa de interés y urgencia
- **Clasificar gastos automáticamente** (ej: "McDonald's" → Comida, "Uber" → Transporte)
- **Detectar gastos inusuales** que se salen del patrón histórico del usuario
- **Responder preguntas en lenguaje natural**, como:
  - *"¿Puedo gastarme $200 este fin de semana?"*
  - *"¿En qué estoy gastando más este mes?"*
  - *"¿Cómo puedo ahorrar para un viaje en 3 meses?"*

---

## ⚙️ Automatizaciones con n8n

FinSmart integra **n8n** como motor de automatización para potenciar la experiencia del usuario sin intervención manual:

| Automatización | Descripción | Impacto |
|---------------|-------------|---------|
| **Recordatorios de pagos** | Cron diario que consulta la BD y notifica pagos próximos | Elimina olvidos |
| **Alertas de sobregasto** | Trigger en cada nuevo gasto, compara con presupuesto | Frena gastos a tiempo |
| **Resumen semanal/mensual** | Reporte automático con ingresos, gastos y recomendación | Engagement continuo |
| **Motor de recomendaciones** | Consulta datos → construye contexto → envía a IA → recomendación | Consejo personalizado |
| **Predicción fin de mes** | Calcula ritmo actual y proyecta saldo final | Anticipación proactiva |
| **Clasificación de gastos** | Analiza descripción y asigna categoría automáticamente | Mejor UX |
| **Detección de anomalías** | Detecta gastos 3x mayores al promedio histórico | Seguridad financiera |
| **Seguimiento de metas** | Monitorea progreso hacia metas de ahorro definidas | Motivación al usuario |
| **Reactivación de usuarios** | Mensaje automático si hay más de 3–7 días sin actividad | Retención |

---

## 🎨 Frontend — Experiencia de Usuario

Construido con tecnologías modernas para una experiencia fluida y profesional:

### Stack de UI
- **Next.js 16.2** con App Router para rendimiento óptimo
- **TypeScript** para código robusto y mantenible
- **Tailwind CSS v4** para diseño responsivo y consistente
- **shadcn/ui** (basado en Radix UI) para componentes accesibles
- **Recharts** para visualización de datos financieros

### Pantallas y Módulos

| Módulo | Funcionalidades |
|--------|----------------|
| **Dashboard** | Balance general, stats cards, gráficos de ingresos vs gastos, alertas activas, transacciones recientes |
| **Ingresos** | Registro, categorización y visualización de entradas |
| **Gastos** | Registro detallado, filtros por categoría, método de pago y fecha |
| **Deudas** | Seguimiento de préstamos, intereses y pagos realizados |
| **Servicios** | Gestión de suscripciones y pagos recurrentes |
| **Reportes** | Análisis detallado, comparativas y exportación de datos |
| **Asistente IA** | Chat interactivo con el consejero financiero inteligente |
| **Configuración** | Preferencias del usuario, categorías personalizadas, notificaciones |

### Características de UX
- Diseño **responsivo** (desktop + mobile)
- Sidebar colapsable en desktop, menú hamburguesa en mobile
- Soporte para **modo oscuro / claro**
- Rutas protegidas con autenticación
- Gráficos interactivos con Recharts

---

## 🏗️ Arquitectura Técnica

### Stack Completo

```
┌─────────────────────────────────────────┐
│           FRONTEND (Next.js)            │
│   TypeScript · Tailwind · shadcn/ui     │
└────────────────────┬────────────────────┘
                     │ HTTP REST
┌────────────────────▼────────────────────┐
│         BACKEND (Spring Boot)           │
│         Java · Clean Architecture       │
│   Controller → Service → Repository     │
└──────────┬─────────────────┬────────────┘
           │                 │
┌──────────▼──────┐  ┌───────▼───────────┐
│   PostgreSQL    │  │      n8n           │
│   Base de datos │  │  Automatizaciones  │
└─────────────────┘  └───────┬───────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
         📧 Email      💬 WhatsApp    🤖 IA API
```

### Estructura del Backend (estado actual)

```
smart-finance-backend/src/main/java/com/smartfinance/backend/
├── config/          → Configuración de seguridad y OpenAPI
├── controller/      → Endpoints REST
├── dto/             → Objetos de transferencia de datos
├── exception/       → Manejo centralizado de errores
├── model/           → Entidades de dominio
├── repository/      → Acceso a datos con Spring Data JPA
└── service/         → Lógica de negocio
```

### Modelo de Base de Datos

```sql
users              → Perfiles de usuario
incomes            → Ingresos (fijos y variables)
expenses           → Gastos con categoría y medio de pago
categories         → Categorías personalizables por usuario
debts              → Deudas con interés y fecha de vencimiento
recurring_payments → Servicios y suscripciones recurrentes
notifications      → Historial de alertas enviadas
financial_analysis → Snapshots mensuales para análisis e IA
```

### API REST Principal

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/users/register` | Registro de usuario |
| POST | `/api/users/login` | Autenticación |

> Los endpoints de ingresos, gastos, deudas, pagos recurrentes y análisis financiero están planificados para próximas iteraciones.

---

## 📊 Estado Actual del Desarrollo

| Componente | Estado | Avance |
|-----------|--------|--------|
| UI / Frontend | ✅ Completado | 100% |
| Diseño responsivo | ✅ Completado | 100% |
| Autenticación UI | ✅ Completado | 100% |
| Dashboard con datos mock | ✅ Completado | 100% |
| Lógica de frontend | 🔄 En progreso | ~60% |
| Backend Spring Boot | 🔄 En progreso | En desarrollo |
| Base de datos PostgreSQL | 🔄 En progreso | Diseño listo |
| Motor financiero | 🔄 En progreso | Lógica definida |
| Integración n8n | 📋 Planificado | Diseño listo |
| IA financiera | 📋 Planificado | Arquitectura definida |
| JWT / Seguridad | ✅ Completado | 100% |

---

## 💰 Modelo de Negocio

FinSmart está diseñado con un modelo **Freemium** escalable:

### Plan Gratuito
- Registro de ingresos y gastos (hasta 50 movimientos/mes)
- Alertas básicas de vencimiento
- Dashboard con balance mensual
- Resumen semanal automático

### Plan Premium ($5–10 USD/mes)
- Movimientos ilimitados
- **IA financiera avanzada** (recomendaciones personalizadas)
- Análisis profundo de hábitos financieros
- Predicciones de fin de mes
- Reportes detallados y exportación
- Notificaciones por WhatsApp
- Metas de ahorro con seguimiento automático

### Escalabilidad Futura
- Integración con bancos (Open Banking)
- Asistente financiero tipo "coach" con sesiones personalizadas
- Recomendaciones de inversión basadas en perfil
- App móvil nativa (iOS / Android)
- Versión para PYMES y equipos

---

## 🎯 Propuesta de Valor Resumida

```
FinSmart = Registro Inteligente
         + Motor Financiero Automático
         + Alertas Predictivas
         + IA Personalizada
         + Automatización Total con n8n
```

**No es solo una app de finanzas. Es tu coach financiero personal disponible 24/7.**

---

## 👨‍💻 Tecnologías Utilizadas

| Capa | Tecnología |
|------|-----------|
| Frontend | Next.js 16 · TypeScript · Tailwind CSS v4 · shadcn/ui · Recharts |
| Backend | Java 21 · Spring Boot · Spring Data JPA · REST API |
| Base de datos | PostgreSQL |
| Automatización | n8n |
| Seguridad | JWT (fase futura) |
| Comunicaciones | Email · WhatsApp API (Twilio) |
| IA | Claude API / OpenAI API |
| DevOps | Docker (en implementación) |

---

## 📞 Contacto y Repositorio

> FinSmart es un proyecto en activo desarrollo, construido con visión de producto real y escalable.

*Desarrollado con pasión para transformar la manera en que las personas gestionan su dinero.*

---

*Documento generado como carta de presentación oficial de FinSmart — Plataforma Inteligente de Gestión Financiera Personal.*
