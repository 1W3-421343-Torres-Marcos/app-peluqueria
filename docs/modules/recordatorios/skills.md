# skills.md — Recordatorios por WhatsApp

**Fase:** F6 · **Depende de:** agenda, clientes, configuración (plantillas).

## Propósito
Enviar recordatorios de turno y saludos de cumpleaños por WhatsApp en pocos toques, sin API oficial ni red propia.

## Modelo de datos
- Plantillas en `Settings`: `reminderTemplate`, `birthdayTemplate`, `waitlistOfferTemplate` (F8).
  - Default recordatorio: `Hola {nombre}, te recuerdo tu turno el {fecha} a las {hora} en {negocio}. ¡Te esperamos!`
  - Default cumpleaños: `¡Feliz cumpleaños, {nombre}! Te saludamos desde {negocio}.`
- `Reminder`: id, type (APPOINTMENT, BIRTHDAY, WAITLIST_OFFER), appointmentId?, clientId, year? (cumpleaños), sentAt, sentBy.

## Reglas de negocio
1. Variables soportadas: `{nombre}` (primer nombre), `{fecha}` (dd/MM/yyyy), `{dia}` (ej. "martes"), `{hora}` (HH:mm), `{servicios}`, `{profesional}`, `{negocio}` ("Metanoia Hair Salon"). Variables desconocidas se dejan literales y el editor las advierte.
2. Se agrega la firma "— Metanoia Hair Salon" si la plantilla no incluye `{negocio}`.
3. Enlace: `https://wa.me/<telefonoE164>?text=<URLEncoder UTF-8>`, abierto con `Intent.ACTION_VIEW`. No requiere permiso INTERNET en la app.
4. Si el cliente no tiene teléfono: acción deshabilitada con explicación.
5. Al volver de WhatsApp, se registra `Reminder` (la app no puede confirmar el envío real; se asume enviado al abrir, con opción "deshacer").
6. Pendientes de mañana: turnos PENDING/CONFIRMED del día siguiente sin `Reminder` de tipo APPOINTMENT.
7. Cumpleaños: clientes activos cuyo día/mes = hoy, sin saludo registrado en el año en curso.
8. Notificación diaria local (WorkManager) con cantidad de pendientes; requiere permiso `POST_NOTIFICATIONS` (Android 13+), solicitado con explicación.

## Pantallas y flujos
- **Recordatorios:** pestañas "Mañana" y "Cumpleaños"; cada fila con botón WhatsApp (1 toque) y estado enviado.
- **Detalle de turno:** acción "Recordar".
- **Editor de plantillas** (en configuración) con vista previa usando datos de ejemplo.

## Tests obligatorios
Render de plantillas (todas las variables, desconocidas, acentos), codificación URL, selección de pendientes de mañana y cumpleaños.

## Criterios de aceptación
- [ ] Plantillas editables con vista previa.
- [ ] Enlace wa.me correcto con mensaje armado y codificado.
- [ ] Lista de turnos de mañana pendientes de recordar.
- [ ] Saludo de cumpleaños con la misma mecánica.
- [ ] Registro de enviados evita duplicados.
- [ ] Sin API oficial ni permiso INTERNET.
