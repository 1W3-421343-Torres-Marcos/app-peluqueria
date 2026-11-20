# updates-plan.md — Plan de actualizaciones de Metanoia

> Tablero separado para correcciones y mejoras posteriores al cierre de las fases F1-F13.
> Version inicial objetivo: `1.1.1` (`versionCode 14`).
> Leyenda: `[ ]` pendiente · `[~]` en curso · `[x]` completada.

## U0 — Guia reutilizable para diagnosticar cierres

1. Reproducir una sola acción a la vez y anotar: versión, `versionCode`, dispositivo, pantalla y hora.
2. Limpiar el registro y mantener una captura activa antes de tocar la acción:

   ```powershell
   $adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
   & $adb logcat -c
   & $adb logcat -v threadtime
   ```

3. Conservar el bloque completo de `FATAL EXCEPTION` o `AndroidRuntime`, incluyendo:
   - `Process`;
   - `PID`;
   - hilo;
   - excepción principal;
   - primeras líneas `Caused by`;
   - últimas clases de la app en el stacktrace.
4. Si el log es ruidoso, filtrar el proceso vivo por su PID:

   ```powershell
   $appPid = & $adb shell pidof com.metanoia.app
   & $adb logcat --pid=$appPid -v threadtime
   ```

5. Clasificar antes de corregir:
   - **Resultado exitoso tratado como fallo:** una rama exitosa llama `getError()` o `getValue()` de forma inválida.
   - **Persistencia Room/SQLite:** revisar transacción, migración, claves foráneas, auditoría y rollback.
   - **Segundo plano/I/O:** revisar excepciones no capturadas en `AppExecutors.io()`, DAO o repositorio.
   - **UI/ciclo de vida:** revisar acceso a vistas, fragmentos o contextos cuando la pantalla fue destruida.
   - **Otro:** guardar el `Caused by` exacto y reproducir con el mismo dato.
6. Antes de editar, registrar en este tablero la causa reproducida, el flujo afectado y el criterio manual que debe pasar.
7. Después de corregir, ejecutar tests/lint/APKs, repetir la acción en dispositivo y guardar commit, merge, tag y resultado.

## U1 — Diagnostico y estabilidad de persistencia

| ID | Estado | Cambio |
|---|---|---|
| U1.1 | [x] | Capturado con `adb logcat`: un cobro exitoso lanzaba `IllegalStateException: No hay error porque el resultado fue exitoso`. Causa: `ChargeAppointmentViewModel` consultaba `getError()` después de un `Result.success`; corregido para recibir el `Result` completo y consultar el error solo en la rama de fallo. |
| U1.2 | [x] | Aislar confirmar/cancelar/completar/cobrar de excepciones Room no controladas. |
| U1.3 | [x] | Ejecutar transacciones atomicas para turno + auditoría en cambios de estado. |
| U1.4 | [x] | Traducir fallos de persistencia a `DomainError.DATABASE_ERROR` y mostrarlos sin cerrar la app. |
| U1.5 | [x] | Agregar pruebas de rollback y manejo de excepciones en los casos afectados. |

## U2 — Verificacion

| ID | Estado | Cambio |
|---|---|---|
| U2.1 | [x] | `./gradlew.bat test` verde. |
| U2.2 | [x] | `./gradlew.bat lint assembleDebug` verde. |
| U2.3 | [x] | Tests instrumentados de caja y agenda compilados. |
| U2.4 | [x] | Prueba manual: confirmar, completar, cobrar, revertir y cancelar. |
| U2.4.1 | [x] | Prueba manual `1.1.2` en Motorola edge 40: confirmar, completar, cobrar, revertir y cancelar permanecieron abiertos sin reinicio. |
| U2.5 | [x] | `assembleRelease` y verificacion `apksigner` verde. |

## U3 — Entrega

| ID | Estado | Cambio |
|---|---|---|
| U3.1 | [x] | Subir `versionCode` a 14 y `versionName` a `1.1.1`; luego subir `versionCode` a 15 y `versionName` a `1.1.2` por la corrección confirmada por logcat. |
| U3.2 | [x] | Registrar cada hito con commit Conventional Commit. |
| U3.3 | [x] | Actualizar `docs/changelog.md` y este tablero. |
| U3.4 | [x] | Fusionar `1.1.1` a `main` con tag `v1.1.1`; fusionar `1.1.2` a `main` con tag `v1.1.2`. |
| U3.5 | [x] | Entregar APK debug y APK release firmados verificados para `1.1.1` y `1.1.2`. |

## Registro de avance

| Fecha | Paso | Nota | Siguiente |
|---|---|---|---|
| 20/11/2026 | U1.1 | No hay dispositivo conectado por ADB; queda pendiente capturar el `FATAL EXCEPTION` real. | Conectar dispositivo y capturar `logcat` |
| 20/11/2026 | U1.2/U1.3 | Se agregan catches de persistencia y transaccion atomica para estado/auditoria y cobros. Compilacion y suite inicial verdes. | Agregar prueba de rollback y ejecutar lint |
| 20/11/2026 | U1.5/U3.1 | Prueba de rollback agregada y version corregida a `1.1.1` (`versionCode 14`). | Ejecutar suite completa y generar release |
| 20/11/2026 | U2.5/U3.4/U3.5 | Suite completa, lint y APK debug/release verdes; firma v2 verificada con el certificado existente; merge y tag `v1.1.1` completados. | Prueba manual al conectar un dispositivo |
| 20/11/2026 | U1.1/U2.4.1 | Capturado el cierre real con `adb logcat`: cobrar consultaba `getError()` sobre un resultado exitoso. Corregido en cobrar y completar y cobrar; versión `1.1.2` (`versionCode 15`), prueba manual en Motorola edge 40 sin cierres. Commit `d5919c0`, merge `7031bc9`, tag `v1.1.2`. | Mantener el tablero actualizado ante futuras incidencias |
