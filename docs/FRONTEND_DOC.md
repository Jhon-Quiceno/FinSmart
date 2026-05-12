# Documentación del Frontend: FinSmart (FinanceAI)

Este documento detalla la estructura, tecnologías y funcionalidades actuales del frontend de FinSmart.

## 🚀 Tecnologías Principales
- **Framework:** Next.js 16.2.0 (App Router)
- **Lenguaje:** TypeScript
- **Estilos:** Tailwind CSS v4
- **Componentes UI:** shadcn/ui (basado en Radix UI)
- **Iconos:** Lucide React
- **Gestión de Estado/Auth:** React Context API
- **Gráficos:** Recharts

## 📂 Estructura de Carpetas
```plaintext
smart-finance-frontend/
├── app/                # Rutas y páginas (App Router)
│   ├── asistente-ia/   # Interfaz de chat con IA
│   ├── configuracion/  # Ajustes de usuario
│   ├── deudas/         # Gestión de préstamos y deudas
│   ├── gastos/         # Registro y visualización de egresos
│   ├── ingresos/       # Registro y visualización de entradas
│   ├── login/          # Página de acceso
│   ├── registro/       # Página de creación de cuenta
│   ├── reportes/       # Análisis detallado y exportación
│   ├── servicios/      # Suscripciones y servicios recurrentes
│   ├── globals.css     # Estilos globales y variables de tema
│   ├── layout.tsx      # Layout raíz
│   └── page.tsx        # Dashboard principal
├── components/         # Componentes reutilizables
│   ├── dashboard/      # Widgets específicos del dashboard (Balance, Stats, Charts)
│   ├── layout/         # Navbar, Sidebar (Desktop/Mobile), AppLayout
│   ├── ui/             # Componentes base de shadcn (Button, Input, Card, etc.)
│   └── ...             # Componentes específicos por dominio (debts, expenses, etc.)
├── contexts/           # Proveedores de contexto (AuthContext)
├── hooks/              # Hooks personalizados (useAuth, useMobile, useToast)
├── lib/                # Utilidades y configuración de clientes (utils.ts)
└── public/             # Activos estáticos (Logos, placeholders)
```

## 🛠️ Funcionalidades Implementadas (UI/UX)

### 1. Dashboard Principal (`/`)
- **Balance Card:** Resumen de saldo total con indicadores de cambio porcentual.
- **Stats Cards:** Desglose de ingresos mensuales, gastos, deudas y ahorros.
- **Gráficos Interactivos:** Comparativa de Ingresos vs. Gastos y distribución de Gastos por Categoría.
- **Alertas y Recomendaciones:** Panel de notificaciones financieras y sugerencias generadas por IA.
- **Transacciones Recientes:** Lista de los últimos movimientos financieros.

### 2. Sistema de Autenticación (`/login`, `/registro`)
- **AuthContext:** Maneja el estado global del usuario y la persistencia en `localStorage`.
- **Rutas Protegidas:** Redirección automática si un usuario no autenticado intenta acceder al dashboard.
- **Formularios Validados:** Registro con requisitos de contraseña (mayúsculas, números, longitud).

### 3. Navegación
- **Layout Responsivo:** Sidebar colapsable en desktop y menú hamburguesa en mobile.
- **Navbar Dinámica:** Muestra el perfil del usuario y notificaciones.
- **Tematización:** Soporte para modo oscuro/claro (ThemeProvider).

### 4. Módulos Temáticos (Estructura lista para lógica)
- **Gastos/Ingresos:** Páginas dedicadas para gestión detallada.
- **Asistente IA:** Espacio para interactuar con un consejero financiero inteligente.
- **Deudas:** Seguimiento de obligaciones financieras.
- **Servicios:** Gestión de suscripciones recurrentes.

## 📋 Estado Actual del Desarrollo
- **UI:** 90% Completada (Diseño moderno, responsivo y consistente).
- **Lógica de Frontend:** Implementada con datos simulados (*mock data*).
- **Conexión Backend:** Pendiente (Actualmente usa `localStorage` para simular persistencia).

---
*Documento generado automáticamente por Gemini CLI para la auditoría técnica de FinSmart.*
