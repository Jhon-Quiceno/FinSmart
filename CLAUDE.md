# FinSmart

Aplicación de finanzas personales. Monorepo con dos proyectos:

- `smart-finance-backend/` — API REST en Java 21 + Spring Boot (Maven). Paquete base: `com.smartfinance.backend`, organizado por dominio de negocio (`common/`, `usuario/`, `ingresos/`, `gastos/`, `deudas/`, `servicios/`, `analisis/`, `ia/`, `reportes/`), con capas técnicas dentro de cada dominio.
- `smart-finance-frontend/` — Next.js (App Router) + TypeScript + pnpm. Organizado por tipo + dominio: `components/<dominio>/`, `hooks/`, `lib/services|types|schemas`. `components/ui/` es shadcn y no se edita a mano.

## Convenciones obligatorias

Leer y aplicar `docs/convenciones.md`. Resumen no negociable:

- **Commits, PRs y comentarios de código: SIEMPRE en español** (conventional commits con tipo en inglés y descripción en español; sin atribución de IA).
- Identificadores de código en inglés.
- Rama base de trabajo y de PRs: `develop`. `main` solo para releases.

## Comandos

- Backend: `cd smart-finance-backend && ./mvnw.cmd compile` / `./mvnw.cmd test`
- Frontend: `cd smart-finance-frontend && pnpm build` / `pnpm lint` / `pnpm test`
