package com.metanoia.app.ui.stock;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.metanoia.app.R;
import com.metanoia.app.data.db.entity.ProductEntity;
import com.metanoia.app.databinding.FragmentStockBinding;
import com.metanoia.app.ui.stock.StockViewModel.TypeFilter;

/**
 * Pantalla de catálogo de stock con filtro por tipo, búsqueda, alertas bajas y CRUD.
 */
public class StockFragment extends Fragment {

    private static final String TAG_PRODUCT_FORM = "product_form_dialog";

    private FragmentStockBinding binding;
    private StockViewModel viewModel;
    private ProductsAdapter adapter;
    private int latestLowStockCount;
    private boolean latestOnlyLowStock;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentStockBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(StockViewModel.class);

        adapter = new ProductsAdapter(new ProductsAdapter.OnProductActionListener() {
            @Override
            public void onProductClick(@NonNull ProductEntity product) {
                openEditProduct(product);
            }

            @Override
            public void onProductEdit(@NonNull ProductEntity product) {
                openEditProduct(product);
            }
        });

        binding.stockRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.stockRecycler.setAdapter(adapter);

        setupControls();
        observeData();
    }

    private void setupControls() {
        binding.stockSearchField.setOnQueryChangeListener(query -> viewModel.setSearchQuery(query));
        binding.stockTypeFilterGroup.check(R.id.btnStockFilterAll);
        binding.stockTypeFilterGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            if (checkedId == R.id.btnStockFilterUse) {
                viewModel.setTypeFilter(TypeFilter.USE);
            } else if (checkedId == R.id.btnStockFilterSale) {
                viewModel.setTypeFilter(TypeFilter.SALE);
            } else {
                viewModel.setTypeFilter(TypeFilter.ALL);
            }
        });
        binding.switchStockIncludeInactive.setOnCheckedChangeListener(
                (buttonView, isChecked) -> viewModel.setIncludeInactive(isChecked));
        binding.fabNewProduct.setOnClickListener(v ->
                ProductFormDialogFragment.newCreateInstance()
                        .show(getChildFragmentManager(), TAG_PRODUCT_FORM));
    }

    private void observeData() {
        viewModel.observeLowStockCount().observe(getViewLifecycleOwner(), count -> {
            latestLowStockCount = count == null ? 0 : count;
            if (latestLowStockCount <= 0) {
                viewModel.clearOnlyLowStock();
            }
            renderLowStockBanner();
        });

        viewModel.observeOnlyLowStock().observe(getViewLifecycleOwner(), onlyLow -> {
            latestOnlyLowStock = Boolean.TRUE.equals(onlyLow);
            renderLowStockBanner();
        });

        viewModel.observeProducts().observe(getViewLifecycleOwner(), list -> {
            boolean empty = list == null || list.isEmpty();
            binding.stockEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.stockRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
            if (empty) {
                binding.stockEmptyState.setMessage(viewModel.isFilteringActive()
                        ? R.string.stock_empty_filtered
                        : R.string.stock_empty);
                adapter.submitList(null);
            } else {
                adapter.submitList(list);
            }
        });

        viewModel.observeMessage().observe(getViewLifecycleOwner(), event -> {
            Integer msgRes = event == null ? null : event.getContentIfNotHandled();
            if (msgRes != null) {
                Toast.makeText(requireContext(), msgRes, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderLowStockBanner() {
        if (binding == null) {
            return;
        }
        if (latestLowStockCount <= 0) {
            binding.stockLowBanner.setVisibility(View.GONE);
            return;
        }
        binding.stockLowBanner.setVisibility(View.VISIBLE);
        if (latestLowStockCount == 1) {
            binding.stockLowBanner.setMessage(R.string.stock_low_banner_single);
        } else {
            binding.stockLowBanner.setMessage(
                    getString(R.string.stock_low_banner_plural, latestLowStockCount));
        }
        binding.stockLowBanner.setAction(
                latestOnlyLowStock
                        ? R.string.stock_low_banner_action_all
                        : R.string.stock_low_banner_action_only,
                v -> viewModel.toggleOnlyLowStock()
        );
    }

    private void openEditProduct(@NonNull ProductEntity product) {
        ProductFormDialogFragment.newEditInstance(product)
                .show(getChildFragmentManager(), TAG_PRODUCT_FORM);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
