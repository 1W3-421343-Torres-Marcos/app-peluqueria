# updates-plan.md — Plan de actualizaciones de Metanoia

> Tablero separado para correcciones y mejoras posteriores al cierre de las fases F1-F13.
> Version inicial objetivo: `1.1.1` (`versionCode 14`).
> Leyenda: `[ ]` pendiente · `[~]` en curso · `[x]` completada.

## U1 — Diagnostico y estabilidad de persistencia

| ID | Estado | Cambio |
|---|---|---|
| U1.1 | [~] | Registrar el cierre real con `adb logcat` y adjuntar la excepcion raiz al registro de avance. |
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
| U2.4 | [ ] | Prueba manual: confirmar, completar, cobrar, revertir y cancelar. |
| U2.5 | [x] | `assembleRelease` y verificacion `apksigner` verde. |

## U3 — Entrega

| ID | Estado | Cambio |
|---|---|---|
| U3.1 | [x] | Subir `versionCode` a 14 y `versionName` a `1.1.1`. |
| U3.2 | [x] | Registrar cada hito con commit Conventional Commit. |
| U3.3 | [x] | Actualizar `docs/changelog.md` y este tablero. |
| U3.4 | [x] | Fusionar a `main` y crear tag `v1.1.1`. |
| U3.5 | [x] | Entregar APK debug y APK release firmados verificados. |

## Registro de avance

| Fecha | Paso | Nota | Siguiente |
|---|---|---|---|
| 20/11/2026 | U1.1 | No hay dispositivo conectado por ADB; queda pendiente capturar el `FATAL EXCEPTION` real. | Conectar dispositivo y capturar `logcat` |
| 20/11/2026 | U1.2/U1.3 | Se agregan catches de persistencia y transaccion atomica para estado/auditoria y cobros. Compilacion y suite inicial verdes. | Agregar prueba de rollback y ejecutar lint |
| 20/11/2026 | U1.5/U3.1 | Prueba de rollback agregada y version corregida a `1.1.1` (`versionCode 14`). | Ejecutar suite completa y generar release |
| 20/11/2026 | U2.5/U3.4/U3.5 | Suite completa, lint y APK debug/release verdes; firma v2 verificada con el certificado existente; merge y tag `v1.1.1` completados. | Prueba manual al conectar un dispositivo |
