# Auditoria de arquitectura — KoroFin (post Sprint 6)

Fecha original: 2026-07-05. Alcance: revision de arquitectura backend y frontend tras el cierre del Sprint 6 (ultimo sprint del MVP), realizada por agentes revisores en contexto limpio mas verificacion manual.

**Actualizacion (estado actual, fase SaaS):** el backend paso de 14 a 12 dominios de nivel superior (`common, usuario, ingresos, gastos, deudas, servicios, analisis, ia, reportes, integraciones, tarjetas, extractos`; el conteo de "14 modulos" del parrafo original contaba sub-modulos de negocio, no carpetas de dominio). Los tres agregados despues de este cierre — `tarjetas` (tarjetas de credito, compras, pagos y cuotas), `integraciones` (vinculo y mensajeria del bot de Telegram) e `extractos` (importacion de extractos bancarios con IA) — se revisaron por encima y respetan el mismo patron `controller -> service -> repository` con DTOs en los bordes; `extractos` es la unica excepcion parcial esperada, ya que no persiste entidades propias (no tiene `repository`/`model/entity`): su servicio arma una previsualizacion en memoria y delega la creacion final a los servicios de `ingresos`/`gastos` ya existentes, en vez de duplicar esa logica — misma decision de reuso que ya se elogio en este documento para `ReportService`. No se hizo una auditoria de seguridad/hallazgos linea por linea de estos tres dominios; si se necesita ese nivel de detalle, amerita una pasada dedicada.

## Resumen ejecutivo

El proyecto es arquitectonicamente consistente. El backend sigue un patron de "vertical slice" uniforme (controller -> service -> repository, DTOs con mappers, validacion declarativa, manejo de excepciones centralizado) en los 14 modulos existentes, incluyendo el nuevo modulo de Reportes. El frontend sigue el mismo patron de hooks caseros con cache en `Map` + `Set` de listeners en todos los dominios, sin desviaciones. No se encontraron violaciones de capas ni introduccion de un patron de estado alternativo. Los hallazgos de esta auditoria son de tipo "deuda menor" o "decision a documentar", no bloqueantes.

## Backend

### Patron de capas (vertical slice)

Cada dominio (Expense, Income, Debt, DebtPayment, RecurringPayment, Category, Notification, Report, User, AI) sigue la misma forma:

```
Controller (REST, @Valid, SecurityUtils.getCurrentUserId())
  -> Service (@Transactional, logica de negocio)
    -> Repository (Spring Data JPA / Specifications)
      -> Model (entidad JPA)
DTO (record) + Mapper (MapStruct) en los bordes de entrada/salida
```

El modulo de Reportes (`ReportController` -> `ReportService` -> `ExpenseRepository`/`IncomeRepository` + `FinancialAnalysisService`) respeta esto exactamente. Decision de arquitectura acertada: `ReportService.getMonthlyReport` delega el calculo completo a `FinancialAnalysisService.getSummary()` (ya existente desde el Sprint 4) en vez de recalcular metricas — evita duplicar logica de negocio financiera en dos lugares.

### Seguridad y scoping por usuario

Todos los endpoints nuevos y modificados (`ReportController`, `UserController.updateProfile/changePassword`) resuelven `SecurityUtils.getCurrentUserId()` en el controller y lo pasan al service, igual que el precedente ya establecido en `AnalysisController.getPrediction()`. Se verifico que ninguna query nueva permite acceso cruzado entre usuarios.

### Manejo de errores

`GlobalExceptionHandler` (existente desde el Sprint 1) sigue siendo el unico punto de traduccion de excepciones a respuestas HTTP con mensajes en espanol. Los nuevos casos (`ConstraintViolationException` por `@Min/@Max` en query params, `EmailAlreadyExistsException`, `InvalidCredentialsException` en cambio de contrasena) reutilizan handlers ya existentes; no se agrego logica de validacion dispersa en los services.

### Hallazgos aplicados durante esta auditoria

Estos hallazgos fueron detectados por el revisor de backend y ya se corrigieron en el working tree (ver commits pendientes de revision del usuario):

1. **CSV injection en la exportacion** (`ReportController.escapeCsv`): un valor que empezara con `=`, `+`, `-` o `@` (ej. una categoria o descripcion) se escribia sin neutralizar, lo que un spreadsheet interpreta como formula ejecutable al abrir el CSV. Corregido: se antepone `'` a esos valores antes de aplicar el quoting RFC4180 existente.
2. **Cambio de contrasena no invalidaba otras sesiones**: `UserService.changePassword` no revocaba refresh tokens existentes. Se agrego `RefreshTokenRepository.revokeAllActiveForUser` (bulk update) y `RefreshTokenService.revokeAllForUser`, invocado al final de `changePassword`.
3. **Condicion de carrera en email unico** (`UserService.updateProfile`): el check-then-act entre `existsByEmailIgnoreCase` y `save` podia degradar a un 500 crudo si dos requests concurrentes tomaban el mismo email antes de que el `UNIQUE` de la base actuara. Se envolvio el `save` en un `try/catch` que mapea `DataIntegrityViolationException` al mismo `EmailAlreadyExistsException` (409) que produce el chequeo previo.
4. **`docker-compose.yml` arrancaba en perfil `prod` por defecto**, contradiciendo el DoD del sprint ("`docker-compose up` deja la app operativa con la configuracion de `dev`"). Se cambio el default de `SPRING_PROFILES_ACTIVE` de `prod` a `dev`.

