# agents.md — Reglas globales del proyecto Metanoia

> Documento normativo. Todo agente (humano o IA) que trabaje en este repositorio DEBE leerlo y cumplirlo. Ante conflicto entre documentos, prevalece este archivo; luego `docs/design.md`, luego cada `skills.md`.

---

## 1. Protocolo de trabajo (obligatorio)

1. **Antes de empezar:** leer `docs/agents.md`, `docs/plan.md` y el `skills.md` del módulo involucrado (`docs/modules/<modulo>/skills.md`). Para UI, leer también `docs/design.md`.
2. **Un paso por vez:** tomar el primer paso pendiente `[ ]` de la fase en curso. Marcarlo `[~]` y marcar la fase `[~]` si no lo estaba. Si es el primer paso de la fase, crear la rama `fase/F<n>` desde `main` (§11).
3. **Implementar** solo lo que pide el paso. No adelantar trabajo de otros pasos o fases.
4. **Verificar:** el paso cumple su criterio de "hecho", `./gradlew test` pasa y el proyecto compila (`./gradlew assembleDebug`).
5. **ACTUALIZAR `docs/plan.md` ANTES DE CONTINUAR (regla estricta):**
   - Marcar el checkbox del paso como `[x]`.
   - Recalcular la tabla de resumen (pasos hechos / total y %). Fase completa = `[x]`.
   - Agregar una fila en "Registro de avance": fecha (dd/MM/yyyy), paso completado, nota breve, paso siguiente.
   - Si no se actualiza `plan.md`, el paso **no** se considera terminado.
6. Al cerrar un módulo, marcar los criterios de aceptación de su `skills.md`.
7. Si surge trabajo no previsto, agregarlo como nuevo paso en `plan.md` (ej. `F3.12`) en lugar de hacerlo "de paso".
8. Si se cambia una decisión de arquitectura o stack, actualizar este archivo en el mismo commit y dejarlo anotado en el registro de avance.
9. Nunca marcar `[x]` con tests fallando, código comentado "para después" o TODOs sin paso asociado en `plan.md`.
10. Commits: Conventional Commits con ID de paso, un commit por paso (incluye `plan.md`). Ej.: `feat(agenda): F3.2 agrega validador de solapamientos`. Ramas, tags y versionado en §11.

---

## 2. Stack tecnológico

| Área | Elección |
|---|---|
| Lenguaje | **Java 17** (100 %). Prohibido código Kotlin (`.kt`, `.kts` de app, plugin `kotlin-android`, kapt, KSP). |
| Build | Gradle con **Groovy DSL** (`build.gradle`) + version catalog `gradle/libs.versions.toml`. |
| SDK | `minSdk 26` (Android 8.0, `java.time` nativo) · `targetSdk/compileSdk 35`. |
| UI | **Vistas XML + ViewBinding**, Material Components (Material 3), Navigation Component (Fragments), single-Activity. |
| Arquitectura | MVVM en capas `ui / domain / data`. ViewModel + LiveData. |
| Persistencia | **Room** (SQLite) con `annotationProcessor`, `exportSchema = true`, migraciones versionadas. |
| Segundo plano | `ExecutorService` (vía `AppExecutors`) y **WorkManager**: periódico (backup AUTO, recordatorios) y `OneTimeWorkRequest` con nombre único + `ExistingWorkPolicy.KEEP` (backups CASH_CLOSE y ON_BACKGROUND). |
| Listas | `ListAdapter` + `DiffUtil` (ver §10). |
| Otros AndroidX | `core-splashscreen`, `biometric`, `documentfile` (SAF), `FileProvider`, `lifecycle-process` (`ProcessLifecycleOwner`). |
| PDF / CSV | `android.graphics.pdf.PdfDocument` (nativo) y escritura CSV propia. Sin librerías externas. |
| Tests | JUnit 4 (domain, JVM puro), AndroidX Test + Room testing (`MigrationTestHelper`), Espresso para flujos críticos. |
| DI | Manual: `AppContainer` creado en `MetanoiaApp` (sin Hilt/Dagger, para simplicidad). |

**Prohibido:** Kotlin Coroutines/Flow, RxJava, Guava, Paging 3, Retrofit/OkHttp/Volley, Firebase, analytics, crash reporters remotos, ads, cualquier librería que requiera red. (El `kotlin-stdlib` que AndroidX trae transitivamente se tolera; no se escribe código Kotlin.)

### 2.1 Justificación: XML + ViewBinding (no Compose)
- Jetpack Compose depende del **plugin de compilador de Kotlin**: las funciones `@Composable` no pueden escribirse en Java. Compose "con Java" no es viable.
- XML + ViewBinding es 100 % Java, estable, maduro, con tooling completo (preview de layouts, temas DayNight) y tipado seguro sin `findViewById`.

