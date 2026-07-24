# Documentación del Frontend: KoroFin

Este documento detalla la estructura, tecnologías y funcionalidades actuales del frontend de KoroFin.

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
│   ├── configuracion/  # Perfil, contraseña, proveedores de IA (solo lectura),
│   │                   # integraciones (Telegram) y preferencias de notificación
│   ├── deudas/         # Deudas, abonos e historial
│   ├── gastos/         # Egresos (con sugerencia de categoría por IA)
│   ├── importar/       # Importar extracto bancario (PDF/CSV/XLSX) con IA + preview
│   ├── ingresos/       # Entradas
│   ├── login/          # Página de acceso
│   ├── registro/       # Página de creación de cuenta
│   ├── reportes/       # Análisis detallado y exportación (Sprint 6, ya cerrado)
│   ├── servicios/      # Servicios recurrentes (pagar/activar)
│   ├── tarjetas/       # Tarjetas de crédito (alta, compras, pagos, movimientos)
│   ├── layout.tsx      # Layout raíz
│   └── page.tsx        # Dashboard principal
├── components/
│   ├── cards/          # Tarjetas de crédito: alta, registrar compra/pago, movimientos
│   ├── dashboard/      # Balance, Stats, Charts, predicción fin de mes, insights IA
│   ├── layout/         # Navbar (campana de notificaciones real), Sidebar, AppLayout
│   ├── settings/       # Perfil, contraseña, proveedores IA (solo lectura),
│   │                   # integraciones (Telegram) y preferencias de notificación
│   ├── statement-import/ # Tabla de previsualización de movimientos importados
│   ├── ui/             # Componentes base de shadcn
│   └── ...             # Componentes por dominio (debts, expenses, incomes, ...)
├── contexts/           # AuthContext
├── hooks/              # use-incomes, use-expenses, use-debts, use-analysis,
│                       # use-notifications, use-ai, use-credit-cards, use-card-movements,
│                       # use-refresh-on-focus (sincronización al volver a la pestaña), ...
│                       # (caché Map + listeners)
├── lib/
│   ├── api-client.ts   # axios + auth + refresh + CSRF + errores en español
│   ├── services/       # *.service.ts por dominio (income, expense, debt,
│   │                   # analysis, notification, ai, telegram-integration,
│   │                   # statement-import, credit-card, ...)
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
- **Sincronización automática (`useRefreshOnFocus`):** el dashboard, gastos e ingresos se refrescan solos al volver a la pestaña (listener `visibilitychange`/`focus`, más un poll de respaldo cada 30s), para reflejar movimientos registrados desde afuera (ej. el bot de Telegram) sin recargar la página a mano.

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
- **Perfil y contraseña:** edición real de nombre/email (`ProfileCard`) y cambio de contraseña (`PasswordCard`) contra el backend; dejaron de ser "próximamente" desde el cierre del Sprint 6.
- **Proveedores de IA:** tarjeta de solo lectura con los 5 proveedores conocidos (Google Gemini, NVIDIA NIM, OpenCode Zen, OpenRouter, Groq), cuáles están configurados (vía variables de entorno del operador) y su orden de prioridad para el failover; sin alta, edición ni borrado desde la UI.
- **Integraciones (`IntegrationsCard`):** vínculo con el bot de Telegram — muestra si la cuenta ya está conectada y, si no, genera un código de un solo uso (vence a los 10 minutos) para enviarlo al bot con `/start <código>`. Una vez vinculado, el usuario puede registrar gastos/ingresos o consultar datos por chat (texto o foto de recibo).
- **Notificaciones:** toggles reales por tipo de alerta + email.

### 5. Importar extracto bancario (`/importar`)
- Subida de extracto del banco en PDF (con contraseña opcional, no se guarda), CSV o XLSX (máx. 10MB).
- La IA configurada extrae los movimientos; se muestra una tabla de previsualización editable (categoría por fila, selección de filas, aviso de posibles duplicados) antes de confirmar.
- Nada se persiste hasta confirmar; al confirmar, crea los ingresos/gastos seleccionados y enlaza a `/gastos` o `/ingresos`.

### 6. Módulos de datos
- **Ingresos/Gastos:** CRUD completo con filtros, paginación y totales; el formulario de gasto incluye "Sugerir categoría" con IA (`POST /api/ai/categorize`).
- **Categorías:** CRUD completo con alta rápida desde formularios.
- **Deudas:** CRUD, registro de abonos e historial por deuda.
- **Servicios:** CRUD, activar/desactivar y "marcar como pagado" (genera el gasto vinculado).
- **Tarjetas (`/tarjetas`):** CRUD de tarjetas de crédito, registro de compras y pagos, historial de movimientos por tarjeta.

### 7. Autenticación y navegación
- **AuthContext + rutas protegidas**, access token en memoria, refresh token en cookie HttpOnly.
- **Layout responsivo** con sidebar colapsable y menú mobile; modo oscuro/claro.

## 📋 Estado Actual del Desarrollo
- **Sprints 1-6 completados (MVP cerrado):** autenticación JWT real, CRUD de movimientos/deudas/servicios/tarjetas, motor financiero + dashboard real, asistente IA multi-proveedor (5 proveedores) + notificaciones + automatizaciones nativas, reportes con exportación, perfil/contraseña reales y manejo global de errores.
- **Sin datos mock:** todas las páginas operan contra el backend (Spring Boot, `NEXT_PUBLIC_API_URL`).
- **Post-MVP:** importación de extractos bancarios con IA (`/importar`) y vinculación con el bot de Telegram (`/configuracion` → Integraciones) para registrar y consultar movimientos por chat.

---
*Actualizado tras el sprint del bot de Telegram y extractos bancarios (fase SaaS; ver historial de commits para el detalle día a día).*
