# ¿Vale la pena importar extractos bancarios?

**Fecha:** 2026-07-21

## La duda

FinSmart es una app donde los gastos e ingresos se cargan a diario o casi a diario (manual, o por el bot de Telegram). El extracto bancario llega recién a fin de mes o a principios del siguiente. Si el usuario ya cargó sus movimientos cuando ocurrieron, ¿qué sentido tiene importar un extracto que en teoría solo repite lo que ya está guardado?

## Lo que el código ya resuelve

`extractos/service/dedup/DuplicateDetector.java` ya está pensado para exactamente este escenario. Un movimiento del extracto se marca como posible duplicado si:

1. El **monto es exactamente igual** (comparación estricta de `BigDecimal`).
2. La **fecha está a ±3 días** de un movimiento existente (`MAX_DAY_DIFFERENCE_ALLOWED`) — cubre la diferencia típica entre "fecha de compra" y "fecha de compensación bancaria".
3. La **descripción es similar** (containment o Jaccard ≥ 0.34 sobre palabras normalizadas).

Si las tres condiciones se cumplen, la fila se marca `isDuplicate=true` en el preview — **no se bloquea**, el usuario decide fila por fila qué confirmar. Es decir: el caso "cargué esto a mano el 3 de marzo, el extracto de marzo (que llega en abril) trae la misma transacción" **ya se detecta correctamente** hoy.

Conclusión parcial: el miedo de "voy a duplicar todo lo que ya cargué" **no es el problema real** — ya está bastante bien cubierto.

## Dónde SÍ está el valor real

El valor de importar un extracto no es "evitar cargar dos veces lo mismo". Es **capturar lo que el usuario nunca cargaría a mano porque no lo sabe hasta que el banco lo informa**:

- Intereses cobrados.
- Comisiones y cargos bancarios (mantenimiento, cuota de manejo, etc.).
- Cargos automáticos que el usuario configuró hace tiempo y se olvidó.
- Transferencias de terceros que el usuario no registró porque no las inició él.

Estos movimientos **no tienen ningún modelado especial hoy**: `MovementType.java` solo distingue `EXPENSE`/`INCOME`, sin ninguna marca de "esto lo aportó el extracto y no el usuario". El sistema técnicamente ya captura este valor (los importa igual, como cualquier otro movimiento), pero **no se lo comunica al usuario** — para él, un extracto importado se ve como una lista plana de filas, sin resaltar qué es genuinamente nuevo información versus qué es una confirmación de algo que ya sabía.

## Conclusión

**Sí tiene valor**, pero hoy está mal aprovechado/comunicado en el producto. El dedup evita el problema que preocupaba (duplicar lo ya cargado); lo que falta es dar vuelta el mensaje: en vez de "importá tu extracto", la propuesta de valor real es "encontrá cargos y movimientos que no sabías que tenías".

### Recomendación (para más adelante, no para hoy)

Agregar un campo `source` (`MANUAL` / `TELEGRAM` / `STATEMENT_IMPORT`) a `Expense`/`Income`, para que:
- El preview de importación pueda destacar "encontramos N movimientos nuevos que no habías registrado" en vez de una lista sin jerarquía.
- El dashboard pueda mostrar cuánto de lo registrado vino de cada canal — información útil por sí misma.

### Hallazgo aparte (bug, no forma parte de la pregunta de valor)

`StatementImportService.confirm()` no vuelve a correr la detección de duplicados al momento de confirmar — si el usuario confirma una fila que el preview marcó como posible duplicado, se crea igual. No se tocó en este sprint (fuera de alcance), pero vale la pena una tarea futura para cerrar esa ventana.
