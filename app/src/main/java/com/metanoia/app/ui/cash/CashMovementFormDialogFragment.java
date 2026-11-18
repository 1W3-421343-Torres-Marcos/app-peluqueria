package com.metanoia.app.ui.cash;

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
import com.google.android.material.chip.Chip;
import com.metanoia.app.R;
import com.metanoia.app.data.db.entity.ProductEntity;
import com.metanoia.app.databinding.DialogCashMovementFormBinding;
import com.metanoia.app.ui.stock.StockUiLabels;
import com.metanoia.app.util.Formats;
import com.metanoia.domain.model.CashMovementType;
import com.metanoia.domain.model.Money;
import com.metanoia.domain.model.PaymentMethod;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Bottom sheet para registrar una venta suelta (manual o desde catálogo de stock),
 * otro ingreso o un gasto en caja.
 */
public class CashMovementFormDialogFragment extends BottomSheetDialogFragment {

    private static final String ARG_IS_EXPENSE = "is_expense";

    private DialogCashMovementFormBinding binding;
    private CashViewModel viewModel;
    private boolean isExpense;
    private List<ProductEntity> saleProducts = Collections.emptyList();
    @Nullable
    private ProductEntity selectedProduct;
    private boolean updatingChips;

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

        if (!isExpense) {
            binding.cashMovementIncomeKindGroup.addOnButtonCheckedListener(
                    (group, checkedId, isChecked) -> {
                        if (isChecked) {
                            updateProductSectionVisibility();
                        }
                    });
            binding.cashMovementProductGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (updatingChips) {
                    return;
                }
                ProductEntity picked = resolveCheckedProduct();
                String currentId = selectedProduct == null ? null : selectedProduct.id;
                String pickedId = picked == null ? null : picked.id;
                if (!java.util.Objects.equals(currentId, pickedId)) {
                    applySelectedProduct(picked);
                }
            });
            binding.inputCashMovementQuantity.setText("1");
            binding.inputCashMovementQuantity.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (selectedProduct != null) {
                        syncFieldsWithSelectedProduct();
                    }
                }
            });
            viewModel.getActiveSaleProducts().observe(getViewLifecycleOwner(), products -> {
                saleProducts = products == null ? Collections.emptyList() : products;
                populateSaleProductChips();
                updateProductSectionVisibility();
            });
        }

        binding.btnSaveCashMovement.setOnClickListener(v -> submit());
    }

    private void populateSaleProductChips() {
        updatingChips = true;
        try {
            binding.cashMovementProductGroup.removeAllViews();

            String previouslySelectedId = selectedProduct == null ? null : selectedProduct.id;
            ProductEntity matchedProduct = null;

            Chip manualChip = new Chip(requireContext());
            manualChip.setId(View.generateViewId());
            manualChip.setCheckable(true);
            manualChip.setText(R.string.cash_form_product_manual);
            manualChip.setTag(null);
            binding.cashMovementProductGroup.addView(manualChip);

            for (ProductEntity product : saleProducts) {
                Chip chip = new Chip(requireContext());
                chip.setId(View.generateViewId());
                chip.setCheckable(true);
                String stockStr = StockUiLabels.formatQuantityWithUnit(
                        requireContext(),
                        product.currentStock,
                        StockUiLabels.parseUnit(product.unit));
                chip.setText(getString(R.string.cash_form_product_chip_format, product.name, stockStr));
                chip.setTag(product);
                binding.cashMovementProductGroup.addView(chip);
                if (product.id.equals(previouslySelectedId)) {
                    matchedProduct = product;
                    chip.setChecked(true);
                }
            }

            if (matchedProduct == null) {
                manualChip.setChecked(true);
                if (selectedProduct != null) {
                    applySelectedProduct(null);
                }
            } else {
                selectedProduct = matchedProduct;
                syncFieldsWithSelectedProduct();
            }
        } finally {
            updatingChips = false;
        }
    }

    @Nullable
    private ProductEntity resolveCheckedProduct() {
        int checkedId = binding.cashMovementProductGroup.getCheckedChipId();
        if (checkedId == View.NO_ID) {
            return null;
        }
        View child = binding.cashMovementProductGroup.findViewById(checkedId);
        if (child != null && child.getTag() instanceof ProductEntity) {
            return (ProductEntity) child.getTag();
        }
        return null;
    }

    private void applySelectedProduct(@Nullable ProductEntity product) {
        selectedProduct = product;
        if (product == null) {
            binding.layoutCashMovementQuantity.setVisibility(View.GONE);
            binding.cashMovementStockWarning.setVisibility(View.GONE);
            binding.inputCashMovementConcept.setEnabled(true);
            return;
        }
        if (parsePositiveQuantity() <= 0) {
            binding.inputCashMovementQuantity.setText("1");
        }
        binding.layoutCashMovementQuantity.setVisibility(View.VISIBLE);
        binding.inputCashMovementConcept.setEnabled(false);
        syncFieldsWithSelectedProduct();
    }

    private void syncFieldsWithSelectedProduct() {
        if (selectedProduct == null) {
            return;
        }
        int qty = parsePositiveQuantity();
        if (qty <= 0) {
            binding.cashMovementStockWarning.setVisibility(View.GONE);
            return;
        }
        binding.inputCashMovementConcept.setText(getString(
                R.string.cash_form_product_concept_format, selectedProduct.name, qty));
        if (selectedProduct.salePriceCents != null && selectedProduct.salePriceCents > 0L) {
            Money total = Money.ofCents(selectedProduct.salePriceCents).multiply(qty);
            binding.inputCashMovementAmount.setText(Formats.formatPesosPlain(total));
        }
        int remaining = selectedProduct.currentStock - qty;
        if (remaining < 0) {
            String remainingStr = StockUiLabels.formatQuantityWithUnit(
                    requireContext(),
                    remaining,
                    StockUiLabels.parseUnit(selectedProduct.unit));
            binding.cashMovementStockWarning.setText(
                    getString(R.string.cash_form_product_negative_warning, remainingStr));
            binding.cashMovementStockWarning.setVisibility(View.VISIBLE);
        } else {
            binding.cashMovementStockWarning.setVisibility(View.GONE);
        }
    }

    private void updateProductSectionVisibility() {
        boolean show = !isExpense
                && binding.cashMovementIncomeKindGroup.getCheckedButtonId()
                        != R.id.btnCashKindOtherIncome
                && !saleProducts.isEmpty();
        binding.cashMovementProductSection.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show && selectedProduct != null) {
            selectedProduct = null;
            binding.inputCashMovementConcept.setEnabled(true);
        }
    }

    private int parsePositiveQuantity() {
        if (binding.inputCashMovementQuantity.getText() == null) {
            return 0;
        }
        String raw = binding.inputCashMovementQuantity.getText().toString().trim();
        if (raw.isEmpty()) {
            return 0;
        }
        try {
            int qty = Integer.parseInt(raw);
            return qty > 0 ? qty : 0;
        } catch (NumberFormatException ex) {
            return 0;
        }
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

        if (type == CashMovementType.PRODUCT_SALE && selectedProduct != null) {
            int qty = parsePositiveQuantity();
            if (qty <= 0) {
                Toast.makeText(requireContext(), R.string.cash_form_validation_error,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            binding.btnSaveCashMovement.setEnabled(false);
            viewModel.sellProduct(selectedProduct.id, qty, method, parsed.get(), success -> {
                if (binding == null) {
                    return;
                }
                binding.btnSaveCashMovement.setEnabled(true);
                if (success) {
                    dismiss();
                }
            });
            return;
        }

        String concept = binding.inputCashMovementConcept.getText() == null
                ? ""
                : binding.inputCashMovementConcept.getText().toString();
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
