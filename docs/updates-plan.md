# updates-plan.md — Plan de actualizaciones de Metanoia

> Tablero separado para correcciones y mejoras posteriores al cierre de las fases F1-F13.
> Version inicial objetivo: `1.1.1` (`versionCode 14`).
> Leyenda: `[ ]` pendiente · `[~]` en curso · `[x]` completada.

## Instrucciones para agentes futuros

Este archivo es el procedimiento obligatorio para planificar, diagnosticar, corregir, verificar y entregar una actualización. Ante una falla o mejora nueva:

1. **No reemplazar ni borrar el historial anterior.** Toda incidencia nueva se agrega como una nueva sección `U<n>` y una nueva release.
2. **Confirmar la versión base antes de editar.** Leer `app/build.gradle`, `docs/changelog.md`, `docs/updates-plan.md` y el último tag.
3. **Crear una rama específica.** Usar `fix/<tema>-<version>` para correcciones o `feat/<tema>-<version>` para funcionalidades nuevas.
4. **Registrar primero la evidencia, después la hipótesis.** Una hipótesis no sustituye el log, prueba o resultado reproducido.
5. **Usar el plan por versión.** Archivar el diagnóstico detallado en `docs/releases/<version>/`.
6. **No modificar el plan principal F1-F13.** `docs/plan.md` continúa como hoja histórica del desarrollo principal. Las mejoras posteriores viven en `docs/updates-plan.md` y `docs/releases/`.
7. **No commitear secretos ni artefactos.** No incluir `keystore.properties`, `.jks`, `.keystore`, APKs, backups, contraseñas, nombres, teléfonos, notas ni datos privados.

## Convención de documentación por versión

Cada actualización instalable debe tener una carpeta:

```text
docs/releases/<version>/
  changelog.md
  updates-plan.md
  diagnostico.md
  verificacion.md
```

- `changelog.md`: resumen ejecutivo de cambios y motivo.
- `updates-plan.md`: tablero de tareas, estado, commit, merge y tag.
- `diagnostico.md`: cómo se reprodujo, evidencia, causa raíz, hipótesis descartadas y corrección.
- `verificacion.md`: tests, lint, APK, firma, prueba manual y dispositivo usado.

El índice global `docs/changelog.md` contiene entradas breves. Este archivo `docs/updates-plan.md` contiene el método general y el estado agregado. El detalle histórico permanece en `docs/releases/<version>/`.

## Índice de releases documentadas

| Versión | Enfoque | Evidencia principal | Carpeta |
|---|---|---|---|
| 1.1.1 | Estabilidad general de confirmaciones y persistencia | Prueba de rollback y suite verde | `docs/releases/1.1.1/` |
| 1.1.2 | Cierre al cobrar un resultado exitoso | `IllegalStateException` confirmado por `adb logcat` | `docs/releases/1.1.2/` |
| 1.1.3 | Agregar el primer servicio a un profesional | Persistencia segura y prueba física aprobada | `docs/releases/1.1.3/` |

## Flujo obligatorio de una actualización

### 1. Reproducir y registrar

- Reproducir una sola acción por vez.
- Registrar versión, `versionCode`, dispositivo, Android, acción, hora y resultado.
- Si hay cierre, obtener `logcat` antes de otra acción que sobrescriba la evidencia.
- Conservar el bloque de `FATAL EXCEPTION` o `AndroidRuntime`.
- Identificar al menos:
  - proceso;
  - PID;
  - hilo;
  - excepción principal;
  - mensaje;
  - primeras líneas `Caused by`;
  - actividad o clase de aplicación que aparece en el stacktrace.

### 2. Clasificar la causa

Elegir y documentar una o varias categorías:

- resultado exitoso tratado como fallo (`getError()` o `getValue()` fuera de su rama válida);
- persistencia Room/SQLite o migración;
- transacción incompleta o actualización destructiva;
- excepción no capturada en `AppExecutors.io()` o repositorio;
- acceso UI/ciclo de vida desde un hilo o pantalla destruida;
- validación de dominio incorrecta;
- dato de prueba o configuración inconsistente;
- fallo externo de Android, permisos, SAF, WorkManager o firma;
- otro, indicando siempre el `Caused by` exacto.

No afirmar una causa raíz solo por una hipótesis. Si no existe evidencia suficiente, marcar la investigación como pendiente.

### 3. Crear el plan

Antes de implementar:

- describir el problema observado;
- listar evidencia disponible;
- escribir hipótesis y su forma de descartarla;
- definir archivos probables a modificar;
- definir una prueba que falle sin la corrección;
- definir la prueba manual que debe pasar;
- no anticipar cambios fuera del alcance.

### 4. Implementar el mínimo seguro

- conservar el comportamiento existente;
- usar transacciones para escrituras relacionadas;
- evitar `REPLACE` destructivo en entidades existentes;
- consultar `getError()` o `getValue()` únicamente en la rama correspondiente;
- capturar excepciones de persistencia sin ocultarlas sin registro;
- devolver `DomainError.DATABASE_ERROR` o el error de dominio existente;
- no comentar ni dejar TODO sin asociarlo a la actualización.

