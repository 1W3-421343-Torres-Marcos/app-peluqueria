# skills.md — Stock

**Fase:** F9 · **Depende de:** caja, configuración.

## Propósito
Controlar productos de uso interno y de venta, con alertas de reposición.

## Modelo de datos
- `Product`: id, name, nameNormalized, brand?, type (USE, SALE, BOTH), unit (unidad, g, ml), salePriceCents? (si es de venta), minStock, active, createdAt, updatedAt.
- `StockMovement`: id, productId, timestamp, type (IN, OUT_USE, OUT_SALE, ADJUSTMENT), quantity (con signo según tipo), reason?, cashMovementId?, operator.
- Stock actual = Σ movimientos (se puede cachear en `Product.currentStock`, actualizado en la misma transacción).

## Reglas de negocio
1. Movimientos inmutables y auditados; corrección = ADJUSTMENT con motivo obligatorio.
2. Stock bajo cuando `currentStock ≤ minStock` → `StatusChip` "Stock bajo" y `AlertBanner` con cantidad de productos.
3. Venta (desde caja): descuenta stock (OUT_SALE) y crea `CashMovement` PRODUCT_SALE en **una única transacción**; si falla una, no se aplica ninguna.
4. Stock negativo: se permite con advertencia (el salón puede registrar tarde un ingreso), y se marca para revisión.
5. Productos solo de uso no aparecen en ventas.
6. Baja lógica; historial de movimientos intacto.

## Pantallas y flujos
- **Stock:** lista con filtro Uso/Venta, búsqueda, cantidad y chip de stock bajo; primero los productos bajos.
- **Producto:** datos + historial de movimientos; acciones Ingreso / Egreso / Ajuste.

## Tests obligatorios
Cálculo de stock, regla de stock bajo, transacción venta+caja (rollback), ajuste con motivo.

## Criterios de aceptación
- [x] Alta/edición/baja lógica de productos de uso y de venta.
- [x] Movimientos de ingreso, egreso y ajuste auditados.
- [x] Aviso de stock bajo visible.
- [x] Venta desde caja descuenta stock atómicamente.
- [x] Tests de stock verdes.
