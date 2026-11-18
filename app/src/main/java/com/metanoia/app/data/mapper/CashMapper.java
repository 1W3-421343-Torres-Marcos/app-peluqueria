package com.metanoia.app.data.mapper;

import androidx.annotation.NonNull;

import com.metanoia.app.data.db.entity.CashClosingEntity;
import com.metanoia.app.data.db.entity.CashMovementEntity;
import com.metanoia.domain.model.CashClosing;
import com.metanoia.domain.model.CashMovement;
import com.metanoia.domain.model.CashMovementType;
import com.metanoia.domain.model.Money;
import com.metanoia.domain.model.PaymentMethod;

import java.util.EnumMap;
import java.util.Map;

/**
 * Conversión entre entidades Room de caja y objetos inmutables del dominio.
 */
public final class CashMapper {

    private CashMapper() {
    }

    @NonNull
    public static CashMovement toDomain(@NonNull CashMovementEntity entity) {
        return new CashMovement.Builder()
                .id(entity.id)
                .day(entity.day)
                .timestamp(entity.timestamp)
                .type(CashMovementType.valueOf(entity.type))
                .method(PaymentMethod.valueOf(entity.method))
                .amount(Money.ofCents(entity.amountCents))
                .concept(entity.concept)
                .appointmentId(entity.appointmentId)
                .clientId(entity.clientId)
                .productId(entity.productId)
                .quantity(entity.quantity)
                .reversesMovementId(entity.reversesMovementId)
                .operator(entity.operator)
                .closingId(entity.closingId)
                .build();
    }

    @NonNull
    public static CashMovementEntity toEntity(@NonNull CashMovement model) {
        return new CashMovementEntity(
                model.id(),
                model.day(),
                model.timestamp(),
                model.type().name(),
                model.method().name(),
                model.amount().cents(),
                model.concept(),
                model.appointmentId().orElse(null),
                model.clientId().orElse(null),
                model.productId().orElse(null),
                model.quantity().orElse(null),
                model.reversesMovementId().orElse(null),
                model.operator(),
                model.closingId().orElse(null)
        );
    }

    @NonNull
    public static CashClosing toDomain(@NonNull CashClosingEntity entity) {
        Map<PaymentMethod, Money> expected = new EnumMap<>(PaymentMethod.class);
        expected.put(PaymentMethod.CASH, Money.ofCents(entity.expectedCashCents));
        expected.put(PaymentMethod.TRANSFER, Money.ofCents(entity.expectedTransferCents));
        expected.put(PaymentMethod.CARD, Money.ofCents(entity.expectedCardCents));
        expected.put(PaymentMethod.MERCADO_PAGO, Money.ofCents(entity.expectedMpCents));

        Map<PaymentMethod, Money> counted = new EnumMap<>(PaymentMethod.class);
        counted.put(PaymentMethod.CASH, Money.ofCents(entity.countedCashCents));
        counted.put(PaymentMethod.TRANSFER, Money.ofCents(entity.countedTransferCents));
        counted.put(PaymentMethod.CARD, Money.ofCents(entity.countedCardCents));
        counted.put(PaymentMethod.MERCADO_PAGO, Money.ofCents(entity.countedMpCents));

        return new CashClosing.Builder()
                .id(entity.id)
                .day(entity.day)
                .expectedByMethod(expected)
                .countedByMethod(counted)
                .difference(Money.ofCents(entity.differenceCents))
                .notes(entity.notes)
                .closedAt(entity.closedAt)
                .operator(entity.operator)
                .build();
    }

    @NonNull
    public static CashClosingEntity toEntity(@NonNull CashClosing model) {
        return new CashClosingEntity(
                model.id(),
                model.day(),
                model.expectedFor(PaymentMethod.CASH).cents(),
                model.expectedFor(PaymentMethod.TRANSFER).cents(),
                model.expectedFor(PaymentMethod.CARD).cents(),
                model.expectedFor(PaymentMethod.MERCADO_PAGO).cents(),
                model.countedFor(PaymentMethod.CASH).cents(),
                model.countedFor(PaymentMethod.TRANSFER).cents(),
                model.countedFor(PaymentMethod.CARD).cents(),
                model.countedFor(PaymentMethod.MERCADO_PAGO).cents(),
                model.difference().cents(),
                model.notes(),
                model.closedAt(),
                model.operator()
        );
    }
}