---

## 3. Arquitectura

### 3.1 Módulos Gradle
```
:domain   → biblioteca Java pura (java-library). Sin imports android.*/androidx.*
:app      → Android: capa data (Room, SAF, WorkManager) + capa ui (Fragments, ViewModels)
```
`:domain` es portable a otras plataformas y se testea en JVM sin emulador.

### 3.2 Paquetes
```
domain/src/main/java/com/metanoia/domain/
  model/        entidades de negocio inmutables (Appointment, Client, Money…)
  rules/        validadores y cálculos puros (AvailabilityChecker, CommissionCalculator…)
  usecase/      casos de uso (CreateAppointmentUseCase…), un caso por clase
  repository/   interfaces de repositorio (síncronas)
  common/       Result<T>, DomainError, Clock, IdGenerator

app/src/main/java/com/metanoia/app/
  MetanoiaApp.java, AppContainer.java, AppExecutors.java
  data/db/        AppDatabase, entity/, dao/, migration/, converter/
  data/repository/ implementaciones de domain.repository
  data/mapper/     Entity <-> modelo de dominio
  data/backup/     BackupManager, BackupWorker, RestoreManager
  data/prefs/      DevicePrefs (solo datos del dispositivo)
  ui/<modulo>/     Fragment, ViewModel, Adapter, por módulo
  ui/common/       componentes reutilizables (ver design.md)
  util/            formatos es-AR, intents (WhatsApp, compartir)
```

### 3.3 Reglas de dependencia
- `ui → domain` (casos de uso). `data → domain` (implementa interfaces). `domain → nada`.
- **Lecturas reactivas:** como `domain` no puede usar LiveData, los ViewModels pueden leer listas observables desde DAOs/repositorios de `data` (solo lectura).
- **Toda escritura y toda regla de negocio pasa por un caso de uso de `domain`.** Prohibido validar reglas en Fragments, ViewModels o DAOs.
- Fragments solo renderizan estado y reenvían eventos al ViewModel. Sin lógica.
- Errores: los casos de uso devuelven `Result<T>` con `DomainError` (código enum); el ViewModel lo mapea a un `@StringRes`.
- Hilos: nunca acceso a BD en el hilo principal. Escrituras en `AppExecutors.io()`, resultados vía `LiveData.postValue`. Eventos de un solo uso con `SingleLiveEvent`/wrapper `Event<T>`.

---

## 4. Convenciones de código

- **Identificadores en inglés**, textos de UI en español (es-AR) en `res/values/strings.xml`. Nunca texto hardcodeado en Java o XML.
- Glosario obligatorio:

| Negocio | Código | Negocio | Código |
|---|---|---|---|
| Turno | `Appointment` | Ficha de color | `ColorFormula` |
| Ítem de turno | `AppointmentItem` | Recordatorio | `Reminder` |
| Cliente | `Client` | Lista de espera | `WaitlistEntry` |
| Servicio | `SalonService` | Movimiento de caja | `CashMovement` |
| Profesional | `Professional` | Cierre de caja | `CashClosing` |
| Horario laboral | `WorkSchedule` | Comisión | `Commission` |
| Bloqueo de agenda | `ScheduleBlock` | Producto | `Product` |
| Serie recurrente | `RecurrenceRule` | Movimiento de stock | `StockMovement` |
| Auditoría | `AuditLog` | Configuración | `Settings` |

- Estados de turno (`AppointmentStatus`, únicos válidos): Pendiente `PENDING` · Confirmado `CONFIRMED` · Completado (servicio realizado, sin cobrar del todo) `COMPLETED` · Pagado (total cubierto) `PAID` · Cancelado `CANCELLED` · Ausente `NO_SHOW`. Reglas en `agenda/skills.md`.

- Sufijos: `*Fragment`, `*ViewModel`, `*Adapter`, `*UseCase`, `*Repository` (interfaz), `*RepositoryImpl`, `*Dao`, `*Entity`, `*Mapper`, `*Worker`.
- Estilo: Google Java Style con indentación de 4 espacios. Una clase pública por archivo. Archivos < 400 líneas; métodos < 40 líneas.
- Modelos de dominio inmutables (`final`, sin setters; builder si > 4 campos). Sin `null` en retornos de dominio: usar `Optional`.
- Colores, dimensiones y estilos siempre desde el tema/recursos (`?attr/`, `@dimen/`, `@style/`). Prohibido hex o dp sueltos en layouts.
- Javadoc breve en clases públicas de `domain`. Sin comentarios obvios.

---

## 5. Datos y persistencia

