package com.metanoia.app.ui.commissions;

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
import com.metanoia.app.databinding.ItemCommissionLineBinding;
import com.metanoia.app.ui.commissions.CommissionsViewModel.CommissionLineItem;
import com.metanoia.app.ui.commissions.CommissionsViewModel.CommissionRowKind;
import com.metanoia.app.util.Formats;

import java.util.Objects;

/**
 * Adaptador para mostrar el detalle por turno de comisiones, ajustes y pendientes de cobro.
 */
public class CommissionDetailsAdapter
        extends ListAdapter<CommissionLineItem, CommissionDetailsAdapter.LineViewHolder> {

    public CommissionDetailsAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public LineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCommissionLineBinding binding = ItemCommissionLineBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new LineViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull LineViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static final class LineViewHolder extends RecyclerView.ViewHolder {

        private final ItemCommissionLineBinding binding;

        LineViewHolder(@NonNull ItemCommissionLineBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull CommissionLineItem item) {
            Context context = binding.getRoot().getContext();
            binding.commissionRowHeader.setText(context.getString(
                    R.string.commissions_row_header_format,
                    Formats.formatDayWithShortWeekday(item.day),
                    Formats.formatTime(item.startMin),
                    item.clientName
            ));
            binding.commissionRowItems.setText(String.join("\n", item.itemSummaries));
            binding.commissionRowBase.setText(context.getString(
                    R.string.commissions_row_base_format,
                    Formats.formatMoney(item.baseCents)));
            binding.commissionRowAmount.setMoneyCents(item.commissionCents);

            if (item.kind == CommissionRowKind.REVERSAL_ADJUSTMENT) {
                binding.commissionRowBadge.setText(R.string.commissions_reversal_badge);
                binding.commissionRowBadge.setVisibility(View.VISIBLE);
                binding.commissionRowAmount.setTextColor(
                        ContextCompat.getColor(context, R.color.accent));
                binding.getRoot().setAlpha(1.0f);
            } else if (item.kind == CommissionRowKind.PENDING_COLLECTION) {
                binding.commissionRowBadge.setText(R.string.commissions_pending_badge);
                binding.commissionRowBadge.setVisibility(View.VISIBLE);
                binding.commissionRowAmount.setTextColor(MaterialColors.getColor(
                        binding.commissionRowAmount,
                        com.google.android.material.R.attr.colorOnSurfaceVariant));
                binding.getRoot().setAlpha(0.75f);
            } else {
                binding.commissionRowBadge.setVisibility(View.GONE);
                binding.commissionRowAmount.setTextColor(MaterialColors.getColor(
                        binding.commissionRowAmount,
                        com.google.android.material.R.attr.colorOnSurface));
                binding.getRoot().setAlpha(1.0f);
            }
        }
    }

    private static final DiffUtil.ItemCallback<CommissionLineItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<CommissionLineItem>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull CommissionLineItem oldItem,
                        @NonNull CommissionLineItem newItem
                ) {
                    return oldItem.key.equals(newItem.key);
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull CommissionLineItem oldItem,
                        @NonNull CommissionLineItem newItem
                ) {
                    return oldItem.day == newItem.day
                            && oldItem.startMin == newItem.startMin
                            && oldItem.kind.equals(newItem.kind)
                            && oldItem.baseCents == newItem.baseCents
                            && oldItem.commissionCents == newItem.commissionCents
                            && oldItem.clientName.equals(newItem.clientName)
                            && Objects.equals(oldItem.itemSummaries, newItem.itemSummaries);
                }
            };
}