### Deuda documentada, no bloqueante

- **Export "streaming" solo a nivel HTTP, no a nivel BD**: `ReportService.getMovements` materializa la lista completa de movimientos del periodo antes de que el controller escriba nada al response. Para el volumen actual (un usuario, un mes) es irrelevante en la practica, pero la decision de arquitectura #2 del sprint dice explicitamente "streaming para no cargar todo el periodo en memoria" — el nombre no coincide del todo con la implementacion. No amerita cambio de codigo ahora; se deja como nota de precision para si el volumen de datos crece.

## Frontend

### Patron de hooks (Map cache + Set de listeners)

`hooks/use-report.ts` replica el mismo patron que `use-expenses.ts`, `use-incomes.ts`, `use-debts.ts`, etc.: un `Map` module-level como cache, un `Set` de listeners para notificar refetch, y un hook que expone `{ data, isLoading, error, refetch }`. No hay desviacion.

`hooks/use-user.ts` es una excepcion intencional y correcta: es un hook de mutacion pura (perfil/contrasena), sin lista que cachear, por lo que no usa el patron de cache — decision correcta, no inconsistencia.

### Manejo global de errores

El interceptor de `lib/api-client.ts` centraliza `401` (refresh + retry, o redirect a login si el refresh falla), `403` (toast o tratar como no autenticado segun el mensaje) y ahora `500` (toast generico), excluyendo `/api/ai/*` porque esos endpoints ya manejan sus propios estados de error especificos. Esto cumple la decision de arquitectura #3 del sprint ("ninguna pagina maneja estos codigos por su cuenta").

### Hallazgos aplicados durante esta auditoria

1. **`PUT /api/users/password` con 401 se trataba como sesion expirada**: el interceptor intentaba renovar el access token y reintentar la misma contrasena incorrecta en vez de mostrar el error de negocio, y en el peor caso podia terminar redirigiendo a login por un simple error de tipeo. Se agrego `/api/users/password` a la lista de rutas excluidas de la recuperacion automatica de sesion (`isAuthEndpoint`), igual que login/register/refresh/logout.
2. **Doble toast en errores 5xx**: como el interceptor ahora muestra un toast generico para cualquier 500, las paginas que ya tenian su propio `catch { toast.error(getApiErrorMessage(...)) }` (ingresos, gastos, categorias, deudas, servicios, reportes) duplicaban el mensaje. Se introdujo `toastApiError(error, fallback)` en `lib/api-client.ts`, que omite el toast local cuando el status es >= 500 (porque el interceptor ya lo cubre), y se reemplazaron los ~13 sitios de mutacion afectados.
3. **`hooks/use-report.ts` mostraba el mensaje crudo de Axios** en vez del mensaje en espanol del backend. Se cambio a usar `getApiErrorMessage`, igual que el resto de hooks.

### Deuda documentada, no bloqueante

- **Paginas con estado de error via `useEffect(() => { if (error) toast.error(error) }, [error])`** (dashboard, deudas, servicios, reportes, etc., patron previo al Sprint 6): estas siguen duplicando el toast en un 500 de carga de lista, porque el string de error ya perdio el codigo de estado HTTP para cuando llega al `useEffect`. Arreglarlo bien requeriria que los hooks devuelvan el error crudo (o un flag) ademas del mensaje, tocando ~6 hooks preexistentes fuera del alcance de este sprint. Ver `pendientes-fuera-de-sprint6.md`.
- **Boton "Exportar CSV" no se deshabilita cuando no hay datos** (`app/reportes/page.tsx`): no es un bug (el backend responde igual con solo el header), pero es una mejora de UX menor pendiente.

## Veredicto

La base de codigo es arquitectonicamente consistente en ambos modulos: el nuevo trabajo del Sprint 6 sigue exactamente los patrones ya establecidos, sin atajos ni abstracciones nuevas innecesarias. Los hallazgos de seguridad y consistencia detectados por los revisores (CSV injection, revocacion de sesion, TOCTOU de email, doble toast, clasificacion incorrecta del 401 de contrasena) ya fueron corregidos y verificados con la suite de tests completa en verde (281 backend, 88 frontend). Los items restantes son deuda de bajo riesgo, documentada explicitamente para que quede a criterio del equipo priorizarla despues del MVP.
