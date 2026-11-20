# instalacion.md — Instalación, firma y mantenimiento (v1.1.1)

Guía rápida para instalar el APK release de **Metanoia Hair Salon** (`v1.1.1`, `versionCode 14`) y mantener futuras compilaciones firmadas.

---

## 1. Requisitos del dispositivo

- **Sistema operativo:** Android 8.0 Oreo (API 26) o superior (`targetSdk 35`).
- **Conectividad:** No requiere internet ni cuenta en la nube. Toda la base de datos e historial quedan guardados localmente en el teléfono.
- **Permisos opcionales:**
  - *Notificaciones* (`POST_NOTIFICATIONS`, Android 13+): solo si activás el resumen diario de recordatorios.
  - *Biometría / huella* (`USE_BIOMETRIC`): solo si activás el bloqueo local por PIN y desbloqueo biométrico.
  - *WhatsApp*: se abre mediante enlaces locales cuando tocás enviar recordatorio o mensaje.

---

## 2. Instalar `app-release.apk` en Android

1. Generá o ubicá el archivo firmado en:
   `app/build/outputs/apk/release/app-release.apk` (`versionName "1.0.0"`, `versionCode 12`, optimizado con R8).
2. Transferí el APK al teléfono por cable USB o almacenamiento local.
3. Tocá el archivo desde el gestor de archivos del teléfono.
   - Si Android solicita permiso para instalar desde esa fuente, habilitá temporalmente **"Instalar aplicaciones desconocidas"** para el gestor de archivos y confirmá.
4. Al finalizar, abrí **Metanoia Hair Salon**.

> **Actualización desde una compilación de desarrollo (debug):** Android no permite instalar un APK *release* sobre un APK firmado con clave *debug* manteniendo los datos. Si venís de una versión *debug*, primero creá una copia de seguridad en *Más → Copia de seguridad → Exportar backup* dentro de la app, desinstalá la versión *debug*, instalá `app-release.apk` y restaurá el `.json` desde *Más → Copia de seguridad → Importar*.

---

## 3. Ubicación y resguardo de la keystore

La clave de firma **nunca se guarda dentro del repositorio Git** (`.gitignore` excluye `*.jks`, `*.keystore`, `keystore.properties` y `*.apk`).

| Copia | Ruta local fuera del repositorio | Contenido |
| :--- | :--- | :--- |
| **Principal** | `C:\Users\marco\Documents\MetanoiaReleaseKeys\metanoia-release.jks` | Keystore PKCS12 (`alias: metanoia-release`, RSA 2048) + `RESGUARDO.txt` |
| **Respaldo** | `C:\Users\marco\Documents\MetanoiaReleaseKeys_Backup\metanoia-release.jks` | Copia idéntica de `metanoia-release.jks` + `RESGUARDO.txt` |

**Reglas obligatorias de resguardo:**
1. Conservá al menos dos copias fuera del equipo principal (por ejemplo, disco externo cifrado / pendrive seguro / gestor de contraseñas), ya que **perder la keystore o sus contraseñas impide publicar actualizaciones instalables sobre la versión `1.0.0` ya instalada** (obligaría a desinstalar e importar backup manualmente).
2. Jamás subas archivos `.jks`, `.keystore`, `RESGUARDO.txt` ni `keystore.properties` a Git ni a chats sin cifrar.
3. Certificado vigente: `CN=Metanoia Hair Salon, OU=Mobile, O=Metanoia, L=Buenos Aires, ST=Buenos Aires, C=AR` (SHA-256 `55ce4dcf75b17574b01fbced6b0d08c3cbd7a33cf92d555353bc0c4da3c87603`).

---

## 4. Cómo compilar y firmar futuras versiones

1. Aumentá `versionCode` (+1 entero) y `versionName` en `app/build.gradle`.
2. Asegurate de tener `keystore.properties` en la raíz del proyecto (archivo ignorado por Git), guardado en UTF-8 **sin BOM** (en Windows PowerShell usar barras `/` en la ruta para evitar escapes):

```properties
storeFile=C:/Users/marco/Documents/MetanoiaReleaseKeys/metanoia-release.jks
storePassword=TU_PASSWORD_DEL_RESGUARDO
keyAlias=metanoia-release
keyPassword=TU_PASSWORD_DEL_RESGUARDO
```

3. Compilá y verificá pruebas, lint y firma:

```powershell
./gradlew.bat check assembleRelease
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\apksigner.bat" verify --verbose --print-certs app\build\outputs\apk\release\app-release.apk
git status
```

4. Confirmá que `apksigner` imprima `Verifies` con `Verified using v2 scheme: true` y que `git status` no muestre ni `keystore.properties` ni ningún `.jks`/`.apk`.
