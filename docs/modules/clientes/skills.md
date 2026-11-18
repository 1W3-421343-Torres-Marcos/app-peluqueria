# skills.md — Clientes

**Fase:** F2 (base) · historial en F3/F5 · **Depende de:** configuración.

## Propósito
Registro de clientes con búsqueda instantánea y visión completa de su relación con el salón.

## Modelo de datos
- `Client`: id, name, nameNormalized, phone (E.164 sin `+`), birthday? (día y mes obligatorios, año opcional), notes (alergias, preferencias), noShowCount, active, createdAt, updatedAt.
- Índices: `nameNormalized`, `phone` (único entre activos).
- Derivados (consultas, no columnas): última visita, cantidad de visitas (turnos COMPLETED o PAID), gasto total (Σ pagos netos de reversos en `CashMovement`), ticket promedio, saldo pendiente (Σ `balanceCents` de turnos COMPLETED).

## Reglas de negocio
1. Nombre obligatorio (2–80 caracteres). Teléfono obligatorio para habilitar WhatsApp; se normaliza (agrega `549` a números locales argentinos de 10 dígitos).
2. Teléfono duplicado: advertir y ofrecer abrir el cliente existente.
3. Búsqueda por nombre (normalizado: minúsculas, sin acentos, coincidencia por prefijo de palabra) o por dígitos de teléfono.
4. Baja lógica (`active = false`): no aparece en búsqueda por defecto; su historial se conserva.
5. `noShowCount` solo lo modifica la agenda (estado NO_SHOW) o una corrección auditada.
6. Notas son datos sensibles: nunca en logs ni exportaciones automáticas salvo backup.

## Pantallas y flujos
- **Lista:** `SearchField` arriba, lista con proyección liviana (id, nombre, teléfono) y búsqueda en SQL, fluida con 5.000 clientes; orden alfabético, FAB "Nuevo cliente".
- **Alta/edición:** nombre, teléfono, cumpleaños, notas.
- **Detalle:** datos + acciones rápidas (nuevo turno, WhatsApp, llamar); indicador de ausencias y de **saldo pendiente** si lo hay; pestañas: Visitas (COMPLETED/PAID), Gastos (F5), Ficha de color (F7).
- **Alta rápida** reutilizable desde el flujo de crear turno (nombre + teléfono).

## Tests obligatorios
Normalización de texto y teléfono, validaciones, búsqueda (DAO) con acentos y prefijos.

## Criterios de aceptación
- [x] Alta, edición y baja lógica de clientes.
- [x] Búsqueda por nombre o teléfono responde al tipear con 5.000 clientes.
- [x] Teléfonos normalizados y advertencia de duplicados.
- [x] Detalle muestra historial de visitas (COMPLETED/PAID), gastos y saldo pendiente.
- [x] Contador de ausencias visible y correcto.
- [x] Alta rápida disponible desde crear turno.