- **Dinero:** `long` en centavos (clase `Money` en domain). Nunca `float`/`double`. Formato: `$ 12.345,50`.
- **Fechas:** día como `epochDay` (`long`), hora como minutos desde 00:00 (`int`), instantes como epoch millis UTC. Zona `America/Argentina/Buenos_Aires`. Formato UI `dd/MM/yyyy` y `HH:mm` (24 hs). Semana inicia el lunes.
- **Baja lógica:** columnas `active` (catálogos) o `deletedAt` (registros). Nunca `DELETE` sobre datos históricos.
- **Claves foráneas:** `onDelete = RESTRICT`. Prohibido `CASCADE` en turnos, cobros, fichas, movimientos y auditoría.
- **Precio congelado:** `AppointmentItem` copia nombre, precio y duración del servicio al crearse el turno.
- **Auditoría:** toda operación de dinero o stock genera `AuditLog` (timestamp, operador, acción, entidad, id, monto, concepto). Movimientos de dinero inmutables: las correcciones se hacen con un movimiento de reverso, nunca editando.
- **Saldos derivados:** `paidCents` y `balanceCents` de un turno se calculan desde `CashMovement` (pagos − reversos); nunca se editan a mano. El paso COMPLETED ↔ PAID es automático.
- **Operador:** cada registro auditable guarda el operador actual (por defecto "Dueño"; ver módulo configuración).
- **Migraciones:** cada cambio de esquema incrementa la versión, agrega una clase `Migration_N_M` y su test con `MigrationTestHelper`. JSON de esquema versionado en `app/schemas/`. **Prohibido `fallbackToDestructiveMigration`.**
- **Todo dato de negocio vive en Room** (incluida la configuración del negocio), para que el backup sea un único archivo completo. `SharedPreferences` (`DevicePrefs`) solo para datos propios del dispositivo: URI de carpeta de backup, tema, flag de PIN.
- Índices obligatorios en columnas de búsqueda y filtrado (`client.nameNormalized`, `client.phone`, `appointment(professionalId, day)`, `appointment(clientId)`, etc.).
- Búsqueda de texto sobre columnas normalizadas (minúsculas, sin acentos).

---

## 6. Privacidad y seguridad

- El manifiesto **NO** declara `android.permission.INTERNET`. Verificar el manifiesto fusionado en cada fase.
- **Contactos (`android.permission.READ_CONTACTS`):** permitido desde F13 **únicamente en modo solo lectura** para importar y sincronizar clientes desde la agenda local. Se leen exclusivamente nombre para mostrar, teléfonos y evento de cumpleaños (`BIRTHDAY`) si existe; **nunca** se modifican ni eliminan contactos, los datos nunca salen del teléfono, se muestra una pantalla explicativa antes de solicitar el permiso y, si el permiso se revoca, la sincronización se pausa mostrando un aviso en Configuración sin provocar fallos.
- `android:allowBackup="false"` y `dataExtractionRules` que excluyan todo: los datos no salen por el backup automático de Google.
- Los datos solo salen del dispositivo por acción explícita del dueño: backup en carpeta elegida, exportación, compartir, WhatsApp.
- Prohibido loguear datos personales (nombres, teléfonos, notas, montos) en `Log`. Release con R8/minify activado.
- Compartir archivos solo vía `FileProvider` con URIs temporales.
- **Backup:** nunca depender solo de WorkManager periódico. Disparadores `AUTO`, `MANUAL`, `PRE_RESTORE`, `CASH_CLOSE` (al cerrar caja) y `ON_BACKGROUND` (app a segundo plano si el último backup exitoso supera X horas, default 6). Retención: el más reciente de cada uno de los últimos N días con backup (default 7) + últimos 3 `PRE_RESTORE`. Detalle en `backup/skills.md`.
- PIN/huella opcional (fase F12).

---

## 7. Identidad visual y logo

- Toda la UI sigue `docs/design.md` (paleta, tipografías, componentes).
- `assets/branding/metanoia.png` es una **foto de referencia** (vidriera con reflejos): usar SOLO para inferir forma y estilo. **Prohibido incrustarla en la app, PDFs o mensajes.**
- Logo reemplazable: fuente maestra `assets/branding/logo.svg` → recurso Android `app/src/main/res/drawable/logo_metanoia.xml` (VectorDrawable). Hasta tener la versión limpia, se usa el **placeholder tipográfico** (`LogoView`: "METANOIA" en Montserrat Bold + "Hair Salon" fino). Reemplazar el logo debe requerir solo cambiar ese recurso.
- **No inventar ni dibujar otro logo.**
- Fuentes empaquetadas en `app/src/main/res/font/` (sin descargas). Iconos como VectorDrawables locales.

