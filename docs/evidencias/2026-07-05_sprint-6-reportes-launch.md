# Evidencia Sprint 6 — Reportes, Pulido y Launch

> **Fecha:** Julio 2026
> **Total de tareas:** 12/14 completadas (2 parciales: Postman collection, responsividad mobile)
> **Migraciones:** V11 (índices de cierre), V12 (debt_payment_id en expenses — post-cierre)

---

## 1. Objetivo

Completar el MVP con reportes detallados, exportación de datos, configuración de usuario, Docker para despliegue y una revisión de calidad integral.

## 2. Alcance Implementado

### Backend (6 tareas completas, 1 parcial)
- Endpoints de reportes por período — `GET /api/reports/monthly` con breakdown completo
- Exportación a CSV — `GET /api/reports/export` con formato legible (fechas traducidas, montos formateados)
- Dockerizar Spring Boot — `Dockerfile` + `docker-compose` con PostgreSQL
- Variables de entorno para producción (perfiles dev/prod)
- Validaciones exhaustivas con `@Valid` y mensajes en español
- Smoke testing cubierto por suite automatizada (MockMvc)
- **Parcial**: Colección Postman exportable pendiente (ver auditoria/pendientes.md)

### Base de Datos (2 tareas)
- V11: índices de cierre restantes (debt_payments)
- V12: columna `debt_payment_id` en expenses (post-cierre — corrección de bug)

### Frontend (6 tareas completas, 1 parcial)
- Página `/reportes` con selector de período y gráficos
- Exportación CSV desde `/reportes`
- Manejo global de errores HTTP (401/403/500)
- Empty states para todas las páginas
- Página `/configuracion` con cambio de contraseña y datos de perfil
- Build de producción Next.js configurado
- **Parcial**: Responsividad mobile pendiente de verificación manual

## 3. Decisiones Técnicas

| Decisión | Justificación |
|----------|---------------|
| Reutiliza `FinancialAnalysisService.getSummary()` para reportes | Evita duplicar lógica de cálculos financieros |
| CSV escapa =/+/-/@ al inicio del campo | Previene inyección de fórmulas en Excel |
| Cambiar contraseña revoca todos los refresh tokens | Invalida sesiones existentes por seguridad |
| `/users/password` excluido del interceptor de refresh | El 401 de "contraseña incorrecta" no debe tratarse como sesión expirada |
| toastApiError para 5xx | Evita duplicación de toasts de error |

## 4. Corrección Post-Cierre (V12)

Se detectó que registrar un abono a deuda (`POST /debts/{id}/payments`) no generaba un gasto, a diferencia de los servicios recurrentes. Esto significaba que los abonos no aparecían en `/gastos` ni en el dashboard. Se corrigió:

1. **Migración V12**: columna `debt_payment_id` (FK nullable, ON DELETE SET NULL) en `expenses`
2. **`DebtPaymentService.createPayment`**: ahora crea un `Expense` vinculado en la misma transacción
3. **Frontend**: invalida caché de gastos al registrar un abono

## 5. Diferenciación Reportes vs Dashboard

| Aspecto | Dashboard | Reportes |
|---------|-----------|----------|
| Período | Mes en curso | Cualquier mes/año |
| Contenido | Resumen ejecutivo + gráficos | Breakdown detallado + tabla de movimientos |
| Exportación | No | CSV con datos traducidos |
| Llamada API | `/analysis/summary` | `/reports/monthly` + `/reports/movements` |

## 6. Conclusión

El Sprint 6 completa el MVP con funcionalidades de reportes, exportación y configuración. Las revisiones adversariales en contexto limpio (backend, frontend, tests+arquitectura) encontraron y corrigieron bugs críticos invisibles para los tests con mocks. La corrección V12 post-cierre demuestra el compromiso con la integridad de los datos financieros.
