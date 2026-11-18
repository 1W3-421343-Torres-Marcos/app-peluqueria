package com.metanoia.app.ui.cash;

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
import com.metanoia.app.databinding.DialogCashMovementFormBinding;
import com.metanoia.app.util.Formats;
import com.metanoia.domain.model.CashMovementType;
import com.metanoia.domain.model.Money;
import com.metanoia.domain.model.PaymentMethod;

import java.util.Optional;

/**
 * Bottom sheet para registrar una venta suelta, otro ingreso o un gasto en caja.
 */
public class CashMovementFormDialogFragment extends BottomSheetDialogFragment {

    private static final String ARG_IS_EXPENSE = "is_expense";

    private DialogCashMovementFormBinding binding;
    private CashViewModel viewModel;
    private boolean isExpense;

    public static CashMovementFormDialogFragment newInstance(boolean isExpense) {
        CashMovementFormDialogFragment fragment = new CashMovementFormDialogFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_EXPENSE, isExpense);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = DialogCashMovementFormBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        isExpense = getArguments() != null && getArguments().getBoolean(ARG_IS_EXPENSE, false);
        viewModel = new ViewModelProvider(requireParentFragment()).get(CashViewModel.class);

        binding.cashMovementFormTitle.setText(isExpense
                ? R.string.cash_form_expense_title
                : R.string.cash_form_income_title);
        binding.cashMovementIncomeKindSection.setVisibility(isExpense ? View.GONE : View.VISIBLE);
        CashUiLabels.populatePaymentMethodChips(
                requireContext(), binding.cashMovementMethodGroup, PaymentMethod.CASH);

        binding.btnSaveCashMovement.setOnClickListener(v -> submit());
    }

    private void submit() {
        CashMovementType type;
        if (isExpense) {
            type = CashMovementType.EXPENSE;
        } else {
            int checkedKindId = binding.cashMovementIncomeKindGroup.getCheckedButtonId();
            type = checkedKindId == R.id.btnCashKindOtherIncome
                    ? CashMovementType.OTHER_INCOME
                    : CashMovementType.PRODUCT_SALE;
        }
        String concept = binding.inputCashMovementConcept.getText() == null
                ? ""
                : binding.inputCashMovementConcept.getText().toString();
        String amountText = binding.inputCashMovementAmount.getText() == null
                ? ""
                : binding.inputCashMovementAmount.getText().toString();
        Optional<Money> parsed = Formats.parsePesosInput(amountText);
        if (!parsed.isPresent() || !parsed.get().isPositive()) {
            Toast.makeText(requireContext(), R.string.cash_form_validation_error, Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        PaymentMethod method = CashUiLabels.selectedPaymentMethod(binding.cashMovementMethodGroup);
        binding.btnSaveCashMovement.setEnabled(false);

        viewModel.recordMovement(type, method, parsed.get(), concept, success -> {
            if (binding == null) {
                return;
            }
            binding.btnSaveCashMovement.setEnabled(true);
            if (success) {
                dismiss();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
