# plan.md — Hoja de ruta y tablero de progreso

> Leyenda: `[ ]` pendiente · `[~]` en curso · `[x]` completada.
> Regla (ver `agents.md` §1): al terminar cada paso, actualizar su checkbox, esta tabla y el Registro de avance **antes** de continuar.

## Resumen

| ID | Fase | Estado | Pasos | Avance |
|---|---|---|---|---|
| F1 | Base, base de datos e identidad visual | [x] | 10/10 | 100 % |
| F2 | Configuración base, servicios, profesionales y clientes | [x] | 8/8 | 100 % |
| F3 | Agenda de turnos | [x] | 12/12 | 100 % |
| F4 | Backup y restauración | [x] | 8/8 | 100 % |
| F5 | Caja | [x] | 10/10 | 100 % |
| F6 | Recordatorios por WhatsApp | [x] | 7/7 | 100 % |
| F7 | Ficha de color | [x] | 4/4 | 100 % |
| F8 | Lista de espera | [~] | 2/4 | 50 % |
| F9 | Stock | [ ] | 0/6 | 0 % |
| F10 | Comisiones | [ ] | 0/4 | 0 % |
| F11 | Reportes | [ ] | 0/6 | 0 % |
| F12 | Seguridad y pulido final | [ ] | 0/7 | 0 % |
| **Total** | | | **61/86** | **71 %** |

---

## F1 — Base, base de datos e identidad visual `[x]`
Entregable: app instalable que abre con splash, tema claro/oscuro y navegación vacía. Módulos: `agents.md`, `design.md`.

1. [x] **F1.1 Crear repositorio y proyecto Gradle**
   - Archivos: `.gitignore`, `settings.gradle`, `build.gradle`, `gradle/libs.versions.toml`, `app/build.gradle`, `domain/build.gradle`
   - Hecho: `git init` si no existe repo; `.gitignore` según `agents.md` §11 (sin keystores ni contraseñas); commit inicial de `docs/` en `main` y rama `fase/F1` creada; módulos `:app` (Android) y `:domain` (java-library), Java 17, minSdk 26, targetSdk 35, ViewBinding activo; `versionCode 1` y `versionName "0.0.0"` solo en `app/build.gradle`; version catalog sin Paging, Guava ni RxJava (incluye `lifecycle-process` y WorkManager); `assembleDebug` OK; cero archivos Kotlin.
2. [x] **F1.2 Esqueleto de capas y DI manual**
   - Archivos: `MetanoiaApp`, `AppContainer`, `AppExecutors`, `domain/common/{Result,DomainError,Clock}`
   - Hecho: paquetes según `agents.md` §3.2; `AppContainer` accesible desde ViewModels; test de `Result`.
3. [x] **F1.3 Manifiesto de privacidad**
   - Archivos: `AndroidManifest.xml`, `res/xml/data_extraction_rules.xml`
   - Hecho: sin permiso INTERNET en manifiesto fusionado; `allowBackup=false`; reglas de extracción excluyen todo.
4. [x] **F1.4 Dinero y formatos es-AR**
   - Archivos: `domain/model/Money`, `app/util/Formats`, tests
   - Hecho: `Money` en centavos con suma/resta/porcentaje; formatos `$ 12.345,50`, `05/03/2026`, `14:30`; tests verdes.
5. [x] **F1.5 Room base + auditoría**
   - Archivos: `data/db/AppDatabase` (v1), `AuditLogEntity`, `AuditLogDao`, `app/schemas/`, test de migración base
   - Hecho: BD creada con `exportSchema`; `AuditLog` insertable; `MigrationTestHelper` configurado; sin migración destructiva.
6. [x] **F1.6 Tema Metanoia claro/oscuro**
   - Archivos: `res/values/{colors,themes,dimens,type}.xml`, `values-night/`, `res/font/` (Montserrat, Inter)
   - Hecho: tokens de `design.md` §2–4 implementados; cambio Sistema/Claro/Oscuro funciona.
7. [x] **F1.7 Logo reemplazable**
   - Archivos: `ui/common/LogoView`, `res/drawable/` (slot `logo_metanoia`)
   - Hecho: placeholder tipográfico visible en ambos modos; reemplazar el vector no requiere tocar código.
8. [x] **F1.8 Splash y navegación principal**
   - Archivos: `MainActivity`, `res/navigation/nav_main.xml`, `res/menu/bottom_nav.xml`, fragments vacíos
   - Hecho: splash con logo < 1 s; bottom nav Agenda/Clientes/Caja/Más con estados vacíos.
9. [x] **F1.9 Componentes base**
   - Archivos: `ui/common/{StatusChip,MoneyTextView,EmptyStateView,AlertBanner,ListRowView}`, estilos de botones
   - Hecho: pantalla de catálogo (solo debug) muestra todos los componentes en claro y oscuro según `design.md` §7.
10. [x] **F1.10 Verificación de fase**
    - Hecho: `./gradlew check` verde; APK debug instalado en dispositivo real; checklist global de `agents.md` §12 OK; `versionName 0.1.0`; merge de `fase/F1` a `main`, tag `fase-F1` y entrada en `docs/changelog.md`.

## F2 — Configuración base, servicios, profesionales y clientes `[x]`
Entregable: cargar el catálogo del salón y la base de clientes. Módulos: `configuracion`, `servicios`, `profesionales`, `clientes`.

1. [x] **F2.1 Configuración base**
   - Archivos: `SettingsEntity`, `SettingsRepository`, `ui/settings/`
   - Hecho: datos del negocio, horario general, duración mínima de bloque y operador actual persistidos en Room; migración + test; pantalla Acerca de con `versionName`/`versionCode` desde `BuildConfig`.
2. [x] **F2.2 Servicios: dominio y datos**
   - Archivos: `SalonService`, `SalonServiceEntity/Dao`, casos de uso, migración
   - Hecho: alta/edición/desactivación vía casos de uso; validaciones con tests.
3. [x] **F2.3 Servicios: UI**
   - Archivos: `ui/services/`
   - Hecho: lista por categoría, filtro activos/inactivos, formulario; criterios de `servicios/skills.md` OK.
4. [x] **F2.4 Profesionales: dominio y datos**
   - Archivos: `Professional`, `WorkSchedule`, `ProfessionalServiceCrossRef`, DAOs, migración
   - Hecho: horario semanal, días libres, servicios y % comisión persistidos; validaciones con tests.
5. [x] **F2.5 Profesionales: UI**
   - Archivos: `ui/professionals/`
   - Hecho: alta/edición con editor de horario semanal; criterios de `profesionales/skills.md` OK.
