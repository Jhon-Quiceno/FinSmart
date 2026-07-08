# DESIGN.md — Sistema de Diseño FinSmart

Documento fuente de verdad del sistema visual de FinSmart, resultado del refactor de diseño en `feature/visual-polish-motion`. Pensado para portarse a desarrollo móvil (React Native / Flutter) manteniendo consistencia de marca.

## 1. Identidad

- **Nombre:** FinSmart — Gestión Financiera Inteligente
- **Logo:** `public/logo_finsmart.svg` (ícono de línea de tendencia ascendente, verde)
- **Tono visual:** fintech "evolución pulida" — verde de marca sobre superficies neutras, sin gradientes genéricos de IA. Motion sutil, nunca protagonista.

## 2. Color — tokens OKLCH

Todos los colores están definidos como variables CSS en `oklch(L C H)` y mapeados vía `@theme inline` (`app/globals.css`). Dos temas completos: claro (`:root`) y oscuro (`.dark`).

**Regla de contraste:** el verde primario tiene lightness distinto por tema — `L=0.60` en claro, `L=0.75` en oscuro — mismo hue (145). En `L=0.75` el verde falla AA como texto/borde sobre blanco (~2.4:1); en oscuro (fondo casi negro) sí funciona. Si se porta a un design system sin variables por tema, usar el valor de `:root` como base "on light" y el de `.dark` como "on dark", nunca uno solo para ambos.

| Token | Claro (`:root`) | Oscuro (`.dark`) | Uso |
|---|---|---|---|
| `background` | `oklch(0.99 0.004 260)` | `oklch(0.12 0.005 260)` | Fondo de página |
| `foreground` | `oklch(0.21 0.015 260)` | `oklch(0.95 0.01 260)` | Texto principal |
| `card` | `oklch(1 0 0)` | `oklch(0.16 0.008 260)` | Superficie de tarjetas |
| `card-foreground` | `oklch(0.21 0.015 260)` | `oklch(0.95 0.01 260)` | Texto sobre tarjetas |
| `popover` / `popover-foreground` | `oklch(1 0 0)` / `oklch(0.21 0.015 260)` | `oklch(0.14 0.006 260)` / `oklch(0.95 0.01 260)` | Dropdowns, tooltips |
| `primary` | `oklch(0.60 0.17 145)` | `oklch(0.75 0.18 145)` | Marca, CTAs, links activos |
| `primary-foreground` | `oklch(0.99 0.01 145)` | `oklch(0.15 0.02 145)` | Texto sobre `primary` |
| `secondary` / `secondary-foreground` | `oklch(0.96 0.006 260)` / `oklch(0.28 0.015 260)` | `oklch(0.22 0.01 260)` / `oklch(0.88 0.01 260)` | Botones secundarios |
| `muted` / `muted-foreground` | `oklch(0.96 0.006 260)` / `oklch(0.50 0.015 260)` | `oklch(0.20 0.008 260)` / `oklch(0.65 0.01 260)` | Fondos sutiles, texto secundario |
| `accent` / `accent-foreground` | `oklch(0.94 0.010 260)` / `oklch(0.24 0.015 260)` | `oklch(0.25 0.015 260)` / `oklch(0.95 0.01 260)` | Hover states |
| `destructive` / `destructive-foreground` | `oklch(0.58 0.22 25)` / `oklch(0.99 0.01 25)` | `oklch(0.65 0.22 25)` / `oklch(0.98 0.01 25)` | Errores, gastos, eliminar |
| `success` / `success-foreground` | `oklch(0.58 0.17 145)` / `oklch(0.99 0.01 145)` | `oklch(0.72 0.19 145)` / `oklch(0.15 0.02 145)` | Ingresos, confirmaciones |
| `warning` / `warning-foreground` | `oklch(0.70 0.15 75)` / `oklch(0.22 0.03 75)` | `oklch(0.78 0.16 75)` / `oklch(0.18 0.02 75)` | Alertas |
| `border` | `oklch(0.90 0.006 260)` | `oklch(0.28 0.01 260)` | Bordes |
| `input` | `oklch(0.92 0.006 260)` | `oklch(0.20 0.008 260)` | Fondos de inputs |
| `ring` | `oklch(0.60 0.17 145)` | `oklch(0.75 0.18 145)` | Focus ring |
| `chart-1..5` | verde / rojo / ámbar / azul-violeta / cian | (ver `globals.css`) | Series de gráficos (recharts) |
| `sidebar*` | tono ligeramente distinto de `background`/`accent` | ídem | Superficie de navegación lateral |

