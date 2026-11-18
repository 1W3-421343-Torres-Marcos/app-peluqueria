package com.metanoia.domain.rules;

import com.metanoia.domain.model.CashMovement;
import com.metanoia.domain.model.CashMovementType;
import com.metanoia.domain.model.Money;
import com.metanoia.domain.model.PaymentMethod;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Cálculo puro y determinista de saldos esperados y diferencias para el cierre de caja diario.
 */
public final class CashClosingCalculator {

    public static final class ClosingPreview {
        private final Map<PaymentMethod, Money> expectedByMethod;
        private final Map<PaymentMethod, Money> countedByMethod;
        private final Money totalExpected;
        private final Money totalCounted;
        private final Money totalDifference;

        ClosingPreview(
                Map<PaymentMethod, Money> expectedByMethod,
                Map<PaymentMethod, Money> countedByMethod,
                Money totalExpected,
                Money totalCounted,
                Money totalDifference
        ) {
            this.expectedByMethod = Collections.unmodifiableMap(expectedByMethod);
            this.countedByMethod = Collections.unmodifiableMap(countedByMethod);
            this.totalExpected = totalExpected;
            this.totalCounted = totalCounted;
            this.totalDifference = totalDifference;
        }

        public Map<PaymentMethod, Money> expectedByMethod() {
            return expectedByMethod;
        }

        public Map<PaymentMethod, Money> countedByMethod() {
            return countedByMethod;
        }

        public Money expectedFor(PaymentMethod method) {
            return expectedByMethod.getOrDefault(method, Money.zero());
        }

        public Money countedFor(PaymentMethod method) {
            return countedByMethod.getOrDefault(method, Money.zero());
        }

        public Money differenceFor(PaymentMethod method) {
            return countedFor(method).minus(expectedFor(method));
        }

        public Money totalExpected() {
            return totalExpected;
        }

        public Money totalCounted() {
            return totalCounted;
        }

        public Money totalDifference() {
            return totalDifference;
        }
    }

    private CashClosingCalculator() {
    }

    public static Map<PaymentMethod, Money> computeExpected(
            List<CashMovement> dayMovements,
            Map<String, CashMovementType> externalOriginalTypes
    ) {
        EnumMap<PaymentMethod, Money> expected = new EnumMap<>(PaymentMethod.class);
        for (PaymentMethod method : PaymentMethod.values()) {
            expected.put(method, Money.zero());
        }
        if (dayMovements == null || dayMovements.isEmpty()) {
            return Collections.unmodifiableMap(expected);
        }

        Map<String, CashMovementType> typesById = new HashMap<>();
        if (externalOriginalTypes != null) {
            typesById.putAll(externalOriginalTypes);
        }
        for (CashMovement m : dayMovements) {
            typesById.put(m.id(), m.type());
        }

        for (CashMovement movement : dayMovements) {
            long deltaCents = signedCents(movement, typesById);
            PaymentMethod method = movement.method();
            expected.put(method, expected.get(method).plus(Money.ofCents(deltaCents)));
        }
        return Collections.unmodifiableMap(expected);
    }

    public static ClosingPreview preview(
            List<CashMovement> dayMovements,
            Map<String, CashMovementType> externalOriginalTypes,
            Map<PaymentMethod, Money> countedInput
    ) {
        Map<PaymentMethod, Money> expected = computeExpected(dayMovements, externalOriginalTypes);
        EnumMap<PaymentMethod, Money> counted = new EnumMap<>(PaymentMethod.class);
        Money totalExpected = Money.zero();
        Money totalCounted = Money.zero();

        for (PaymentMethod method : PaymentMethod.values()) {
            Money exp = expected.get(method);
            Money cnt = countedInput == null ? Money.zero() : countedInput.get(method);
            if (cnt == null) {
                cnt = Money.zero();
            }
            if (cnt.isNegative()) {
                throw new IllegalArgumentException("Monto contado negativo para " + method);
            }
            counted.put(method, cnt);
            totalExpected = totalExpected.plus(exp);
            totalCounted = totalCounted.plus(cnt);
        }

        Money totalDifference = totalCounted.minus(totalExpected);
        return new ClosingPreview(expected, counted, totalExpected, totalCounted, totalDifference);
    }

    public static long signedCents(
            CashMovement movement,
            Map<String, CashMovementType> typesById
    ) {
        Objects.requireNonNull(movement, "movement");
        CashMovementType type = movement.type();
        long amount = movement.amount().cents();
        if (type.isStandardIncome()) {
            return amount;
        }
        if (type.isStandardOutflow()) {
            return -amount;
        }
        String originalId = movement.reversesMovementId().orElse(null);
        CashMovementType originalType = originalId == null || typesById == null
                ? null
                : typesById.get(originalId);
        if (originalType != null && originalType.isStandardOutflow()) {
            return amount;
        }
        return -amount;
    }
}
