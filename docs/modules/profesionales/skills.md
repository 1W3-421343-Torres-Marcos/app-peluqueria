# skills.md — Profesionales

**Fase:** F2 · **Depende de:** servicios, configuración.

## Propósito
Datos de cada profesional, su disponibilidad y su esquema de comisión.

## Modelo de datos
- `Professional`: id, name, phone?, colorTag? (gris de la escala, para distinguir en agenda), commissionPercent (0–100, 2 decimales, guardado como entero en centésimas), active, createdAt, updatedAt.
- `WorkSchedule`: id, professionalId, dayOfWeek (1=lunes…7), startMin, endMin. Permite varios tramos por día (ej. 09:00–13:00 y 15:00–20:00).
- `ProfessionalDayOff`: id, professionalId, day (fechas puntuales). Las vacaciones largas se cargan como `ScheduleBlock` en agenda.
- `ProfessionalServiceCrossRef`: professionalId, serviceId.

## Reglas de negocio
1. Tramos de un mismo día no pueden solaparse; `startMin < endMin`; deben caer dentro del horario general del salón.
2. Solo servicios activos pueden asignarse.
3. Cambiar el % de comisión aplica a turnos que pasen a COMPLETED **después** del cambio (el % se congela en cada ítem al completar el turno).
4. Desactivar un profesional con turnos futuros: bloquear la acción y listar esos turnos para reasignar o cancelar.
5. Baja lógica; nunca borrar.
6. Debe existir al menos un profesional activo para crear turnos (el dueño puede ser profesional).

## Pantallas y flujos
- **Lista** con estado y servicios resumidos.
- **Formulario:** datos, % comisión, servicios (checklist por categoría), editor de horario semanal (7 filas, tramos agregables, "copiar a todos los días").
- **Días libres:** calendario para marcar fechas.

## Tests obligatorios
Validación de tramos, solapamiento de tramos, rango de comisión, bloqueo de desactivación con turnos futuros.

## Criterios de aceptación
- [x] Alta/edición/baja lógica de profesionales.
- [x] Horario semanal con múltiples tramos por día validado.
- [x] Días libres cargables y respetados por la agenda.
- [x] Asignación de servicios que filtra el flujo de crear turno.
- [x] % de comisión configurable y congelado por ítem.