**Hue base:** 145 (verde marca), 260 (gris azulado neutro para toda la escala de grises), 25 (rojo), 75 (ámbar), 200 (cian, solo en charts).

**Para portar a móvil sin OKLCH:** convertir cada token a HEX/RGB en build time (herramientas: `culori`, `colorjs.io`) — no hardcodear un snapshot estático en este doc porque los valores fuente viven en `globals.css` y son la única verdad.

## 3. Tipografía

- **Fuente:** Inter (variable, cargada vía `next/font/google`, expuesta como `--font-inter`). Fallback declarado: Geist → system-ui → sans-serif (Geist no está realmente cargado, es fallback teórico).
- **Mono:** Geist Mono / system mono (para cifras tabulares si se necesita alinear montos).
- Sin escala tipográfica custom definida aparte de las utilidades de Tailwind — usar la escala estándar (`text-sm` a `text-2xl`) con `font-semibold`/`font-bold` para jerarquía. En móvil: usar Inter también si está disponible (Google Fonts / bundling), o la fuente del sistema como fallback aceptable.

## 4. Espaciado y forma

- **Radio base:** `--radius: 0.75rem` (12px). Escala derivada: `sm = radius-4px`, `md = radius-2px`, `lg = radius`, `xl = radius+4px`.
- **Contenedores:** tarjetas con `rounded-lg` a `rounded-xl`, `border border-border`, `bg-card`.
- **Sidebar:** ancho expandido `16rem` (256px / `w-64`), colapsado `4rem` (64px / `w-16`), transición `duration-300 ease-in-out`. El contenido principal debe desplazar su `padding-left` en sincronía (`lg:pl-64` ↔ `lg:pl-16`) — el estado `collapsed` vive en el layout padre, no solo en el componente sidebar.

## 5. Motion

Principios (inspirados en Emil Kowalski, design engineering):

- **Duración:** 150–300ms. Nunca más — el motion debe sentirse instantáneo, no una animación de presentación.
- **Easing propio, nunca el default del navegador:**
  - Entradas/reveals: `cubic-bezier` tipo ease-out-expo, array `[0.22, 1, 0.36, 1]`.
  - Micro-interacciones (hover/tap): `[0.4, 0, 0.2, 1]` — mismo curve que la utilidad CSS existente `.transition-smooth`.
- **Siempre respetar `prefers-reduced-motion`** — toda animación debe tener una salida instantánea sin movimiento.
- **Nunca animar** elementos de alta frecuencia: inputs mientras se escribe, filtros/orden de tablas, paginación, polling de notificaciones.
- **Patrones implementados** (`components/motion/`, React/framer-motion — portar el *concepto*, no la librería, a móvil con Reanimated/Motion equivalente):
  - `FadeIn` — entrada de opacidad + 8px de elevación vertical.
  - `StaggerContainer` / `StaggerItem` — revelado secuencial de listas/grids (delay ~0.05s entre ítems, tope total ~400ms).
  - `PageTransition` — cross-fade sutil al cambiar de ruta.
  - `CountUp` — anima un número ya calculado desde 0 hasta su valor final (nunca calcula el dato, solo lo revela).
  - `TapScale` — `hover: y:-2` + `tap: scale:0.98` para tarjetas y botones.

## 6. Responsive

