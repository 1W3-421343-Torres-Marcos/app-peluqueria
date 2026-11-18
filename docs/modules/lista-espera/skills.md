# skills.md — Lista de espera

**Fase:** F8 · **Depende de:** agenda, clientes, recordatorios.

## Propósito
Registrar clientes que quieren un turno en un día/horario sin disponibilidad y ofrecerles los turnos que se liberan.

## Modelo de datos
- `WaitlistEntry`: id, clientId, serviceIds, professionalId? (null = cualquiera), preferredDays (set de días de semana o fecha puntual), fromMin, toMin (franja), validUntilDay, status (ACTIVE, OFFERED, FULFILLED, EXPIRED, CANCELLED), notes, createdAt, offeredAt?.

## Reglas de negocio
1. Al cancelar o reprogramar un turno, `WaitlistMatcher` busca entradas ACTIVE compatibles con el hueco liberado:
   - mismo profesional o "cualquiera" (y el profesional realiza los servicios);
   - día y franja preferida cubren el hueco;
   - la duración requerida cabe en el hueco.
2. Orden de sugerencia: coincidencia exacta de profesional > antigüedad de la entrada > menos ausencias del cliente.
3. Ofrecer = mensaje WhatsApp con `waitlistOfferTemplate`; estado pasa a OFFERED. Asignar = crear turno con el flujo normal (revalida disponibilidad) y estado FULFILLED.
4. Entradas vencidas (`validUntilDay` < hoy) pasan a EXPIRED automáticamente.
5. Un cliente puede tener varias entradas activas, pero no duplicadas (misma preferencia).

## Pantallas y flujos
- **Lista de espera:** entradas activas con preferencias resumidas; alta rápida desde ficha de cliente o desde la agenda (día sin lugar).
- **Diálogo post-cancelación:** "Este horario puede interesarle a…" con candidatos y acciones Ofrecer / Asignar / Ignorar.

## Tests obligatorios
Matcher: compatibilidad de franja, duración, profesional, orden de prioridad, exclusión de vencidas.

## Criterios de aceptación
- [ ] Alta, edición y cancelación de entradas.
- [ ] Al cancelar un turno se sugieren candidatos compatibles y ordenados.
- [ ] Ofrecer por WhatsApp y asignar turno desde la sugerencia.
- [ ] Vencimiento automático de entradas.
- [ ] Tests del matcher verdes.
