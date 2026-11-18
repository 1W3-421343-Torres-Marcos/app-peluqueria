# skills.md — Servicios

**Fase:** F2 · **Depende de:** —

## Propósito
Catálogo de servicios del salón con precio y duración, base para turnos, caja y comisiones.

## Modelo de datos
- `SalonService`: id, name, category, priceCents, durationMin, isColorService (habilita ficha de color), active, sortOrder, createdAt, updatedAt.
- Categorías iniciales (editables como texto con sugerencias): Corte, Color, Mechas, Alisado, Tratamiento, Peinado, Barba.

## Reglas de negocio
1. Nombre único entre servicios activos de la misma categoría.
2. Precio ≥ 0 (en centavos). Duración > 0 y múltiplo del bloque mínimo de configuración.
3. **Nunca se borra:** desactivar (`active = false`) lo oculta para turnos nuevos, pero sigue visible en historial y reportes.
4. Cambiar precio/duración no afecta turnos existentes (snapshots en `AppointmentItem`).
5. Un servicio inactivo no puede asignarse a turnos nuevos ni a profesionales.

## Pantallas y flujos
- **Lista** agrupada por categoría, filtro Activos/Inactivos, precio y duración visibles.
- **Formulario:** nombre, categoría, precio (teclado numérico, formato `$`), duración (selector por bloques), es coloración, activo.

## Tests obligatorios
Validaciones de precio/duración/nombre, desactivación, no asignable si inactivo.

## Criterios de aceptación
- [x] Alta y edición de servicios con validaciones.
- [x] Desactivar sin borrar; historial intacto.
- [x] Lista agrupada por categoría con filtro.
- [x] Cambios de precio no alteran turnos existentes.
- [x] Marca de coloración disponible para F7.
