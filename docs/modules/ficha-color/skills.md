# skills.md — Ficha de color

**Fase:** F7 · **Depende de:** clientes, servicios (`isColorService`), agenda.

## Propósito
Guardar las fórmulas de coloración de cada cliente para repetir resultados con exactitud.

## Modelo de datos
- `ColorFormula`: id, clientId, appointmentId?, professionalId?, day, exposureMin, developerVolume? (ej. "20 vol"), notes, result? (texto libre), deletedAt?, createdAt.
- `ColorFormulaComponent`: id, formulaId, brand, productLine?, shade (tono, ej. "7.1"), amount (decimal como texto normalizado o gramos ×10), unit (g, ml, partes), position.
- Proporción expresada por cantidades de cada componente (ej. 30 g 7.1 + 30 g 7.0 + 90 ml oxidante).

## Reglas de negocio
1. Al menos un componente con marca y tono.
2. Una fórmula vinculada a un turno COMPLETED o PAID no se edita: se crea una nueva versión (duplicar) para conservar el historial.
3. "Repetir última fórmula": al crear un turno con un servicio de coloración, se ofrece copiar la fórmula más reciente del cliente y vincularla al ítem (`AppointmentItem.colorFormulaId`) con un toque; queda como borrador editable hasta completar el turno.
4. Baja lógica (`deletedAt`); nunca borrado físico.
5. Las fórmulas de un cliente se ordenan por fecha descendente.

## Pantallas y flujos
- **Pestaña en ficha de cliente:** lista cronológica (fecha, tonos principales, profesional).
- **Detalle/edición:** componentes (filas agregables), exposición, oxidante, notas, resultado; acción "Duplicar".
- **En crear turno:** chip/botón "Repetir última fórmula" visible solo si hay servicio de coloración y fórmula previa.

## Tests obligatorios
Validación de componentes, obtención de la última fórmula, regla de versión para fórmulas cerradas.

## Criterios de aceptación
- [ ] Alta, edición (si no está cerrada) y duplicado de fórmulas.
- [ ] Historial por cliente ordenado por fecha.
- [ ] Repetir última fórmula con un toque al crear turno de coloración.
- [ ] Fórmulas de turnos COMPLETED/PAID inmutables.
- [ ] Incluidas en backup y restauración.
