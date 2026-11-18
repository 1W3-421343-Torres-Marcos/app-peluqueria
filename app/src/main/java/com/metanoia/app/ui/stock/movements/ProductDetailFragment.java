package com.metanoia.app.ui.stock.movements;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.metanoia.app.R;
import com.metanoia.app.data.db.entity.ProductEntity;
import com.metanoia.app.databinding.FragmentProductDetailBinding;
import com.metanoia.app.ui.common.StatusChip;
import com.metanoia.app.ui.stock.ProductFormDialogFragment;
import com.metanoia.app.ui.stock.StockUiLabels;
import com.metanoia.domain.model.ProductType;
import com.metanoia.domain.model.ProductUnit;
import com.metanoia.domain.model.StockMovementType;
import com.metanoia.domain.rules.LowStockRule;

/**
 * Pantalla de detalle de producto con acciones Ingreso / Uso / Ajuste e historial de movimientos.
 */
public class ProductDetailFragment extends Fragment {

    public static final String ARG_PRODUCT_ID = "product_id";
    private static final String TAG_EDIT_PRODUCT = "edit_product_form";
    private static final String TAG_STOCK_MOVEMENT = "stock_movement_form";

    private FragmentProductDetailBinding binding;
    private ProductDetailViewModel viewModel;
    private StockMovementsAdapter adapter;
    @Nullable
    private ProductEntity currentProduct;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentProductDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String productId = getArguments() == null ? null : getArguments().getString(ARG_PRODUCT_ID);
        if (productId == null || productId.trim().isEmpty()) {
            Toast.makeText(requireContext(), R.string.product_detail_not_found, Toast.LENGTH_SHORT)
                    .show();
            NavHostFragment.findNavController(this).navigateUp();
            return;
        }

        viewModel = new ViewModelProvider(this).get(ProductDetailViewModel.class);
        viewModel.setProductId(productId);

        adapter = new StockMovementsAdapter();
        binding.productMovementsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.productMovementsRecycler.setAdapter(adapter);

        binding.btnProductDetailEdit.setOnClickListener(v -> {
            if (currentProduct != null) {
                ProductFormDialogFragment.newEditInstance(currentProduct)
                        .show(getChildFragmentManager(), TAG_EDIT_PRODUCT);
            }
        });
        binding.btnStockIn.setOnClickListener(v -> openMovementDialog(StockMovementType.IN));
        binding.btnStockOutUse.setOnClickListener(v -> openMovementDialog(StockMovementType.OUT_USE));
        binding.btnStockAdjust.setOnClickListener(v -> openMovementDialog(StockMovementType.ADJUSTMENT));

        viewModel.observeProduct().observe(getViewLifecycleOwner(), entity -> {
            if (entity == null) {
                return;
            }
            currentProduct = entity;
            bindProduct(entity);
        });

        viewModel.observeMovements().observe(getViewLifecycleOwner(), list -> {
            boolean empty = list == null || list.isEmpty();
            binding.productMovementsEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.productMovementsRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
            adapter.submitList(empty ? null : list);
        });

        viewModel.observeMessage().observe(getViewLifecycleOwner(), event -> {
            Integer msgRes = event == null ? null : event.getContentIfNotHandled();
            if (msgRes != null) {
                Toast.makeText(requireContext(), msgRes, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindProduct(@NonNull ProductEntity entity) {
        ProductType type = StockUiLabels.parseType(entity.type);
        ProductUnit unit = StockUiLabels.parseUnit(entity.unit);
        adapter.setUnit(unit);

        binding.productDetailName.setText(entity.name);
        String typeLabel = StockUiLabels.typeLabel(requireContext(), type);
        if (entity.brand != null && !entity.brand.trim().isEmpty()) {
            binding.productDetailMeta.setText(getString(
                    R.string.stock_meta_with_brand_format, entity.brand, typeLabel));
        } else {
            binding.productDetailMeta.setText(typeLabel);
        }

        if (type.isForSale() && entity.salePriceCents != null) {
            binding.productDetailPrice.setMoneyCents(entity.salePriceCents);
            binding.productDetailPrice.setVisibility(View.VISIBLE);
        } else {
            binding.productDetailPrice.setVisibility(View.GONE);
        }

        binding.productDetailCurrentStock.setText(
                StockUiLabels.formatQuantityWithUnit(requireContext(), entity.currentStock, unit));
        binding.productDetailMinStock.setText(getString(
                R.string.product_detail_min_stock_label,
                StockUiLabels.formatQuantityWithUnit(requireContext(), entity.minStock, unit)));

        boolean isLow = entity.active
                && LowStockRule.isLowStock(entity.currentStock, entity.minStock);
        if (isLow) {
            binding.productDetailLowChip.setVariant(StatusChip.Variant.LOW_STOCK);
            binding.productDetailLowChip.setVisibility(View.VISIBLE);
        } else {
            binding.productDetailLowChip.setVisibility(View.GONE);
        }

        binding.productDetailInactiveBadge.setVisibility(entity.active ? View.GONE : View.VISIBLE);
        if (LowStockRule.isNegativeStock(entity.currentStock)) {
            binding.productDetailNegativeBanner.setVisibility(View.VISIBLE);
            binding.productDetailNegativeBanner.setMessage(R.string.product_detail_negative_warning);
            binding.productDetailNegativeBanner.setAction((CharSequence) null, null);
        } else {
            binding.productDetailNegativeBanner.setVisibility(View.GONE);
        }

        binding.btnStockIn.setEnabled(entity.active);
        binding.btnStockOutUse.setEnabled(entity.active);
        binding.btnStockAdjust.setEnabled(entity.active);
    }

    private void openMovementDialog(@NonNull StockMovementType mode) {
        if (currentProduct == null || !currentProduct.active) {
            return;
        }
        StockMovementFormDialogFragment.newInstance(
                currentProduct.id,
                currentProduct.name,
                StockUiLabels.parseUnit(currentProduct.unit),
                currentProduct.currentStock,
                mode
        ).show(getChildFragmentManager(), TAG_STOCK_MOVEMENT);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
