# FinSmart

Monorepo con:
- `smart-finance-backend` (Spring Boot)
- `smart-finance-frontend` (Next.js)

## CI configurado (GitHub Actions)

Archivo: `.github/workflows/ci.yml`

Checks que ejecuta en cada PR a `main`:
1. `backend-tests`: `mvn test` en backend.
2. `frontend-tests`: `pnpm install`, `pnpm test` y `pnpm build` en frontend.

## Tests agregados

### Backend
- `smart-finance-backend/src/test/java/com/smartfinance/backend/service/UserServiceTest.java`
  - Cubre `register` y `login` (casos exitosos y errores).

### Frontend
- `smart-finance-frontend/lib/api-client.test.ts`
  - Cubre `registerRequest`, `loginRequest` y manejo de errores (`getApiErrorMessage`).

## Configuración exacta en GitHub para bloquear `main`

1. En GitHub entra a **Settings** > **Branches**.
2. En **Branch protection rules** crea una regla para `main`.
3. Activa **Require a pull request before merging**.
4. Activa **Require status checks to pass before merging**.
5. Selecciona como checks requeridos:
   - `backend-tests`
   - `frontend-tests`
6. Activa **Require branches to be up to date before merging**.
7. Activa **Restrict who can push to matching branches** y no agregues usuarios/equipos (o solo administradores si lo necesitas).
8. (Recomendado) Activa **Do not allow bypassing the above settings**.
9. Guarda la regla.

Con eso nadie podrá hacer push directo a `main` y solo se podrá fusionar mediante PR con checks en verde.
