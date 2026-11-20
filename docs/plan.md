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
| F8 | Lista de espera | [x] | 4/4 | 100 % |
| F9 | Stock | [x] | 6/6 | 100 % |
| F10 | Comisiones | [x] | 4/4 | 100 % |
| F11 | Reportes | [x] | 6/6 | 100 % |
| F12 | Seguridad y pulido final | [x] | 7/7 | 100 % |
| F13 | Clientes desde contactos | [~] | 5/7 | 71 % |
| **Total** | | | **91/93** | **98 %** |

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

## F8 — Lista de espera `[x]`
Módulo: `lista-espera`.

1. [x] **F8.1 Modelo y persistencia**
   - Archivos: `WaitlistEntry`, DAO, migración
   - Hecho: preferencias de día/franja/profesional/servicio; vencimiento.
2. [x] **F8.2 UI de lista de espera**
   - Archivos: `ui/waitlist/`
   - Hecho: alta rápida desde cliente o agenda; listado activo.
3. [x] **F8.3 Sugerencias al cancelar**
   - Archivos: `WaitlistMatcher`, diálogo post-cancelación
   - Hecho: al cancelar, sugiere candidatos ordenados; ofrecer por WhatsApp o asignar el turno; tests del matcher.
4. [x] **F8.4 Verificación de fase**
   - Hecho: criterios de `lista-espera/skills.md` OK; `versionName 0.8.0`; merge de `fase/F8` a `main`, tag `fase-F8` y entrada en `docs/changelog.md`.

## F9 — Stock `[x]`
Módulo: `stock`.

1. [x] **F9.1 Modelo y persistencia**
   - Archivos: `Product`, `StockMovement`, DAOs, migración
   - Hecho: stock = suma de movimientos; tipos uso/venta; tests.
2. [x] **F9.2 UI de productos**
   - Archivos: `ui/stock/`
   - Hecho: lista con filtro por tipo y búsqueda; alta/edición/desactivación.
3. [x] **F9.3 Movimientos**
   - Archivos: `ui/stock/movements/`
   - Hecho: ingreso, egreso por uso, ajuste con motivo; auditados.
4. [x] **F9.4 Alertas de stock bajo**
   - Archivos: `LowStockRule`, `AlertBanner`
   - Hecho: chip y banner cuando cantidad ≤ mínimo; tests.
5. [x] **F9.5 Integración con caja**
   - Archivos: venta suelta de caja
   - Hecho: vender producto descuenta stock y registra movimiento de caja en una única transacción.
6. [x] **F9.6 Verificación de fase**
   - Hecho: criterios de `stock/skills.md` OK; `versionName 0.9.0`; merge de `fase/F9` a `main`, tag `fase-F9` y entrada en `docs/changelog.md`.

## F10 — Comisiones `[x]`
Módulo: `comisiones`.

1. [x] **F10.1 Calculadora de comisiones**
   - Archivos: `CommissionCalculator`, tests
   - Hecho: solo turnos PAID del período; usa % congelado al completar; COMPLETED con saldo no liquida; redondeo a centavo definido; tests de casos borde.
2. [x] **F10.2 UI por profesional y período**
   - Archivos: `ui/commissions/`
   - Hecho: detalle por turno y total; filtros semana/mes/rango.
3. [x] **F10.3 Liquidación**
   - Archivos: `SettleCommissionUseCase`
   - Hecho: registrar pago de comisión como egreso de caja auditado; período liquidado no se recalcula.
4. [x] **F10.4 Verificación de fase**
   - Hecho: criterios de `comisiones/skills.md` OK; `versionName 0.10.0`; merge de `fase/F10` a `main`, tag `fase-F10` y entrada en `docs/changelog.md`.

## F11 — Reportes `[x]`
Módulo: `reportes`.

1. [x] **F11.1 Consultas agregadas**
   - Archivos: `ReportsDao`, `ReportsRepository`
   - Hecho: ingresos por día/mes (desde `CashMovement`), servicios top, clientes frecuentes y rendimiento por profesional (turnos COMPLETED o PAID), ocupación por franja, indicador "por cobrar"; tests DAO.
2. [x] **F11.2 Pantalla de reportes**
   - Archivos: `ui/reports/`
   - Hecho: filtros de período/profesional; resultados en < 1 s con 50.000 turnos.
