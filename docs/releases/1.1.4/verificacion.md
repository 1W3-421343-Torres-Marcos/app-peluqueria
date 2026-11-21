# 1.1.4 — verificacion

## Automatizada

- `./gradlew.bat test`: verde.
- `./gradlew.bat lintDebug`: verde.
- APK debug: generado en `app/build/outputs/apk/debug/app-debug.apk`.
- APK release: generado en `app/build/outputs/apk/release/app-release.apk`.
- Compilacion Android tests: verde.
- Verificacion `apksigner`: v2 verde.
- Certificado SHA-256: `55ce4dcf75b17574b01fbced6b0d08c3cbd7a33cf92d555353bc0c4da3c87603`.

## Prueba manual

- Pendiente en dispositivo: crear un turno superpuesto y confirmar la advertencia.
- Pendiente: reprogramar un turno sobre otro horario ocupado.
- Pendiente: generar recurrencias con una ocurrencia superpuesta.
- Pendiente: confirmar que un bloque de agenda real siga impidiendo la asignacion.
