# Roadmap: cuentas y tarjetas reales en FinSmart

Cómo evolucionar FinSmart desde el registro manual de movimientos hacia funcionalidades
conectadas con las finanzas reales de los usuarios. Son **tres niveles incrementales**:
cada uno aporta valor por sí solo y prepara el siguiente.

---

## Nivel 1 — Modelar cuentas y tarjetas dentro de la app (sin terceros)

**Qué es:** un nuevo dominio `cuentas/` donde el usuario registra sus productos
financieros (tarjetas de crédito, cuentas de ahorro) y asocia sus movimientos a ellos.

**Qué desbloquea para el usuario:**

- Tarjetas de crédito con **cupo, fecha de corte y fecha límite de pago**.
- Alertas del tipo "tu tarjeta corta en 3 días, llevas $X en este ciclo"
  (reutiliza los jobs y notificaciones existentes).
- Cupo disponible y porcentaje de utilización por tarjeta.
- La deuda de cada tarjeta alimenta el dominio `deudas/` y el motor de análisis.

**Costo:** cero terceros, cero regulación. Es puro modelado de dominio.

### 1.1 Rediseño de deudas: de préstamo fijo a crédito rotativo

> Diseño detallado (modelo, reglas, fases y decisiones abiertas) en
> `docs/rediseno-deudas-tarjetas.md`.

El modelo actual (`Debt` con `totalAmount` y `remainingAmount` que solo baja con
`DebtPayment`) representa un **préstamo**: nace con un monto y solo disminuye.
Una tarjeta de crédito es **crédito rotativo**: la deuda sube con cada compra,
baja con cada pago y genera intereses según cómo se difiera cada compra.

**Principio de diseño: la deuda no se edita, se deriva.** El saldo nunca debe ser un
campo que alguien modifica a mano; debe ser el resultado de un libro de movimientos
(ledger). Así hay trazabilidad total y el número siempre cuadra.

Modelo propuesto:

```
CreditCard                      → producto: nombre, banco, cupo, tasa E.M. (efectiva
                                  mensual), día de corte, día límite de pago
CardMovement (ledger)           → cada evento con signo:
  - PURCHASE  (+)                 compra a 1 cuota
  - INSTALLMENT_PURCHASE (+)      compra diferida (guarda # de cuotas y tasa aplicada)
  - PAYMENT   (−)                 pago (total, mínimo o abono libre)
  - INTEREST  (+)                 intereses del ciclo
  - FEE       (+)                 cuota de manejo, avances, seguros

saldo actual = suma de movimientos     (nunca un campo editable)
```

Reglas de negocio clave (mercado colombiano):

- **1 cuota no genera intereses; 2 o más sí.** Cada compra diferida guarda su número
  de cuotas y la tasa vigente al momento de la compra.
- Cada compra diferida genera su **plan de cuotas** (amortización): capital/n cuotas
  más interés sobre el saldo pendiente de esa compra.
- El **ciclo de facturación** agrupa movimientos entre fechas de corte y produce el
  "pago del mes" (suma de cuotas que vencen + compras a 1 cuota del ciclo).

Ejemplo (el caso Rappi): deuda de $1.500.000 → pago del mes de $225.000 → compra de
TV por $700.000 a 3 cuotas ⇒ saldo = 1.500.000 − 225.000 + 700.000 = **$1.975.000**,
y la compra del TV genera 3 cuotas de ~$233.333 de capital más el interés de cada mes
sobre el saldo pendiente del TV.

**Implementación sugerida en dos fases:**

| Fase | Alcance | Esfuerzo |
|------|---------|----------|
| A — cargos en deudas | Agregar `DebtCharge` (espejo de `DebtPayment` con signo +): la deuda existente puede subir. Resuelve el caso rotativo básico sin romper nada. | Bajo |
| B — dominio tarjetas | `CreditCard` + ledger + plan de cuotas + ciclo de corte + intereses. La fase A migra naturalmente: un `Debt` de tipo tarjeta pasa a derivarse del ledger. | Medio/alto — diseñar con SDD |

## Nivel 2 — Importar extractos bancarios (datos reales sin credenciales)

**Qué es:** el usuario descarga el extracto de su banco (CSV/Excel, todos los bancos
colombianos lo ofrecen) y lo sube a FinSmart. El backend lo parsea y crea las
transacciones, categorizadas por la IA que ya existe en la app.