3. [x] **F11.3 Gráficos simples**
   - Archivos: `ui/common/BarChartView`
   - Hecho: barras monocromáticas propias (sin librerías).
4. [x] **F11.4 Exportar CSV**
   - Archivos: `CsvExporter`
   - Hecho: formato de `design.md` §9; guardado con SAF o compartir.
5. [x] **F11.5 Exportar PDF**
   - Archivos: `ReportPdfBuilder`
   - Hecho: PDF A4 con cabecera de marca y tablas.
6. [x] **F11.6 Verificación de fase**
   - Hecho: criterios de `reportes/skills.md` OK; `versionName 0.11.0`; merge de `fase/F11` a `main`, tag `fase-F11` y entrada en `docs/changelog.md`.

## F12 — Seguridad y pulido final `[x]`
Módulos: `configuracion` y todos.

1. [x] **F12.1 PIN y huella**
   - Archivos: `ui/lock/`, `BiometricPrompt`
   - Hecho: bloqueo opcional al abrir y al volver de segundo plano (tiempo configurable); PIN con hash + sal.
2. [x] **F12.2 Accesibilidad**
   - Hecho: TalkBack con `contentDescription`; texto escalable al 130 % sin cortes; contraste verificado.
3. [x] **F12.3 Rendimiento**
   - Hecho: arranque en frío < 2 s; sin jank en agenda con datos de prueba (perfilado).
4. [x] **F12.4 Revisión de seguridad**
   - Hecho: manifiesto sin INTERNET; R8 activo; sin logs sensibles; exportaciones solo por acción explícita.
5. [x] **F12.5 APK release firmado (v1.0.0)**
   - Archivos: `app/build.gradle` (`versionName "1.0.0"`, `versionCode` +1, `signingConfig` leyendo `keystore.properties`), `docs/instalacion.md`
   - Hecho: keystore generada y guardada **fuera del repo** con copia en al menos dos lugares; `keystore.properties` ignorado por Git (verificado con `git status`); APK release instalable; `docs/instalacion.md` documenta instalación, ubicación/resguardo de la keystore y cómo firmar futuras versiones.
6. [x] **F12.6 Manual de usuario breve**
   - Archivos: `docs/manual.md`
   - Hecho: tareas frecuentes y backup/restauración explicadas en ≤ 2 páginas.
7. [x] **F12.7 Verificación final**
   - Hecho: todos los `skills.md` con criterios marcados; `./gradlew check` verde; merge de `fase/F12` a `main`, tags `fase-F12` y `v1.0.0`, entrada `1.0.0` en `docs/changelog.md`.

---

## F13 — Clientes desde contactos `[~]`
Entregable: importación masiva inicial desde agenda, filtros "Sin cumpleaños" e "Importados recientemente", sincronización automática en segundo plano y al volver a la app (Room v14), APK release firmado `v1.1.0`. Módulos: `clientes`, `configuracion`.

1. [x] **F13.1 Dominio de importación, cumpleaños y sincronización de contactos**
   - Archivos: `domain/.../model/{Client,ClientOrigin,DeviceContactCandidate,ContactImportPreview,ContactSyncOutcome}`, `domain/.../rules/{ContactPhoneSelector,ContactBirthdayParser,ClientContactSyncPlanner}`, `domain/.../usecase/{PreviewContactImportUseCase,ImportAllContactsUseCase,SyncNewContactsUseCase}`, tests en `domain/src/test/...`
   - Hecho: selección de teléfono principal (primer móvil válido o primero disponible), normalización `+54`/espacios/guiones/`0`/`15`, parseo de cumpleaños con y sin año (`--MM-DD`), nombre recortado 2–80 caracteres o teléfono si falta, deduplicación por `contactLookupKey` y teléfono normalizado, protección contra sobrescritura y reactivación de inactivos, idempotencia y `AuditLog` sin PII; 100 % de los tests unitarios obligatorios en verde (`./gradlew.bat :domain:test`).
