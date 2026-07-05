# Documentación del Frontend: FinSmart (FinanceAI)

Este documento detalla la estructura, tecnologías y funcionalidades actuales del frontend de FinSmart.

## 🚀 Tecnologías Principales
- **Framework:** Next.js 16.2.0 (App Router)
- **Lenguaje:** TypeScript
- **Estilos:** Tailwind CSS v4
- **Componentes UI:** shadcn/ui (basado en Radix UI)
- **Iconos:** Lucide React
- **Gestión de Estado/Auth:** React Context API (auth) + hooks con caché propia (datos)
- **Formularios:** react-hook-form + zod
- **HTTP:** axios (`lib/api-client.ts`, access token en memoria + refresh automático + CSRF)
- **Gráficos:** Recharts
- **Toasts:** sonner
- **Tests:** Vitest (86 tests)

## 📂 Estructura de Carpetas
```plaintext
smart-finance-frontend/
├── app/                # Rutas y páginas (App Router)
│   ├── asistente-ia/   # Chat IA real (historial, proveedor/modelo) + insights
│   ├── categorias/     # CRUD de categorías
│   ├── configuracion/  # Estado de proveedores de IA (solo lectura) + preferencias de notificación
│   ├── deudas/         # Deudas, abonos e historial
│   ├── gastos/         # Egresos (con sugerencia de categoría por IA)
│   ├── ingresos/       # Entradas
│   ├── login/          # Página de acceso
│   ├── registro/       # Página de creación de cuenta
│   ├── reportes/       # Análisis detallado y exportación (Sprint 6)
│   ├── servicios/      # Servicios recurrentes (pagar/activar)
│   ├── layout.tsx      # Layout raíz
│   └── page.tsx        # Dashboard principal
├── components/
│   ├── dashboard/      # Balance, Stats, Charts, predicción fin de mes, insights IA
│   ├── layout/         # Navbar (campana de notificaciones real), Sidebar, AppLayout
│   ├── settings/       # Estado de proveedores IA (solo lectura) y preferencias de notificación
│   ├── ui/             # Componentes base de shadcn
│   └── ...             # Componentes por dominio (debts, expenses, incomes, ...)
├── contexts/           # AuthContext
├── hooks/              # use-incomes, use-expenses, use-debts, use-analysis,
│                       # use-notifications, use-ai, ... (caché Map + listeners)
├── lib/
│   ├── api-client.ts   # axios + auth + refresh + CSRF + errores en español
│   ├── services/       # *.service.ts por dominio (income, expense, debt,
│   │                   # analysis, notification, ai, ...)
│   ├── schemas/        # Schemas zod de formularios
│   └── types/          # Tipos 1:1 con los DTOs del backend
└── public/             # Activos estáticos
```

## 🛠️ Funcionalidades Implementadas (UI/UX)

### 1. Dashboard Principal (`/`)
- **Balance Card / Stats Cards:** datos reales de `GET /api/analysis/summary`.
- **Gráficos:** Ingresos vs. Gastos y distribución por categoría (serie real de 6 meses).
- **Alertas y Recomendaciones:** motor de reglas del backend (`/api/analysis/recommendations`).
- **Predicción fin de mes:** tarjeta con saldo proyectado, gasto máximo diario recomendado y alerta (`GET /api/analysis/prediction`).
- **Insights IA:** último insight generado por el proveedor de IA configurado + botón regenerar.
- **Transacciones Recientes:** últimos movimientos combinados reales.

### 2. Asistente IA (`/asistente-ia`)
- Chat real contra `POST /api/ai/chat`, con contexto financiero del usuario inyectado por el backend.
- Historial persistido (`GET /api/ai/chat/history`), burbuja optimista, indicador de escritura, badge de proveedor/modelo en cada respuesta.
- El chat está siempre disponible para el usuario final (no depende de que él configure nada); si el proveedor en uso falla, el backend reintenta con el siguiente de forma transparente. Solo si fallan todos los proveedores configurados (o no hay ninguno) se muestra un mensaje genérico de no disponibilidad, sin link accionable (la configuración de proveedores es responsabilidad del operador, vía `.env`, no del usuario). Errores puntuales del proveedor se normalizan en español conservando el texto escrito.
- Panel de insights financieros (mismo dato que el dashboard).

### 3. Notificaciones
- Campana del navbar con badge de no leídas (`GET /api/notifications/unread-count`, refresco periódico).
- Panel con listado, marcar leída y marcar todas (`PATCH .../read`, `/read-all`).
- Preferencias por tipo + email en `/configuracion` (persistidas en backend).

### 4. Configuración (`/configuracion`)
- **Proveedores de IA:** tarjeta de solo lectura con los 4 proveedores conocidos (Google Gemini, NVIDIA NIM, Groq, OpenRouter), cuáles están configurados (vía variables de entorno del operador) y su orden de prioridad para el failover; sin alta, edición ni borrado desde la UI.
- **Notificaciones:** toggles reales por tipo de alerta + email.
- Perfil/seguridad: marcados "próximamente" (Sprint 6).

### 5. Módulos de datos
- **Ingresos/Gastos:** CRUD completo con filtros, paginación y totales; el formulario de gasto incluye "Sugerir categoría" con IA (`POST /api/ai/categorize`).
- **Categorías:** CRUD completo con alta rápida desde formularios.
- **Deudas:** CRUD, registro de abonos e historial por deuda.
- **Servicios:** CRUD, activar/desactivar y "marcar como pagado" (genera el gasto vinculado).

### 6. Autenticación y navegación
- **AuthContext + rutas protegidas**, access token en memoria, refresh token en cookie HttpOnly.
- **Layout responsivo** con sidebar colapsable y menú mobile; modo oscuro/claro.

## 📋 Estado Actual del Desarrollo
- **Sprints 1-5 completados:** autenticación JWT real, CRUD de movimientos, deudas y servicios, motor financiero + dashboard real, asistente IA multi-proveedor + notificaciones + automatizaciones nativas.
- **Sin datos mock:** todas las páginas operan contra el backend (Spring Boot, `NEXT_PUBLIC_API_URL`).
- **Pendiente (Sprint 6):** reportes con exportación, configuración de perfil/contraseña, manejo global de errores refinado y build de producción.

---
*Actualizado en Sprint 5 — ver `docs/sprints/sprint5.md` y `docs/finsmart_mvp_sprints.md`.*
