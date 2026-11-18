package com.metanoia.app.ui.cash.closing;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.metanoia.app.R;
import com.metanoia.app.databinding.DialogCashClosingBinding;
import com.metanoia.app.ui.cash.CashViewModel;
import com.metanoia.app.ui.cash.CashViewModel.CashDayState;
import com.metanoia.app.util.Formats;
import com.metanoia.domain.model.Money;
import com.metanoia.domain.model.PaymentMethod;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Bottom sheet para realizar el cierre diario de caja con arqueo por medio de pago.
 */
public class CashClosingDialogFragment extends BottomSheetDialogFragment {

    private static final String ARG_DAY_EPOCH = "day_epoch";

    private DialogCashClosingBinding binding;
    private CashViewModel viewModel;
    private long dayEpoch;
    private boolean initializedInputs;
    private final EnumMap<PaymentMethod, Long> expectedCents = new EnumMap<>(PaymentMethod.class);

    public static CashClosingDialogFragment newInstance(long dayEpoch) {
        CashClosingDialogFragment fragment = new CashClosingDialogFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_DAY_EPOCH, dayEpoch);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = DialogCashClosingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireParentFragment()).get(CashViewModel.class);
        dayEpoch = getArguments() != null
                ? getArguments().getLong(ARG_DAY_EPOCH, viewModel.todayEpochDay())
                : viewModel.todayEpochDay();
        binding.closingDateSubtitle.setText(Formats.formatDayWithShortWeekday(dayEpoch));

        for (PaymentMethod method : PaymentMethod.values()) {
            expectedCents.put(method, 0L);
        }

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (initializedInputs) {
                    refreshTotalsAndDifferences();
                }
            }
        };
        binding.inputClosingCashCounted.addTextChangedListener(watcher);
        binding.inputClosingTransferCounted.addTextChangedListener(watcher);
        binding.inputClosingCardCounted.addTextChangedListener(watcher);
        binding.inputClosingMercadoPagoCounted.addTextChangedListener(watcher);

        viewModel.getDayState().observe(getViewLifecycleOwner(), state -> {
            if (state == null || state.dayEpoch != dayEpoch) {
                return;
            }
            applyExpectedState(state);
        });

        binding.btnConfirmCashClosing.setOnClickListener(v -> submitClosing());
    }

    private void applyExpectedState(@NonNull CashDayState state) {
        expectedCents.put(PaymentMethod.CASH, state.cashCents);
        expectedCents.put(PaymentMethod.TRANSFER, state.transferCents);
        expectedCents.put(PaymentMethod.CARD, state.cardCents);
        expectedCents.put(PaymentMethod.MERCADO_PAGO, state.mercadoPagoCents);

        binding.closingCashExpected.setText(getString(
                R.string.cash_closing_expected_format, Formats.formatMoney(state.cashCents)));
        binding.closingTransferExpected.setText(getString(
                R.string.cash_closing_expected_format, Formats.formatMoney(state.transferCents)));
        binding.closingCardExpected.setText(getString(
                R.string.cash_closing_expected_format, Formats.formatMoney(state.cardCents)));
        binding.closingMercadoPagoExpected.setText(getString(
                R.string.cash_closing_expected_format, Formats.formatMoney(state.mercadoPagoCents)));

        if (!initializedInputs) {
            binding.inputClosingCashCounted.setText(
                    Formats.formatPesosPlain(Money.ofCents(Math.max(0L, state.cashCents))));
            binding.inputClosingTransferCounted.setText(
                    Formats.formatPesosPlain(Money.ofCents(Math.max(0L, state.transferCents))));
            binding.inputClosingCardCounted.setText(
                    Formats.formatPesosPlain(Money.ofCents(Math.max(0L, state.cardCents))));
            binding.inputClosingMercadoPagoCounted.setText(
                    Formats.formatPesosPlain(Money.ofCents(Math.max(0L, state.mercadoPagoCents))));
            initializedInputs = true;
        }
        refreshTotalsAndDifferences();
    }

    private void refreshTotalsAndDifferences() {
        long expCash = expectedCents.get(PaymentMethod.CASH);
        long expTransfer = expectedCents.get(PaymentMethod.TRANSFER);
        long expCard = expectedCents.get(PaymentMethod.CARD);
        long expMp = expectedCents.get(PaymentMethod.MERCADO_PAGO);

        long cntCash = parseOrZero(binding.inputClosingCashCounted);
        long cntTransfer = parseOrZero(binding.inputClosingTransferCounted);
        long cntCard = parseOrZero(binding.inputClosingCardCounted);
        long cntMp = parseOrZero(binding.inputClosingMercadoPagoCounted);

        binding.closingCashDiff.setText(getString(
                R.string.cash_closing_diff_format, Formats.formatMoney(cntCash - expCash)));
        binding.closingTransferDiff.setText(getString(
                R.string.cash_closing_diff_format, Formats.formatMoney(cntTransfer - expTransfer)));
        binding.closingCardDiff.setText(getString(
                R.string.cash_closing_diff_format, Formats.formatMoney(cntCard - expCard)));
        binding.closingMercadoPagoDiff.setText(getString(
                R.string.cash_closing_diff_format, Formats.formatMoney(cntMp - expMp)));

        long totalExpected = expCash + expTransfer + expCard + expMp;
        long totalCounted = cntCash + cntTransfer + cntCard + cntMp;
        binding.closingTotalExpected.setMoneyCents(totalExpected);
        binding.closingTotalCounted.setMoneyCents(totalCounted);
        binding.closingTotalDiff.setMoneyCents(totalCounted - totalExpected);
    }

    private void submitClosing() {
        Map<PaymentMethod, Money> countedMap = new EnumMap<>(PaymentMethod.class);
        if (!readNonNegative(binding.inputClosingCashCounted, PaymentMethod.CASH, countedMap)
                || !readNonNegative(binding.inputClosingTransferCounted, PaymentMethod.TRANSFER, countedMap)
                || !readNonNegative(binding.inputClosingCardCounted, PaymentMethod.CARD, countedMap)
                || !readNonNegative(binding.inputClosingMercadoPagoCounted, PaymentMethod.MERCADO_PAGO, countedMap)) {
            Toast.makeText(requireContext(), R.string.cash_closing_validation_error, Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        String notes = binding.inputClosingNotes.getText() == null
                ? ""
                : binding.inputClosingNotes.getText().toString();
        binding.btnConfirmCashClosing.setEnabled(false);

        viewModel.closeDay(dayEpoch, countedMap, notes, success -> {
            if (binding == null) {
                return;
            }
            binding.btnConfirmCashClosing.setEnabled(true);
            if (success) {
                dismiss();
            }
        });
    }

    private static long parseOrZero(@NonNull TextInputEditText input) {
        String text = input.getText() == null ? "" : input.getText().toString().trim();
        if (text.isEmpty()) {
            return 0L;
        }
        return Formats.parsePesosInput(text).map(Money::cents).orElse(0L);
    }

    private static boolean readNonNegative(
            @NonNull TextInputEditText input,
            @NonNull PaymentMethod method,
            @NonNull Map<PaymentMethod, Money> target) {
        String text = input.getText() == null ? "" : input.getText().toString().trim();
        if (text.isEmpty()) {
            target.put(method, Money.zero());
            return true;
        }
        Optional<Money> parsed = Formats.parsePesosInput(text);
        if (!parsed.isPresent() || parsed.get().isNegative()) {
            return false;
        }
        target.put(method, parsed.get());
        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