6. [x] **F2.6 Clientes: dominio y datos**
   - Archivos: `Client`, `ClientEntity/Dao`, `TextNormalizer`, `PhoneNormalizer`, migración
   - Hecho: búsqueda por nombre normalizado/teléfono con índices; teléfono E.164; tests.
7. [x] **F2.7 Clientes: UI**
   - Archivos: `ui/clients/`
   - Hecho: lista con proyección liviana y búsqueda en SQL (debounce), fluida con 5.000 clientes; alta/edición; ficha de detalle con secciones vacías de historial.
8. [x] **F2.8 Verificación de fase**
   - Hecho: tests verdes; tests de migración v1→actual; flujo manual completo en dispositivo; `versionName 0.2.0`; merge de `fase/F2` a `main`, tag `fase-F2` y entrada en `docs/changelog.md`.

## F3 — Agenda de turnos `[x]`
Entregable: agenda operativa diaria/semanal con turnos válidos. Módulos: `agenda`.

1. [x] **F3.1 Modelo de turno y estados**
   - Archivos: `Appointment`, `AppointmentItem`, `AppointmentStatus` (PENDING, CONFIRMED, COMPLETED, PAID, CANCELLED, NO_SHOW), `StatusTransitions`, `AppointmentBalance`
   - Hecho: duración = suma de ítems; tests de todas las transiciones válidas e inválidas, del paso automático COMPLETED ↔ PAID según `paidCents`/`balanceCents` derivados, de la corrección COMPLETED → CONFIRMED (solo sin cobros) y del congelado de comisión al completar.
2. [x] **F3.2 Validador de disponibilidad**
   - Archivos: `domain/rules/AvailabilityChecker`, tests
   - Hecho: rechaza solapamientos (COMPLETED y PAID ocupan agenda), fuera de horario, días libres y bloqueos; bordes (fin = inicio) permitidos; tests exhaustivos.
3. [x] **F3.3 Persistencia de agenda**
   - Archivos: `AppointmentEntity`, `AppointmentItemEntity`, `ScheduleBlockEntity`, DAOs, migración
   - Hecho: índices `(professionalId, day)` y `(clientId)`; FK RESTRICT; test DAO de consulta por rango.
4. [x] **F3.4 Caso de uso crear/editar turno**
   - Archivos: `CreateAppointmentUseCase`, `UpdateAppointmentUseCase`
   - Hecho: precio/duración congelados; validación de disponibilidad; profesional habilitado para cada servicio; tests.
5. [x] **F3.5 Vista diaria**
   - Archivos: `ui/agenda/DayFragment`, `AppointmentCardView`
   - Hecho: línea de tiempo por bloques, navegación entre días, filtro por profesional.
6. [x] **F3.6 Vista semanal**
   - Archivos: `ui/agenda/WeekFragment`
   - Hecho: 7 días (lunes a domingo) con ocupación; toque abre el día.
7. [x] **F3.7 Flujo crear turno**
   - Archivos: `ui/agenda/create/`
   - Hecho: cliente → servicios → profesional → horario libre sugerido → confirmar en ≤ 4 toques desde la agenda (cliente existente).
8. [x] **F3.8 Gestión del turno**
   - Archivos: `ui/agenda/detail/`, casos de uso de estado
   - Hecho: acciones según estado — editar, reprogramar y cancelar (motivo) en PENDING/CONFIRMED; confirmar; completar (congela comisión, queda con saldo); ausente (incrementa contador del cliente); corrección COMPLETED → CONFIRMED auditada sin cobros. Detalle muestra total, pagado y saldo. (Cobro y "Completar y cobrar" en F5.)
9. [x] **F3.9 Bloqueos de agenda**
   - Archivos: `ScheduleBlock`, `ui/agenda/blocks/`
   - Hecho: bloqueos por profesional o generales (vacaciones, almuerzo, feriado); se muestran en la agenda y bloquean turnos.
10. [x] **F3.10 Turnos recurrentes**
    - Archivos: `RecurrenceRule`, `GenerateRecurringAppointmentsUseCase`
    - Hecho: serie cada N semanas hasta fecha/cantidad; conflictos reportados sin crear el turno conflictivo; editar/cancelar "este" o "este y siguientes"; tests.
11. [x] **F3.11 Historial del cliente y rendimiento**
    - Archivos: `ui/clients/detail`, `DebugSeeder`
    - Hecho: historial de visitas en ficha; con 5.000 clientes y 50.000 turnos la agenda y la búsqueda responden sin trabas.
12. [x] **F3.12 Verificación de fase**
    - Hecho: criterios de `agenda/skills.md` marcados; tests verdes; `versionName 0.3.0`; merge de `fase/F3` a `main`, tag `fase-F3` y entrada en `docs/changelog.md`.

## F4 — Backup y restauración `[x]`
Entregable: datos protegidos con backup automático y restauración segura. Módulo: `backup`.

1. [x] **F4.1 Carpeta de destino (SAF)**
   - Archivos: `DevicePrefs`, `ui/backup/`
   - Hecho: `ACTION_OPEN_DOCUMENT_TREE` con permiso persistente; se muestra la carpeta elegida; detecta permiso perdido.
2. [x] **F4.2 Generador de backup**
   - Archivos: `data/backup/BackupManager`, `BackupManifest`
   - Hecho: checkpoint WAL, ZIP con BD + `manifest.json` (versión esquema, app, fecha, SHA-256, conteos).
3. [x] **F4.3 Exportar y compartir**
   - Archivos: `ui/backup/`, `FileProvider`
   - Hecho: `ACTION_CREATE_DOCUMENT` y compartir por menú de Android funcionan.
4. [x] **F4.4 Backup automático, en segundo plano y retención**
   - Archivos: `BackupWorker`, `BackupScheduler`, `domain/rules/BackupRetentionPolicy`, observer con `ProcessLifecycleOwner`
   - Hecho: AUTO periódico (diario por defecto); ON_BACKGROUND como `OneTimeWorkRequest` único (KEEP) solo si el último backup exitoso supera X horas (default 6); retención = el más reciente de cada uno de los últimos N días con backup (default 7) + últimos 3 PRE_RESTORE; tests de la política de retención y del umbral de horas; registra resultado y trigger.
5. [x] **F4.5 Validador y restaurador**
   - Archivos: `domain/rules/BackupValidator`, `data/backup/RestoreManager`
   - Hecho: rechaza ZIP inválido, hash incorrecto, esquema más nuevo, `PRAGMA integrity_check` fallido; tests.
