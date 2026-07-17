# Ideas adicionales de producto (fuera del roadmap actual)

> Investigación puntual pedida durante sprint1. Complementa
> `docs/roadmap-saas-cuentas-reales.md` — **no lo duplica**: cada idea de acá está fuera
> de los 3 niveles de cuentas reales, de la automatización de correo/notificaciones y del
> modelo de negocio ya documentados ahí. Contrastado contra el código actual (dominios
> `analisis/`, `servicios/`, `reportes/`, `deudas/`) para no proponer algo que ya existe, y
> contra el estado del mercado en 2026 (apps de finanzas personales EE.UU. y fintech
> Colombia) para priorizar lo que ya se volvió tabla-stakes.

## Ya existe — no confundir con lo de abajo

Antes de proponer, esto es lo que el backend **ya calcula** y que una función nueva no debe
reinventar: `debtRatio`, `savingsRate` y `expenseRatio` en `FinancialAnalysisService`
(expuestos en `AnalysisSummaryResponse`), y una predicción de fin de mes ya corriendo en
`MonthEndPredictionJob`. Las ideas de "score de salud financiera" de abajo **componen**
sobre estos números existentes, no los recalculan.

---

## 1. Presupuestos por categoría con alertas

**Qué es:** el usuario define un límite mensual por categoría (ej. "Comida: $700.000") y
recibe una alerta al acercarse o superarlo. Es la función #1 en casi todas las apps de
presupuesto revisadas (Forbes Advisor, NerdWallet, PocketGuard) — hoy KoroFin no la tiene:
solo compara contra reglas fijas del motor de análisis (ej. "gasto en comida > 30% del
ingreso"), no contra un límite que el usuario mismo defina.

- **Valor:** alto — es la función que más piden los usuarios de presupuesto personal, y
  KoroFin ya tiene categorías y el `NotificationDispatcher` de `servicios/` para avisar.
- **Esfuerzo:** medio. Nuevo dominio o extensión de `analisis/`: entidad `Budget`
  (`categoryId`, `monthlyLimit`, `period`), un job que compare gasto acumulado del mes vs.
  límite, y reutiliza los canales de notificación existentes.
- **Dependencia:** ninguna del roadmap actual — se puede construir en paralelo a Nivel 1/2/3.

## 2. Metas de ahorro

**Qué es:** el usuario crea una meta ("Vacaciones: $3.000.000 para diciembre") y registra
aportes; la app muestra progreso. Es la función "Bolsillos" de Nequi y "Metas de Ahorro" —
el patrón más replicado en fintech colombiana (Nequi, Tyba) según la búsqueda de mercado.

- **Valor:** alto — enganche emocional fuerte, diferencia una app de "registro" de una de
  "planificación". Encaja bien con el ángulo de KoroFin de motor de análisis propio.
- **Esfuerzo:** medio. Nuevo dominio pequeño: entidad `SavingsGoal` (`name`, `targetAmount`,
  `targetDate`, `currentAmount`), aportes como movimientos simples (puede ser un tipo de
  `Income` interno o una tabla propia de aportes).
- **Dependencia:** ninguna.

## 3. Detección de suscripciones y cargos duplicados

**Qué es:** dos funciones relacionadas que hoy no existen: (a) detectar automáticamente
gastos recurrentes que el usuario nunca declaró como `RecurringPayment` (agrupando por
descripción similar + monto similar + periodicidad mensual), y (b) alertar sobre un posible
cargo duplicado (mismo monto, misma categoría, mismo día o consecutivo). Rocket Money
construyó buena parte de su crecimiento sobre exactamente esto — "la mayoría de los hogares
no se da cuenta de cuánto suman sus suscripciones hasta que una app se lo muestra junto".

- **Valor:** alto — resuelve un dolor real y es fácil de demostrar en una demo ("mirá todo lo
  que gastás en suscripciones que no sabías").
- **Esfuerzo:** medio. La detección de recurrencia es un query de agrupación sobre
  `expenses` (sin IA necesariamente — patrón determinístico primero, IA como mejora
  posterior). La detección de duplicados es una regla simple sobre inserciones cercanas en
  tiempo con mismo monto/categoría.
- **Dependencia:** se potencia con el Nivel 2 (extractos bancarios) y el pipeline de correo,
  pero **no los necesita** — funciona igual de bien sobre datos cargados manualmente o vía
  quick-add.

## 4. Score de salud financiera

**Qué es:** un único número/letra (ej. 0-100 o A-F) que resume `debtRatio` + `savingsRate` +
`expenseRatio` (ya calculados hoy) en un solo indicador visual, con una breve explicación de
qué lo está bajando. Varias apps de 2026 (PocketGuard, Origin) usan esto como la pantalla de
entrada del dashboard.

- **Valor:** medio-alto — no agrega datos nuevos, pero mejora mucho la primera impresión del
  producto (un número es más digerible que 3 ratios sueltos).
- **Esfuerzo:** bajo — es una función de composición sobre datos que `FinancialAnalysisService`
  ya expone. Es la idea de más valor por esfuerzo de esta lista.
- **Dependencia:** ninguna — se puede construir hoy mismo sin esperar nada del roadmap.

## 5. Exportación de reportes (CSV/PDF)

**Qué es:** descargar el reporte mensual (`reportes/`, `ReportService` ya existe) como
CSV o PDF, para declaraciones, contadores o registro personal fuera de la app.

- **Valor:** medio — no es lo más pedido, pero es tabla-stakes esperado en cualquier app
  financiera seria, y hoy KoroFin no lo tiene pese a ya calcular los datos.
- **Esfuerzo:** bajo — el dominio `reportes/` ya arma los datos; falta solo la capa de
  serialización (CSV es trivial; PDF vía una librería tipo OpenPDF/iText).
- **Dependencia:** ninguna.

## 6. Recordatorio de fechas fiscales (declaración de renta, Colombia)

**Qué es:** un recordatorio de temporada según el calendario de la DIAN (declaración de
renta de personas naturales), reutilizando el `NotificationDispatcher` ya existente. Es
100% específico de mercado — ningún competidor genérico (YNAB, Monarch) lo tiene, y es
exactamente el tipo de diferencial de posicionamiento que el roadmap ya identifica como
ángulo de KoroFin (foco en mercados donde la agregación tipo Plaid es débil).

- **Valor:** medio — nicho pero genuino diferencial local, esfuerzo mínimo.
- **Esfuerzo:** muy bajo — una fecha fija (o rango) por año + una notificación, sin lógica de
  negocio nueva.
- **Dependencia:** ninguna. Ojo: las fechas de la DIAN varían por año y por últimos dígitos de
  cédula — si se construye, verificar el calendario vigente cada año, no hardcodear una fecha
  para siempre.

## 7. Modo offline / PWA

**Qué es:** que el registro manual de un gasto funcione sin conexión (service worker +
cola local que sincroniza al recuperar señal). Relevante en Colombia por zonas con
conectividad irregular — un caso de uso real que YNAB/Monarch (mercados con conectividad
consistente) no priorizan tanto.

- **Valor:** medio — no es un pedido explícito de los usuarios todavía, pero previene una
  frustración silenciosa (perder un registro por quedarse sin señal).
- **Esfuerzo:** medio-alto — requiere service worker, estrategia de cache, cola de
  sincronización y manejo de conflictos si el mismo dato se edita offline en dos lugares.
- **Dependencia:** ninguna, pero tiene más sentido **después** de la migración a
  react-query ya listada en el backlog técnico del roadmap (el cache casero actual no está
  pensado para persistencia offline).

---

## Explícitamente NO incluidas acá (ya evaluadas en el roadmap, no duplicar)

- **Cuentas compartidas / división de gastos entre personas** — el roadmap ya lo evalúa y
  decide explícitamente no construir `accounts`/`account_members` "antes de que exista ese
  requisito real". Cualquier función de "meta compartida" o "presupuesto de pareja" depende
  de ese prerequisito no resuelto — no se propone acá para no repetir esa decisión.
- **Multi-moneda** — no se propone: KoroFin es hoy 100% COP, sin señal de usuarios con
  necesidad real de otra moneda. Agregarlo sin esa señal es complejidad especulativa
  (justo lo que las convenciones del repo piden evitar).
- **Reglas de categorización por remitente/palabra clave** (`automation_rules`) — ya está en
  el boceto de tablas del roadmap (sección "Módulos backend nuevos"), asociado al pipeline de
  correo/notificaciones. No se repite acá.
