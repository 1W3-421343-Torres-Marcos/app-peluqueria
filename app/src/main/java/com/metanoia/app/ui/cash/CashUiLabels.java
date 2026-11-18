package com.metanoia.app.ui.cash;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.metanoia.app.R;
import com.metanoia.domain.model.CashMovementType;
import com.metanoia.domain.model.PaymentMethod;

import java.util.EnumMap;
import java.util.Map;

/**
 * Etiquetas y controles reutilizables de medios de pago y tipos de movimiento en UI.
 */
public final class CashUiLabels {

    private static final Map<PaymentMethod, Integer> METHOD_LABELS = buildMethodLabels();

    private CashUiLabels() {
    }

    private static Map<PaymentMethod, Integer> buildMethodLabels() {
        EnumMap<PaymentMethod, Integer> map = new EnumMap<>(PaymentMethod.class);
        map.put(PaymentMethod.CASH, R.string.cash_method_cash);
        map.put(PaymentMethod.TRANSFER, R.string.cash_method_transfer);
        map.put(PaymentMethod.CARD, R.string.cash_method_card);
        map.put(PaymentMethod.MERCADO_PAGO, R.string.cash_method_mercado_pago);
        return map;
    }

    @StringRes
    public static int methodStringRes(@NonNull PaymentMethod method) {
        Integer resId = METHOD_LABELS.get(method);
        return resId == null ? R.string.cash_method_cash : resId;
    }

    @NonNull
    public static String methodLabel(@NonNull Context context, @NonNull PaymentMethod method) {
        return context.getString(methodStringRes(method));
    }

    @NonNull
    public static String methodLabel(@NonNull Context context, @NonNull String methodRaw) {
        try {
            return methodLabel(context, PaymentMethod.valueOf(methodRaw));
        } catch (IllegalArgumentException ex) {
            return methodRaw;
        }
    }

    public static void populatePaymentMethodChips(
            @NonNull Context context,
            @NonNull ChipGroup group,
            @NonNull PaymentMethod defaultMethod
    ) {
        group.removeAllViews();
        for (PaymentMethod method : PaymentMethod.values()) {
            Chip chip = new Chip(context);
            chip.setId(View.generateViewId());
            chip.setText(methodStringRes(method));
            chip.setTag(method);
            chip.setCheckable(true);
            chip.setChecked(method == defaultMethod);
            group.addView(chip);
        }
    }

    @NonNull
    public static PaymentMethod selectedPaymentMethod(@NonNull ChipGroup group) {
        int checkedId = group.getCheckedChipId();
        if (checkedId != View.NO_ID) {
            View child = group.findViewById(checkedId);
            if (child != null && child.getTag() instanceof PaymentMethod) {
                return (PaymentMethod) child.getTag();
            }
        }
        return PaymentMethod.CASH;
    }

    public static boolean isPositiveDisplayType(@NonNull CashMovementType type) {
        return type.isStandardIncome();
    }

    @NonNull
    public static String typeLabel(@NonNull Context context, @NonNull CashMovementType type) {
        switch (type) {
            case APPOINTMENT_PAYMENT:
                return context.getString(R.string.cash_type_appointment_payment);
            case PRODUCT_SALE:
                return context.getString(R.string.cash_type_product_sale);
            case OTHER_INCOME:
                return context.getString(R.string.cash_type_other_income);
            case EXPENSE:
                return context.getString(R.string.cash_type_expense);
            case COMMISSION_PAYOUT:
                return context.getString(R.string.cash_type_commission_payout);
            case REVERSAL:
            default:
                return context.getString(R.string.cash_type_reversal);
        }
    }

    @NonNull
    public static String typeLabel(@NonNull Context context, @NonNull String rawType) {
        try {
            return typeLabel(context, CashMovementType.valueOf(rawType));
        } catch (IllegalArgumentException ex) {
            return rawType;
        }
    }
}
