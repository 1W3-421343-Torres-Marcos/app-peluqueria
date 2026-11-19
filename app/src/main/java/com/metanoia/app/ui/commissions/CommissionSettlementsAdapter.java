package com.metanoia.app.ui.commissions;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.metanoia.app.R;
import com.metanoia.app.data.db.entity.CommissionSettlementEntity;
import com.metanoia.app.databinding.ItemCommissionSettlementBinding;
import com.metanoia.app.util.Formats;

/**
 * Adaptador para listar las liquidaciones históricas registradas de un profesional.
 */
public class CommissionSettlementsAdapter extends
        ListAdapter<CommissionSettlementEntity, CommissionSettlementsAdapter.SettlementViewHolder> {

    public CommissionSettlementsAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public SettlementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCommissionSettlementBinding binding = ItemCommissionSettlementBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new SettlementViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SettlementViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static final class SettlementViewHolder extends RecyclerView.ViewHolder {

        private final ItemCommissionSettlementBinding binding;

        SettlementViewHolder(@NonNull ItemCommissionSettlementBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull CommissionSettlementEntity entity) {
            Context context = binding.getRoot().getContext();
            binding.settlementRowPeriod.setText(context.getString(
                    R.string.commissions_settlement_period_format,
                    Formats.formatDate(entity.fromDay),
                    Formats.formatDate(entity.toDay)));
            binding.settlementRowAmount.setMoneyCents(entity.commissionCents);
            binding.settlementRowBase.setText(context.getString(
                    R.string.commissions_row_base_format,
                    Formats.formatMoney(entity.baseCents)));
            binding.settlementRowMeta.setText(context.getString(
                    R.string.commissions_settlement_meta_format,
                    Formats.formatDateTime(entity.settledAt),
                    entity.operator));
        }
    }

    private static final DiffUtil.ItemCallback<CommissionSettlementEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<CommissionSettlementEntity>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull CommissionSettlementEntity oldItem,
                        @NonNull CommissionSettlementEntity newItem
                ) {
                    return oldItem.id.equals(newItem.id);
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull CommissionSettlementEntity oldItem,
                        @NonNull CommissionSettlementEntity newItem
                ) {
                    return oldItem.fromDay == newItem.fromDay
                            && oldItem.toDay == newItem.toDay
                            && oldItem.baseCents == newItem.baseCents
                            && oldItem.commissionCents == newItem.commissionCents
                            && oldItem.settledAt == newItem.settledAt
                            && oldItem.operator.equals(newItem.operator);
                }
            };
}
