# 1.1.1 — 20/11/2026

- Captura y manejo de fallos de persistencia en confirmaciones y cobros.
- Transacción común para turno y auditoría.
- Traducción de errores Room inesperados a `DATABASE_ERROR`.
- Cobros y completar-y-cobrar protegidos dentro de transacciones Room.
- Prueba de rollback ante fallo de auditoría.