2. [x] **F13.2 Migración Room v14, DAOs y backup/restauración v14**
   - Archivos: `ClientEntity`, `ClientMapper`, `ClientDao`, `ClientRepositoryImpl`, `AppDatabase` (`version = 14`), `Migration_13_14`, `app/schemas/.../14.json`, `BackupManager`, `RestoreManager`, `MigrationTest`, `SchemaExportTest`, test de backup round-trip
   - Hecho: columnas `contact_lookup_key` (indexada), `origin` (default `MANUAL`) y `created_from_contact_at` en `clients` con migración sin pérdida de datos; consultas SQL para clientes sin cumpleaños, contador de pendientes e importados en los últimos 7 días; `SchemaExportTest` (v14) y test de ida y vuelta del backup verde; `MigrationTest` compilado en `androidTest`.
3. [x] **F13.3 Permiso `READ_CONTACTS` y lector de solo lectura del dispositivo**
   - Archivos: `AndroidManifest.xml`, `data/contacts/DeviceContactsReader.java`, actualización de `ManifestPrivacyTest` y `SecurityAuditTest`
   - Hecho: permiso `READ_CONTACTS` declarado (mientras `INTERNET` sigue bloqueado con `tools:node="remove"` y `allowBackup="false"`); lectura en solo lectura de todas las cuentas (`ContactsContract`: nombre, teléfonos con tipo y evento `BIRTHDAY`) sin logs de nombres o teléfonos; auditorías de manifiesto y seguridad en verde.
4. [x] **F13.4 Flujo de importación masiva inicial (Clientes y asistente inicial)**
   - Archivos: `ClientsFragment`, `SettingsFragment` / asistente inicial (`SetupWizard` o diálogo inicial en primer uso), `ContactImportDialogFragment`, `ContactImportViewModel`, layouts y `strings.xml`
   - Hecho: botón "Importar desde contactos" visible en Clientes y en el asistente inicial; pantalla explicativa antes de solicitar `READ_CONTACTS`; vista previa con 3 contadores (a importar, ya cargados, omitidos) y botón "Importar todos"; ejecución en una sola transacción dentro de `AppExecutors.io()` con indicador de progreso y activación automática por defecto al finalizar.
5. [x] **F13.5 Filtros de cumpleaños pendientes e importados recientemente**
   - Archivos: `ClientsFragment`, `ClientsViewModel`, `ClientsAdapter`, `ClientFormDialogFragment`, layouts y `strings.xml`
   - Hecho: filtro **"Sin cumpleaños"** con contador visible y edición directa del campo cumpleaños con un toque; filtro **"Importados recientemente"** (últimos 7 días, `origin = CONTACT_SYNC`) para revisar altas automáticas y permitir baja lógica manual.
6. [ ] **F13.6 Sincronización automática (WorkManager URI trigger, respaldo 15 min y Configuración)**
   - Archivos: `data/contacts/ContactSyncWorker.java`, `ContactSyncScheduler.java`, `DevicePrefs`, `MetanoiaApp`, `MainActivity`, `SecuritySettingsFragment` / `SettingsFragment`, layouts y `strings.xml`
   - Hecho: `ContactSyncWorker` con `Constraints.Builder.addContentUriTrigger(ContactsContract.Contacts.CONTENT_URI, true)` auto-reprogramable; respaldo al abrir y volver a primer plano con límite mínimo de 15 min; pausa sin fallos con aviso visible en Configuración cuando se revoca `READ_CONTACTS`; interruptor "Sincronizar contactos automáticamente" con fecha y resultado de la última sincronización.
