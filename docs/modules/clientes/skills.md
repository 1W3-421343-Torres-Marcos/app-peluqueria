# skills.md — Clientes

**Fase:** F2 (base) · historial en F3/F5 · contactos en F13 · **Depende de:** configuración.

## Propósito
Registro de clientes con búsqueda instantánea y visión completa de su relación con el salón.

## Modelo de datos
- `Client`: id, name, nameNormalized, phone (E.164 sin `+`), birthday? (día y mes obligatorios, año opcional), notes (alergias, preferencias), noShowCount, contactLookupKey? (nullable), origin (`MANUAL` | `CONTACT_SYNC`), createdFromContactAt? (epoch millis nullable), active, createdAt, updatedAt.
- Índices: `nameNormalized`, `phone` (único entre activos), `contactLookupKey`.
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

## Importación y sincronización desde contactos (F13)

### 1. Carga masiva inicial
- Botón **"Importar desde contactos"** disponible en **Clientes** y en el asistente inicial (configuración inicial).
- Antes de solicitar `READ_CONTACTS`, se muestra una pantalla explicativa clara: se lee la agenda solo para cargar clientes, nada sale del teléfono y nunca se modifica ni elimina ningún contacto.
- Lectura de todas las cuentas del dispositivo (teléfono, Google, SIM) deduplicando por teléfono normalizado E.164 sin `+` (`549` para números locales argentinos de 10 dígitos).
- Vista previa previa a guardar con tres contadores (**a importar**, **ya cargados**, **omitidos**) y un único botón **"Importar todos"** (sin selección individual).
- Selección de teléfono: si el contacto tiene varios números, se elige el primer móvil válido (`TYPE_MOBILE`); si no hay móvil, el primero válido disponible. Los contactos sin teléfono válido se omiten y se informa su cantidad en el contador correspondiente.
- Nombre del cliente: `DISPLAY_NAME` recortado y validado según las reglas vigentes (2–80 caracteres); si no tiene nombre válido, usar el teléfono normalizado como nombre.
- Cumpleaños: si el contacto tiene evento `BIRTHDAY` cargado, importarlo aceptando tanto fechas completas con año como fechas sin año (`--MM-DD`); si no tiene o es inválido, queda vacío.
- Ejecución en una sola transacción dentro de `AppExecutors.io()`, con indicador de progreso en pantalla.

### 2. Cumpleaños pendientes
- Filtro **"Sin cumpleaños"** en la lista de Clientes junto con un contador visible de cuántos clientes no tienen fecha cargada.
- Al tocar un cliente desde ese filtro se abre directamente el campo/editor de cumpleaños para completarlo sin buscar uno por uno.

### 3. Sincronización automática de contactos nuevos
- Cada contacto nuevo en la agenda del teléfono que tenga teléfono válido se da de alta automáticamente como cliente sin intervención manual del dueño.
- Mecanismo principal: WorkManager con disparador por cambio de contenido (`Constraints.Builder.addContentUriTrigger(ContactsContract.Contacts.CONTENT_URI, true)` notificando descendientes). Al ejecutarse, el worker se reprograma a sí mismo porque los disparadores por URI son de un solo disparo.
- Respaldo: la misma sincronización se ejecuta al abrir la app y al volver a primer plano, con límite mínimo de 1 vez cada 15 minutos (`throttle`), mitigando restricciones de fondo de algunos fabricantes.
- Idempotencia garantizada: ejecuciones consecutivas nunca duplican clientes.
- Vínculo mediante `contactLookupKey` (nullable) para reconocer contactos ya importados aunque cambie el nombre o el teléfono, combinado siempre con deduplicación por teléfono normalizado.
- Reglas estrictas de protección de datos:
  1. Nunca borrar ni desactivar clientes existentes si el contacto se elimina del teléfono.
  2. Nunca sobrescribir nombre, notas ni cumpleaños de clientes existentes con datos del contacto.
  3. Si el teléfono normalizado coincide con un cliente inactivo (`active = false`), no crear un cliente nuevo ni reactivarlo automáticamente: omitirlo.
  4. Si el permiso `READ_CONTACTS` es revocado, pausar la sincronización automáticamente y mostrar aviso en Configuración sin fallar ni lanzar excepciones.
