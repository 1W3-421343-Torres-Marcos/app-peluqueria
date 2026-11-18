package com.metanoia.domain.rules;

import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.Appointment;
import com.metanoia.domain.model.AppointmentItem;
import com.metanoia.domain.model.AuditRecord;
import com.metanoia.domain.model.CashMovement;
import com.metanoia.domain.model.CashMovementType;
import com.metanoia.domain.model.Money;
import com.metanoia.domain.model.PaymentMethod;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.CashRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Utilidades compartidas de validación y registro contable para cobro y completar-y-cobrar.
 */
public final class CashPaymentSupport {

    public static final int MIN_REASON_LENGTH = 2;

    public static final class PaymentSplit {
        private final PaymentMethod method;
        private final Money amount;

        public PaymentSplit(PaymentMethod method, Money amount) {
            this.method = method;
            this.amount = amount;
        }

        public PaymentMethod method() {
            return method;
        }

        public Money amount() {
            return amount;
        }
    }

    private CashPaymentSupport() {
    }

    public static Result<Money> validateAndSumSplits(List<PaymentSplit> splits) {
        if (splits == null || splits.isEmpty()) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "Ingresá al menos un medio y monto de pago");
        }
        long sumCents = 0L;
        for (PaymentSplit split : splits) {
            if (split == null || split.method() == null
                    || split.amount() == null || !split.amount().isPositive()) {
                return Result.failure(DomainError.VALIDATION_ERROR,
                        "Cada pago debe tener método y un importe positivo");
            }
            sumCents = Math.addExact(sumCents, split.amount().cents());
        }
        return Result.success(Money.ofCents(sumCents));
    }

    public static Result<Money> resolveAdjustment(
            Money currentAdjustment,
            Money requestedAdjustment,
            String adjustmentReason
    ) {
        Money safeCurrent = currentAdjustment == null ? Money.zero() : currentAdjustment;
        Money effective = requestedAdjustment == null ? safeCurrent : requestedAdjustment;
        if (!effective.equals(safeCurrent)) {
            String cleanReason = adjustmentReason == null ? "" : adjustmentReason.trim();
            if (cleanReason.length() < MIN_REASON_LENGTH) {
                return Result.failure(DomainError.VALIDATION_ERROR,
                        "Indicá un motivo válido para el descuento o recargo");
            }
        }
        return Result.success(effective);
    }

    public static String buildServicesSummary(Appointment appointment) {
        Objects.requireNonNull(appointment, "appointment");
        List<AppointmentItem> items = appointment.items();
        if (items.isEmpty()) {
            return "Turno";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                sb.append(" + ");
            }
            sb.append(items.get(i).serviceNameSnapshot());
        }
        return sb.toString();
    }

    public static List<CashMovement> recordAppointmentSplits(
            Appointment appointment,
            List<PaymentSplit> splits,
            long dayEpoch,
            long nowMillis,
            String operator,
            CashRepository cashRepository,
            AuditLogRepository auditLogRepository,
            IdGenerator idGenerator
    ) {
        String concept = "Cobro turno: " + buildServicesSummary(appointment);
        List<CashMovement> created = new ArrayList<>(splits.size());
        for (PaymentSplit split : splits) {
            CashMovement movement = new CashMovement.Builder()
                    .id(idGenerator.nextId())
                    .day(dayEpoch)
                    .timestamp(nowMillis)
                    .type(CashMovementType.APPOINTMENT_PAYMENT)
                    .method(split.method())
                    .amount(split.amount())
                    .concept(concept)
                    .appointmentId(appointment.id())
                    .clientId(appointment.clientId())
                    .operator(operator)
                    .build();
            created.add(movement);
            auditLogRepository.record(new AuditRecord.Builder()
                    .id(idGenerator.nextId())
                    .timestamp(nowMillis)
                    .operator(operator)
                    .action("APPOINTMENT_PAYMENT")
                    .entityType("CASH_MOVEMENT")
                    .entityId(movement.id())
                    .amountCents(split.amount().cents())
                    .concept(concept + " (" + split.method().name() + ")")
                    .build());
        }
        cashRepository.saveMovements(created);
        return created;
    }
}
