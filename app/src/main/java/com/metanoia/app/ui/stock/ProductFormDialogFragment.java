package com.metanoia.app.ui.stock;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.color.MaterialColors;
import com.metanoia.app.R;
import com.metanoia.app.data.db.entity.ProductEntity;
import com.metanoia.app.databinding.DialogProductFormBinding;
import com.metanoia.app.ui.common.ConfirmDialog;
import com.metanoia.app.util.Formats;
import com.metanoia.domain.model.Money;
import com.metanoia.domain.model.ProductType;
import com.metanoia.domain.model.ProductUnit;

import java.util.Optional;

/**
 * Bottom sheet reutilizable para alta, edición y baja lógica/reactivación de productos.
 */
public class ProductFormDialogFragment extends BottomSheetDialogFragment {

    private static final String ARG_ID = "id";
    private static final String ARG_NAME = "name";
    private static final String ARG_BRAND = "brand";
    private static final String ARG_TYPE = "type";
    private static final String ARG_UNIT = "unit";
    private static final String ARG_PRICE_CENTS = "price_cents";
    private static final String ARG_MIN_STOCK = "min_stock";
    private static final String ARG_ACTIVE = "active";

    private DialogProductFormBinding binding;
    private StockViewModel viewModel;

    public static ProductFormDialogFragment newCreateInstance() {
        return new ProductFormDialogFragment();
    }