7. [ ] **F13.7 Verificación final F13, firma v1.1.0 y entrega**
   - Archivos: `app/build.gradle` (`versionCode 13`, `versionName "1.1.0"`), `docs/modules/clientes/skills.md`, `docs/changelog.md`, `docs/plan.md`
   - Hecho: todos los criterios de `clientes/skills.md` marcados `[x]`; `./gradlew.bat check assembleDebug assembleRelease compileDebugAndroidTestJavaWithJavac` verde sin errores de Lint; APK release firmado con la keystore existente de F12.5 (`keystore.properties`) verificado con `apksigner`; entrada `1.1.0` en `docs/changelog.md`; merge `--no-ff` de `fase/F13` a `main`, tags `fase-F13` y `v1.1.0`, entrega de ruta exacta del APK y guía breve de prueba manual.

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
| 18/11/2026 | F8.3 | `WaitlistMatcher` + `WaitlistMatcherTest` (2/2 OK), `FindWaitlistCandidatesUseCase`, `OfferWaitlistEntryUseCase`, `AssignWaitlistEntryUseCase` + `WaitlistActionsUseCaseTest` (2/2 OK), diálogo `WaitlistSuggestionsDialogFragment` con Ofrecer (WhatsApp)/Asignar (turno con revalidación)/Ignorar disparado al cancelar o reprogramar desde `AppointmentDetailFragment`; `./gradlew.bat test assembleDebug` OK. | F8.4 |
| 18/11/2026 | F8.4 | Verificación de fase F8: `./gradlew.bat check assembleDebug compileDebugAndroidTestJavaWithJavac` OK (`lintDebug`, `lint`, `check`, 147 tests unitarios: 125 en `:domain` + 22 en `:app:testDebugUnitTest`, 0 fallos), reglas estáticas §12 OK, criterios `lista-espera/skills.md` (5/5) marcados `[x]`, `versionCode 8` / `versionName "0.8.0"` y `changelog.md` actualizados; merge `--no-ff` en `main` con tags `fase-F8` y `v0.8.0`. | F9.1 |
| 18/11/2026 | F9.1 | Rama `fase/F9`; modelos `ProductType`, `ProductUnit`, `StockMovementType`, `Product` y `StockMovement`, regla `LowStockRule`, `StockRepository`, `SaveProductUseCase`, `SetProductActiveUseCase` + `SaveProductUseCaseTest` (3/3 OK), Room v12 (`ProductEntity`, `StockMovementEntity` FK RESTRICT, `StockDao`, `StockMapper`, `StockRepositoryImpl`, `Migration_11_12`, `12.json`, `BackupManager` v12), `SchemaExportTest` (12/12 OK) y `MigrationTest` v12 compilado. | F9.2 |
| 18/11/2026 | F9.2 | UI de catálogo de stock (`StockFragment`, `StockViewModel`, `ProductsAdapter`, `ProductFormDialogFragment`, `StockUiLabels`, enlace `rowStock` en `MoreFragment` y `nav_stock`), filtro por tipo Todos/Uso/Venta/Inactivos, buscador con debounce 250 ms, orden con stock bajo primero y alta/edición/baja lógica; `./gradlew.bat :domain:test :app:testDebugUnitTest assembleDebug` OK. | F9.3 |
| 18/11/2026 | F9.3 | `RecordStockMovementUseCase` + `RecordStockMovementUseCaseTest` (3/3 OK: suma de movimientos, ajuste con motivo obligatorio, stock negativo permitido con advertencia y auditoría), detalle `ProductDetailFragment` / `ProductDetailViewModel`, historial `StockMovementsAdapter` y formulario `StockMovementFormDialogFragment` (`IN`/`OUT_USE`/`ADJUSTMENT`), conectado desde `StockFragment`; `./gradlew.bat :domain:test :app:testDebugUnitTest assembleDebug` OK. | F9.4 |
| 18/11/2026 | F9.4 | `LowStockRuleTest` (3/3 OK: límite `<= minStock`, negativos, suma firmada, exclusión de inactivos en conteo y orden con bajos primero) y `AlertBanner` reactivo de stock bajo en `AgendaFragment` / `AgendaViewModel` con acceso directo a `nav_stock`, sumado a `StatusChip` y banner en `StockFragment`; `./gradlew.bat :domain:test :app:testDebugUnitTest assembleDebug` OK. | F9.5 |
| 18/11/2026 | F9.5 | `SellProductUseCase` (venta + descuento `OUT_SALE` atómicos en una única transacción, rechazo de productos `USE` y días cerrados), reposición automática de stock en `ReverseMovementUseCase` al revertir `PRODUCT_SALE`, `SellProductUseCaseTest` (4/4 OK con rollback transaccional), `ReverseMovementUseCaseTest` (4/4 OK), `ProductSaleAtomicityTest` (`androidTest`) y selector de productos `SALE`/`BOTH` con cantidad y alerta negativa en `CashMovementFormDialogFragment`/`CashViewModel`; build y tests OK. | F9.6 |
| 18/11/2026 | F9.6 | Verificación de fase F9: `./gradlew.bat check assembleDebug compileDebugAndroidTestJavaWithJavac` OK (`lintDebug`, `lint`, `check`, 162 tests unitarios: 139 en `:domain` + 23 en `:app:testDebugUnitTest`, 0 fallos), reglas estáticas §12 OK, criterios `stock/skills.md` (5/5) marcados `[x]`, `versionCode 9` / `versionName "0.9.0"` y `changelog.md` actualizados; merge `--no-ff` en `main` con tags `fase-F9` y `v0.9.0`. | F10.1 |
| 19/11/2026 | F10.1 | Rama `fase/F10`; modelos `CommissionSettlementEntryType`, `CommissionSettlementEntry`, `CommissionSettlement`, `CommissionRepository`, métodos default por rango/IDs en `AppointmentRepository`, y calculadora pura `CommissionCalculator` (prorrateo de descuentos/recargos, redondeo `HALF_UP` por ítem, % congelado, exclusión de no-`PAID`, prevención de doble liquidación y ajustes negativos por reverso) + `CommissionCalculatorTest` (4/4 OK); `./gradlew.bat :domain:test` OK. | F10.2 |
| 19/11/2026 | F10.2 | Consultas por profesional/rango e IDs en `AppointmentDao`/`AppointmentRepositoryImpl`/`ClientDao`, y UI `ui/commissions/` (`CommissionsFragment`, `CommissionsViewModel`, `CommissionDetailsAdapter`) enlazada desde `MoreFragment` (`rowCommissions`, `nav_commissions`) con filtros Semana/Mes/Rango, total base/comisión/pendiente de cobro y detalle por turno e ítems; `./gradlew.bat :domain:test :app:testDebugUnitTest assembleDebug` OK. | F10.3 |
| 19/11/2026 | F10.3 | Persistencia Room v13 (`CommissionSettlementEntity`, `CommissionSettlementEntryEntity` FK RESTRICT, `CommissionDao`, `CommissionMapper`, `CommissionRepositoryImpl`, `Migration_12_13`, `13.json`, `BackupManager` v13), `SettleCommissionUseCase` (egreso `COMMISSION_PAYOUT` + liquidación inmutable + auditoría en una única transacción), diálogo `SettleCommissionDialogFragment`, `CommissionSettlementsAdapter` + alerta de solapamiento en `CommissionsFragment`; `SettleCommissionUseCaseTest` (4/4 OK), `SchemaExportTest` (13/13 OK), `MigrationTest` v13 y `CommissionSettlementAtomicityTest` compilados OK. | F10.4 |
| 19/11/2026 | F10.4 | Cierre F10 Comisiones (`versionCode 10`, `versionName 0.10.0`, `docs/changelog.md`, 6/6 criterios de `comisiones/skills.md` verificados); fix Lint `DiffUtilEquals` en `CommissionDetailsAdapter`; `./gradlew.bat check assembleDebug compileDebugAndroidTestJavaWithJavac` OK (`:domain:test` 147/147, `:app:testDebugUnitTest` 24/24, total 171 unit tests OK, `:app:lintDebug` OK), checks §12 OK (`allowBackup="false"`, sin `INTERNET`, esquema `13.json`), `androidTest` compilados (sin ejecución en dispositivo/emulador); merge `--no-ff fase/F10` en `main` y tags `fase-F10` / `v0.10.0`. | F11.1 |
| 19/11/2026 | F11.1 | Rama `fase/F11`; modelos de dominio para los 6 reportes (`ReportType`, `ReportFilter`, `IncomeReport`, `TopServicesReport`, `FrequentClientsReport`, `OccupancyReport`, `ProfessionalPerformanceReport`, `ReceivablesReport`), regla `OccupancyCalculator` + `ScheduleValidator.mergeAdjacentByDay`, `ReportsRepository`, proyecciones + `ReportsDao` (`GROUP BY`/CTEs SQL), `ReportsRepositoryImpl` y wiring en `AppDatabase`/`AppContainer`; `OccupancyCalculatorTest` (3/3 OK), `IncomeReportTest` (1/1 OK), `ReportsDaoTest` compilado (incluye benchmark 50.000 turnos); `./gradlew.bat :domain:test :app:testDebugUnitTest assembleDebug compileDebugAndroidTestJavaWithJavac` OK. | F11.2 |
| 19/11/2026 | F11.2 | Pantalla `ui/reports/` (`ReportsFragment`, `ReportsViewModel`, `ReportsUiModels`, `ReportPresentationMapper`, `ReportOperationalMapper`, `ReportRowsAdapter`, `fragment_reports.xml`, `item_report_row.xml`) enlazada desde `MoreFragment` (`rowReports`, `nav_reports`) con selector de los 6 reportes, filtros Hoy/Semana/Mes/Rango + profesional, sub-filtros contextuales, tarjetas KPI, desglose por método, menor demanda en Ocupación y saldo histórico en Por cobrar; `./gradlew.bat :domain:test :app:testDebugUnitTest assembleDebug` OK. | F11.3 |
| 19/11/2026 | F11.3 | Vista propia monocromática `BarChartView` (tema Claro/Oscuro vía `MaterialColors`, soporte de negativos y tope fijo 100 % en Ocupación con barras atenuadas para menor demanda, salto automático de etiquetas y `contentDescription` accesible) + `ReportChartMapper` integrado en `fragment_reports.xml`/`ReportsFragment`, y `BarChartScaleTest` (3/3 OK); `./gradlew.bat :domain:test :app:testDebugUnitTest assembleDebug` OK. | F11.4 |
| 19/11/2026 | F11.4 | Exportador `CsvExporter` (`design.md` §9: UTF-8 con BOM, `;`, decimales con coma, fechas `dd/MM/yyyy`, escape RFC 4180, protección contra inyección de fórmulas y opt-in de teléfono en Clientes frecuentes sin notas) + `shared_reports` en `file_paths.xml`, guardado SAF / compartir `FileProvider` vía `ReportExportUiHelper` y `ReportFilterUiHelper` (`ReportsFragment` < 400 líneas), y `CsvExporterTest` (3/3 OK); `./gradlew.bat :domain:test :app:testDebugUnitTest assembleDebug` OK. | F11.5 |
| 19/11/2026 | F11.5 | Exportador PDF A4 blanco y negro `ReportPdfBuilder` + `ReportPdfSpec` + `ReportPdfSpecFactory` + `ReportExportPayloadHelper` (cabecera con logo reemplazable y marca Metanoia Hair Salon, KPIs, tablas con bordes `gray200`, alineación numérica y truncamiento, paginación multipágina y pie `"Página X de Y"`), botón `Exportar PDF` (SAF `CreateDocument("application/pdf")` / compartir `FileProvider`) y `ReportPdfSpecTest` (2/2 OK); `./gradlew.bat :domain:test :app:testDebugUnitTest assembleDebug` OK. | F11.6 |
| 19/11/2026 | F11.6 | Cierre F11 Reportes (`versionCode 11`, `versionName 0.11.0`, `docs/changelog.md`, 5/5 criterios de `reportes/skills.md` verificados); `./gradlew.bat check assembleDebug compileDebugAndroidTestJavaWithJavac` OK (`:domain:test` 151/151, `:app:testDebugUnitTest` 32/32, total 183 unit tests OK, `:app:lintDebug` OK), checks §12 OK (`allowBackup="false"`, sin `INTERNET`, 0 `.kt`/`.kts`, `:domain` puro, sin valores sueltos en layouts F11), `ReportsDaoTest` (benchmark 50.000 turnos) compilado en `androidTest` sin ejecución en dispositivo/emulador; merge `--no-ff fase/F11` en `main` y tags `fase-F11` / `v0.11.0`. | F12.1 |
| 19/11/2026 | F12.1 | Rama `fase/F12`; reglas `PinHasher` (PBKDF2-HMAC-SHA256 con sal 16 bytes e `isEqual` constante) y `PinLockPolicy` (timeouts 0/1/5/15 min y cooldown progresivo 30 s tras 5 fallos) + `PinHasherTest` (2/2 OK) y `PinLockPolicyTest` (2/2 OK), almacenamiento local en `DevicePrefs`, `AppLockController` enlazado a `MetanoiaApp`/`MainActivity`, `LockScreenDialogFragment` (`FLAG_SECURE` + `BiometricPrompt`), `PinSetupDialogFragment` y `SecuritySettingsFragment` en `SettingsFragment`/`nav_main.xml`; `./gradlew.bat :domain:test :app:testDebugUnitTest assembleDebug` OK. | F12.2 |
| 19/11/2026 | F12.2 | Descriptores TalkBack completos en los 7 `ExtendedFloatingActionButton`, `AppointmentCardView` (`cd_appointment_card_format` / `pending_format`), marcado `importantForAccessibility="no"` en íconos y divisores decorativos (`view_appointment_card`, `view_list_row`, `view_alert_banner`, `view_status_chip`), `maxLines="2"` en cliente de tarjeta de turno para escala 130 %, y `AccessibilityAuditTest` (2/2 OK: contraste WCAG AA >= 4.5:1 en Claro/Oscuro, cobertura TalkBack y cero alturas fijas en textos/botones de todos los layouts); `./gradlew.bat :domain:test :app:testDebugUnitTest assembleDebug` OK. | F12.3 |
| 19/11/2026 | F12.3 | `MetanoiaApp` mueve la programación de backups, recordatorios y vencimiento de lista de espera a `executors().io()` (`runStartupBackgroundTasks`), `StatusChip` cachea paleta/dimensiones y reutiliza un único `GradientDrawable` con retorno temprano, `AppointmentCardView` cachea los `Drawable` laterales por estado en `EnumMap`, `AgendaFragment` configura `setItemViewCacheSize(12)` y filtra renders de observadores inactivos; `AgendaRenderPerformanceTest` (2/2 OK: 520 filas día en 7 ms y 3.500 turnos semana en 9 ms) + `AgendaStartupPerformanceTest` (50.000 turnos, `< 500 ms` compilado en `androidTest`); `./gradlew.bat :domain:test :app:testDebugUnitTest assembleDebug compileDebugAndroidTestJavaWithJavac` OK. | F12.4 |
| 19/11/2026 | F12.4 | R8 activo en release (`minifyEnabled true` y `shrinkResources true` en `app/build.gradle`), reglas en `app/proguard-rules.pro` (Room, Fragments/ViewModels, Custom Views, Workers y `-assumenosideeffects` para `android.util.Log`) y `res/raw/keep.xml` para logo reemplazable; `ManifestPrivacyTest` actualizado a `merged_manifests/debug` y `release` (3/3 OK) y `SecurityAuditTest` (2/2 OK: cero logs sensibles o clientes HTTP en fuentes principales, `.gitignore` y manifiesto seguro); `./gradlew.bat :domain:test :app:testDebugUnitTest assembleDebug assembleRelease` OK. | F12.5 |
| 19/11/2026 | F12.5 | `versionCode 12`, `versionName "1.0.0"` y `signingConfigs.release` leyendo `keystore.properties` ignorado por Git en `app/build.gradle`; keystore PKCS12 generada y respaldada fuera del repo en `MetanoiaReleaseKeys` y `MetanoiaReleaseKeys_Backup`; APK `app/build/outputs/apk/release/app-release.apk` (4.123.389 bytes) compilado con R8 y verificado con `apksigner` v2 (`CN=Metanoia Hair Salon`, RSA 2048, SHA-256 `55ce4dcf...`); `git status` libre de secretos y nueva guía `docs/instalacion.md`. | F12.6 |
| 19/11/2026 | F12.6 | Creado `docs/manual.md` breve (<= 2 páginas imprimibles) cubriendo puesta en marcha inicial, operativa diaria (agenda, clientes, ficha de color, caja/cierre, WhatsApp, lista de espera, stock, comisiones y reportes CSV/PDF), bloqueo por PIN/biometría con procedimiento ante olvido y flujo de copia de seguridad / restauración JSON. | F12.7 |
| 19/11/2026 | F12.7 | Cierre F12 y lanzamiento `v1.0.0`: todos los `skills.md` al 100 % (0 ítems pendientes tras marcar `configuracion/skills.md`), entrada `1.0.0` en `docs/changelog.md`; `./gradlew.bat check assembleDebug assembleRelease compileDebugAndroidTestJavaWithJavac` OK (`:domain:test` 155/155, `:app:testDebugUnitTest` 38/38, `:app:testReleaseUnitTest` 38/38, total 193 tests JVM únicos con 0 fallos, `:app:lintDebug` y `:app:lint` OK); checks §12 OK (`0` `.kt`/`.kts`, `0` `TODO`/`FIXME`, `0` `Log.*`, `allowBackup="false"` y `internetCount=0` en manifiestos `debug` y `release`), `androidTest` compilados (sin ejecución en dispositivo/emulador); `git status` sin secretos; merge `--no-ff fase/F12` en `main` con tags `fase-F12` y `v1.0.0`. | Proyecto v1.0.0 completado |
| 19/11/2026 | — | Documentación Paso 0 para F13 "Clientes desde contactos": habilitado `READ_CONTACTS` en solo lectura en `docs/agents.md` §6 (manteniendo prohibido `INTERNET`), agregada especificación y criterios de importación/sincronización en `docs/modules/clientes/skills.md` y planificados 7 pasos (`F13.1`–`F13.7`) en `docs/plan.md` (total `86/93`, 92 %). | F13.1 |
| 19/11/2026 | F13.1 | Rama `fase/F13`; `Client` ampliado (`contactLookupKey`, `origin`, `createdFromContactAt`) y `SaveClientUseCase` preservando metadatos de origen; modelos `ClientOrigin`, `ContactPhone`, `DeviceContactCandidate`, `ContactBirthday`, `ContactImportPreview` y `ContactSyncOutcome`; reglas `ContactPhoneSelector`, `ContactBirthdayParser` (con año y `--MM-DD`), `ContactClientNameResolver` (2–80 chars / teléfono) y `ClientContactSyncPlanner` (deduplicación multi-cuenta, no sobrescritura ni reactivación de inactivos, idempotencia); casos de uso `PreviewContactImportUseCase` y `SyncContactsUseCase` (`AuditLog` sin PII); `:domain:test` 165/165 OK (`PhoneNormalizerTest`, `ContactPhoneSelectorTest`, `ContactBirthdayParserTest`, `ContactSyncUseCaseTest`) y `assembleDebug` OK. | F13.2 |
| 19/11/2026 | F13.2 | Room `v14`: `ClientEntity` con columnas `contact_lookup_key` (indexada), `origin` (`DEFAULT 'MANUAL'`) y `created_from_contact_at`, `Migration_13_14`, `AppDatabase` v14, esquema `14.json` exportado, `ClientRowProjection`/`ClientMapper`/`ClientDao` (`observeSummariesFiltered`, `observeMissingBirthdayCount`, `observeActiveCount`, `upsert` seguro), `ClientRepositoryImpl` transaccional, `BackupManager` v14, `SchemaExportTest` (v14 OK), `MigrationTest` (`13→14` y `1→14`) y `BackupRestoreRoundTripTest` compilados en `androidTest`; `:domain:test` 165/165 y `:app:testDebugUnitTest` 39/39 OK. | F13.3 |
| 19/11/2026 | F13.3 | Declarado `READ_CONTACTS` solo lectura en `AndroidManifest.xml` (sin `WRITE_CONTACTS`, manteniendo `INTERNET` removido y `allowBackup="false"`); implementado `DeviceContactsReader` en `app/data/contacts/` leyendo cuentas (`Contacts`, `Phone`, evento `BIRTHDAY`) sin modificar contactos ni loguear PII y cableado en `AppContainer`; `ManifestPrivacyTest` (3/3 OK) y `SecurityAuditTest` (2/2 OK), `:app:testDebugUnitTest` 39/39 OK y `assembleDebug` OK. | F13.4 |
| 19/11/2026 | F13.4 | Flujo de importación masiva inicial: `DevicePrefs` ampliado con banderas de onboarding/sync; `ContactsImportViewModel`, `ContactsImportDialogFragment` y `dialog_contacts_import.xml` (explicación previa a `READ_CONTACTS`, vista previa con 3 contadores `a importar`/`ya cargados`/`omitidos`, botón `Importar todos` transaccional en `AppExecutors.io()` con progreso y activación automática por defecto); botón en `ClientsFragment` + estado vacío y asistente único `InitialSetupDialogFragment` en `AgendaFragment`/`AgendaViewModel`; `:domain:test` 165/165, `:app:testDebugUnitTest` 39/39 (`AccessibilityAuditTest` y `SecurityAuditTest` OK) y `assembleDebug` OK. | F13.5 |
| 19/11/2026 | F13.5 | Filtros en Clientes (`fragment_clients.xml`, `ClientsViewModel`, `ClientsFragment`, `ClientsAdapter` con `Objects.equals`, `ClientFormDialogFragment.newEditFocusBirthdayInstance`): chip `Sin cumpleaños (N)` con contador reactivo y apertura directa del editor enfocado en cumpleaños en un toque; chip `Importados recientemente` (últimos 7 días, `origin = CONTACT_SYNC`) para revisión y baja lógica desde detalle; `:domain:test` 165/165, `:app:testDebugUnitTest` 39/39 y `assembleDebug` OK. | F13.6 |
