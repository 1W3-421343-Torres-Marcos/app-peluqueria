# skills.md — Agenda de turnos

**Fase:** F3 (base) · extendida en F5, F6, F7, F8 · **Depende de:** clientes, servicios, profesionales, configuración.

## Propósito
Gestionar los turnos del salón por profesional, garantizando que nunca haya conflictos de horario.

## Modelo de datos
- `Appointment`: id, clientId, professionalId, day (epochDay), startMin, endMin, status, adjustmentCents (descuento/recargo con motivo, lo fija caja), notes, recurrenceRuleId?, cancelReason?, createdAt, updatedAt, createdBy.
- `AppointmentItem`: id, appointmentId, serviceId, serviceNameSnapshot, priceCentsSnapshot, durationMinSnapshot, commissionPercentSnapshot (se fija al pasar a COMPLETED), colorFormulaId?, position.
- Derivados (no editables, calculados desde `CashMovement`): `totalCents` = Σ `priceCentsSnapshot` + `adjustmentCents`; `paidCents` = Σ pagos − Σ reversos; `balanceCents` = `totalCents` − `paidCents`.
- `ScheduleBlock`: id, professionalId? (null = todo el salón), startDay, endDay, startMin?, endMin? (null = día completo), type (VACATION, LUNCH, HOLIDAY, OTHER), reason, active.
- `RecurrenceRule`: id, intervalWeeks, untilDay? o occurrences?, templateItems.
- Enum `AppointmentStatus`: PENDING, CONFIRMED, COMPLETED, PAID, CANCELLED, NO_SHOW.
  - COMPLETED = servicio realizado, con saldo pendiente de cobro.
  - PAID = pagos registrados cubren el total (`paidCents ≥ totalCents`).

## Reglas de negocio
1. Duración total = suma de `durationMinSnapshot` de los ítems; `endMin = startMin + duración`, redondeada hacia arriba al bloque mínimo de configuración.
2. **Disponibilidad** (`AvailabilityChecker`): un turno es válido solo si
   - está dentro del horario laboral del profesional ese día de la semana y del horario general del salón;
   - el día no es día libre del profesional;
   - no se solapa con otro turno que ocupa agenda (PENDING, CONFIRMED, COMPLETED o PAID) del mismo profesional; intervalos semiabiertos `[inicio, fin)` (fin = inicio del siguiente es válido);
   - no cae en un `ScheduleBlock` del profesional o general.
3. El profesional debe estar habilitado para todos los servicios del turno.
4. Precio, nombre y duración se **congelan** al crear el turno. Editar un turno existente conserva los snapshots de ítems ya cargados.
5. Transiciones de estado permitidas (`StatusTransitions`):
   - PENDING → CONFIRMED | COMPLETED | CANCELLED | NO_SHOW
   - CONFIRMED → COMPLETED | CANCELLED | NO_SHOW
   - COMPLETED → PAID (**automático** al cubrirse el total)
   - PAID → COMPLETED (**automático** si un reverso de cobro deja `balanceCents > 0`)
   - COMPLETED → CONFIRMED (corrección auditada, solo si no hay cobros asociados)
   - CANCELLED y NO_SHOW son finales.
   - Cualquier otra transición se rechaza con `DomainError`. PAID nunca se asigna a mano.
6. Al pasar a COMPLETED se congela `commissionPercentSnapshot` de cada ítem (la corrección a CONFIRMED lo libera).
7. NO_SHOW incrementa `client.noShowCount`. CANCELLED libera el horario y dispara sugerencias de lista de espera (F8).
8. Reprogramar = cambiar día/hora/profesional revalidando disponibilidad; conserva id e historial. Solo turnos PENDING/CONFIRMED.
9. Recurrentes: generar cada N semanas; los conflictos se informan y se omiten (no se crean turnos inválidos). Editar/cancelar admite "solo este" o "este y siguientes".
10. Turnos nunca se borran: cancelación = estado. Turnos pasados no editables salvo estado/notas.
11. Bloqueo nuevo que pisa turnos existentes: se permite crearlo pero se listan los turnos afectados para reprogramar.
12. Visitas del cliente = turnos COMPLETED o PAID.

## Pantallas y flujos
- **Día:** línea de tiempo por bloques (granularidad de configuración), columnas o filtro por profesional, `AppointmentCardView`, bloqueos en gris. FAB "Nuevo turno".
- **Semana:** lunes–domingo, ocupación por día; toque abre el día.
- **Crear turno:** buscar cliente (o alta rápida nombre+teléfono) → servicios (multi) → profesional (solo habilitados) → horarios libres sugeridos (`TimeSlotGrid`) → confirmar.
- **Detalle:** según estado — PENDING/CONFIRMED: confirmar, completar, **completar y cobrar** (F5), ausente, cancelar, reprogramar, recordar (F6). COMPLETED: cobrar saldo (F5), corregir a confirmado. PAID: ver pagos, reverso (F5). Muestra total, pagado y saldo.
- **Bloqueos:** lista y formulario.

## Tests obligatorios
Solapamientos (bordes, contenidos, idénticos; COMPLETED/PAID ocupan agenda), fuera de horario, día libre, bloqueo parcial/total, todas las transiciones válidas e inválidas, paso automático COMPLETED ↔ PAID, congelado de comisión al completar, cálculo de duración y redondeo, generación recurrente con conflictos.

## Criterios de aceptación
- [x] Vista diaria y semanal funcionan y filtran por profesional.
- [x] Alta, edición, reprogramación y cancelación operativas.
- [x] Los 6 estados con transiciones validadas por tests (incluido COMPLETED ↔ PAID automático).
- [x] Turno con varios servicios suma duraciones correctamente.
- [x] Imposible guardar turnos solapados, fuera de horario, en día libre o en bloqueo (tests verdes).
- [x] Precio congelado: cambiar el catálogo no altera turnos existentes.
- [x] `paidCents`/`balanceCents` derivados de caja, no editables.
- [x] Turnos recurrentes con manejo de conflictos.
- [x] Bloqueos de agenda visibles y respetados.
- [x] Ausente incrementa el contador del cliente.
- [x] Crear turno para cliente existente en ≤ 4 toques.
- [x] Fluido con 50.000 turnos.
