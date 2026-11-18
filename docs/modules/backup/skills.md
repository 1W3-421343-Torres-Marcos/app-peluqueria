# skills.md — Backup y restauración (CRÍTICO)

**Fase:** F4 · disparador CASH_CLOSE en F5 · **Depende de:** base de datos (F1).

## Propósito
Que los datos del salón nunca dependan solo del almacenamiento interno del teléfono ni de un único mecanismo de disparo.

## Formato de backup
- Archivo `metanoia-backup-yyyyMMdd-HHmmss-<trigger>.zip` con:
  - `metanoia.db` (copia consistente tras `PRAGMA wal_checkpoint(TRUNCATE)`).
  - `manifest.json`: `format` (1), `schemaVersion`, `appVersionCode`, `createdAt` (ISO-8601), `sha256` del `.db`, conteos (clientes, turnos, movimientos), `trigger`.
- Valores de `trigger`:

| Trigger | Cuándo |
|---|---|
| `AUTO` | `BackupWorker` periódico (WorkManager). |
| `MANUAL` | El usuario toca "Hacer backup ahora" / Exportar / Compartir. |
| `PRE_RESTORE` | Automático, justo antes de restaurar. |
| `CASH_CLOSE` | Al completar el cierre de caja diario (F5). |
| `ON_BACKGROUND` | Al pasar la app a segundo plano (`ProcessLifecycleOwner`), solo si el último backup exitoso tiene más de X horas (default 6, configurable). |

- `DevicePrefs`: URI de carpeta, último backup exitoso (fecha, archivo, trigger), N días a conservar, frecuencia AUTO, horas para ON_BACKGROUND, días para aviso.

## Reglas de negocio
1. **Carpeta destino:** el dueño la elige una vez con `ACTION_OPEN_DOCUMENT_TREE`; se llama `takePersistableUriPermission` (lectura+escritura). Puede ser carpeta sincronizada con Google Drive, Descargas, SD o pendrive OTG. Si el permiso se pierde o la carpeta no existe, se avisa y se pide reelegir.
2. **Automático (AUTO):** `BackupWorker` periódico (default cada 24 h, sin requerir red ni carga). No es suficiente por sí solo: algunos fabricantes matan tareas de apps poco usadas, por eso existen CASH_CLOSE y ON_BACKGROUND.
3. **CASH_CLOSE y ON_BACKGROUND:** se encolan como `OneTimeWorkRequest` con nombre único (`backup_cash_close`, `backup_on_background`) y `ExistingWorkPolicy.KEEP`, para evitar duplicados y no bloquear la UI. Todos los triggers usan el mismo `BackupManager` y una exclusión mutua para no escribir dos backups a la vez.
4. **Retención** (`BackupRetentionPolicy`, regla pura en `domain`), aplicada tras cada backup exitoso, solo sobre archivos de la app (por prefijo de nombre) en la carpeta destino:
   - Backups no `PRE_RESTORE`: conservar **el más reciente de cada uno de los últimos N días con backup** (default N = 7); borrar el resto.
   - `PRE_RESTORE`: se conservan aparte, los últimos 3.
   - Nunca borrar el backup recién creado ni archivos ajenos a la app.
5. **Manual:** `ACTION_CREATE_DOCUMENT` (el usuario elige destino) y "Compartir" (archivo temporal en cache vía `FileProvider`).
6. **Restauración:**
   1. Elegir archivo con `ACTION_OPEN_DOCUMENT`.
   2. Validar: ZIP legible, `manifest.json` presente y parseable, SHA-256 coincide, `schemaVersion ≤` actual (si es menor, se migrará al abrir; si es mayor, rechazar), `PRAGMA integrity_check = ok` sobre copia temporal.
   3. Mostrar resumen (fecha, conteos) y pedir confirmación explícita.
   4. Backup previo automático (`PRE_RESTORE`) a la carpeta destino (o cache si no hay carpeta).
   5. Cerrar Room, reemplazar archivo, borrar `-wal`/`-shm`, reiniciar la app.
   6. Si algo falla tras reemplazar, restaurar el backup previo.
7. **Aviso:** banner en Agenda si el último backup exitoso tiene más de X días (default 3) o no hay carpeta configurada.
8. Nunca se escribe el backup solo en almacenamiento interno de la app como destino final.
9. El backup contiene datos sensibles: se informa al usuario al elegir carpeta en la nube. (Cifrado opcional con contraseña: evaluar en F12, no bloqueante.)

## Pantallas y flujos
- **Backup (en Más):** carpeta actual, último backup (fecha/trigger/estado), botones "Hacer backup ahora", "Exportar…", "Compartir", "Restaurar…", ajustes (frecuencia AUTO, días a conservar N, horas para ON_BACKGROUND, días de aviso).
- Asistente inicial: al primer uso, pedir carpeta de backup (omitible, con recordatorio por banner).

## Tests obligatorios
Validador (ZIP corrupto, sin manifest, hash incorrecto, esquema mayor/menor); política de retención (varios backups el mismo día → queda el último; días sin backup; más de N días; PRE_RESTORE separados, últimos 3; nunca borra el recién creado); umbral de horas de ON_BACKGROUND; encolado único con KEEP; ida y vuelta backup→restauración con conteos idénticos.

## Criterios de aceptación
- [ ] Carpeta elegida una vez con permiso persistente.
- [ ] Backup AUTO periódico funcionando.
- [ ] Backups CASH_CLOSE y ON_BACKGROUND encolados como trabajo único sin duplicados ni bloqueo de UI.
- [ ] Retención: el más reciente de cada uno de los últimos N días con backup + últimos 3 PRE_RESTORE (tests verdes).
- [ ] Exportación manual y compartir funcionan.
- [ ] Restauración valida integridad y versión antes de reemplazar.
- [ ] Backup previo automático antes de restaurar.
- [ ] Aviso visible de backup vencido.
- [ ] Prueba completa: desinstalar, reinstalar, restaurar sin pérdida de datos.
