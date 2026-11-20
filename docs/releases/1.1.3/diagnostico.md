# 1.1.3 — diagnóstico

## Flujo reportado

Un profesional creado sin servicios se editaba después y se le asignaba el primer servicio. La app se cerraba y la asignación no quedaba guardada.

## Hipótesis corregida

`ProfessionalDao.upsertProfessional()` usaba `OnConflictStrategy.REPLACE`. Esa estrategia puede borrar y recrear la fila padre. Al existir horarios u relaciones con claves `RESTRICT`, el reemplazo puede fallar.

## Corrección

- Profesionales nuevos: `INSERT IGNORE`.
- Profesionales existentes: `UPDATE`.
- Relaciones, horarios y días libres: actualizados dentro de la transacción `saveComplete`.
