# Rediseño de deudas: de préstamo fijo a crédito rotativo

Propuesta de diseño para soportar tarjetas de crédito reales en FinSmart.
Complementa el Nivel 1 de `docs/roadmap-cuentas-reales.md`. **Estado: idea aprobada,
pendiente de implementación** (la fase B debe diseñarse con SDD antes de codificar).

---

## 1. El problema

El modelo actual representa un **préstamo de monto fijo**:

```
Debt        → totalAmount, remainingAmount, interestRate, dueDate
DebtPayment → amount, paymentDate   (solo DISMINUYE remainingAmount)
```

Nace con un monto y solo puede bajar. Una tarjeta de crédito es **crédito rotativo**:

- La deuda **sube** con cada compra y **baja** con cada pago, todos los meses.
- Hoy no hay forma de subir el valor de la deuda: solo se registran pagos.
- Las compras diferidas tienen reglas propias: **1 cuota no genera intereses,
  2 o más cuotas sí**, con la tasa vigente al momento de la compra.

### Caso real (tarjeta Rappi)

| Evento | Movimiento | Saldo |
|--------|-----------|-------|
| Deuda inicial | — | $1.500.000 |
| Pago del mes | −$225.000 | $1.275.000 |
| TV a 3 cuotas | +$700.000 | **$1.975.000** |

Además, la compra del TV genera 3 cuotas de ~$233.333 de capital cada una, más el
interés mensual sobre el saldo pendiente de esa compra.

> Nota: el sistema debe hacer imposibles los errores de cálculo manual — este mismo
> ejemplo se calculó mal a mano una vez ($1.950.000). Ese es el punto del rediseño.

---

## 2. Principio de diseño

**La deuda no se edita: se deriva.**

El saldo nunca es un campo que alguien modifica a mano. Es el resultado de un libro
de movimientos (ledger). Beneficios:

- Trazabilidad total: cada peso del saldo tiene un movimiento que lo explica.
- El número siempre cuadra: no existen "ajustes manuales" que desincronicen datos.
- Historial gratis: el estado de la deuda en cualquier fecha es la suma hasta esa fecha.

---

## 3. Modelo propuesto

```
CreditCard
├── name, bank, franchise
├── creditLimit            → cupo total
├── monthlyRate            → tasa efectiva mensual (E.M.) vigente
├── cutoffDay              → día de corte del ciclo
└── paymentDueDay          → día límite de pago

CardMovement (ledger — el corazón del diseño)
├── card (FK)
├── type: PURCHASE | INSTALLMENT_PURCHASE | PAYMENT | INTEREST | FEE
├── amount                 → siempre positivo; el signo lo da el type
├── date
├── description
└── installmentPlan (FK, solo INSTALLMENT_PURCHASE)

InstallmentPlan (plan de cuotas por compra diferida)
├── movement (FK)          → la compra que lo originó
├── installmentCount       → número de cuotas elegido
├── rateAtPurchase         → tasa congelada al momento de la compra
└── installments[]         → cuota n: capital, interés, fecha de vencimiento, estado

saldo actual de la tarjeta = Σ movimientos (+PURCHASE, +INSTALLMENT_PURCHASE,
                                            −PAYMENT, +INTEREST, +FEE)
cupo disponible            = creditLimit − saldo actual
```

### Reglas de negocio (mercado colombiano)

1. **Compra a 1 cuota**: no genera intereses. Entra completa al pago del ciclo en curso.
2. **Compra a 2+ cuotas**: genera `InstallmentPlan` con la tasa vigente al momento de
   la compra (congelada — si el banco cambia la tasa después, las compras viejas no
   cambian). Amortización: capital/n por cuota + interés sobre el saldo pendiente de
   ESA compra (sistema de cuota decreciente; validar contra el extracto real del banco,
   algunos usan cuota fija/sistema francés).
3. **Ciclo de facturación**: los movimientos entre fecha de corte y fecha de corte
   forman el extracto del mes. El "pago del mes" = compras a 1 cuota del ciclo +
   cuotas que vencen + intereses + fees.
4. **Pagos**: pago total (no genera intereses corrientes), pago mínimo o abono libre.
   Un pago reduce el saldo global; la asignación a compras específicas puede
   simplificarse en la v1 (no imputar pago por compra).

### Integración con lo existente

- El saldo de cada tarjeta alimenta `deudas/` y el `debtRatio` del motor de análisis
  (hoy `DebtRepository.sumRemainingAmountByUser`).
- Los recordatorios de corte y fecha límite reutilizan los jobs y el
  `NotificationDispatcher` de `servicios/`.
- Cada `PURCHASE` puede vincularse a un `Expense` existente (la compra ya se registra
  como gasto; el movimiento de tarjeta es su reflejo en el pasivo).

---

## 4. Plan de implementación en dos fases

### Fase A — Cargos en deudas (esfuerzo bajo, valor inmediato)

Agregar `DebtCharge`: el espejo de `DebtPayment` con signo contrario.
`remainingAmount` puede subir cuando se gasta con la tarjeta.

- No rompe nada del modelo actual ni de la API existente.
- Resuelve el dolor de hoy: "la deuda de mi tarjeta solo puede bajar".
- Cambios: entidad + repository + endpoint `POST /api/debts/{id}/charges` + UI mínima.
- Puede entrar como una rama corta en cualquier momento.

### Fase B — Dominio de tarjetas completo (diseñar con SDD)

El modelo de la sección 3: `CreditCard` + ledger + planes de cuotas + ciclos.

- Es un cambio con reglas de negocio de verdad → **proposal, spec y design con SDD
  antes de una línea de código**.
- La fase A migra naturalmente: una `Debt` de tipo tarjeta pasa a derivar su saldo
  del ledger.
- Decisiones abiertas para la fase de diseño:
  - ¿Amortización de cuota fija (francés) o capital fijo? Validar contra extractos
    reales de los bancos objetivo.
  - ¿Los intereses se materializan como movimiento al cierre de ciclo (recomendado)
    o se calculan on-the-fly?
  - ¿Migración de deudas existentes: convertir o convivir dos tipos de deuda?
  - Vínculo `CardMovement` ↔ `Expense`: ¿obligatorio, opcional, automático?

---

## 5. Criterio para retomar

Cuando se decida arrancar: fase A directa (rama corta), fase B con `/sdd-new`.
El contexto completo de esta decisión está en la memoria del proyecto
(`architecture/cuentas-reales`) y en `docs/roadmap-cuentas-reales.md`.
