package com.metanoia.app.ui.stock;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.metanoia.app.R;
import com.metanoia.app.data.db.entity.ProductEntity;
import com.metanoia.app.databinding.ItemProductBinding;
import com.metanoia.app.ui.common.StatusChip;
import com.metanoia.domain.model.ProductType;
import com.metanoia.domain.model.ProductUnit;
import com.metanoia.domain.rules.LowStockRule;

import java.util.Objects;

/**
 * Adaptador para listar productos ordenados con alertas de stock bajo primero.
 */
public class ProductsAdapter extends ListAdapter<ProductEntity, ProductsAdapter.ProductViewHolder> {

    public interface OnProductActionListener {
        void onProductClick(@NonNull ProductEntity product);

        void onProductEdit(@NonNull ProductEntity product);
    }

    private final OnProductActionListener listener;

    public ProductsAdapter(@NonNull OnProductActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = Objects.requireNonNull(listener, "listener");
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProductBinding binding = ItemProductBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ProductViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static final class ProductViewHolder extends RecyclerView.ViewHolder {

        private final ItemProductBinding binding;

        ProductViewHolder(@NonNull ItemProductBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull ProductEntity entity, @NonNull OnProductActionListener listener) {
            Context context = binding.getRoot().getContext();
            ProductType type = StockUiLabels.parseType(entity.type);
            ProductUnit unit = StockUiLabels.parseUnit(entity.unit);

            binding.productRowName.setText(entity.name);

            String typeLabel = StockUiLabels.typeLabel(context, type);
            if (entity.brand != null && !entity.brand.trim().isEmpty()) {
                binding.productRowMeta.setText(context.getString(
                        R.string.stock_meta_with_brand_format, entity.brand, typeLabel));
            } else {
                binding.productRowMeta.setText(typeLabel);
            }

            if (type.isForSale() && entity.salePriceCents != null) {
                binding.productRowPrice.setMoneyCents(entity.salePriceCents);
                binding.productRowPrice.setVisibility(View.VISIBLE);
            } else {
                binding.productRowPrice.setVisibility(View.GONE);
            }

            String currentLabel = StockUiLabels.formatQuantityWithUnit(
                    context, entity.currentStock, unit);
            String minLabel = StockUiLabels.formatQuantityWithUnit(
                    context, entity.minStock, unit);
            binding.productRowStock.setText(context.getString(
                    R.string.stock_quantity_format, currentLabel, minLabel));

            boolean isLow = entity.active
                    && LowStockRule.isLowStock(entity.currentStock, entity.minStock);
            if (isLow) {
                binding.productRowLowStockChip.setVariant(StatusChip.Variant.LOW_STOCK);
                binding.productRowLowStockChip.setVisibility(View.VISIBLE);
            } else {
                binding.productRowLowStockChip.setVisibility(View.GONE);
            }

            boolean isNegative = LowStockRule.isNegativeStock(entity.currentStock);
            binding.productRowNegativeBadge.setVisibility(isNegative ? View.VISIBLE : View.GONE);
            binding.productRowInactiveBadge.setVisibility(entity.active ? View.GONE : View.VISIBLE);

            binding.getRoot().setAlpha(entity.active ? 1.0f : 0.65f);
            binding.getRoot().setOnClickListener(v -> listener.onProductClick(entity));
            binding.btnProductRowEdit.setOnClickListener(v -> listener.onProductEdit(entity));
        }
    }

    private static final DiffUtil.ItemCallback<ProductEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ProductEntity>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull ProductEntity oldItem,
                        @NonNull ProductEntity newItem
                ) {
                    return oldItem.id.equals(newItem.id);
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull ProductEntity oldItem,
                        @NonNull ProductEntity newItem
                ) {
                    return oldItem.updatedAt == newItem.updatedAt
                            && oldItem.currentStock == newItem.currentStock
                            && oldItem.minStock == newItem.minStock
                            && oldItem.active == newItem.active
                            && oldItem.name.equals(newItem.name)
                            && Objects.equals(oldItem.brand, newItem.brand)
                            && oldItem.type.equals(newItem.type)
                            && oldItem.unit.equals(newItem.unit)
                            && Objects.equals(oldItem.salePriceCents, newItem.salePriceCents);
                }
            };
}
