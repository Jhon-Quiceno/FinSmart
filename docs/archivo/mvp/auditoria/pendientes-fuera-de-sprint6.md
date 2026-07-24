# Pendientes fuera del alcance del Sprint 6 — KoroFin

Fecha: 2026-07-05. El Sprint 6 cierra el tablero del MVP (ver el historial de git de la rama principal para el detalle dia a dia del MVP), pero eso no significa que el proyecto no tenga trabajo pendiente. Este documento lista, sin filtro de prioridad de negocio, todo lo que quedo fuera del alcance de este sprint y deberia decidirse conscientemente (hacerlo, posponerlo o aceptarlo como limitacion conocida del MVP).

## 1. Explicitamente diferido por decision del usuario durante la planificacion de este sprint

Estos items estaban mencionados en la planificacion del Sprint 6 (historico, sin archivo de sprint dedicado — ver `docs/archivo/mvp/evidencias/2026-07-05_sprint-6-reportes-launch.md` para la evidencia de cierre correspondiente) o en la exploracion previa, pero se decidio conscientemente priorizar "funcional primero" (reportes + configuracion + errores + empty states + tests + arquitectura) y dejarlos como seguimiento liviano:

- **Coleccion de Postman documentada** (alcance backend punto 6, DoD punto 8 de la planificacion del Sprint 6). Verificado: no existe ningun archivo de coleccion Postman en el repo. El smoke testing de los endpoints se hizo via los tests automatizados (MockMvc), no con una coleccion exportable para probar manualmente contra el backend dockerizado.
- **Dockerizar el frontend** (DoD punto 10 solo pide "build de produccion... con `NEXT_PUBLIC_API_URL`", no un `Dockerfile`, pero el `docker-compose.yml` del repo solo levanta backend + Postgres). Verificado: no existe `Dockerfile` en `smart-finance-frontend/`. Si el plan de despliegue es un solo `docker-compose up` para todo el stack, falta ese servicio.

## 2. Deuda tecnica identificada por los revisores de este sprint, no corregida por alcance

Ver `arquitectura.md` para el detalle tecnico completo. Resumen de lo que quedo pendiente (lo demas ya se corrigio):

- **Doble toast en errores 500 para paginas que cargan listas via `useEffect(() => { if (error) toast.error(error) }, [error])`** (dashboard, deudas, servicios, reportes, y potencialmente otras). Se corrigio el patron de mutacion (`toastApiError`) en ~13 sitios, pero el patron de carga de lista pierde el codigo de estado HTTP antes de llegar al `useEffect`, por lo que seguiria duplicando el toast generico del interceptor en un 500 de carga. Arreglarlo bien implica que los hooks (`use-expenses`, `use-incomes`, `use-debts`, `use-categories`, `use-recurring-payments`, `use-report`, `use-analysis`) devuelvan el error crudo o un flag ademas del string, lo cual toca ~6-7 hooks preexistentes fuera del alcance de este sprint.
- **Boton "Exportar CSV" en `/reportes` no se deshabilita cuando el periodo no tiene datos** — mejora de UX menor, no bug (el backend responde igual con solo el encabezado del CSV).
- **Indice redundante `idx_recurring_payments_next_date`** (ver `base-de-datos.md`): cubierto por el indice compuesto de `V10`, listo para eliminar en una futura `V12` de una linea.

## 3. Cobertura de pruebas mas alla del backend/servicios

- **No hay tests de componente ni end-to-end en el frontend**: la suite actual (`vitest`, 12 archivos, 88 tests) cubre exclusivamente `lib/services/*` y `lib/schemas/*` con `axios-mock-adapter`. No hay ningun test que monte un componente React (React Testing Library no esta instalado) ni ningun test end-to-end (Playwright/Cypress no estan instalados). Esto significa que regresiones puramente de UI (ej. un boton que deja de renderizarse, un formulario que no dispara el submit) no las agarra la suite automatizada — solo pruebas manuales.
- **No hay `vitest.config.ts`**: los tests corren con la configuracion por defecto de Vitest inferida del `package.json`/Vite. Funciona hoy, pero si el proyecto crece (alias de paths mas complejos, cobertura de codigo, entorno jsdom para tests de componente) va a hacer falta un archivo de configuracion explicito.
- **Sin medicion de cobertura de tests** (`--coverage`) configurada en ningun script de `package.json` ni en el backend (JaCoCo no esta en el `pom.xml`). No hay forma automatizada de saber que porcentaje del codigo esta cubierto.

## 4. Observabilidad y produccion

- **Sin logging estructurado ni correlacion de requests**: los logs del backend son el formato por defecto de Spring Boot (texto plano por consola), sin un request ID que permita correlacionar logs de un mismo request a traves de capas, ni integracion con un backend de logs centralizado.
- **`spring-boot-starter-actuator` esta en el `pom.xml`, pero no se verifico su configuracion de exposicion** (`management.endpoints.web.exposure.include`, seguridad de `/actuator/*`). Si esta expuesto sin restriccion, `/actuator/env` o `/actuator/heapdump` podrian filtrar secretos; si no esta expuesto ni siquiera `/actuator/health`, el `docker-compose` no tiene forma de saber si el backend esta listo mas alla de que el proceso arranque.
- **Sin rate limiting** en ningun endpoint (login, refresh, IA) — un endpoint de login sin rate limiting es superficie de fuerza bruta; los endpoints de IA sin rate limiting exponen a abuso de cuota de los proveedores externos.
- **Sin estrategia de backup de PostgreSQL documentada** para el `docker-compose` (el volumen `postgres_data` persiste en el host, pero no hay ningun script ni documentacion de backup/restore).

## 5. Seguridad (mas alla de lo ya corregido este sprint)

- **Sin cabeceras de seguridad HTTP explicitas** (CSP, `X-Content-Type-Options`, `Strict-Transport-Security`) configuradas en Spring Security mas alla de lo que trae por defecto.
- **Sin politica de expiracion/rotacion obligatoria de contrasena** ni requisito de complejidad minima verificado en `ChangePasswordRequest`/`RegisterRequest` (deberia confirmarse que existe una validacion de longitud/complejidad minima consistente entre registro y cambio de contrasena).
- **Sin 2FA** — fuera de alcance de un MVP, pero vale dejarlo listado como decision consciente de producto, no un olvido.

## 6. Producto / UX

- **Sin internacionalizacion (i18n)**: toda la UI y los mensajes de error estan hardcodeados en espanol. Si en algun momento se necesita soporte multi-idioma, hoy implicaria una migracion de strings, no un simple toggle.
- **Responsividad mobile**: el Sprint 6 pide "revision de responsividad mobile en todas las paginas con datos reales" como parte del DoD — esto requiere una pasada manual en el navegador (viewport mobile) que el usuario dijo que va a hacer el mismo; no se puede verificar automaticamente sin herramientas de testing visual.

## Como usar este documento

Ninguno de estos items bloquea considerar el Sprint 6 (y por lo tanto el MVP) como terminado segun su propio DoD. Son candidatos para un backlog post-MVP; se recomienda que el usuario los priorice explicitamente (por ejemplo, creando un "Sprint 7" o un backlog de mantenimiento) en vez de que queden implicitos.