    public static ProductFormDialogFragment newEditInstance(@NonNull ProductEntity existing) {
        Bundle args = new Bundle();
        args.putString(ARG_ID, existing.id);
        args.putString(ARG_NAME, existing.name);
        args.putString(ARG_BRAND, existing.brand);
        args.putString(ARG_TYPE, existing.type);
        args.putString(ARG_UNIT, existing.unit);
        if (existing.salePriceCents != null) {
            args.putLong(ARG_PRICE_CENTS, existing.salePriceCents);
        }
        args.putInt(ARG_MIN_STOCK, existing.minStock);
        args.putBoolean(ARG_ACTIVE, existing.active);
        ProductFormDialogFragment fragment = new ProductFormDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = DialogProductFormBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(StockViewModel.class);

        Bundle args = getArguments();
        String existingId = args == null ? null : args.getString(ARG_ID);
        boolean isEdit = existingId != null && !existingId.isEmpty();
        boolean isActive = args == null || args.getBoolean(ARG_ACTIVE, true);

        binding.dialogProductTitle.setText(isEdit
                ? R.string.product_form_edit_title
                : R.string.product_form_new_title);
        binding.layoutProductInitialStock.setVisibility(isEdit ? View.GONE : View.VISIBLE);

        binding.productTypeGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                updatePriceVisibility();
            }
        });

        if (isEdit && args != null) {
            binding.inputProductName.setText(args.getString(ARG_NAME, ""));
            String brand = args.getString(ARG_BRAND);
            if (brand != null) {
                binding.inputProductBrand.setText(brand);
            }
            ProductType type = StockUiLabels.parseType(args.getString(ARG_TYPE, "USE"));
            binding.productTypeGroup.check(typeButtonId(type));
            ProductUnit unit = StockUiLabels.parseUnit(args.getString(ARG_UNIT, "UNIT"));
            binding.productUnitGroup.check(unitButtonId(unit));
            if (args.containsKey(ARG_PRICE_CENTS)) {
                long cents = args.getLong(ARG_PRICE_CENTS);
                if (cents > 0L) {
                    binding.inputProductSalePrice.setText(
                            Formats.formatPesosPlain(Money.ofCents(cents)));
                }
            }
            binding.inputProductMinStock.setText(String.valueOf(args.getInt(ARG_MIN_STOCK, 0)));
            setupActiveToggleButton(existingId, isActive);
        } else {
            binding.productTypeGroup.check(R.id.btnProductTypeUse);
            binding.productUnitGroup.check(R.id.btnProductUnitUnit);
            binding.inputProductMinStock.setText("0");
            binding.inputProductInitialStock.setText("0");
            binding.btnToggleProductActive.setVisibility(View.GONE);
        }
        updatePriceVisibility();

        Context appContext = requireContext().getApplicationContext();
        viewModel.observeMessage().observe(getViewLifecycleOwner(), event -> {
            Integer msgRes = event == null ? null : event.getContentIfNotHandled();
            if (msgRes != null) {
                Toast.makeText(appContext, msgRes, Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnSaveProduct.setOnClickListener(v -> submit(existingId, isEdit));
    }

    private void setupActiveToggleButton(@NonNull String productId, boolean isActive) {
        binding.btnToggleProductActive.setVisibility(View.VISIBLE);
        if (isActive) {
            binding.btnToggleProductActive.setText(R.string.product_deactivate_btn);
            binding.btnToggleProductActive.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.accent));
            binding.btnToggleProductActive.setOnClickListener(v -> ConfirmDialog.show(
                    requireContext(),
                    R.string.product_deactivate_confirm_title,
                    R.string.product_deactivate_confirm_message,
                    R.string.product_deactivate_btn,
                    true,
                    () -> {
                        binding.btnSaveProduct.setEnabled(false);
                        binding.btnToggleProductActive.setEnabled(false);
                        viewModel.setProductActive(productId, false, success -> {
                            if (binding == null) {
                                return;
                            }
                            binding.btnSaveProduct.setEnabled(true);
                            binding.btnToggleProductActive.setEnabled(true);
                            if (success) {
                                dismiss();
                            }
                        });
                    }
            ));
        } else {
            binding.btnToggleProductActive.setText(R.string.product_reactivate_btn);
            binding.btnToggleProductActive.setTextColor(MaterialColors.getColor(
                    binding.btnToggleProductActive,
                    androidx.appcompat.R.attr.colorPrimary));
            binding.btnToggleProductActive.setOnClickListener(v -> {
                binding.btnSaveProduct.setEnabled(false);
                binding.btnToggleProductActive.setEnabled(false);
                viewModel.setProductActive(productId, true, success -> {
                    if (binding == null) {
                        return;
                    }
                    binding.btnSaveProduct.setEnabled(true);
                    binding.btnToggleProductActive.setEnabled(true);
                    if (success) {
                        dismiss();
                    }
                });
            });
        }
    }

    private void updatePriceVisibility() {
        ProductType selectedType = selectedType();
        binding.layoutProductSalePrice.setVisibility(
                selectedType.isForSale() ? View.VISIBLE : View.GONE);
    }

    private void submit(@Nullable String existingId, boolean isEdit) {
        String name = textOf(binding.inputProductName);
        String brand = textOf(binding.inputProductBrand);
        ProductType type = selectedType();
        ProductUnit unit = selectedUnit();

        Money salePrice = null;
        if (type.isForSale()) {
            Optional<Money> parsedPrice = Formats.parsePesosInput(
                    textOf(binding.inputProductSalePrice));
            if (!parsedPrice.isPresent() || !parsedPrice.get().isPositive()) {
                Toast.makeText(requireContext(), R.string.product_error_validation,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            salePrice = parsedPrice.get();
        }

        Integer minStock = parseNonNegativeInt(textOf(binding.inputProductMinStock), 0);
        if (minStock == null) {
            Toast.makeText(requireContext(), R.string.product_error_validation,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        int initialStock = 0;
        if (!isEdit) {
            Integer parsedInit = parseNonNegativeInt(
                    textOf(binding.inputProductInitialStock), 0);
            if (parsedInit == null) {
                Toast.makeText(requireContext(), R.string.product_error_validation,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            initialStock = parsedInit;
        }

        binding.btnSaveProduct.setEnabled(false);
        viewModel.saveProduct(
                existingId,
                name,
                brand,
                type,
                unit,
                salePrice,
                minStock,
                initialStock,
                success -> {
                    if (binding == null) {
                        return;
                    }
                    binding.btnSaveProduct.setEnabled(true);
                    if (success) {
                        dismiss();
                    }
                }
        );
    }

    @NonNull
    private ProductType selectedType() {
        int id = binding.productTypeGroup.getCheckedButtonId();
        if (id == R.id.btnProductTypeSale) {
            return ProductType.SALE;
        }
        if (id == R.id.btnProductTypeBoth) {
            return ProductType.BOTH;
        }
        return ProductType.USE;
    }

    @NonNull
    private ProductUnit selectedUnit() {
        int id = binding.productUnitGroup.getCheckedButtonId();
        if (id == R.id.btnProductUnitGram) {
            return ProductUnit.GRAM;
        }
        if (id == R.id.btnProductUnitMilliliter) {
            return ProductUnit.MILLILITER;
        }
        return ProductUnit.UNIT;
    }

    private static int typeButtonId(@NonNull ProductType type) {
        if (type == ProductType.SALE) {
            return R.id.btnProductTypeSale;
        }
        if (type == ProductType.BOTH) {
            return R.id.btnProductTypeBoth;
        }
        return R.id.btnProductTypeUse;
    }

    private static int unitButtonId(@NonNull ProductUnit unit) {
        if (unit == ProductUnit.GRAM) {
            return R.id.btnProductUnitGram;
        }
        if (unit == ProductUnit.MILLILITER) {
            return R.id.btnProductUnitMilliliter;
        }
        return R.id.btnProductUnitUnit;
    }

    @Nullable
    private static Integer parseNonNegativeInt(@NonNull String raw, int defaultWhenEmpty) {
        if (raw.isEmpty()) {
            return defaultWhenEmpty;
        }
        try {
            int value = Integer.parseInt(raw);
            return value >= 0 ? value : null;
        } catch (NumberFormatException ex) {
            return null;
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
