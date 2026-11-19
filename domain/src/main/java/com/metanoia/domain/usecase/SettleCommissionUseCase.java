package com.metanoia.domain.usecase;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.Appointment;
import com.metanoia.domain.model.AuditRecord;
import com.metanoia.domain.model.CashMovement;
import com.metanoia.domain.model.CashMovementType;
import com.metanoia.domain.model.CommissionSettlement;
import com.metanoia.domain.model.CommissionSettlementEntry;
import com.metanoia.domain.model.CommissionSettlementEntryType;
import com.metanoia.domain.model.Money;
import com.metanoia.domain.model.PaymentMethod;
import com.metanoia.domain.model.Professional;
import com.metanoia.domain.repository.AppointmentRepository;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.CashRepository;
import com.metanoia.domain.repository.CommissionRepository;
import com.metanoia.domain.repository.ProfessionalRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.rules.CommissionCalculator;
import com.metanoia.domain.rules.CommissionCalculator.AppointmentCalculation;
import com.metanoia.domain.rules.CommissionCalculator.PeriodCalculation;
import com.metanoia.domain.rules.CommissionCalculator.ReversalAdjustmentLine;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Liquida comisiones de un profesional para un período en una única transacción atómica,
 * registrando el egreso de caja ({@link CashMovementType#COMMISSION_PAYOUT}) y la liquidación
 * inmutable para evitar recálculos o dobles pagos.
 */
public final class SettleCommissionUseCase {

    private static final DateTimeFormatter PERIOD_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/uuuu");

    public static final class Request {
        private final String professionalId;
        private final long fromDay;
        private final long toDay;
        private final PaymentMethod method;

        public Request(
                String professionalId,
                long fromDay,
                long toDay,
                PaymentMethod method
        ) {
            this.professionalId = professionalId;
            this.fromDay = fromDay;
            this.toDay = toDay;
            this.method = method;
        }
    }

    public static final class Outcome {
        private final CommissionSettlement settlement;
        private final CashMovement cashMovement;
        private final PeriodCalculation calculation;

        public Outcome(
                CommissionSettlement settlement,
                CashMovement cashMovement,
                PeriodCalculation calculation
        ) {
            this.settlement = Objects.requireNonNull(settlement, "settlement");
            this.cashMovement = Objects.requireNonNull(cashMovement, "cashMovement");
            this.calculation = Objects.requireNonNull(calculation, "calculation");
        }

        public CommissionSettlement settlement() {
            return settlement;
        }

        public CashMovement cashMovement() {
            return cashMovement;
        }

        public PeriodCalculation calculation() {
            return calculation;
        }
    }

    private final CommissionRepository commissionRepository;
    private final AppointmentRepository appointmentRepository;
    private final ProfessionalRepository professionalRepository;
    private final CashRepository cashRepository;
    private final SettingsRepository settingsRepository;
    private final AuditLogRepository auditLogRepository;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public SettleCommissionUseCase(
            CommissionRepository commissionRepository,
            AppointmentRepository appointmentRepository,
            ProfessionalRepository professionalRepository,
            CashRepository cashRepository,
            SettingsRepository settingsRepository,
            AuditLogRepository auditLogRepository,
            Clock clock,
            IdGenerator idGenerator
    ) {
        this.commissionRepository = Objects.requireNonNull(
                commissionRepository, "commissionRepository");
        this.appointmentRepository = Objects.requireNonNull(
                appointmentRepository, "appointmentRepository");
        this.professionalRepository = Objects.requireNonNull(
                professionalRepository, "professionalRepository");
        this.cashRepository = Objects.requireNonNull(cashRepository, "cashRepository");
        this.settingsRepository = Objects.requireNonNull(settingsRepository, "settingsRepository");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    }

    /**
     * Reúne todos los turnos relevantes para calcular comisiones del período: los del rango,
     * los referenciados en liquidaciones previas (posibles reversos o recobros) y los turnos
     * de períodos ya liquidados que acaban de cubrir su pago ({@code PAID}).
     */
    public static List<Appointment> loadCandidateAppointments(
            String professionalId,
            long fromDay,
            long toDay,
            List<CommissionSettlement> existingSettlements,
            AppointmentRepository appointmentRepository,
            CommissionRepository commissionRepository
    ) {
        List<Appointment> candidates = new ArrayList<>(
                appointmentRepository.findByProfessionalAndDayRange(professionalId, fromDay, toDay));

        Set<String> settledIds = new LinkedHashSet<>();
        if (existingSettlements != null) {
            for (CommissionSettlement settlement : existingSettlements) {
                if (settlement != null && professionalId.equals(settlement.professionalId())) {
                    for (CommissionSettlementEntry entry : settlement.entries()) {
                        settledIds.add(entry.appointmentId());
                    }
                }
            }
        }
        if (!settledIds.isEmpty()) {
            candidates.addAll(appointmentRepository.findByIds(settledIds));
        }
        candidates.addAll(commissionRepository.findPaidAppointmentsInPreviousSettledPeriods(
                professionalId, fromDay));
        return candidates;
    }

    public Result<Outcome> execute(Request request) {
        if (request == null
                || request.professionalId == null
                || request.professionalId.trim().isEmpty()
                || request.fromDay > request.toDay
                || request.method == null) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "Datos de liquidación inválidos");
        }

        long today = clock.todayEpochDay();
        if (cashRepository.isDayClosed(today)) {
            return Result.failure(DomainError.CONFLICT,
                    "La caja del día actual ya está cerrada; no admite egresos");
        }

        String proId = request.professionalId.trim();
        String operator = settingsRepository.getSettings().currentOperator();
        long now = clock.nowMillis();
        String cashMovementId = idGenerator.nextId();
        String settlementId = idGenerator.nextId();

        AtomicReference<Result<Outcome>> outcome = new AtomicReference<>();
        cashRepository.runInTransaction(() -> {
            if (cashRepository.isDayClosed(today)) {
                outcome.set(Result.failure(DomainError.CONFLICT,
                        "La caja del día actual ya está cerrada"));
                return;
            }
            Optional<Professional> proOpt = professionalRepository.findById(proId);
            if (!proOpt.isPresent()) {
                outcome.set(Result.failure(DomainError.NOT_FOUND, "Profesional no encontrado"));
                return;
            }
            Professional professional = proOpt.get();

            if (commissionRepository.hasOverlappingSettlement(
                    proId, request.fromDay, request.toDay)) {
                outcome.set(Result.failure(DomainError.CONFLICT,
                        "El período se solapa con una liquidación ya registrada"));
                return;
            }

            List<CommissionSettlement> existing =
                    commissionRepository.findSettlementsByProfessional(proId);
            List<Appointment> candidates = loadCandidateAppointments(
                    proId,
                    request.fromDay,
                    request.toDay,
                    existing,
                    appointmentRepository,
                    commissionRepository
            );

            PeriodCalculation calc = CommissionCalculator.calculatePeriod(
                    proId, request.fromDay, request.toDay, candidates, existing);
            if (calc.overlapsExistingSettlement()) {
                outcome.set(Result.failure(DomainError.CONFLICT,
                        "El período ya fue liquidado previamente"));
                return;
            }
            if (!calc.canSettle()) {
                outcome.set(Result.failure(DomainError.VALIDATION_ERROR,
                        "No hay comisiones positivas disponibles para liquidar en este período"));
                return;
            }

            String fromLabel = LocalDate.ofEpochDay(request.fromDay).format(PERIOD_DATE_FORMAT);
            String toLabel = LocalDate.ofEpochDay(request.toDay).format(PERIOD_DATE_FORMAT);
            String concept = "Comisión " + professional.name()
                    + " (" + fromLabel + " – " + toLabel + ")";

            Money payoutAmount = Money.ofCents(calc.totalCommissionCents());
            CashMovement cashMovement = new CashMovement.Builder()
                    .id(cashMovementId)
                    .day(today)
                    .timestamp(now)
                    .type(CashMovementType.COMMISSION_PAYOUT)
                    .method(request.method)
                    .amount(payoutAmount)
                    .concept(concept)
                    .operator(operator)
                    .build();
            cashRepository.saveMovement(cashMovement);

            List<CommissionSettlementEntry> entries = new ArrayList<>();
            for (AppointmentCalculation ac : calc.liquidableAppointments()) {
                entries.add(new CommissionSettlementEntry.Builder()
                        .id(idGenerator.nextId())
                        .settlementId(settlementId)
                        .appointmentId(ac.appointment().id())
                        .type(CommissionSettlementEntryType.SETTLED)
                        .baseCents(ac.adjustedBase().cents())
                        .commissionCents(ac.commission().cents())
                        .build());
            }
            for (ReversalAdjustmentLine rev : calc.reversalAdjustments()) {
                entries.add(new CommissionSettlementEntry.Builder()
                        .id(idGenerator.nextId())
                        .settlementId(settlementId)
                        .appointmentId(rev.appointment().id())
                        .type(CommissionSettlementEntryType.REVERSAL_ADJUSTMENT)
                        .baseCents(rev.baseCents())
                        .commissionCents(rev.commissionCents())
                        .build());
            }

            CommissionSettlement settlement = new CommissionSettlement.Builder()
                    .id(settlementId)
                    .professionalId(proId)
                    .fromDay(request.fromDay)
                    .toDay(request.toDay)
                    .baseAmount(Money.ofCents(calc.totalBaseCents()))
                    .commissionAmount(payoutAmount)
                    .cashMovementId(cashMovement.id())
                    .settledAt(now)
                    .operator(operator)
                    .entries(entries)
                    .build();
            commissionRepository.saveSettlement(settlement);

            auditLogRepository.record(new AuditRecord.Builder()
                    .id(idGenerator.nextId())
                    .timestamp(now)
                    .operator(operator)
                    .action("COMMISSION_SETTLE")
                    .entityType("COMMISSION_SETTLEMENT")
                    .entityId(settlement.id())
                    .amountCents(payoutAmount.cents())
                    .concept(concept)
                    .build());

            outcome.set(Result.success(new Outcome(settlement, cashMovement, calc)));
        });

        return outcome.get();
    }
}
