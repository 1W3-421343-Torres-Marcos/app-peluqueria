# changelog.md — Historial de versiones

> Una entrada por fase cerrada, escrita en el paso de verificación de fase (ver `agents.md` §11). Formato: versión · fecha (dd/MM/yyyy) · fase · qué se agregó. La más reciente arriba.

## [0.1.0] — 17/11/2026 — F1 Base, base de datos e identidad visual
- Agregado: proyecto Gradle multimódulo (`:domain` Java 17 y `:app` Android SDK 26–35 con ViewBinding), wrapper 8.13, `.gitignore` y esqueleto de arquitectura MVVM (`Result`, `DomainError`, `Clock`, `IdGenerator`, `AppContainer`, `AppExecutors`).
- Agregado: manifiesto de privacidad sin permiso `INTERNET` (`allowBackup="false"`, reglas de extracción).
- Agregado: `Money` en centavos y utilidades `Formats` es-AR (`$ 12.345,50`, `dd/MM/yyyy`, `HH:mm` 24 hs).
- Agregado: base de datos Room v1 (`AppDatabase`, `AuditLogEntity`, `AuditLogDao`) con esquema exportado `1.json` y `MigrationTest`.
- Agregado: fuentes estáticas Montserrat e Inter (SIL OFL), paleta monocromática con acento bordó en modo claro/oscuro, `LogoView` reemplazable, splash (`core-splashscreen`), navegación inferior de 4 destinos (Agenda, Clientes, Caja, Más) y componentes base (`StatusChip`, `MoneyTextView`, `EmptyStateView`, `AlertBanner`, `ListRowView`) con catálogo debug.
