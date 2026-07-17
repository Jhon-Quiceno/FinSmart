# Archivo histórico

Documentación cerrada que no se vuelve a tocar, pero se conserva como registro/evidencia.
No es material de referencia para trabajo activo — para eso, ver `docs/` en su raíz.

## `mvp/`

Evidencia, auditorías y demo del MVP de KoroFin (6 sprints, cerrado 2026-07-14, en `main`).
El detalle día a día de cada sprint del MVP vive en el historial de git, no en archivos
sueltos — esta carpeta guarda solo lo que se generó como evidencia/reporte final.

- `evidencias/` — evidencia técnica por sprint (1 a 6) + capturas de pantalla reales de las
  8 pantallas del MVP.
- `auditoria/` — auditoría de arquitectura/BD/código hecha al cierre del Sprint 6, con sus
  hallazgos ya corregidos en el código. `pendientes-fuera-de-sprint6.md` tiene algunos ítems
  (i18n, 2FA, exposición de `/actuator`, backups de Postgres) que todavía no se evaluaron
  para el roadmap SaaS — vale la pena repasarlo antes de asumir que está todo cubierto.
- `video-evidencias/` — demo en video del MVP (~30s, login/registro/dashboard + 8 módulos).
  La carpeta interna sigue llamada `finsmart-demo` (nombre de marca previo al rebrand a
  KoroFin) a propósito, para no reescribir metadata de un archivo histórico.
- `FRONTEND_DOC.md` — documentación de frontend del MVP. Describe el estado hasta el
  Sprint 5/6; el MVP completo y sprint1 de la fase SaaS ya cerraron después de escribirse,
  así que sus referencias a "pendiente Sprint 6" están desactualizadas a propósito (se
  archivó tal cual, sin reescribir, por ser evidencia histórica).
