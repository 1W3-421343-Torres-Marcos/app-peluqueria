# 1.1.2 — diagnóstico del cierre

## Evidencia

Android registró:

- hilo: `pool-2-thread-*`;
- proceso: `com.metanoia.app`;
- excepción: `java.lang.IllegalStateException`;
- mensaje: `No hay error porque el resultado fue exitoso`;
- resultado: `Force finishing activity com.metanoia.app/.MainActivity`.

## Causa

El resultado exitoso de cobrar o completar y cobrar se pasaba a `handleResult(boolean success, DomainError error)`. La llamada posterior a `result.getError()` consultaba un campo inexistente en una operación exitosa.

## Corrección

`ChargeAppointmentViewModel.handleResult` recibe ahora el `Result<ChargeResult>` completo y pregunta `getError()` solamente después de confirmar `isSuccess() == false`.

## Validación

La app quedó instalada como `1.1.2` y permaneció activa al confirmar, completar, cobrar, revertir y cancelar.
