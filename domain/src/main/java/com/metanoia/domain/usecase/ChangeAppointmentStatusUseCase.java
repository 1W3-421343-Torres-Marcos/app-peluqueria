package com.metanoia.domain.usecase;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.Appointment;
import com.metanoia.domain.model.AppointmentItem;
import com.metanoia.domain.model.AppointmentStatus;
import com.metanoia.domain.model.AuditRecord;
import com.metanoia.domain.model.Client;
import com.metanoia.domain.model.Professional;
import com.metanoia.domain.repository.AppointmentRepository;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.ClientRepository;
import com.metanoia.domain.repository.ProfessionalRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.rules.StatusTransitions;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Ejecuta transiciones manuales válidas de un turno: confirmar, completar (congelando comisión),
 * marcar ausente (incrementando ausencias del cliente), cancelar con motivo y revertir completado.
 */
public final class ChangeAppointmentStatusUseCase {

    private final AppointmentRepository appointmentRepository;
    private final ClientRepository clientRepository;
    private final ProfessionalRepository professionalRepository;
    private final SettingsRepository settingsRepository;
    private final AuditLogRepository auditLogRepository;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public ChangeAppointmentStatusUseCase(
            AppointmentRepository appointmentRepository,
            ClientRepository clientRepository,
            ProfessionalRepository professionalRepository,
            SettingsRepository settingsRepository,
            AuditLogRepository auditLogRepository,
            Clock clock,
            IdGenerator idGenerator
    ) {
        this.appointmentRepository = Objects.requireNonNull(
                appointmentRepository, "appointmentRepository");
        this.clientRepository = Objects.requireNonNull(clientRepository, "clientRepository");
        this.professionalRepository = Objects.requireNonNull(
                professionalRepository, "professionalRepository");
        this.settingsRepository = Objects.requireNonNull(settingsRepository, "settingsRepository");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    }

    public Result<Appointment> execute(
            String appointmentId,
            AppointmentStatus targetStatus,
            String cancelReason
    ) {
        return execute(appointmentId, targetStatus, cancelReason, 0L);
    }

    public Result<Appointment> execute(
            String appointmentId,
            AppointmentStatus targetStatus,
            String cancelReason,
            long paidCents
    ) {
        if (appointmentId == null || appointmentId.trim().isEmpty() || targetStatus == null) {
            return Result.failure(DomainError.VALIDATION_ERROR, "Solicitud de estado inválida");
        }
        Optional<Appointment> found = appointmentRepository.findById(appointmentId.trim());
        if (!found.isPresent()) {
            return Result.failure(DomainError.NOT_FOUND, "Turno no encontrado");
        }
        Appointment current = found.get();

        if (!StatusTransitions.canTransitionManually(current.status(), targetStatus, paidCents)) {
            return Result.failure(DomainError.CONFLICT,
                    "No se puede cambiar el estado de " + current.status() + " a " + targetStatus);
        }

        long now = clock.nowMillis();
        String operator = settingsRepository.getSettings().currentOperator();
        Appointment.Builder builder = current.toBuilder()
                .status(targetStatus)
                .updatedAt(now);

        String auditAction;
        String auditConcept;
        Long auditAmountCents = null;

        switch (targetStatus) {
            case CONFIRMED:
                if (current.status() == AppointmentStatus.COMPLETED) {
                    List<AppointmentItem> cleared = StatusTransitions.clearCommissions(
                            current.items());
                    builder.items(cleared);
                    auditAction = "APPOINTMENT_REVERT_COMPLETED";
                    auditConcept = "Corrección de completado a confirmado";
                } else {
                    auditAction = "APPOINTMENT_CONFIRM";
                    auditConcept = "Confirmación de turno";
                }
                break;

            case COMPLETED:
                Optional<Professional> proOpt = professionalRepository.findById(
                        current.professionalId());
                if (!proOpt.isPresent()) {
                    return Result.failure(DomainError.NOT_FOUND, "Profesional no encontrado");
                }
                List<AppointmentItem> frozen = StatusTransitions.freezeCommissions(
                        current.items(), proOpt.get().commissionBasisPoints());
                builder.items(frozen);
                auditAction = "APPOINTMENT_COMPLETE";
                auditConcept = "Servicio completado";
                auditAmountCents = current.totalAmount().cents();
                break;

            case CANCELLED:
                String cleanReason = cancelReason == null ? "" : cancelReason.trim();
                if (cleanReason.length() < 2) {
                    return Result.failure(DomainError.VALIDATION_ERROR,
                            "Indicá un motivo para cancelar el turno");
                }
                builder.cancelReason(cleanReason);
                auditAction = "APPOINTMENT_CANCEL";
                auditConcept = "Cancelación: " + cleanReason;
                break;

            case NO_SHOW:
                Optional<Client> clientOpt = clientRepository.findById(current.clientId());
                if (!clientOpt.isPresent()) {
                    return Result.failure(DomainError.NOT_FOUND, "Cliente no encontrado");
                }
                Client updatedClient = clientOpt.get().toBuilder()
                        .noShowCount(clientOpt.get().noShowCount() + 1)
                        .updatedAt(now)
                        .build();
                clientRepository.save(updatedClient);
                auditAction = "APPOINTMENT_NO_SHOW";
                auditConcept = "Ausencia registrada en turno";
                break;

            default:
                return Result.failure(DomainError.CONFLICT, "Transición no soportada");
        }

        Appointment updated = builder.build();
        final Long recordedAmountCents = auditAmountCents;
        AtomicReference<Result<Appointment>> outcome = new AtomicReference<>();
        try {
            appointmentRepository.runInTransaction(() -> {
                appointmentRepository.save(updated);
                auditLogRepository.record(new AuditRecord.Builder()
                        .id(idGenerator.nextId())
                        .timestamp(now)
                        .operator(operator)
                        .action(auditAction)
                        .entityType("APPOINTMENT")
                        .entityId(updated.id())
                        .amountCents(recordedAmountCents)
                        .concept(auditConcept)
                        .build());
                outcome.set(Result.success(updated));
            });
        } catch (RuntimeException ex) {
            return Result.failure(DomainError.DATABASE_ERROR,
                    "No se pudo guardar la transición del turno");
        }
        return outcome.get();
    }
}