**Por qué es valioso:** datos reales sin pedirle credenciales bancarias a nadie,
sin costos de terceros y sin carga regulatoria. Complementa el nivel 1 (los
movimientos importados se asocian a la cuenta/tarjeta correspondiente).

**Consideraciones:** cada banco tiene su formato → empezar con 1-2 bancos
(parser por banco detrás de una interfaz común) y deduplicar contra lo ya registrado
(fecha + monto + descripción).

## Nivel 3 — Agregación automática vía Open Finance (terceros)

**Qué es:** conexión directa a los bancos con consentimiento del usuario, a través de
un agregador. **Nunca se almacenan credenciales bancarias propias** — el agregador
entrega tokens y datos normalizados.

**Opciones para Colombia/LatAm:**

- **Belvo** — el jugador fuerte de Open Finance en LatAm (Colombia, México, Brasil).
  Cuentas, saldos y movimientos. Tiene sandbox gratuito para desarrollo.
- **Prometeo** — alternativa con cobertura en varios países de la región.
- Plaid (el más conocido) casi no cubre Colombia: descartado.

**Realidad de costos:** el sandbox es gratis y es un excelente proyecto de
aprendizaje/portfolio, pero producción cobra por usuario/conexión y exige madurez de
compliance. Para FinSmart hoy: **explorar en sandbox, no prometer a usuarios.**
Verificar precios vigentes en belvo.com antes de comprometerse.

---

## Resumen ejecutivo del proyecto (actualizado 2026-07-10)

### Hecho y mergeado

| Qué | Dónde |
|-----|-------|
| Refactor por dominios: backend en 9 dominios (`common`, `usuario`, `ingresos`, `gastos`, `deudas`, `servicios`, `analisis`, `ia`, `reportes`) + frontend pulido | `main` (producción) |
| Extracción de componentes de `asistente-ia` (310→137 líneas) y `reportes` (269→119) | `main` |
| Optimización de queries del análisis: summary ~19→9 consultas, recommendations ~19→8, log SQL sin duplicar (verificado en runtime) | `develop` |
| Convenciones del repo en español (`docs/convenciones.md` + `CLAUDE.md`) | `main` |
| Emails Brevo: diagnóstico completo (IP autorizada → remitente verificado → cuenta requiere activación manual; Brevo pidió dominio propio); credenciales SMTP verificadas | Configuración |
| Decisión: mantener módulo de IA custom, no migrar a Spring AI (revisar si llega streaming/tools/RAG) | Documentada |
| Decisión: n8n solo para canales e integraciones (patrón backend→webhook→n8n); la lógica de negocio queda en el backend | Documentada |

### Pendiente (en orden)

1. **Dominio de GitHub Students** (esperando aprobación) → autenticarlo en Brevo
   (DKIM/DMARC) → cambiar `MAIL_FROM` → prueba final de entrega → eliminar el test
   manual `EmailSmokeManualTest.java` (está sin trackear, a propósito).
2. **Subir secrets a GitHub Actions** (18 en total, valores nuevos del `.env`;
   el deploy a Cloud Run los necesita).
3. **PR `develop` → `main`** (en pausa hasta completar 1 y 2) + limpieza de ramas.
4. **Rama n8n**: Docker local + primer flujo (bot de Telegram para registrar gastos).
5. **Nivel 1 de este documento**: dominio de cuentas/tarjetas + rediseño de deudas
   (fase A: `DebtCharge`; fase B: tarjeta rotativa con SDD).

### Backlog técnico (sin urgencia)

- Cache de datos en el frontend (SWR/react-query) — el mayor impacto de perf restante:
  hoy cada cambio de módulo re-fetchea todo.
- `JwtAuthenticationFilter` hace `existsById` (1 consulta) en cada request — cachear o
  confiar en el token para operaciones no sensibles.
- Mover el upsert de `financial_analysis` fuera del `GET /api/analysis/summary`.
- `EmailNotificationSender` se traga las excepciones (`log.warn`) — endurecer cuando
  las notificaciones sean críticas.
- Warning de serialización de `PageImpl` en los logs — migrar a `PagedModel`
  (`@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)`).
