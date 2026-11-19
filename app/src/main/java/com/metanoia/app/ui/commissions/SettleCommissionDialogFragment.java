package com.metanoia.app.ui.commissions;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.metanoia.app.R;
import com.metanoia.app.databinding.DialogSettleCommissionBinding;
import com.metanoia.app.ui.cash.CashUiLabels;
import com.metanoia.app.util.Formats;
import com.metanoia.domain.model.PaymentMethod;

/**
 * Bottom sheet de confirmación para registrar el pago de comisión en caja y cerrar el período.
 */
public class SettleCommissionDialogFragment extends BottomSheetDialogFragment {

    private static final String ARG_PRO_NAME = "pro_name";
    private static final String ARG_FROM_DAY = "from_day";
    private static final String ARG_TO_DAY = "to_day";
    private static final String ARG_BASE_CENTS = "base_cents";
    private static final String ARG_COMM_CENTS = "comm_cents";

    private DialogSettleCommissionBinding binding;
    private CommissionsViewModel viewModel;

    public static SettleCommissionDialogFragment newInstance(
            @NonNull String professionalName,
            long fromDay,
            long toDay,
            long baseCents,
            long commissionCents
    ) {
        Bundle args = new Bundle();
        args.putString(ARG_PRO_NAME, professionalName);
        args.putLong(ARG_FROM_DAY, fromDay);
        args.putLong(ARG_TO_DAY, toDay);
        args.putLong(ARG_BASE_CENTS, baseCents);
        args.putLong(ARG_COMM_CENTS, commissionCents);
        SettleCommissionDialogFragment fragment = new SettleCommissionDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = DialogSettleCommissionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireParentFragment()).get(CommissionsViewModel.class);

        Bundle args = requireArguments();
        String proName = args.getString(ARG_PRO_NAME, "");
        long fromDay = args.getLong(ARG_FROM_DAY);
        long toDay = args.getLong(ARG_TO_DAY);
        long baseCents = args.getLong(ARG_BASE_CENTS);
        long commCents = args.getLong(ARG_COMM_CENTS);

        String periodLabel = getString(
                R.string.commissions_settlement_period_format,
                Formats.formatDate(fromDay),
                Formats.formatDate(toDay));
        binding.settleCommissionContext.setText(getString(
                R.string.commissions_settle_dialog_summary_format, proName, periodLabel));
        binding.settleCommissionBase.setMoneyCents(baseCents);
        binding.settleCommissionAmount.setMoneyCents(commCents);
        CashUiLabels.populatePaymentMethodChips(
                requireContext(), binding.settleCommissionMethodGroup, PaymentMethod.CASH);

        binding.btnConfirmSettleCommission.setOnClickListener(v -> {
            PaymentMethod method = CashUiLabels.selectedPaymentMethod(
                    binding.settleCommissionMethodGroup);
            binding.btnConfirmSettleCommission.setEnabled(false);
            viewModel.settleCurrentPeriod(method, success -> {
                if (binding == null) {
                    return;
                }
                binding.btnConfirmSettleCommission.setEnabled(true);
                if (success) {
                    dismiss();
                }
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