6. [x] **F4.6 Flujo de restauración**
   - Archivos: `ui/backup/restore/`
   - Hecho: `ACTION_OPEN_DOCUMENT` → resumen → confirmación → backup previo automático → reemplazo → reinicio.
7. [x] **F4.7 Aviso de backup vencido**
   - Archivos: `AlertBanner` en Agenda
   - Hecho: banner si el último backup exitoso supera X días (configurable, default 3) o no hay carpeta.
8. [x] **F4.8 Verificación de fase**
   - Hecho: prueba ida y vuelta (backup → borrar app → restaurar) sin pérdida; criterios de `backup/skills.md` OK; `versionName 0.4.0`; merge de `fase/F4` a `main`, tag `fase-F4` y entrada en `docs/changelog.md`.

## F5 — Caja `[x]`
Entregable: cobros, ventas, gastos y cierre diario auditables. Módulo: `caja`.

1. [x] **F5.1 Modelo de caja**
   - Archivos: `CashMovement`, `PaymentMethod`, `ReverseMovementUseCase`
   - Hecho: movimientos inmutables con reverso; cada uno genera `AuditLog`; `paidCents`/`balanceCents` derivados de movimientos; tests.
2. [x] **F5.2 Persistencia de caja**
   - Archivos: entidades/DAOs, migración
   - Hecho: índices por día, método y `appointmentId`; FK RESTRICT; test de migración.
3. [x] **F5.3 Cobro de turno**
   - Archivos: `ChargeAppointmentUseCase`, `ui/cash/charge/`
   - Hecho: solo turnos COMPLETED con saldo; saldo precargado; ajuste con motivo; pagos parciales y divididos por método; pasa solo a PAID al cubrir el total; un reverso que deja saldo lo devuelve a COMPLETED; tests de ambos pasos automáticos y de rechazo en estados no válidos.
4. [x] **F5.4 "Completar y cobrar"**
   - Archivos: `CompleteAndChargeUseCase`, detalle de turno
   - Hecho: desde PENDING/CONFIRMED completa + cobra en una única transacción Room; pago simple en ≤ 3 toques; test instrumentado de atomicidad (falla del cobro ⇒ turno sigue sin completar).
5. [x] **F5.5 Lista "Por cobrar"**
   - Archivos: `ui/cash/receivables/`, consulta con proyección liviana
   - Hecho: turnos COMPLETED con saldo > 0 y monto adeudado; toque abre el cobro; contador en la pestaña Caja.
6. [x] **F5.6 Ventas sueltas y gastos**
   - Archivos: `ui/cash/`
   - Hecho: registro con concepto, monto y método; aparecen en el día.
7. [x] **F5.7 Cierre de caja diario**
   - Archivos: `CashClosingCalculator`, `ui/cash/closing/`
   - Hecho: esperado vs contado por método, diferencia registrada, día cerrado no admite movimientos; tests.
8. [x] **F5.8 Backup al cerrar caja**
   - Archivos: `CloseCashDayUseCase`, `BackupScheduler`
   - Hecho: al completar el cierre se encola backup `CASH_CLOSE` (`OneTimeWorkRequest` único, KEEP) sin bloquear la UI; test de encolado sin duplicados.
9. [x] **F5.9 Recibo y gastos del cliente**
   - Archivos: `ReceiptPdfBuilder`, ficha de cliente
   - Hecho: recibo PDF con logo y "Metanoia Hair Salon" compartible; historial de gastos y saldo pendiente en ficha.
10. [x] **F5.10 Verificación de fase**
    - Hecho: criterios de `caja/skills.md` OK; tests verdes; `versionName 0.5.0`; merge de `fase/F5` a `main`, tag `fase-F5` y entrada en `docs/changelog.md`.

## F6 — Recordatorios por WhatsApp `[x]`
Entregable: recordar turnos y saludar cumpleaños en pocos toques. Módulos: `recordatorios`, `configuracion`.

1. [x] **F6.1 Plantillas**
   - Archivos: `TemplateRenderer`, settings de plantillas
   - Hecho: variables `{nombre}`, `{fecha}`, `{hora}`, `{servicios}`, `{profesional}`, `{negocio}`; editor con vista previa; tests.
2. [x] **F6.2 Lanzador de WhatsApp**
   - Archivos: `util/WhatsAppLauncher`
   - Hecho: abre `https://wa.me/<tel>?text=<url-encoded>`; aviso si WhatsApp no está instalado.
3. [x] **F6.3 Pendientes de mañana**
   - Archivos: `ui/reminders/`, `ReminderEntity`
   - Hecho: lista de turnos del día siguiente sin recordar; enviar marca "recordado" con fecha.
4. [x] **F6.4 Recordar desde el turno**
   - Archivos: detalle de turno
   - Hecho: acción "Recordar" en ≤ 2 toques desde el detalle.
5. [x] **F6.5 Cumpleaños**
   - Archivos: `ui/reminders/birthdays`
   - Hecho: lista de cumpleaños del día con saludo por plantilla; marca enviado por año.
6. [x] **F6.6 Aviso diario local**
   - Archivos: `ReminderDigestWorker`
   - Hecho: notificación local diaria (hora configurable) con cantidad de pendientes; sin red.
7. [x] **F6.7 Verificación de fase**
   - Hecho: criterios de `recordatorios/skills.md` OK; `versionName 0.6.0`; merge de `fase/F6` a `main`, tag `fase-F6` y entrada en `docs/changelog.md`.

## F7 — Ficha de color `[x]`
Módulo: `ficha-color`.

1. [x] **F7.1 Modelo y persistencia**
   - Archivos: `ColorFormula`, `ColorFormulaComponent`, DAOs, migración
   - Hecho: fórmula con componentes, exposición, fecha y notas; tests de validación.
2. [x] **F7.2 UI en ficha del cliente**
   - Archivos: `ui/clients/color/`
   - Hecho: historial de fórmulas, alta/edición, duplicar fórmula.
3. [x] **F7.3 Repetir última fórmula**
   - Archivos: flujo crear turno
   - Hecho: al elegir servicio de coloración, botón "Repetir última fórmula" la vincula con un toque.
4. [x] **F7.4 Verificación de fase**
   - Hecho: criterios de `ficha-color/skills.md` OK; `versionName 0.7.0`; merge de `fase/F7` a `main`, tag `fase-F7` y entrada en `docs/changelog.md`.

## F8 — Lista de espera `[~]`
Módulo: `lista-espera`.

