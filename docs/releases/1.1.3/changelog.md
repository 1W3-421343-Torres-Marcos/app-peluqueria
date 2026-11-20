# 1.1.3 — corrección de profesional sin servicios

- Evita `REPLACE` destructivo al editar profesionales.
- Usa actualización segura para profesionales existentes y alta normal para profesionales nuevos.
- Captura fallos inesperados de Room en el ViewModel.
- Agrega cobertura para asignar el primer servicio a un profesional que ya tenía horario.