- **Breakpoints:** los estándar de Tailwind (`sm` 640px, `md` 768px, `lg` 1024px, `xl` 1280px).
- **Shell:** sidebar fijo visible desde `lg:`, por debajo se reemplaza por un drawer (`MobileSidebar`) controlado por hamburguesa en el navbar.
- **Tablas de datos:** patrón *card-on-mobile* — `<table>` real oculto bajo `md:` (`hidden md:block`), y por debajo de `md:` un listado de tarjetas (`md:hidden`) que reusa los mismos datos y acciones, mostrando el campo principal (descripción + monto) en grande y el resto como filas etiquetadas. Ver `components/expenses/expenses-table.tsx` como referencia del patrón.
- Para móvil nativo: este patrón de "tabla se convierte en lista de tarjetas" es exactamente la estructura recomendada para una pantalla de listado en RN (FlatList de tarjetas), no hace falta reinventar la jerarquía visual.

## 7. Componentes base

Sistema construido sobre **shadcn/ui** + **Radix UI primitives** + Tailwind v4. Componentes disponibles en `components/ui/` (~54): Button, Card, Dialog, Drawer, Sheet, Dropdown Menu, Tooltip, Hover Card, Skeleton, Badge, Table, Tabs, Select, Switch, Progress, Chart (recharts wrapper), entre otros.

- **Iconografía:** Lucide React (outline, stroke consistente, 20–24px según contexto).
- **Toasts:** Sonner, `richColors` (usa los tokens `success`/`destructive`/`warning` automáticamente).
- **Tooltips:** obligatorios en botones solo-ícono (acciones de tabla, sidebar colapsado).
- **Estados de carga:** Skeleton reutilizando el flag `isLoading` ya existente en cada sección — nunca inventar un flag nuevo solo para el loading visual.

## 8. Tema claro/oscuro

- Mecanismo: `next-themes`, `attribute="class"`, toggle en el navbar (`components/layout/theme-toggle.tsx`) y en `/configuracion`.
- Patrón de montaje (evita hydration mismatch): guardar `mounted` en `useState`, setearlo en `useEffect`, y no renderizar el estado dependiente del tema hasta que `mounted === true`.
- `viewport.themeColor` (`app/layout.tsx`) usa un array por `prefers-color-scheme` para que el chrome del navegador móvil combine con el tema activo.
- Utilidades que deben ser theme-aware si se replican: scrollbar (`scrollbar-color` + `var(--muted)`/`var(--border)`), y cualquier efecto de vidrio/blur (`oklch(from var(--card) l c h / alpha)` en vez de un color hardcodeado).

## 9. Notas para el port a móvil

1. **No portar OKLCH literal** — convertir a hex/RGB en el pipeline de build del proyecto móvil, manteniendo el mapeo semántico de tokens (`primary`, `destructive`, `success`, etc.), no los valores crudos copiados a mano.
2. **El patrón card-on-mobile de las tablas web ya ES el diseño de lista nativa** — reusar esa jerarquía de información (campo principal grande, secundarios en gris, acciones al final) directamente en las pantallas de listado de RN/Flutter.
3. **Motion:** mismos principios (150–300ms, easing custom, respetar accesibilidad de movimiento reducido — `prefers-reduced-motion` en web equivale a `AccessibilityInfo.isReduceMotionEnabled` en RN).
4. **Radio de esquina 12px y iconografía Lucide** son los dos elementos más reconocibles de marca fuera del color — mantenerlos consistentes es más importante que replicar cada sombra/blur.
5. **Sidebar colapsable no aplica a móvil nativo** (ahí el patrón es tab bar inferior o drawer), pero el comportamiento de "el contenido debe reaccionar al estado de navegación, no asumir un tamaño fijo" sí aplica — es la lección aprendida del bug corregido en este refactor.

---
*Generado a partir del refactor visual en `feature/visual-polish-motion`. Fuente de verdad viva: `smart-finance-frontend/app/globals.css` y `smart-finance-frontend/components/motion/`.*