1. [x] **F8.1 Modelo y persistencia**
   - Archivos: `WaitlistEntry`, DAO, migración
   - Hecho: preferencias de día/franja/profesional/servicio; vencimiento.
2. [x] **F8.2 UI de lista de espera**
   - Archivos: `ui/waitlist/`
   - Hecho: alta rápida desde cliente o agenda; listado activo.
3. [ ] **F8.3 Sugerencias al cancelar**
   - Archivos: `WaitlistMatcher`, diálogo post-cancelación
   - Hecho: al cancelar, sugiere candidatos ordenados; ofrecer por WhatsApp o asignar el turno; tests del matcher.
4. [ ] **F8.4 Verificación de fase**
   - Hecho: criterios de `lista-espera/skills.md` OK; `versionName 0.8.0`; merge de `fase/F8` a `main`, tag `fase-F8` y entrada en `docs/changelog.md`.

## F9 — Stock `[ ]`
Módulo: `stock`.

1. [ ] **F9.1 Modelo y persistencia**
   - Archivos: `Product`, `StockMovement`, DAOs, migración
   - Hecho: stock = suma de movimientos; tipos uso/venta; tests.
2. [ ] **F9.2 UI de productos**
   - Archivos: `ui/stock/`
   - Hecho: lista con filtro por tipo y búsqueda; alta/edición/desactivación.
3. [ ] **F9.3 Movimientos**
   - Archivos: `ui/stock/movements/`
   - Hecho: ingreso, egreso por uso, ajuste con motivo; auditados.
4. [ ] **F9.4 Alertas de stock bajo**
   - Archivos: `LowStockRule`, `AlertBanner`
   - Hecho: chip y banner cuando cantidad ≤ mínimo; tests.
5. [ ] **F9.5 Integración con caja**
   - Archivos: venta suelta de caja
   - Hecho: vender producto descuenta stock y registra movimiento de caja en una única transacción.
6. [ ] **F9.6 Verificación de fase**
   - Hecho: criterios de `stock/skills.md` OK; `versionName 0.9.0`; merge de `fase/F9` a `main`, tag `fase-F9` y entrada en `docs/changelog.md`.

## F10 — Comisiones `[ ]`
Módulo: `comisiones`.

1. [ ] **F10.1 Calculadora de comisiones**
   - Archivos: `CommissionCalculator`, tests
   - Hecho: solo turnos PAID del período; usa % congelado al completar; COMPLETED con saldo no liquida; redondeo a centavo definido; tests de casos borde.
2. [ ] **F10.2 UI por profesional y período**
   - Archivos: `ui/commissions/`
   - Hecho: detalle por turno y total; filtros semana/mes/rango.
3. [ ] **F10.3 Liquidación**
   - Archivos: `SettleCommissionUseCase`
   - Hecho: registrar pago de comisión como egreso de caja auditado; período liquidado no se recalcula.
4. [ ] **F10.4 Verificación de fase**
   - Hecho: criterios de `comisiones/skills.md` OK; `versionName 0.10.0`; merge de `fase/F10` a `main`, tag `fase-F10` y entrada en `docs/changelog.md`.

## F11 — Reportes `[ ]`
Módulo: `reportes`.

1. [ ] **F11.1 Consultas agregadas**
   - Archivos: `ReportsDao`, `ReportsRepository`
   - Hecho: ingresos por día/mes (desde `CashMovement`), servicios top, clientes frecuentes y rendimiento por profesional (turnos COMPLETED o PAID), ocupación por franja, indicador "por cobrar"; tests DAO.
2. [ ] **F11.2 Pantalla de reportes**
   - Archivos: `ui/reports/`
   - Hecho: filtros de período/profesional; resultados en < 1 s con 50.000 turnos.
3. [ ] **F11.3 Gráficos simples**
   - Archivos: `ui/common/BarChartView`
   - Hecho: barras monocromáticas propias (sin librerías).
4. [ ] **F11.4 Exportar CSV**
   - Archivos: `CsvExporter`
   - Hecho: formato de `design.md` §9; guardado con SAF o compartir.
5. [ ] **F11.5 Exportar PDF**
   - Archivos: `ReportPdfBuilder`
   - Hecho: PDF A4 con cabecera de marca y tablas.
6. [ ] **F11.6 Verificación de fase**
   - Hecho: criterios de `reportes/skills.md` OK; `versionName 0.11.0`; merge de `fase/F11` a `main`, tag `fase-F11` y entrada en `docs/changelog.md`.

## F12 — Seguridad y pulido final `[ ]`
Módulos: `configuracion` y todos.

1. [ ] **F12.1 PIN y huella**
   - Archivos: `ui/lock/`, `BiometricPrompt`
   - Hecho: bloqueo opcional al abrir y al volver de segundo plano (tiempo configurable); PIN con hash + sal.
2. [ ] **F12.2 Accesibilidad**
   - Hecho: TalkBack con `contentDescription`; texto escalable al 130 % sin cortes; contraste verificado.
3. [ ] **F12.3 Rendimiento**
   - Hecho: arranque en frío < 2 s; sin jank en agenda con datos de prueba (perfilado).
4. [ ] **F12.4 Revisión de seguridad**
   - Hecho: manifiesto sin INTERNET; R8 activo; sin logs sensibles; exportaciones solo por acción explícita.
5. [ ] **F12.5 APK release firmado (v1.0.0)**
   - Archivos: `app/build.gradle` (`versionName "1.0.0"`, `versionCode` +1, `signingConfig` leyendo `keystore.properties`), `docs/instalacion.md`
   - Hecho: keystore generada y guardada **fuera del repo** con copia en al menos dos lugares; `keystore.properties` ignorado por Git (verificado con `git status`); APK release instalable; `docs/instalacion.md` documenta instalación, ubicación/resguardo de la keystore y cómo firmar futuras versiones.
6. [ ] **F12.6 Manual de usuario breve**
   - Archivos: `docs/manual.md`
   - Hecho: tareas frecuentes y backup/restauración explicadas en ≤ 2 páginas.
7. [ ] **F12.7 Verificación final**
   - Hecho: todos los `skills.md` con criterios marcados; `./gradlew check` verde; merge de `fase/F12` a `main`, tags `fase-F12` y `v1.0.0`, entrada `1.0.0` en `docs/changelog.md`.

---

## Registro de avance

