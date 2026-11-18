package com.metanoia.domain.rules;

import static org.junit.Assert.assertEquals;

import com.metanoia.domain.model.CashMovement;
import com.metanoia.domain.model.CashMovementType;
import com.metanoia.domain.model.Money;
import com.metanoia.domain.model.PaymentMethod;
import com.metanoia.domain.rules.CashClosingCalculator.ClosingPreview;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class CashClosingCalculatorTest {

    @Test
    public void computesExpectedAcrossMethodsIncludingExpensesAndReversals() {
        CashMovement aptCash = mov("m1", CashMovementType.APPOINTMENT_PAYMENT,
                PaymentMethod.CASH, 1_500_000L, null);
        CashMovement saleMp = mov("m2", CashMovementType.PRODUCT_SALE,
                PaymentMethod.MERCADO_PAGO, 400_000L, null);
        CashMovement expenseCash = mov("m3", CashMovementType.EXPENSE,
                PaymentMethod.CASH, 300_000L, null);
        CashMovement revExpenseCash = mov("m4", CashMovementType.REVERSAL,
                PaymentMethod.CASH, 300_000L, "m3");
        CashMovement transferIncome = mov("m5", CashMovementType.OTHER_INCOME,
                PaymentMethod.TRANSFER, 200_000L, null);
        CashMovement revTransferIncome = mov("m6", CashMovementType.REVERSAL,
                PaymentMethod.TRANSFER, 200_000L, "m5");

        List<CashMovement> movements = Arrays.asList(
                aptCash, saleMp, expenseCash, revExpenseCash, transferIncome, revTransferIncome);
        Map<PaymentMethod, Money> counted = new EnumMap<>(PaymentMethod.class);
        counted.put(PaymentMethod.CASH, Money.ofCents(1_490_000L));
        counted.put(PaymentMethod.MERCADO_PAGO, Money.ofCents(400_000L));
        counted.put(PaymentMethod.TRANSFER, Money.zero());
        counted.put(PaymentMethod.CARD, Money.ofCents(20_000L));

        ClosingPreview preview = CashClosingCalculator.preview(
                movements, Collections.emptyMap(), counted);

        assertEquals(1_500_000L, preview.expectedFor(PaymentMethod.CASH).cents());
        assertEquals(0L, preview.expectedFor(PaymentMethod.TRANSFER).cents());
        assertEquals(0L, preview.expectedFor(PaymentMethod.CARD).cents());
        assertEquals(400_000L, preview.expectedFor(PaymentMethod.MERCADO_PAGO).cents());
        assertEquals(1_900_000L, preview.totalExpected().cents());
        assertEquals(1_910_000L, preview.totalCounted().cents());
        assertEquals(-10_000L, preview.differenceFor(PaymentMethod.CASH).cents());
        assertEquals(20_000L, preview.differenceFor(PaymentMethod.CARD).cents());
        assertEquals(10_000L, preview.totalDifference().cents());
    }

    @Test
    public void crossDayReversalOfOutflowUsesExternalOriginalTypesMap() {
        CashMovement reversalOfPreviousExpense = mov(
                "m-rev", CashMovementType.REVERSAL, PaymentMethod.CASH, 120_000L, "old-expense");

        Map<PaymentMethod, Money> expected = CashClosingCalculator.computeExpected(
                Collections.singletonList(reversalOfPreviousExpense),
                Collections.singletonMap("old-expense", CashMovementType.EXPENSE));

        assertEquals(120_000L, expected.get(PaymentMethod.CASH).cents());
    }

    private static CashMovement mov(
            String id, CashMovementType type, PaymentMethod method, long cents, String revId) {
        return new CashMovement.Builder()
                .id(id)
                .day(20_500L)
                .timestamp(1_000L)
                .type(type)
                .method(method)
                .amount(Money.ofCents(cents))
                .concept("Test " + id)
                .reversesMovementId(revId)
                .operator("Dueño")
                .build();
    }
}
