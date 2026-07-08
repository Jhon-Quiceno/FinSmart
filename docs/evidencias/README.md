# 📋 Evidencias de Desarrollo — FinSmart

> **Propósito:** Consolidar la evidencia técnica del desarrollo de FinSmart, demostrando qué se implementó, por qué y cómo se verificó.

---

## Estructura

```
evidencias/
├── README.md                                          ← Este archivo
├── 2026-07-05_sprint-1-autenticacion.md               ← Sprint 1: JWT, registro/login
├── 2026-07-05_sprint-2-ingresos-gastos.md             ← Sprint 2: CRUD ingresos/gastos/categorías
├── 2026-07-05_sprint-3-deudas-servicios.md            ← Sprint 3: Deudas, abonos, servicios recurrentes
├── 2026-07-05_sprint-4-motor-financiero-dashboard.md  ← Sprint 4: Dashboard, análisis, recomendaciones
├── 2026-07-05_sprint-5-ia-notificaciones.md           ← Sprint 5: IA multi-proveedor, notificaciones
├── 2026-07-05_sprint-6-reportes-launch.md             ← Sprint 6: Reportes, exportación, configuración
└── capturas/                                          ← Capturas de pantalla reales, por fecha y feature
    └── 2026-07-06_pantallas-app/                      ← Login, registro, dashboard y los 8 módulos
```

---

## Criterios de Calidad

Cada evidencia debe demostrar:

1. **Comprensión del problema** y del objetivo del sprint
2. **Trazabilidad** con los requisitos funcionales
3. **Justificación** de decisiones de diseño y técnicas
4. **Correspondencia** entre documentación y código
5. **Validación técnica** real (tests, verificaciones)
6. **Limitaciones** y trabajo pendiente identificados

---

## Estructura de Cada Evidencia

Cada documento incluye las siguientes secciones:

1. **Identificación del sprint y objetivo**
2. **Alcance implementado**
3. **Trazabilidad con requisitos**
4. **Decisiones técnicas y justificación**
5. **Artefactos desarrollados** (backend, frontend, BD)
6. **Evidencia técnica verificable** (tests, resultados)
7. **Limitaciones, dependencias y trabajo pendiente**
8. **Conclusión técnica**

---

## Resumen de Sprints

| Sprint | Título | Tareas | BE | FE | DB | Migraciones |
|--------|--------|--------|----|----|----|-------------|
| 1 | Base del Sistema (JWT Real) | 15/15 | 7 | 5 | 3 | V1, V2 |
| 2 | Ingresos y Gastos | 17/17 | 9 | 7 | 1 | V3 |
| 3 | Deudas y Servicios | 18/18 | 8 | 8 | 2 | V4, V5 |
| 4 | Motor Financiero + Dashboard | 15/15 | 8 | 6 | 1 | V6 |
| 5 | IA Multi-Proveedor + Notificaciones | 26/26 | 15 | 8 | 3 | V7, V8, V10 |
| 6 | Reportes y Launch | 12/14 (2 parciales) | 6 | 7 | 2 | V11, V12 |
| **Total** | | **103/105** | **53** | **41** | **12** | **12 migraciones** |

---

## Convenciones

- Formato de archivo: `YYYY-MM-DD_sprint-N-nombre-descriptivo.md`
- Las capturas de pantalla reales van en `capturas/YYYY-MM-DD_nombre-feature/`, nombradas `pagina-anchoxalto.png`
- Las evidencias se actualizan si cambian decisiones de diseño o implementación
- Los diagramas referenciados están en `docs/analisis-diseno/diagramas/`

---

*Documento de evidencias — FinSmart MVP — Julio 2026*
