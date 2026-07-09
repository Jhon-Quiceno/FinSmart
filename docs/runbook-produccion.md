# Runbook de producción — diagnóstico de problemas en FinSmart

Guía para analizar y resolver fallas en producción. El deploy es automático
(push a `main` → GitHub Actions → Cloud Run, y Vercel → frontend), pero el
diagnóstico siempre es manual: este documento dice dónde mirar según el síntoma.

## Arquitectura en producción

```
Navegador
   │  (mismo origen: https://fin-smart-ten.vercel.app)
   ▼
Vercel (Next.js) ── rewrite /api/* ──► Cloud Run (Spring Boot) ──► Neon (PostgreSQL)
```

- Frontend: `https://fin-smart-ten.vercel.app`
- Backend: `https://finsmart-backend-639943658710.us-central1.run.app`
- El navegador nunca llama a Cloud Run directo: el rewrite de
  `smart-finance-frontend/next.config.mjs` proxea `/api/*` usando la variable
  `BACKEND_API_URL` (configurada en Vercel).

## Ruta rápida ante cualquier falla

1. **F12 en el navegador** (pestañas Console y Network): anotar el status HTTP
   y el endpoint exacto que falla. Eso es el síntoma, no la causa.
2. **Aislar la capa con `curl`** (ver tabla de síntomas abajo): ¿responde el
   backend directo? ¿responde a través del proxy?
3. **Leer la causa en los logs de Cloud Run**: ahí está el stack trace real.
4. Arreglar en una rama → PR a `main` → el pipeline valida y deploya.

## Primera pregunta: ¿falló el deploy o falla la app?

| Situación | Dónde mirar |
|-----------|-------------|
| El push a `main` no llegó a producción (run rojo) | GitHub → Actions → job `deploy-cloud-run` → paso fallido. CLI: `gh run list` y `gh run view <id> --log-failed` |
| El deploy está verde pero la app se comporta mal | Logs de Cloud Run (runtime). Ver sección siguiente |
| El frontend no refleja el cambio | Vercel → Deployments: confirmar que el deploy de producción esté READY y que las variables de entorno no cambiaron |

## Logs del backend (la fuente de la verdad)

Consola web: Google Cloud → Cloud Run → `finsmart-backend` → pestaña **Logs**
(o Logs Explorer filtrando severidad `ERROR`). Todo lo que Spring Boot escribe
(stack traces, errores de conexión a Neon, excepciones) aparece ahí.

Con gcloud CLI instalado:

```bash
# Últimos logs del servicio
gcloud run services logs read finsmart-backend --region us-central1 --limit 50

# Qué revisión está recibiendo el tráfico (ver caso 2 abajo)
gcloud run services describe finsmart-backend --region us-central1 \
  --format="value(status.traffic)"
```

## Tabla de síntomas

| Síntoma | Sospechoso | Verificación |
|---------|------------|--------------|
| 403 en endpoints con body `Invalid CORS request` | Secret `APP_CORS_ALLOWED_ORIGINS` no incluye el dominio del frontend | `curl` con y sin header `Origin` (ver abajo): si solo falla con `Origin`, es CORS |
| 403 en POST sin mensaje CORS | CSRF: falta el header `X-XSRF-TOKEN` (solo register/login/logout están exentos) | Ver cookie `XSRF-TOKEN` en F12 → Application → Cookies del dominio del frontend |
| 500 | Excepción en el backend | Stack trace en logs de Cloud Run |
| 503 / timeouts | Cloud Run no levanta (falla al arrancar) o Neon inaccesible | Logs de arranque de la revisión; probar `/actuator/health` |
| El fix deployado "no hace nada" | Tráfico anclado a una revisión vieja | Pestaña Revisiones en Cloud Run: la última debe tener 100% del tráfico |
| Pantalla de "verificando el navegador" o 403 en recursos estáticos | Protección anti-bots de Vercel (challenge), no es un bug | Probar en un navegador normal; `curl` siempre va a ser bloqueado |

## Comandos de verificación

```bash
# ¿El backend está vivo y conectado a la base?
curl https://finsmart-backend-639943658710.us-central1.run.app/actuator/health

# ¿Es un problema de CORS? Comparar con y sin Origin:
curl -s -o /dev/null -w "%{http_code}\n" -X POST \
  https://finsmart-backend-639943658710.us-central1.run.app/api/users/login \
  -H "Content-Type: application/json" -d '{}'

curl -s -X POST \
  https://finsmart-backend-639943658710.us-central1.run.app/api/users/login \
  -H "Origin: https://fin-smart-ten.vercel.app" \
  -H "Content-Type: application/json" -d '{}'
# Si el primero responde 400/401 y el segundo "Invalid CORS request",
# el problema es el secret APP_CORS_ALLOWED_ORIGINS.
```

Para actualizar un secret y redeployar sin tocar código:

```bash
gh secret set APP_CORS_ALLOWED_ORIGINS --body "https://dominio1,https://dominio2"
gh run rerun <id-del-ultimo-deploy>   # los re-runs leen el valor nuevo del secret
```

## Casos reales (julio 2026)

Tres fallas encadenadas que produjeron "pantalla negra tras el login". Sirven
como plantilla de diagnóstico:

1. **403 en `/api/users/refresh`** — El frontend en Vercel no podía leer la
   cookie `XSRF-TOKEN` emitida por el dominio de Cloud Run (JavaScript no lee
   cookies de otro dominio). Solución: proxy same-origin en `next.config.mjs`
   (PR #69). Lección: si funciona en localhost pero no en producción, revisar
   supuestos de dominio/cookies.
2. **Deploy verde pero producción servía código viejo** — El deploy de
   validación en PRs usa `--no-traffic`, y eso ancla el tráfico de Cloud Run a
   una revisión fija; los deploys siguientes no movían el tráfico. Solución:
   paso `update-traffic --to-latest` tras cada deploy de producción (PR #70).
   Lección: "pipeline verde" no garantiza "código nuevo sirviendo".
3. **403 `Invalid CORS request` en login** — El secret `APP_CORS_ALLOWED_ORIGINS`
   no contenía el dominio real del frontend; la revisión vieja anclada tenía un
   valor correcto "congelado" que enmascaraba el error. Solución: `gh secret set`
   + re-run del deploy. Lección: los secrets solo se aplican al deployar; una
   revisión vieja puede ocultar un secret roto.

## Checklist post-deploy

- [ ] Run de Actions en verde (`gh run list --branch main --limit 1`)
- [ ] La última revisión de Cloud Run recibe 100% del tráfico
- [ ] `/actuator/health` responde `UP`
- [ ] Login/registro funcionan en el navegador en `https://fin-smart-ten.vercel.app`
- [ ] Sin errores nuevos en F12 → Console ni en logs de Cloud Run

## Variables y secrets: quién vive dónde

| Lugar | Variables | Se aplican cuando |
|-------|-----------|-------------------|
| GitHub Secrets | `SPRING_DATASOURCE_*`, `JWT_SECRET`, `APP_CORS_ALLOWED_ORIGINS`, `GCP_*` | En cada deploy del backend (workflow `deploy-backend.yml`) |
| Vercel (Environment Variables) | `BACKEND_API_URL` (servidor, destino del proxy). `NEXT_PUBLIC_API_URL` debe quedar vacía/ausente | En cada build del frontend; cambiarlas requiere Redeploy |
| Cloud Run (env vars de la revisión) | Copia de los secrets al momento del deploy | Quedan congeladas en la revisión hasta el próximo deploy |
