# 1.1.2 — 20/11/2026

## Corrección

Al cobrar exitosamente, `ChargeAppointmentViewModel` consultaba `getError()` sobre un `Result.success`. Esto lanzaba:

`IllegalStateException: No hay error porque el resultado fue exitoso`

Se corrigió para pasar el `Result` completo a `handleResult` y consultar `getError()` únicamente en ramas de fallo. El arreglo cubre:

- cobrar;
- completar y cobrar.