| Fecha | Paso completado | Nota | Siguiente |
|---|---|---|---|
| 17/11/2026 | — | Documentación inicial creada (agents, readme, plan, design, skills). | F1.1 |
| 17/11/2026 | — | Correcciones de docs: (1) se quita Paging 3 → proyección liviana + búsqueda SQL + `LiveData<List>`; (2) backup con triggers CASH_CLOSE y ON_BACKGROUND (trabajo único KEEP) y retención por día (N=7) + 3 PRE_RESTORE; F4.4 ampliado y nuevo F5.8; (3) estado PAID separado de COMPLETED, "Completar y cobrar" (F5.4) y "Por cobrar" (F5.5), comisiones sobre PAID. F5 pasa de 7 a 10 pasos (total 86). | F1.1 |
| 17/11/2026 | — | Docs: nueva sección "Control de versiones y versionado de la app" en `agents.md` §11 (Git, commits por paso, ramas `fase/F<n>`, tags `fase-F<n>`/`v<versionName>`, versión 0.<fase>.0 → 1.0.0, keystore fuera del repo). F1.1 incluye `git init` y `.gitignore`; cada verificación de fase incluye merge, tag y changelog; F12.5 genera v1.0.0 firmada. Creado `docs/changelog.md`. Sin cambios en cantidad de pasos (total 86). | F1.1 |
| 17/11/2026 | F1.1 | Proyecto Gradle multimódulo (`:domain` y `:app`), wrapper 8.13, `libs.versions.toml`, `.gitignore`, rama `fase/F1`, `assembleDebug` OK. | F1.2 |
| 17/11/2026 | F1.2 | Esqueleto de capas (`domain`/`data`/`ui`), `Result`/`DomainError`/`Clock`/`IdGenerator`, `AppContainer`/`AppExecutors`/`MetanoiaApp`/`Event`, 6 tests `ResultTest` en verde. | F1.3 |
| 17/11/2026 | F1.3 | Manifiesto sin permiso `INTERNET` (`tools:node="remove"`), `allowBackup="false"`, `data_extraction_rules.xml` y `backup_rules.xml`, 3 tests `ManifestPrivacyTest` OK. | F1.4 |
| 17/11/2026 | F1.4 | `Money` inmutable en centavos con redondeo half-up, `Formats` es-AR (`$ 12.345,50`, `dd/MM/yyyy`, `HH:mm`), 5 tests `MoneyTest` y 3 tests `FormatsTest` verdes. | F1.5 |
| 17/11/2026 | F1.5 | Room v1 (`AppDatabase`, `AuditLogEntity`, `AuditLogDao`), esquema `1.json` exportado, `MigrationTest` y `SchemaExportTest` OK. | F1.6 |
| 17/11/2026 | F1.6 | Fuentes estáticas Montserrat e Inter (SIL OFL) en `assets/fonts/` y `res/font/`, tokens `colors`/`dimens`/`type`/`themes` claro y oscuro, `DevicePrefs` tema, `readme.md` actualizado, 3 tests `ThemeAndFontResourcesTest` OK. | F1.7 |
| 17/11/2026 | F1.7 | `LogoView` con placeholder tipográfico (`METANOIA` + `Hair Salon`) y slot reemplazable `drawable/logo_metanoia`, `LogoViewSlotTest` OK. | F1.8 |
| 17/11/2026 | F1.8 | Splash (`core-splashscreen`), `MainActivity` single-Activity, `BottomNavigationView` (Agenda, Clientes, Caja, Más), selector de tema y fragments iniciales. | F1.9 |
| 17/11/2026 | F1.9 | Estilos de botones (`Primary`, `Secondary`, `Text`), `StatusChip` (7 variantes), `MoneyTextView`, `EmptyStateView`, `AlertBanner`, `ListRowView`, `ComponentCatalogFragment` (solo debug), `StatusChipVariantTest` OK. | F1.10 |
| 17/11/2026 | F1.10 | Fase F1 cerrada (`versionName 0.1.0`): `./gradlew check assembleDebug compileDebugAndroidTestJavaWithJavac` verde (23 tests OK, Lint 0 errores), checklist `agents.md` §12 OK, `changelog.md` actualizado, merge a `main` y tags `fase-F1` / `v0.1.0`. | F2.1 |
| 18/11/2026 | F2.1 | Rama `fase/F2`, `Settings`/`TimeRange`/`AuditRecord`, `ScheduleValidator`, `GetSettingsUseCase`/`UpdateSettingsUseCase`, Room v2 (`Migration_1_2`, `2.json`), `SettingsFragment`/`AboutFragment` y `WeeklyScheduleEditorView`; 31 tests unitarios OK + `MigrationTest` compilado. | F2.2 |
| 18/11/2026 | F2.2 | `TextNormalizer`, `SalonService`, `SalonServiceValidator`, `SaveSalonServiceUseCase`/`SetSalonServiceActiveUseCase`, Room v3 (`Migration_2_3`, `3.json`), 39 tests unitarios OK + `MigrationTest` compilado. | F2.3 |
| 18/11/2026 | F2.3 | UI de Servicios (`ServicesFragment`, `ServicesViewModel`, `ServicesAdapter`, `ServiceFormDialogFragment`), enlace desde Más, criterios marcados en `servicios/skills.md`, build y tests OK. | F2.4 |
| 18/11/2026 | F2.4 | `Professional`, `ProfessionalValidator`, `SaveProfessionalUseCase`/`SetProfessionalActiveUseCase`/`ManageProfessionalDayOffUseCase`, Room v4 (`Migration_3_4`, `4.json` con FK RESTRICT), 46 tests unitarios OK + `MigrationTest` compilado. | F2.5 |
| 18/11/2026 | F2.5 | UI de Profesionales (`ProfessionalsFragment`, `ProfessionalsViewModel`, `ProfessionalsAdapter`, `ProfessionalFormFragment` con checklist de servicios, `WeeklyScheduleEditorView` y días libres), enlace desde Más, `Formats.formatPercent` + test OK. | F2.6 |
| 18/11/2026 | F2.6 | `Client`, `ClientSummary`, `PhoneNormalizer`, `ClientValidator`, `SaveClientUseCase`/`SetClientActiveUseCase` (auditoría y conflicto teléfono activo), Room v5 (`ClientEntity`, `ClientRowProjection`, `ClientDao`, `Migration_4_5`, `5.json`), 58 tests unitarios OK + `MigrationTest` compilado. | F2.7 |
| 18/11/2026 | F2.7 | UI de Clientes (`SearchField` 250 ms, `ConfirmDialog`, `ClientsFragment`, `ClientsViewModel`, `ClientsAdapter` con `ClientRowProjection`, `ClientFormDialogFragment` completo/alta rápida y advertencia duplicado, `ClientDetailFragment` con pestañas vacías F3/F5/F7), criterios F2 marcados en `clientes/skills.md`, build y tests OK. | F2.8 |
| 18/11/2026 | F2.8 | Fase F2 cerrada (`versionName 0.2.0`, `versionCode 2`): `./gradlew.bat check assembleDebug compileDebugAndroidTestJavaWithJavac` verde (58 unit tests OK, Lint 0 errores, migraciones v1→v5 compiladas), criterios F2 marcados en `configuracion/skills.md`, checklist `agents.md` §12 OK, `changelog.md` actualizado, merge `--no-ff` a `main` y tags `fase-F2` / `v0.2.0`. | F3.1 |
| 18/11/2026 | F3.1 | Rama `fase/F3`; `AppointmentStatus`, `AppointmentItem`, `Appointment` (duración redondeada a `slotMinutes`), `AppointmentBalance` y `StatusTransitions` (transiciones válidas/inválidas, paso automático `COMPLETED <-> PAID`, corrección `COMPLETED -> CONFIRMED` sin cobros y congelado de comisiones), 64 tests unitarios OK. | F3.2 |
| 18/11/2026 | F3.2 | `ScheduleBlockType`, `ScheduleBlock` y `AvailabilityChecker` (solapamientos en intervalos semiabiertos, `COMPLETED`/`PAID` bloquean, horarios salón/profesional, días libres, bloqueos generales/parciales y `suggestFreeStartMinutes`), 68 tests unitarios OK. | F3.3 |
| 18/11/2026 | F3.3 | `RecurrenceRule`, repositorios de agenda, Room v6 (`AppointmentEntity`, `AppointmentItemEntity`, `ScheduleBlockEntity`, `RecurrenceRuleEntity`, `AppointmentCardProjection`, `AppointmentDao`, `Migration_5_6`, `6.json`, FK `RESTRICT`), enlace `hasFutureActiveAppointments`, 69 tests unitarios OK + `MigrationTest`/`AppointmentDaoTest` compilados. | F3.4 |
| 18/11/2026 | F3.4 | `AppointmentSupport`, `CreateAppointmentUseCase` y `UpdateAppointmentUseCase` (snapshots de precios/duraciones conservados, habilitación por servicio, validación con `AvailabilityChecker` y auditoría), cableado en `AppContainer`, 75 tests unitarios OK. | F3.5 |
| 18/11/2026 | F3.5 | `AppointmentCardView` (borde 1 dp, barra lateral por estado y rayas en `COMPLETED`, saldo adeudado), `MoneyTextView` con soporte `textAppearance`, `AgendaViewModel`, `DayTimelineAdapter` y `AgendaFragment` (selector Día/Semana, navegación fecha, filtro profesional, bloqueos diarios y FAB). | F3.6 |
| 18/11/2026 | F3.6 | Vista semanal lunes a domingo (`WeekDaysAdapter` + `item_week_day_row.xml`) con cantidad de turnos, horas ocupadas y bloqueos, más navegación al día elegido al tocar una fila. | F3.7 |
| 18/11/2026 | F3.7 | `TimeSlotGrid`, `SlotAvailabilityLoader`, `AppointmentCreateViewModel`/`AppointmentCreateFragment` (buscador `LIMIT 12` + alta rápida de cliente, multi-servicios, filtro de profesionales habilitados, grilla libre/ocupada y edición), conectado en `nav_main.xml` y FAB de `AgendaFragment`. | F3.8 |
| 18/11/2026 | F3.8 | `ChangeAppointmentStatusUseCase` (confirmar, completar congelando comisiones, ausente incrementando cliente, cancelar con motivo y revertir completado sin cobros), `RescheduleAppointmentUseCase`, detalle `AppointmentDetailFragment` y `RescheduleDialogFragment`, 80 tests unitarios OK. | F3.9 |
| 18/11/2026 | F3.9 | `CreateScheduleBlockUseCase` (aviso de turnos afectados) y `DeactivateScheduleBlockUseCase`, UI `ScheduleBlocksFragment`/`ScheduleBlockFormDialogFragment`, enlace en `AgendaFragment` y `nav_main.xml`, 82 tests unitarios OK. | F3.10 |
| 18/11/2026 | F3.10 | `RecurrenceRule`, `GenerateRecurringAppointmentsUseCase` (series cada N semanas y reporte de fechas en conflicto) y `CancelRecurringSeriesUseCase` (cancelación única o este y siguientes), UI en `AppointmentCreateFragment` y `AppointmentDetailFragment`, 84 tests unitarios OK. | F3.11 |
| 18/11/2026 | F3.11 | `ClientVisitsAdapter`, `AppointmentCardView.bindForClientHistory` e historial real de visitas `COMPLETED`/`PAID` más saldo pendiente reactivo en `ClientDetailFragment` / `ClientsViewModel`; `DebugSeeder` SQLite por lotes (5.000 clientes y 50.000 turnos) accesible en build debug y test DAO compilado OK. | F3.12 |
| 18/11/2026 | F3.12 | Fase F3 cerrada (`versionName 0.3.0`, `versionCode 3`): `./gradlew.bat check assembleDebug compileDebugAndroidTestJavaWithJavac` verde (Lint y 84 tests unitarios OK, migraciones v1→v6 compiladas), criterios marcados en `agenda/skills.md`, `configuracion/skills.md`, `servicios/skills.md` y `profesionales/skills.md`, checklist `agents.md` §12 OK, `changelog.md` actualizado, merge `--no-ff` a `main` y tags `fase-F3` / `v0.3.0`. | F4.1 |
| 18/11/2026 | F4.1 | `DevicePrefs` ampliado (URI SAF, último backup y parámetros AUTO/retención/`ON_BACKGROUND`/aviso), `BackupFolderAccess` con `takePersistableUriPermission` y detección de permiso perdido, UI `BackupFragment`/`BackupViewModel` conectada desde `MoreFragment` y `nav_main.xml`. | F4.2 |
| 18/11/2026 | F4.2 | `BackupTrigger`, `BackupManifest`, `BackupManifestCodec` + `BackupManifestCodecTest`, y `BackupManager` (checkpoint WAL `TRUNCATE`, hash SHA-256, paquete ZIP `metanoia.db` + `manifest.json`, lock mutuo, `AuditLog` y botón manual) OK. | F4.3 |
| 18/11/2026 | F4.3 | `FileProvider` (`file_paths.xml`) en manifiesto, exportación manual con `ACTION_CREATE_DOCUMENT` y acción Compartir (`ACTION_SEND`) con ZIP en caché en `BackupFragment`/`BackupViewModel`. | F4.4 |
| 18/11/2026 | F4.4 | `BackupRetentionPolicy` (más reciente por cada uno de los últimos N días con backup, últimos 3 `PRE_RESTORE` aparte, umbral `ON_BACKGROUND` y protección de archivos propios/nuevos) + `BackupRetentionPolicyTest` (5/5 OK), `BackupWorker`, `BackupScheduler` (`AUTO` periódico, `ON_BACKGROUND` y `CASH_CLOSE` únicos con `KEEP`) y observer `ProcessLifecycleOwner` en `MetanoiaApp`. | F4.5 |
| 18/11/2026 | F4.5 | `BackupValidator` + `BackupValidatorTest` (3/3 OK: esquema igual/menor vs mayor, formato y SHA-256) y `RestoreManager` (extracción ZIP segura, validación de hash, `PRAGMA integrity_check`, backup previo `PRE_RESTORE` automático, rollback ante fallos y `MetanoiaApp.resetContainer`). | F4.6 |
| 18/11/2026 | F4.6 | Flujo completo de restauración en `BackupFragment`/`BackupViewModel` (`OpenDocument`, inspección previa, diálogo de resumen y confirmación, `PRE_RESTORE` automático, reinicio de `AppContainer` y `MainActivity`). | F4.7 |
| 18/11/2026 | F4.7 | `AlertBanner` en `fragment_agenda.xml` y `AgendaFragment` para alertar carpeta faltante, permiso perdido o último backup superior a N días configurados (default 3) con acceso directo a `BackupFragment`. | F4.8 |
| 18/11/2026 | F4.8 | Fase F4 cerrada (`versionName 0.4.0`, `versionCode 4`): `BackupPackageRoundTripTest` y `BackupRestoreRoundTripTest` (`androidTest`), `./gradlew.bat check assembleDebug compileDebugAndroidTestJavaWithJavac` verde (Lint y 95 tests unitarios OK), 9 criterios marcados en `backup/skills.md`, checklist `agents.md` §12 OK, `changelog.md` actualizado, merge `--no-ff` a `main` y tags `fase-F4` / `v0.4.0`. | F5.1 |
| 18/11/2026 | F5.1 | Modelos inmutables `PaymentMethod`, `CashMovementType`, `CashMovement`, `CashClosing`, `AppointmentPaymentCalculator`, interfaz `CashRepository` y `ReverseMovementUseCase` (reverso con auditoría, paso automático `PAID -> COMPLETED` y cierre previo respetado) + `AppointmentPaymentCalculatorTest` (1/1 OK) y `ReverseMovementUseCaseTest` (3/3 OK). | F5.2 |
| 18/11/2026 | F5.2 | Persistencia Room v7 (`CashClosingEntity`, `CashMovementEntity` con FK `RESTRICT`, `CashDao`, `CashMapper`, `CashRepositoryImpl`, `Migration_6_7`, `7.json`), proyección `paid_cents` y consultas "Por cobrar" en `AppointmentDao`/`AppointmentCardProjection`, `BackupManager` v7, `SchemaExportTest` (7/7 OK) y `MigrationTest` v7 compilado. | F5.3 |
| 18/11/2026 | F5.3 | `CashPaymentSupport`, `ChargeAppointmentUseCase` + `ChargeAppointmentUseCaseTest` (3/3 OK), `ChargeAppointmentDialogFragment` (parcial/dividido/ajuste con motivo), pagos registrados y reverso en `AppointmentDetailFragment`. | F5.4 |
| 18/11/2026 | F5.4 | `CompleteAndChargeUseCase` atómico en una sola transacción (congela comisiones + registra cobro), acción en <= 3 toques desde `AppointmentDetailFragment` (`PENDING`/`CONFIRMED`), `CompleteAndChargeUseCaseTest` (3/3 OK) y test instrumentado `CompleteAndChargeAtomicityTest` compilado. | F5.5 |
| 18/11/2026 | F5.5 | `ReceivablesViewModel`, `ReceivablesAdapter` (reutilizando `AppointmentCardView.bindForReceivable`), `ReceivablesFragment` + `fragment_receivables.xml` conectados en `nav_main.xml` (`nav_receivables`), apertura directa del cobro y contador reactivo `Por cobrar (N)` en `CashFragment`. | F5.6 |
| 18/11/2026 | F5.6 | `RecordCashMovementUseCase` + `RecordCashMovementUseCaseTest` (4/4 OK), pantalla diaria `CashFragment`/`CashViewModel` con navegación de fecha, resumen neto por 4 medios de pago, listado de movimientos con reverso inmutable y diálogo `CashMovementFormDialogFragment` para ventas sueltas, otros ingresos y gastos. | F5.7 |
| 18/11/2026 | F5.7 | `CashClosingCalculator` + `CashClosingCalculatorTest` (2/2 OK), `CloseCashDayUseCase` + `CloseCashDayUseCaseTest` (3/3 OK), `CashClosingDialogFragment` con arqueo esperado vs contado por medio en vivo, alerta `AlertBanner` de días previos sin cerrar, resumen de cierre y bloqueo de día cerrado en `CashFragment`. | F5.8 |
| 18/11/2026 | F5.8 | `CashCloseBackupTrigger` inyectado en `CloseCashDayUseCase` y conectado en `AppContainer` a `BackupScheduler.enqueueCashClose` (`UNIQUE_WORK_CASH_CLOSE`, `KEEP`, no bloqueante); tests actualizados en `CloseCashDayUseCaseTest` (3/3 OK). | F5.9 |
| 18/11/2026 | F5.9 | `ReceiptPdfBuilder` compartible por `FileProvider` desde `AppointmentDetailFragment` (logo reemplazable + "METANOIA Hair Salon", ítems, pagos y saldo), pestaña real de Gastos e historial en `ClientDetailFragment` (`ClientSpendingAdapter`) y criterio 4 marcado en `clientes/skills.md`. | F5.10 |
| 18/11/2026 | F5.10 | Fase F5 cerrada (`versionName 0.5.0`, `versionCode 5`): `./gradlew.bat check assembleDebug compileDebugAndroidTestJavaWithJavac` verde (Lint, 115 tests unitarios OK y tests instrumentados compilados), 11 criterios marcados en `caja/skills.md`, checklist `agents.md` §12 OK, `changelog.md` actualizado, merge `--no-ff` a `main` y tags `fase-F5` / `v0.5.0`. | F6.1 |
| 18/11/2026 | F6.1 | `TemplateRenderer` + `TemplateRendererTest` (4/4 OK), 3 plantillas editables en `Settings`/`UpdateSettingsUseCase`, Room v8 (`SettingsEntity`, `Migration_7_8`, `8.json`, `BackupManager` v8, `SchemaExportTest` 8/8 OK, `MigrationTest` v8 compilado) y editor `TemplatesFragment` con vista previa en vivo accesible desde `SettingsFragment`. | F6.2 |
| 18/11/2026 | F6.2 | `WhatsAppUrlBuilder` (`https://wa.me/<tel>?text=...` UTF-8) + `WhatsAppUrlBuilderTest` (2/2 OK) y `WhatsAppLauncher` con manejo de teléfono faltante o WhatsApp no instalado, sin permiso `INTERNET`. | F6.3 |
| 18/11/2026 | F6.3 | `ReminderType`, `Reminder`, `ReminderEligibility` + `ReminderEligibilityTest` (2/2 OK), `ReminderRepository`, `RecordReminderSentUseCase` + `RecordReminderSentUseCaseTest` (2/2 OK), Room v9 (`ReminderEntity` FK RESTRICT, `ReminderDao`, `Migration_8_9`, `9.json`, `BackupManager` v9, `SchemaExportTest` 9/9 OK, `MigrationTest` v9 compilado) y pantalla `RemindersFragment` con lista de pendientes de mañana y deshacer envío. | F6.4 |
| 18/11/2026 | F6.4 | Acción `Recordar por WhatsApp` en un toque desde `AppointmentDetailFragment` / `AppointmentDetailViewModel` con plantilla renderizada, registro de envío, deshabilitación con aviso si falta teléfono y opción Deshacer. | F6.5 |
| 18/11/2026 | F6.5 | `BirthdayRemindersAdapter` y pestaña Cumpleaños en `RemindersFragment` para saludar por WhatsApp con registro anual anti-duplicados, edad cuando hay año cargado y opción Deshacer. | F6.6 |
| 18/11/2026 | F6.6 | `DigestScheduleCalculator` + `DigestScheduleCalculatorTest` (2/2 OK), `DevicePrefs` con hora/activación configurables, `POST_NOTIFICATIONS` con explicación previa en Android 13+, `ReminderNotificationHelper`, `ReminderDigestWorker` y `ReminderDigestScheduler` 100 % locales sin red más acceso directo desde notificación en `MainActivity`. | F6.7 |
| 18/11/2026 | F6.7 | Verificación de fase F6: `./gradlew.bat check assembleDebug compileDebugAndroidTestJavaWithJavac` OK (`lintDebug`, `lint`, 129 tests unitarios: 109 en `:domain` + 20 en `:app:testDebugUnitTest`, 0 fallos), reglas estáticas §12 OK, criterios `recordatorios/skills.md` (6/6) y `configuracion/skills.md` (criterio 3) marcados `[x]`, `versionCode 6` / `versionName "0.6.0"` y `changelog.md` actualizados; merge `--no-ff` en `main` con tags `fase-F6` y `v0.6.0`. | F7.1 |
| 18/11/2026 | F7.1 | Modelos `ColorComponentUnit`, `ColorFormulaComponent` y `ColorFormula`, `ColorFormulaRules` + `ColorFormulaRulesTest` (5/5 OK), `SaveColorFormulaUseCase`, `DuplicateColorFormulaUseCase`, `DeleteColorFormulaUseCase` + `SaveColorFormulaUseCaseTest` (3/3 OK), Room v10 (`Migration_9_10`, `10.json`, `ColorFormulaDao`, upsert seguro en `AppointmentDao`), `BackupManager` v10, `SchemaExportTest` (10/10 OK) y `MigrationTest` v10 compilado. | F7.2 |
| 18/11/2026 | F7.2 | UI en pestaña Color de `ClientDetailFragment`: `ClientColorFormulasAdapter` (`item_color_formula_row.xml`), `ColorFormulaDialogFragment` (`dialog_color_formula_form.xml`, `item_color_component_input.xml`) con filas agregables de componentes, selector de fecha/profesional, modo solo lectura + duplicado en fórmulas cerradas y baja lógica; `./gradlew.bat test assembleDebug` OK. | F7.3 |
| 18/11/2026 | F7.3 | Repetir última fórmula en crear turno: `AppointmentSupport` + `CreateAppointmentUseCase` + `GenerateRecurringAppointmentsUseCase` con duplicado borrador y vínculo `colorFormulaId` en ítems de coloración, test en `CreateAppointmentUseCaseTest`, `AppointmentCreateFragment`/`ViewModel` (chip de 1 toque si hay servicio de coloración y fórmula previa) y etiqueta vinculada en `AppointmentDetailFragment`; `./gradlew.bat test assembleDebug` OK. | F7.4 |
| 18/11/2026 | F7.4 | Verificación de fase F7: `./gradlew.bat check assembleDebug compileDebugAndroidTestJavaWithJavac` OK (`lintDebug`, `lint`, `check`, 139 tests unitarios: 118 en `:domain` + 21 en `:app:testDebugUnitTest`, 0 fallos), reglas estáticas §12 OK, criterios `ficha-color/skills.md` (5/5) marcados `[x]`, `versionCode 7` / `versionName "0.7.0"` y `changelog.md` actualizados; merge `--no-ff` en `main` con tags `fase-F7` y `v0.7.0`. | F8.1 |
| 18/11/2026 | F8.1 | Modelos `WaitlistStatus` y `WaitlistEntry`, `SaveWaitlistEntryUseCase`, `CancelWaitlistEntryUseCase`, `ExpireWaitlistEntriesUseCase` + `SaveWaitlistEntryUseCaseTest` (3/3 OK), Room v11 (`WaitlistEntryEntity`, `WaitlistEntryServiceCrossRef`, `WaitlistDao`, `Migration_10_11`, `11.json`, `BackupManager` v11, auto-vencimiento en `MetanoiaApp`), `SchemaExportTest` (11/11 OK) y `MigrationTest` v11 compilado. | F8.2 |
| 18/11/2026 | F8.2 | UI de lista de espera (`WaitlistFragment`, `WaitlistViewModel`, `WaitlistAdapter`, `WaitlistFormDialogFragment` reutilizable con prefills, `ClientDao.observeSummariesByIds`, `nav_waitlist`) y accesos desde `AgendaFragment`, `AppointmentCreateFragment` (al no haber horarios libres), `ClientDetailFragment` y `MoreFragment`; `./gradlew.bat test assembleDebug` OK. | F8.3 |