- Interruptor **"Sincronizar contactos automáticamente"** en Configuración (activado por defecto tras la importación inicial), mostrando fecha/hora y resultado de la última sincronización.
- Origen y revisión: clientes creados por sincronización/importación desde agenda quedan marcados con `origin = CONTACT_SYNC` (los manuales y preexistentes, con `MANUAL`). En Clientes se ofrece el filtro **"Importados recientemente"** (últimos 7 días) para revisar altas recientes y dar de baja lógica si alguno no corresponde.
- Auditoría: cada sincronización o importación que agregue clientes registra un `AuditLog` con cantidad agregada y fecha/hora, sin loguear ni almacenar en auditoría nombres ni teléfonos.

### 4. Esquema y backup
- Room incrementa versión de `13` a `14`, con `Migration_13_14` (sin `fallbackToDestructiveMigration`) y exportación de `app/schemas/com.metanoia.app.data.db.AppDatabase/14.json`. Clientes preexistentes quedan con `origin = MANUAL`, `contact_lookup_key = NULL` y `created_from_contact_at = NULL`.
- Backup y restauración JSON actualizados a esquema v14 preservando los campos nuevos y manteniendo compatibilidad con backups previos.

## Tests obligatorios
- En `:domain` (JUnit): normalización de teléfonos provenientes de contactos (`+54`, espacios, guiones, prefijos `0` y `15`), selección de teléfono principal en contactos con múltiples números, detección de duplicados (activo, inactivo y distinto formato), regla de no sobrescribir datos de clientes existentes, idempotencia de sincronización y respeto de clientes inactivos, parseo de cumpleaños con año y sin año (`--MM-DD`), y registro de `AuditLog` sin PII.
- En `:app` (`androidTest`): migración `13 → 14` (y `1 → 14`) con `MigrationTestHelper` y test de ida y vuelta (round-trip) de backup/restauración con el esquema nuevo.
- En `:app` (JUnit): validación de `AndroidManifest.xml` con `READ_CONTACTS`, ausencia total de `INTERNET` y ausencia de logs con nombres o teléfonos.

## Criterios de aceptación
- [x] Alta, edición y baja lógica de clientes.
- [x] Búsqueda por nombre o teléfono responde al tipear con 5.000 clientes.
- [x] Teléfonos normalizados y advertencia de duplicados.
- [x] Detalle muestra historial de visitas (COMPLETED/PAID), gastos y saldo pendiente.
- [x] Contador de ausencias visible y correcto.
- [x] Alta rápida disponible desde crear turno.
- [ ] Botón "Importar desde contactos" en Clientes y asistente inicial, con pantalla explicativa previa a `READ_CONTACTS`.
- [ ] Lectura de todas las cuentas del dispositivo, elección de móvil/primero válido, vista previa con 3 contadores ("Importar todos") e importación transaccional con progreso en `AppExecutors.io()`.
- [ ] Parseo de cumpleaños con y sin año (`--MM-DD`), nombre recortado 2–80 caracteres (o teléfono si falta) y teléfonos E.164 sin `+` (`549`).
- [ ] Filtro "Sin cumpleaños" con contador visible y edición directa del cumpleaños al tocar un cliente filtrado.
- [ ] Sincronización automática vía WorkManager (`addContentUriTrigger` auto-reprogramable) + respaldo al abrir/primer plano cada >= 15 min, idempotente.
- [ ] Protección de datos: vínculo `contactLookupKey`, sin borrados/desactivaciones, sin sobrescritura de nombre/notas/cumpleaños, omisión de inactivos y pausa con aviso si se revoca `READ_CONTACTS`.
- [ ] Interruptor en Configuración con última fecha/resultado, `origin` (`MANUAL` / `CONTACT_SYNC`), filtro "Importados recientemente" (7 días) y `AuditLog` sin nombres ni teléfonos.
- [ ] Migración Room `13 → 14` probada con `MigrationTestHelper`, esquema `14.json` exportado y backup/restauración v14 verificados de ida y vuelta.
