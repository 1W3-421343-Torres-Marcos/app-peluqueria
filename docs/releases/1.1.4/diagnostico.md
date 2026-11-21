# 1.1.4 — diagnostico

## Incidencia

El sistema rechazaba cualquier turno cuya duracion estimada se superpusiera con una cita existente del mismo profesional.

## Evidencia

- `AvailabilityChecker.check()` devolvia `OVERLAPS_EXISTING_APPOINTMENT`.
- Los casos de uso convertian ese motivo en `DomainError.CONFLICT`.
- `SlotAvailabilityLoader` excluia esas opciones de `freeStartMinutes`.
- `TimeSlotGrid` deshabilitaba las opciones no disponibles.

## Causa

La disponibilidad horaria combinaba dos conceptos distintos: bloques que impiden asignar y solapamientos que solo requieren informacion al operador.

## Correccion

- Se incorporaron reglas para distinguir solapamiento permitible de bloque real.
- Alta normal permite el solapamiento y pide confirmacion.
- Recurrencias permiten el solapamiento y lo reportan como advertencia.
- Reprogramacion permite el solapamiento y pide confirmacion.
- Lista de espera conserva el rechazo de solapamientos.