### 5. Verificar

Ejecutar como mínimo:

```powershell
./gradlew.bat test
./gradlew.bat lintDebug
./gradlew.bat assembleDebug
./gradlew.bat assembleRelease
./gradlew.bat compileDebugAndroidTestJavaWithJavac
```

Además:

- agregar una prueba unitaria o instrumentada que reproduzca el fallo;
- probar el flujo manual en un dispositivo;
- verificar que no aparece `FATAL EXCEPTION`;
- verificar APK release con `apksigner`;
- verificar `versionCode` y `versionName` embebidos.

No marcar un paso como completado si queda una prueba fallando o no fue ejecutada.

### 6. Registrar y commitear

- usar un commit por hito probado;
- seguir Conventional Commits:
  - `fix(módulo): U<n> resumen`;
  - `test(módulo): U<n> agrega prueba`;
  - `docs(release): U<n> registra actualizacion`;
  - `chore(release): U<n> entrega version`;
- actualizar este tablero y la carpeta de la versión antes del commit;
- no subir APK, keystore ni datos privados.
- verificar el commit firmado/normal según la configuración disponible y confirmar el estado Git.

### 7. Entregar

- subir `versionCode` y `versionName` únicamente después de que las pruebas y la documentación estén verdes;
- generar APK debug y release;
- verificar el certificado existente con `apksigner`;
- registrar rutas exactas de los APK;
- crear tag `v<versionName>`;
- fusionar a `main` con merge explícito;
- registrar el commit de corrección, el merge y el tag;
- no hacer `push` salvo aprobación explícita del usuario.

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

### Registro mínimo de una incidencia nueva

Agregar una fila o bloque con:

```text
Versión:
versionCode:
Dispositivo/Android:
Acción reproducida:
Momento del cierre:
Evidencia: FATAL EXCEPTION / AndroidRuntime / log / test fallido
Excepción principal:
Caused by:
Hilo/PID:
Hipótesis inicial:
Hipótesis descartadas:
Causa confirmada:
Archivos modificados:
Prueba automática:
Prueba manual:
Commit:
Merge:
Tag:
Estado:
```

Si falta el dispositivo o no se puede reproducir, dejar el estado como pendiente y no afirmar una causa.

## Casos históricos resueltos

### Cobro exitoso que cerraba la app

- **Síntoma:** cerrar al confirmar un cobro o un flujo “Completar y cobrar”.
- **Evidencia real:** `FATAL EXCEPTION: pool-2-thread-*`, proceso `com.metanoia.app`, `java.lang.IllegalStateException: No hay error porque el resultado fue exitoso`.
- **Causa:** se consultaba `result.getError()` sobre un `Result.success`.
- **Corrección:** pasar el `Result<ChargeResult>` completo a `handleResult` y llamar a `getError()` solamente dentro de la rama de fallo.
- **Verificación:** `1.1.2`, `versionCode 15`, Motorola edge 40; cobrar, completar, revertir, confirmar y cancelar sin nuevo cierre.
- **Detalle:** `docs/releases/1.1.2/diagnostico.md`.

### Primer servicio agregado a profesional sin servicios

- **Síntoma:** editar un profesional sin servicios, agregar el primer servicio, guardar y que la app se cierre sin conservar la asignación.
- **Hipótesis inicial:** persistencia destructiva al actualizar la entidad padre.
- **Hallazgo:** `ProfessionalDao.upsertProfessional()` usaba `OnConflictStrategy.REPLACE`; al actualizar podía borrar y recrear la fila de `professionals`, afectando relaciones `RESTRICT`.
- **Corrección:** profesionista nuevo usa `INSERT IGNORE`; profesional existente usa `UPDATE`; horarios, servicios y días libres se actualizan dentro de `saveComplete`.
- **Protección:** el ViewModel captura excepciones de Room y devuelve `DATABASE_ERROR`.
- **Verificación:** prueba unitaria e instrumentada; versión `1.1.3`, `versionCode 16`, prueba física exitosa con asignación persistida y sin cierre.
- **Detalle:** `docs/releases/1.1.3/diagnostico.md`.

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
| 20/11/2026 | U4.5 | Prueba física aprobada en `1.1.3`: al editar un profesional sin servicios, agregar el primer servicio y guardar, la asignación quedó persistida y la app no se cerró. | Cierre documentado y listo para entrega |

## U4 — Profesional sin servicios al agregar el primer servicio

| ID | Estado | Cambio |
|---|---|---|
| U4.1 | [x] | Identificada la persistencia destructiva por `OnConflictStrategy.REPLACE` en profesionales existentes. |
| U4.2 | [x] | Reemplazar `REPLACE` por actualización segura y proteger el ViewModel. |
| U4.3 | [x] | Agregar prueba instrumentada con horario existente y primer servicio; suite Android compilada. |
| U4.4 | [x] | Suite completa, lint y APK `1.1.3` generados; firma v2 verificada. |
| U4.5 | [x] | Prueba física exitosa: editar el profesional sin servicios, agregar el primer servicio y guardar conservó la asignación sin cerrar la app. |
