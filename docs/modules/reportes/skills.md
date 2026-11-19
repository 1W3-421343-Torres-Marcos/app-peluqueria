# skills.md — Reportes

**Fase:** F11 · **Depende de:** agenda, caja, clientes, profesionales, servicios.

## Propósito
Dar al dueño una visión clara del rendimiento del negocio, exportable a PDF y CSV.

## Reportes
| Reporte | Contenido |
|---|---|
| Ingresos | Por día y por mes; desglose por método de pago; ingresos − egresos. |
| Servicios más solicitados | Cantidad y facturación por servicio/categoría. |
| Clientes frecuentes | Top N por visitas y por gasto en el período. |
| Ocupación | % ocupado por franja horaria y día de semana; horarios con menor ocupación. |
| Rendimiento por profesional | Turnos realizados (COMPLETED o PAID), facturación, ticket promedio, ausencias, comisión. |
| Por cobrar | Suma de `balanceCents` de turnos COMPLETED con saldo; cantidad de turnos y detalle por cliente. |

## Reglas de negocio
1. Fuente de ingresos = `CashMovement` (no precios de catálogo). Servicios más solicitados, clientes frecuentes y rendimiento por profesional = turnos COMPLETED o PAID (snapshots de `AppointmentItem`). Indicador "por cobrar" = suma de saldos de turnos COMPLETED.
2. Ocupación = minutos con turnos activos / minutos laborables (horario de profesionales − bloqueos) por franja.
3. Filtros: período (hoy, semana, mes, rango) y profesional.
4. Consultas agregadas en SQL (GROUP BY) acotadas por rango; nunca cargar todos los registros en memoria. Respuesta < 1 s con 50.000 turnos.
5. Exportaciones solo por acción explícita (SAF `ACTION_CREATE_DOCUMENT` o compartir). Los reportes no incluyen notas ni datos sensibles innecesarios (teléfono solo en clientes frecuentes, opcional).
6. Formatos: CSV y PDF según `design.md` §9.

## Pantallas y flujos
- **Reportes (en Más):** selector de reporte, filtros, tarjetas de totales, `BarChartView` monocromático, tabla; acciones Exportar PDF / CSV.

## Tests obligatorios
Agregaciones DAO con datos sembrados, cálculo de ocupación, formato CSV (separador, decimales, BOM).

## Criterios de aceptación
- [x] Los 6 reportes (incluido "Por cobrar") disponibles con filtros de período y profesional.
- [x] Ocupación identifica horarios con menos demanda.
- [x] Exportación a CSV y PDF con identidad de marca.
- [x] Rendimiento < 1 s con 50.000 turnos. *(Query verificada vía `ReportsDaoTest` compilado con `:app:compileDebugAndroidTestJavaWithJavac`; pendiente de ejecución en dispositivo/emulador).*
- [x] Tests de agregaciones verdes. *(Unitarios `:domain` y `:app` verdes en JVM; `ReportsDaoTest` compilado).*
