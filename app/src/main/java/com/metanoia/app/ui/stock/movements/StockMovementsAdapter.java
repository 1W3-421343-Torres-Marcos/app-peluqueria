package com.metanoia.app.ui.stock.movements;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.metanoia.app.R;
import com.metanoia.app.data.db.entity.StockMovementEntity;
import com.metanoia.app.databinding.ItemStockMovementBinding;
import com.metanoia.app.ui.stock.StockUiLabels;
import com.metanoia.app.util.Formats;
import com.metanoia.domain.model.ProductUnit;
import com.metanoia.domain.model.StockMovementType;

import java.util.Objects;

/**
 * Adaptador para listar el historial cronológico inmutable de movimientos de un producto.
 */
public class StockMovementsAdapter
        extends ListAdapter<StockMovementEntity, StockMovementsAdapter.MovementViewHolder> {

    private ProductUnit unit = ProductUnit.UNIT;

    public StockMovementsAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setUnit(@NonNull ProductUnit unit) {
        if (this.unit != unit) {
            this.unit = unit;
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public MovementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStockMovementBinding binding = ItemStockMovementBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new MovementViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MovementViewHolder holder, int position) {
        holder.bind(getItem(position), unit);
    }

    static final class MovementViewHolder extends RecyclerView.ViewHolder {

        private final ItemStockMovementBinding binding;

        MovementViewHolder(@NonNull ItemStockMovementBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull StockMovementEntity entity, @NonNull ProductUnit unit) {
            Context context = binding.getRoot().getContext();
            StockMovementType type = StockUiLabels.parseMovementType(entity.type);

            binding.stockMovementRowType.setText(StockUiLabels.movementTypeLabel(context, type));
            binding.stockMovementRowQty.setText(
                    StockUiLabels.formatSignedQuantityWithUnit(context, entity.quantity, unit));
            if (entity.quantity < 0) {
                binding.stockMovementRowQty.setTextColor(
                        ContextCompat.getColor(context, R.color.accent));
            } else {
                binding.stockMovementRowQty.setTextColor(MaterialColors.getColor(
                        binding.stockMovementRowQty,
                        com.google.android.material.R.attr.colorOnSurface));
            }

            binding.stockMovementRowMeta.setText(context.getString(
                    R.string.stock_movement_operator_format,
                    Formats.formatDateTime(entity.timestamp),
                    entity.operator));

            if (entity.reason != null && !entity.reason.trim().isEmpty()) {
                binding.stockMovementRowReason.setVisibility(View.VISIBLE);
                binding.stockMovementRowReason.setText(entity.reason);
            } else {
                binding.stockMovementRowReason.setVisibility(View.GONE);
            }
        }
    }

    private static final DiffUtil.ItemCallback<StockMovementEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<StockMovementEntity>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull StockMovementEntity oldItem,
                        @NonNull StockMovementEntity newItem
                ) {
                    return oldItem.id.equals(newItem.id);
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull StockMovementEntity oldItem,
                        @NonNull StockMovementEntity newItem
                ) {
                    return oldItem.timestamp == newItem.timestamp
                            && oldItem.quantity == newItem.quantity
                            && oldItem.type.equals(newItem.type)
                            && Objects.equals(oldItem.reason, newItem.reason)
                            && oldItem.operator.equals(newItem.operator);
                }
            };
}
