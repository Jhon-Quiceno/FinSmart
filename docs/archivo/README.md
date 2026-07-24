# Archivo histórico

Documentación cerrada que no se vuelve a tocar, pero se conserva como registro/evidencia.
No es material de referencia para trabajo activo — para eso, ver `docs/` en su raíz.

"No se vuelve a tocar" aplica distinto según el tipo de contenido dentro de `mvp/` — ver el
detalle en cada subcarpeta más abajo. En resumen: lo que tiene **fecha en el nombre del
archivo** es evidencia puntual y se congela tal cual, imperfecciones incluidas; lo que
**no tiene fecha en el nombre** es documentación de referencia técnica y sí se actualiza
cuando el estado del proyecto que describe cambia (para que no quede desactualizada de forma
permanente y engañosa).

## `mvp/`

Evidencia, auditorías y demo del MVP de KoroFin (6 sprints, cerrado 2026-07-14, en `main`).
El detalle día a día de cada sprint del MVP vive en el historial de git, no en archivos
sueltos — esta carpeta guarda solo lo que se generó como evidencia/reporte final.

- `evidencias/` — evidencia técnica por sprint (1 a 6), fechada en el nombre de archivo
  (`2026-07-05_sprint-N-*.md`), + capturas de pantalla reales de las 8 pantallas del MVP.
  **Se congela tal cual**: describe un momento puntual del proyecto, así que su cuerpo
  narrativo no se reescribe aunque el proyecto siga avanzando (solo se corrigen links rotos
  si aparecen, ya que un link roto no es parte del registro histórico, es un defecto).
- `auditoria/` — auditoría de arquitectura/BD/código/pendientes hecha al cierre del Sprint 6.
  A diferencia de `evidencias/`, estos archivos **no llevan fecha en el nombre** y funcionan
  como documentación de referencia técnica continua: se actualizan cuando el estado real del
  proyecto cambia (nuevos dominios, tablas, hallazgos resueltos), dejando claro qué es
  hallazgo original del Sprint 6 y qué es una actualización posterior. `pendientes-fuera-de-sprint6.md`
  sigue teniendo ítems (i18n, 2FA, exposición de `/actuator`, backups de Postgres) que no se
  evaluaron para el roadmap SaaS — vale la pena repasarlo antes de asumir que está todo cubierto.
- `video-evidencias/` — demo en video del MVP (~30s, login/registro/dashboard + 8 módulos).
  La carpeta interna sigue llamada `finsmart-demo` (nombre de marca previo al rebrand a
  KoroFin) a propósito, para no reescribir metadata de un archivo histórico.
- `FRONTEND_DOC.md` — documentación de frontend de KoroFin. Igual que `auditoria/`, no lleva
  fecha en el nombre y se mantiene actualizada al estado actual del frontend (proveedores de
  IA, integraciones, sprints ya cerrados), en vez de congelarse en el momento en que se
  escribió por primera vez.
