# manual.md — Manual de usuario rápido (Metanoia Hair Salon)

Guía breve para el equipo del salón. La aplicación funciona **100 % sin internet**: todos los turnos, clientes, caja, fórmulas y stock viven en tu teléfono.

---

## 1. Primeros pasos (única vez)

Abrí la pestaña **Más → Configuración** para dejar listo el salón:
1. **Datos del salón y horario general:** nombre del negocio (se usa en WhatsApp y reportes PDF), teléfono de contacto, días y horario de apertura/cierre.
2. **Servicios y catálogo:** cargá servicios con precio, duración estimada y si llevan ficha de color (*Más → Servicios*).
3. **Profesionales:** agregá estilistas, su porcentaje de comisión y horario habitual (*Más → Profesionales*).
4. **Tema y operador:** podés elegir tema Claro/Oscuro/Sistema y cambiar el nombre del operador actual para la auditoría de movimientos.

---

## 2. Tareas frecuentes del día a día

- **Agenda (Día / Semana):** tocá **+** para agendar un turno rápido. Filtrá arriba por profesional, cambiá entre **Día** y **Semana**, o marcá bloqueos por vacaciones/capacitación desde *Más → Bloqueos de agenda*.
  - Tocá un turno para ver su detalle, editarlo, cambiar estado (**Confirmar**, **Completar**, **Cancelar**, **Ausente**), reprogramar o **Cobrar** en un toque.
- **Clientes y ficha de color:** desde **Clientes** podés buscar por nombre o teléfono, ver historial de visitas, saldo pendiente y la pestaña **Color**. Allí registrás fórmulas por gramos/ml/partes. Al crear un nuevo turno de coloración para ese cliente, podés **repetir la última fórmula** con un toque como borrador editable.
- **Caja y cierre diario:** en **Caja** ves ingresos y egresos del día y turnos pendientes de cobro.
  - Al cobrar elegís **Efectivo**, **Transferencia**, **Tarjeta** o pago **Mixto**.
  - Si te equivocás en un monto, usá **Revertir movimiento** indicando el motivo (los registros nunca se borran).
  - Al finalizar la jornada tocá **Cierre de caja**: se guarda el arqueo del día y se dispara una copia de seguridad local automática.
- **Recordatorios WhatsApp y lista de espera:**
  - En *Más → Recordatorios* ves los turnos de **Mañana** y los **Cumpleaños** del día: enviás el saludo con 1 toque (abre WhatsApp con el mensaje listo) y marcás enviado para no duplicar. También podés programar un resumen diario local con notificación.
  - Si no hay lugar libre para un cliente, anotalo en **Lista de espera**; al cancelar o mover un turno, la app sugiere automáticamente candidatos compatibles.
- **Stock, comisiones y reportes:**
  - En *Más → Stock* controlás insumos y productos de reventa, alertas de stock bajo y ventas directas vinculadas con Caja.
  - En *Más → Comisiones* liquidás las comisiones sobre turnos pagados por período en una operación atómica.
  - En *Más → Reportes* consultás ingresos, servicios top, clientes frecuentes, ocupación, desempeño y cuentas por cobrar, con exportación **CSV** y **PDF A4**.

---

## 3. Seguridad (PIN y huella)

Para proteger los datos cuando el teléfono queda sobre el mostrador:
1. Entrá a *Más → Configuración → Seguridad*.
2. Activá **Bloqueo con PIN**, elegí un PIN de **4 a 6 dígitos**, configurá el tiempo tras pasar a segundo plano (Inmediato, 1 min, 5 min o 15 min) y activá **Biometría / huella** si querés desbloquear más rápido.
3. Ante 5 intentos fallidos, la pantalla aplica una pausa de seguridad progresiva desde 30 segundos.

> **¿Olvidaste tu PIN?** Por seguridad no existe recuperación ni puerta trasera sin borrar datos. El procedimiento es desinstalar la app, volver a instalarla y restaurar tu última copia de seguridad JSON.

---

## 4. Copias de seguridad y restauración

Entrá a *Más → Copia de seguridad* para resguardar tus datos ante pérdida, robo o cambio de teléfono:

- **Backup automático:** la app guarda copias locales periódicas en su almacenamiento privado y genera un respaldo al realizar el cierre de caja.
- **Exportar copia manual (recomendado semanalmente):**
  1. Tocá **Exportar backup**.
  2. Guardá el archivo `.json` en otra carpeta del teléfono, Google Drive personal, correo o pendrive.
- **Restaurar una copia de seguridad:**
  1. Entrá a *Más → Copia de seguridad* y tocá **Importar / Restaurar backup**.
  2. Seleccioná el archivo `.json`. La aplicación valida la estructura y esquema antes de tocar nada.
  3. Confirmá la restauración: se guarda un respaldo preventivo automático del estado actual, se restauran todos los registros y la app reinicia sus pantallas con los datos recuperados.
