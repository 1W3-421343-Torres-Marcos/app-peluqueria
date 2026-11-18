# skills.md — Caja

**Fase:** F5 · integración de productos en F9 · **Depende de:** agenda, clientes, configuración, backup.

## Propósito
Registrar todo el dinero que entra y sale del salón de forma inmutable y auditable, con cierre diario.

## Modelo de datos
- `CashMovement`: id, day, timestamp, type (APPOINTMENT_PAYMENT, PRODUCT_SALE, OTHER_INCOME, EXPENSE, COMMISSION_PAYOUT, REVERSAL), method (CASH, TRANSFER, CARD, MERCADO_PAGO), amountCents (> 0; el signo lo da el tipo), concept, appointmentId?, clientId?, productId?, quantity?, reversesMovementId?, operator, closingId?.
- `CashClosing`: id, day, expectedByMethod, countedByMethod, differenceCents, notes, closedAt, operator.
- `AuditLog` (global) por cada movimiento, reverso y cierre.

## Reglas de negocio
1. **Inmutabilidad:** un movimiento nunca se edita ni borra. Corrección = movimiento REVERSAL que referencia al original (mismo monto y método) + nuevo movimiento correcto.
2. **Cobro de turno:** solo sobre turnos COMPLETED con `balanceCents > 0` (o PAID que volvió a COMPLETED por un reverso). Total = suma de `priceCentsSnapshot` + ajuste (descuento/recargo con motivo obligatorio). Se permiten pagos parciales y divididos por método. El turno queda COMPLETED hasta cubrir el total y pasa **solo** a PAID.
3. **"Completar y cobrar":** acción rápida desde un turno PENDING/CONFIRMED que ejecuta completar (congela comisiones) + cobro en **una única transacción Room**; si falla cualquier parte, no se aplica nada. Pago simple en ≤ 3 toques.
4. **Reverso de cobro:** si deja `paidCents < totalCents`, el turno vuelve automáticamente de PAID a COMPLETED.
5. **Por cobrar:** turnos COMPLETED con `balanceCents > 0`, con monto adeudado.
6. Ventas sueltas: concepto + monto + método (F9: producto + cantidad, descuenta stock en la misma transacción).
7. Gastos: concepto, categoría libre, monto, método.
8. **Cierre diario:** esperado por método = Σ ingresos − Σ egresos del día; el operador ingresa lo contado (al menos efectivo); se guarda la diferencia. Tras el cierre, el día no acepta movimientos; correcciones posteriores se registran en el día actual referenciando el original.
9. Al completar el cierre se encola un backup `CASH_CLOSE` (trabajo único, política KEEP; ver `backup/skills.md`). No bloquea la UI.
10. Un día sin cerrar se advierte al abrir la caja del día siguiente.
11. Todo movimiento registra fecha, operador, monto y concepto (auditable).

## Pantallas y flujos
- **Caja (tab):** total del día por método, lista de movimientos, accesos Cobrar / Venta / Gasto / Cerrar caja, contador "Por cobrar".
- **Por cobrar:** lista de turnos COMPLETED con saldo (cliente, fecha, monto adeudado); toque → Cobrar.
- **Cobrar / Completar y cobrar:** desde detalle de turno o "Por cobrar"; saldo precargado, métodos como chips grandes, opción "dividir pago", confirmar.
- **Cierre:** tabla esperado/contado/diferencia, confirmación.
- **Recibo PDF** compartible con logo y "Metanoia Hair Salon".

## Tests obligatorios
Cierre de caja (múltiples métodos, reversos, egresos, diferencia), pagos parciales y divididos, paso automático COMPLETED → PAID al cubrir y PAID → COMPLETED por reverso, "Completar y cobrar" atómico (rollback), rechazo de cobro sobre estados no válidos, rechazo de movimientos en día cerrado, encolado de backup CASH_CLOSE.

## Criterios de aceptación
- [x] Cobro solo sobre turnos COMPLETED con saldo; pagos parciales y divididos.
- [x] Turno pasa solo a PAID al cubrir el total y vuelve a COMPLETED si un reverso deja saldo.
- [x] "Completar y cobrar" atómico en ≤ 3 toques.
- [x] Lista "Por cobrar" con montos adeudados.
- [x] Ventas sueltas y gastos registrados.
- [x] Movimientos inmutables; correcciones solo por reverso.
- [x] Cierre diario con esperado vs contado y diferencia; día cerrado bloqueado.
- [x] Cierre de caja dispara backup CASH_CLOSE.
- [x] Auditoría completa (fecha, operador, monto, concepto).
- [x] Recibo PDF con identidad de marca.
- [x] Tests de cierre de caja y cobros verdes.
