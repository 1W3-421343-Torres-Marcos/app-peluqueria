# skills.md — Comisiones

**Fase:** F10 · **Depende de:** profesionales, agenda, caja.

## Propósito
Calcular automáticamente lo que corresponde a cada profesional por período y registrar su liquidación.

## Modelo de datos
- Fuente: `AppointmentItem` de turnos **PAID** con `commissionPercentSnapshot` (congelado al pasar a COMPLETED).
- `CommissionSettlement`: id, professionalId, fromDay, toDay, baseCents, commissionCents, cashMovementId, settledAt, operator.

## Reglas de negocio
1. Comisión por ítem = `priceCentsSnapshot` ajustado proporcionalmente por descuentos/recargos del cobro × `commissionPercentSnapshot`.
2. Redondeo: half-up al centavo por ítem; el total es la suma de ítems redondeados.
3. Solo cuentan turnos PAID (totalmente cobrados) del período (fecha del turno). COMPLETED con saldo pendiente no liquida todavía (se muestra aparte como "pendiente de cobro"). Cancelados y ausentes = 0.
4. Ventas de productos no generan comisión (salvo que se configure en el futuro; fuera de alcance).
5. Liquidar genera un `CashMovement` tipo COMMISSION_PAYOUT y un `CommissionSettlement`; los ítems liquidados no se vuelven a liquidar (no hay solapamiento de períodos por profesional).
6. Un reverso de cobro sobre un turno ya liquidado (vuelve a COMPLETED) se refleja como ajuste negativo en la próxima liquidación; cuando vuelva a PAID, se liquida nuevamente.

## Pantallas y flujos
- **Comisiones:** selector de profesional y período (semana, mes, rango); total base, total comisión, detalle por turno; botón "Liquidar".
- **Historial de liquidaciones** por profesional.

## Tests obligatorios
Cálculo con varios %, descuentos prorrateados, redondeo, solo PAID (excluye COMPLETED con saldo, cancelados y ausentes), no doble liquidación, ajuste por reverso.

## Criterios de aceptación
- [x] Cálculo automático por profesional y período sobre turnos PAID.
- [x] Usa el % congelado en cada ítem (cambios de % no alteran el pasado).
- [x] Detalle por turno coherente con el total.
- [x] Liquidación registrada como egreso de caja auditado.
- [x] Sin doble liquidación.
- [x] Tests de comisiones verdes.
