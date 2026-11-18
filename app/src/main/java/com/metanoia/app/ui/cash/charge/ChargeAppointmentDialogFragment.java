package com.metanoia.app.ui.cash.charge;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.metanoia.app.R;
import com.metanoia.app.databinding.DialogChargeAppointmentBinding;
import com.metanoia.app.ui.cash.CashUiLabels;
import com.metanoia.app.ui.cash.charge.ChargeAppointmentViewModel.ChargeUiSummary;
import com.metanoia.app.util.Formats;
import com.metanoia.domain.model.Appointment;
import com.metanoia.domain.model.Money;
import com.metanoia.domain.model.PaymentMethod;
import com.metanoia.domain.rules.CashPaymentSupport.PaymentSplit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Bottom sheet para cobrar el saldo de un turno (simple, parcial, dividido o con ajuste).
 */
public class ChargeAppointmentDialogFragment extends BottomSheetDialogFragment {

    private static final String ARG_APPOINTMENT_ID = "appointment_id";
    private static final String ARG_COMPLETE_AND_CHARGE = "complete_and_charge";

    private DialogChargeAppointmentBinding binding;
    private ChargeAppointmentViewModel viewModel;
    private String appointmentId;
    private boolean completeAndCharge;
    @Nullable
    private ChargeUiSummary loadedSummary;

    public static ChargeAppointmentDialogFragment newInstance(@NonNull String appointmentId) {
        return newInstance(appointmentId, false);
    }

    public static ChargeAppointmentDialogFragment newInstance(
            @NonNull String appointmentId,
            boolean completeAndCharge
    ) {
        ChargeAppointmentDialogFragment fragment = new ChargeAppointmentDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_APPOINTMENT_ID, appointmentId);
        args.putBoolean(ARG_COMPLETE_AND_CHARGE, completeAndCharge);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = DialogChargeAppointmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        appointmentId = getArguments() == null
                ? null
                : getArguments().getString(ARG_APPOINTMENT_ID);
        if (appointmentId == null || appointmentId.trim().isEmpty()) {
            dismiss();
            return;
        }
        appointmentId = appointmentId.trim();
        completeAndCharge = getArguments() != null
                && getArguments().getBoolean(ARG_COMPLETE_AND_CHARGE, false);

        CashUiLabels.populatePaymentMethodChips(
                requireContext(), binding.chargePrimaryMethodGroup, PaymentMethod.CASH);
        CashUiLabels.populatePaymentMethodChips(
                requireContext(), binding.chargeSecondaryMethodGroup, PaymentMethod.TRANSFER);
        binding.chargeAdjustmentSignGroup.check(R.id.btnAdjustmentDiscount);

        binding.switchChargeAdjustment.setOnCheckedChangeListener((buttonView, isChecked) ->
                binding.chargeAdjustmentContainer.setVisibility(
                        isChecked ? View.VISIBLE : View.GONE));
        binding.switchSplitPayment.setOnCheckedChangeListener((buttonView, isChecked) ->
                binding.chargeSecondaryContainer.setVisibility(
                        isChecked ? View.VISIBLE : View.GONE));

        viewModel = new ViewModelProvider(this).get(ChargeAppointmentViewModel.class);
        viewModel.observeSummary().observe(getViewLifecycleOwner(), summary -> {
            if (summary != null) {
                loadedSummary = summary;
                bindSummary(summary);
            }
        });
        viewModel.observeFeedback().observe(getViewLifecycleOwner(), event -> {
            Integer resId = event.getContentIfNotHandled();
            if (resId != null) {
                Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show();
            }
        });
        viewModel.observeCloseSheet().observe(getViewLifecycleOwner(), event -> {
            if (Boolean.TRUE.equals(event.getContentIfNotHandled())) {
                dismiss();
            }
        });

        binding.btnConfirmCharge.setOnClickListener(v -> submitCharge());
        viewModel.loadAppointment(appointmentId);
    }

    private void bindSummary(@NonNull ChargeUiSummary summary) {
        Appointment apt = summary.appointment;
        binding.txtChargeTitle.setText(completeAndCharge
                ? R.string.charge_complete_dialog_title
                : R.string.charge_dialog_title);
        binding.txtChargeClient.setText(summary.clientName);
        binding.txtChargeSubtotalSummary.setText(getString(
                R.string.charge_summary_format,
                Formats.formatMoney(apt.itemsSubtotal()),
                Formats.formatMoney(summary.balance.paid())));
        binding.txtChargeRemainingBalance.setMoney(summary.balance.balance());
        binding.inputPrimaryChargeAmount.setText(
                Formats.formatPesosPlain(summary.balance.balance()));

        if (!apt.adjustment().isZero()) {
            binding.switchChargeAdjustment.setChecked(true);
            binding.chargeAdjustmentSignGroup.check(apt.adjustment().isNegative()
                    ? R.id.btnAdjustmentDiscount
                    : R.id.btnAdjustmentSurcharge);
            binding.inputAdjustmentAmount.setText(
                    Formats.formatPesosPlain(Money.ofCents(Math.abs(apt.adjustment().cents()))));
        }
    }

    private void submitCharge() {
        if (loadedSummary == null) {
            return;
        }
        Money requestedAdjustment = null;
        String adjustmentReason = null;
        if (binding.switchChargeAdjustment.isChecked()) {
            String rawAdj = textOf(binding.inputAdjustmentAmount);
            Optional<Money> parsedAdj = Formats.parsePesosInput(rawAdj);
            if (!parsedAdj.isPresent() || parsedAdj.get().isNegative()) {
                Toast.makeText(requireContext(), R.string.charge_error_validation,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            boolean isDiscount = binding.chargeAdjustmentSignGroup.getCheckedButtonId()
                    == R.id.btnAdjustmentDiscount;
            long signedCents = isDiscount
                    ? -parsedAdj.get().cents()
                    : parsedAdj.get().cents();
            requestedAdjustment = Money.ofCents(signedCents);
            adjustmentReason = textOf(binding.inputAdjustmentReason);
        }

        Optional<Money> primaryAmount = Formats.parsePesosInput(
                textOf(binding.inputPrimaryChargeAmount));
        if (!primaryAmount.isPresent() || !primaryAmount.get().isPositive()) {
            Toast.makeText(requireContext(), R.string.charge_error_validation, Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        List<PaymentSplit> splits = new ArrayList<>();
        splits.add(new PaymentSplit(
                CashUiLabels.selectedPaymentMethod(binding.chargePrimaryMethodGroup),
                primaryAmount.get()));

        if (binding.switchSplitPayment.isChecked()) {
            Optional<Money> secondaryAmount = Formats.parsePesosInput(
                    textOf(binding.inputSecondaryChargeAmount));
            if (!secondaryAmount.isPresent() || !secondaryAmount.get().isPositive()) {
                Toast.makeText(requireContext(), R.string.charge_error_validation,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            splits.add(new PaymentSplit(
                    CashUiLabels.selectedPaymentMethod(binding.chargeSecondaryMethodGroup),
                    secondaryAmount.get()));
        }

        if (completeAndCharge) {
            viewModel.submitCompleteAndCharge(
                    appointmentId, requestedAdjustment, adjustmentReason, splits);
        } else {
            viewModel.submitCharge(
                    appointmentId, requestedAdjustment, adjustmentReason, splits);
        }
    }

    @NonNull
    private static String textOf(@NonNull android.widget.TextView view) {
        CharSequence cs = view.getText();
        return cs == null ? "" : cs.toString().trim();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
