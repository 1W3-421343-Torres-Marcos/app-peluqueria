package com.metanoia.app.ui.stock.movements;

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
import com.metanoia.app.R;
import com.metanoia.app.databinding.DialogStockMovementFormBinding;
import com.metanoia.app.ui.stock.StockUiLabels;
import com.metanoia.domain.model.ProductUnit;
import com.metanoia.domain.model.StockMovementType;

/**
 * Bottom sheet para registrar ingreso, egreso por uso o ajuste auditado con motivo obligatorio.
 */
public class StockMovementFormDialogFragment extends BottomSheetDialogFragment {

    private static final String ARG_PRODUCT_ID = "product_id";
    private static final String ARG_PRODUCT_NAME = "product_name";
    private static final String ARG_UNIT = "unit";
    private static final String ARG_CURRENT_STOCK = "current_stock";
    private static final String ARG_MODE = "mode";

    private DialogStockMovementFormBinding binding;
    private ProductDetailViewModel viewModel;
    private StockMovementType mode = StockMovementType.IN;
    private ProductUnit unit = ProductUnit.UNIT;
    private int currentStock;

    public static StockMovementFormDialogFragment newInstance(
            @NonNull String productId,
            @NonNull String productName,
            @NonNull ProductUnit unit,
            int currentStock,
            @NonNull StockMovementType mode
    ) {
        Bundle args = new Bundle();
        args.putString(ARG_PRODUCT_ID, productId);
        args.putString(ARG_PRODUCT_NAME, productName);
        args.putString(ARG_UNIT, unit.name());
        args.putInt(ARG_CURRENT_STOCK, currentStock);
        args.putString(ARG_MODE, mode.name());
        StockMovementFormDialogFragment fragment = new StockMovementFormDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = DialogStockMovementFormBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireParentFragment()).get(ProductDetailViewModel.class);

        Bundle args = requireArguments();
        String productId = args.getString(ARG_PRODUCT_ID, "");
        String productName = args.getString(ARG_PRODUCT_NAME, "");
        unit = StockUiLabels.parseUnit(args.getString(ARG_UNIT, ProductUnit.UNIT.name()));
        currentStock = args.getInt(ARG_CURRENT_STOCK, 0);
        mode = StockUiLabels.parseMovementType(args.getString(ARG_MODE, StockMovementType.IN.name()));

        binding.dialogStockMovementProduct.setText(productName);
        binding.layoutStockMovementQty.setHint(getString(
                R.string.stock_movement_quantity_label,
                StockUiLabels.unitShortLabel(requireContext(), unit)));

        if (mode == StockMovementType.IN) {
            binding.dialogStockMovementTitle.setText(R.string.stock_movement_form_in_title);
            binding.layoutStockAdjustDirection.setVisibility(View.GONE);
            binding.layoutStockMovementReason.setHint(R.string.stock_movement_reason_optional_label);
        } else if (mode == StockMovementType.OUT_USE) {
            binding.dialogStockMovementTitle.setText(R.string.stock_movement_form_out_use_title);
            binding.layoutStockAdjustDirection.setVisibility(View.GONE);
            binding.layoutStockMovementReason.setHint(R.string.stock_movement_reason_optional_label);
        } else {
            mode = StockMovementType.ADJUSTMENT;
            binding.dialogStockMovementTitle.setText(R.string.stock_movement_form_adjust_title);
            binding.layoutStockAdjustDirection.setVisibility(View.VISIBLE);
            binding.stockAdjustDirectionGroup.check(R.id.btnStockAdjustSubtract);
            binding.layoutStockMovementReason.setHint(R.string.stock_movement_reason_required_label);
        }

        binding.stockAdjustDirectionGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                updateNegativePreview();
            }
        });
        binding.inputStockMovementQty.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateNegativePreview();
            }
        });

        updateNegativePreview();
        binding.btnSaveStockMovement.setOnClickListener(v -> submit(productId));
    }

    private void updateNegativePreview() {
        if (binding == null) {
            return;
        }
        int qty = parsePositiveInt();
        if (qty <= 0) {
            binding.stockMovementNegativePreview.setVisibility(View.GONE);
            return;
        }
        int delta = resolveSignedDelta(qty);
        int resulting = currentStock + delta;
        if (resulting < 0) {
            binding.stockMovementNegativePreview.setVisibility(View.VISIBLE);
            binding.stockMovementNegativePreview.setText(getString(
                    R.string.stock_movement_negative_preview,
                    StockUiLabels.formatQuantityWithUnit(requireContext(), resulting, unit)));
        } else {
            binding.stockMovementNegativePreview.setVisibility(View.GONE);
        }
    }

    private void submit(@NonNull String productId) {
        int qty = parsePositiveInt();
        if (qty <= 0) {
            Toast.makeText(requireContext(), R.string.stock_movement_error_validation,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        String reason = binding.inputStockMovementReason.getText() == null
                ? ""
                : binding.inputStockMovementReason.getText().toString().trim();
        if (mode == StockMovementType.ADJUSTMENT && reason.length() < 2) {
            Toast.makeText(requireContext(), R.string.stock_movement_error_validation,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        int signedDelta = resolveSignedDelta(qty);
        binding.btnSaveStockMovement.setEnabled(false);
        viewModel.recordMovement(productId, mode, signedDelta, reason, success -> {
            if (binding == null) {
                return;
            }
            binding.btnSaveStockMovement.setEnabled(true);
            if (success) {
                dismiss();
            }
        });
    }

    private int parsePositiveInt() {
        if (binding.inputStockMovementQty.getText() == null) {
            return 0;
        }
        String raw = binding.inputStockMovementQty.getText().toString().trim();
        if (raw.isEmpty()) {
            return 0;
        }
        try {
            int val = Integer.parseInt(raw);
            return val > 0 ? val : 0;
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private int resolveSignedDelta(int positiveQty) {
        if (mode == StockMovementType.IN) {
            return positiveQty;
        }
        if (mode == StockMovementType.OUT_USE) {
            return -positiveQty;
        }
        boolean isAdd = binding.stockAdjustDirectionGroup.getCheckedButtonId() == R.id.btnStockAdjustAdd;
        return isAdd ? positiveQty : -positiveQty;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
