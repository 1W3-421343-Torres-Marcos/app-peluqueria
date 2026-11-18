# skills.md — Configuración

**Fase:** F2 (base) · plantillas en F6 · seguridad en F12 · **Depende de:** —

## Propósito
Parámetros del negocio y preferencias que gobiernan al resto de los módulos.

## Modelo de datos
- `Settings` (Room, fila única o clave-valor tipado; viaja en el backup):
  - Negocio: businessName ("Metanoia"), subtitle ("Hair Salon"), address?, phone?, cuit?.
  - Horario general por día de semana (tramos, igual estructura que `WorkSchedule`).
  - `slotMinutes`: duración mínima de bloque (10, 15, 20, 30; default 15).
  - Plantillas de mensajes (ver recordatorios).
  - Operadores: lista simple de nombres (default "Dueño") y operador actual (para auditoría).
  - Días de vencimiento de lista de espera (default 30).
- `DevicePrefs` (SharedPreferences, no viaja en backup): tema (Sistema/Claro/Oscuro), PIN habilitado, hash+sal del PIN, biometría habilitada, tiempo de bloqueo, parámetros de backup (URI de carpeta, frecuencia AUTO, N días a conservar, horas para ON_BACKGROUND, días de aviso; ver `backup/skills.md`).

## Reglas de negocio
1. Cambiar `slotMinutes` no altera turnos existentes; aplica a nuevos.
2. Reducir el horario general advierte si hay turnos futuros fuera del nuevo horario (los lista, no los cancela).
3. Los horarios de profesionales deben quedar dentro del horario general.
4. **Seguridad (F12):** PIN de 4–6 dígitos guardado como hash (PBKDF2 + sal), nunca en texto plano; huella vía `BiometricPrompt` como alternativa al PIN; bloqueo al abrir y tras X minutos en segundo plano. 5 intentos fallidos → espera de 30 s incremental. Desactivar PIN requiere ingresarlo.
5. Olvido de PIN: no hay recuperación remota; se documenta que la única vía es reinstalar y restaurar backup.
6. Todo cambio de configuración relevante (horarios, operadores) se registra en `AuditLog`.

## Pantallas y flujos
- **Configuración (en Más):** Negocio · Horario de atención · Agenda (bloque mínimo) · Plantillas · Operadores · Apariencia (tema) · Seguridad · Backup (enlace al módulo) · Acerca de.
- **Acerca de:** nombre de la app, `versionName` y `versionCode` leídos de `BuildConfig` (definidos solo en `app/build.gradle`).

## Tests obligatorios
Validación de horario general, render de plantillas por defecto, hash/verificación de PIN.

## Criterios de aceptación
- [x] Datos del negocio editables y usados en PDF/WhatsApp.
- [x] Horario general y bloque mínimo aplicados en la agenda.
- [x] Plantillas de mensajes editables.
- [x] Operador actual registrado en auditoría.
- [x] Tema Sistema/Claro/Oscuro.
- [x] Acerca de muestra `versionName` y `versionCode` actuales.
- [ ] PIN/huella opcional funcionando y PIN almacenado como hash.