---

## 8. Localización

- Idioma único: español (Argentina), `values/strings.xml`. Usar `plurals` y placeholders (`%1$s`).
- `Locale.forLanguageTag("es-AR")` para todo formateo numérico/fecha, centralizado en `util/Formats`.
- Teléfonos se guardan normalizados en formato E.164 sin `+` para wa.me (ej. `5491122334455`).

---

## 9. Testing

- **Obligatorio (JUnit, módulo `:domain`):** solapamiento de turnos, disponibilidad (horario, bloqueos, días libres), transiciones de estado (incluido COMPLETED ↔ PAID automático), recurrencias, cálculo de comisiones, cierre de caja, stock bajo, render de plantillas, validación y política de retención de backup.
- Instrumentados obligatorios: "Completar y cobrar" atómico (rollback si falla una parte).
- Room: tests instrumentados para DAOs con consultas complejas y para cada migración.
- Nombres: `metodo_condicion_resultadoEsperado`. Usar `Clock` inyectable (nunca `System.currentTimeMillis()` directo en domain).
- Antes de marcar un paso `[x]`: `./gradlew test` en verde y `assembleDebug` OK.

---

## 10. Rendimiento y UX

- Objetivo: fluidez con 5.000 clientes y 50.000 turnos; arranque en frío < 2 s en gama media.
- Consultas acotadas por rango de fechas; nunca cargar tablas completas en memoria.
- Nada de consultas en `onBindViewHolder`. Usar `ListAdapter`/`DiffUtil`.
- **Listas grandes sin Paging:** consultas Room con proyección liviana (POJO con solo las columnas que muestra la fila, no la entidad completa), filtro/búsqueda en SQL y resultado en `LiveData<List<...>>`. Si en el futuro una lista lo requiere, paginar manualmente con `LIMIT/OFFSET` y botón "Cargar más".
- Tareas frecuentes (crear turno, cobrar, recordar) en ≤ 4 toques desde la pantalla principal. Acciones principales en la mitad inferior de la pantalla (uso con una mano). Objetivos táctiles ≥ 48 dp.

---

## 11. Control de versiones y versionado de la app

- **Repositorio:** Git. `.gitignore` mínimo: `build/`, `.gradle/`, `.idea/`, `local.properties`, `*.apk`, `*.aab`, `*.jks`, `*.keystore`, `keystore.properties`, capturas y temporales (`captures/`, `*.log`, `*.tmp`, `.DS_Store`, `Thumbs.db`). **Nunca** se commitean keystores ni contraseñas.
- **Commits:** Conventional Commits con ID de paso (§1.10). **Un commit por paso completado**, que incluya la actualización de `plan.md`. Mensaje en español, imperativo, sin punto final. Ej.: `feat(caja): F5.4 agrega completar y cobrar atómico`.
- **Ramas:** `main` siempre estable. Al iniciar una fase se crea `fase/F<n>` desde `main`; todos los pasos de la fase se commitean ahí. En el paso de verificación de fase se fusiona a `main` (`--no-ff`).
- **Tags:** al cerrar cada fase, `fase-F<n>` (ej. `fase-F1`) sobre el merge en `main`. Al entregar un APK al dueño, además `v<versionName>` (ej. `v0.3.0`).
- **Versión:** `versionName` = MAJOR.MINOR.PATCH. En desarrollo `0.<fase>.0` al cerrar cada fase (ej. `0.3.0` tras F3); correcciones entregadas entre fases suben PATCH (`0.3.1`). `1.0.0` al cerrar F12. `versionCode` entero que sube en 1 con cada APK instalable entregado. Ambos definidos **solo** en `app/build.gradle` y mostrados en Configuración > Acerca de (vía `BuildConfig`).
- **Keystore:** se genera en F12.5 y se guarda **fuera del repositorio**, con copia de resguardo en al menos dos lugares distintos. Ruta y contraseñas se leen de `keystore.properties` (ignorado por Git). Procedimiento en `docs/instalacion.md`. Perder la keystore impide actualizar la app instalada.
- **Changelog:** `docs/changelog.md`, una entrada breve por fase cerrada (versión, fecha, qué se agregó), escrita en el paso de verificación de fase.

---

## 12. Definición global de "hecho"

Un paso está hecho cuando: cumple su criterio en `plan.md` · compila · tests en verde · sin texto/colores hardcodeados · sin permiso INTERNET · respeta capas · `plan.md` actualizado (checkbox, tabla, registro) · commit del paso realizado en la rama de fase.

## 13. Fuera de alcance

Backend, sincronización en nube propia, reservas online, pagos online, multi-sucursal, iOS.
