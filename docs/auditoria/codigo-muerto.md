# Auditoria de codigo muerto — KoroFin (post Sprint 6)

Fecha: 2026-07-05. Criterio aplicado (decision explicita del usuario): **borrado conservador** — solo se elimina codigo verificablemente muerto (cero referencias en toda la base de codigo), y no se poda la libreria de componentes shadcn/ui solo por tener componentes sin usar todavia, ya que forman parte de la libreria de diseno reutilizable del proyecto.

## Removido en este sprint

### `components/ui/toast.tsx`, `components/ui/toaster.tsx`, `hooks/use-toast.ts`

**Por que es codigo muerto (no solo "libreria sin usar")**: son los primitivos de toast de shadcn/ui basados en `@radix-ui/react-toast`, de una implementacion de notificaciones que fue **reemplazada por completo por `sonner`** (`components/ui/sonner.tsx`, montado en `app/layout.tsx`). Se verifico con busqueda exhaustiva que ningun archivo del proyecto importa `components/ui/toaster` ni `hooks/use-toast`; las 15 referencias a notificaciones toast del proyecto usan `import { toast } from "sonner"`. A diferencia de un componente shadcn sin usar todavia (como `carousel` o `chart`, ver mas abajo), esto no es "libreria disponible para el futuro" sino el remanente de una migracion ya completada — mantenerlo es confuso para quien llegue a tocar notificaciones despues, porque sugiere que hay dos sistemas de toast conviviendo.

**Accion tomada**: se eliminaron los 3 archivos y la dependencia `@radix-ui/react-toast` de `package.json` (unico consumidor era `toast.tsx`), y se corrio `pnpm install` para actualizar `pnpm-lock.yaml`. Verificado: `npm run lint`, `npm run build` y `npm run test` (88/88) siguen en verde tras la remocion.

## Revisado y dejado intacto (decision conservadora)

### Componentes shadcn/ui sin uso actual: `carousel`, `chart`, `menubar`, `drawer`

Se confirmo por busqueda exhaustiva que ningun archivo de `app/` o `components/` (fuera de sus propios archivos de definicion) importa estos 4 componentes. **No se eliminan** por decision explicita del usuario: son parte de la libreria base de shadcn/ui instalada en el proyecto (scaffolding estandar), no un remanente de una funcionalidad removida como el caso del toast. Removerlos ahora solo para "limpiar" seria podar la libreria, que el usuario pidio explicitamente no hacer. Si en el futuro se confirma que ninguno de estos 4 se va a necesitar, es una decision de producto (no de limpieza tecnica) y puede revisarse en otro momento.

### Backend: sin hallazgos de codigo muerto

Se reviso el modulo de Reportes (nuevo) y los modulos existentes tocados este sprint (`UserController`, `UserService`, `AnalysisController`, `ExpenseRepository`, `IncomeRepository`) sin encontrar metodos, imports o clases sin uso. Los artefactos Flyway huerfanos de la funcionalidad BYOK removida (`V9`, `V11` viejo) ya estaban confinados a `target/classes/` (nunca en `src`) y se purgan automaticamente con cualquier `mvnw clean`; no son "codigo muerto" en el sentido de codigo fuente versionado, sino build artifacts obsoletos — ver `base-de-datos.md` para el detalle de riesgo Flyway asociado.

## Veredicto

El unico codigo muerto real y verificable encontrado este sprint fue el trio de primitivos de toast de shadcn/ui, ya reemplazado en su totalidad por sonner — se elimino junto con su dependencia. No se encontro codigo muerto adicional en el backend. Los componentes shadcn sin uso (`carousel`, `chart`, `menubar`, `drawer`) se dejan intactos por ser libreria de diseno, no deuda tecnica, siguiendo el criterio conservador acordado.
