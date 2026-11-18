package com.metanoia.app.ui.clients;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.metanoia.app.R;
import com.metanoia.app.data.db.entity.CashMovementEntity;
import com.metanoia.app.databinding.ItemCashMovementRowBinding;
import com.metanoia.app.ui.cash.CashUiLabels;
import com.metanoia.app.util.Formats;
import com.metanoia.domain.model.CashMovementType;

import java.util.Objects;

/**
 * Adaptador de historial de pagos y reversos del cliente en su ficha de detalle.
 */
public class ClientSpendingAdapter extends
        ListAdapter<CashMovementEntity, ClientSpendingAdapter.Holder> {

    public ClientSpendingAdapter() {
        super(DIFF);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCashMovementRowBinding binding = ItemCashMovementRowBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new Holder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position));
    }

    static final class Holder extends RecyclerView.ViewHolder {

        private final ItemCashMovementRowBinding binding;

        Holder(@NonNull ItemCashMovementRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull CashMovementEntity entity) {
            Context context = binding.getRoot().getContext();
            binding.cashMovementConcept.setText(entity.concept);
            CashMovementType type = parseType(entity.type);
            long signed = type.isStandardIncome() ? entity.amountCents : -entity.amountCents;
            binding.cashMovementAmount.setMoneyCents(signed);
            binding.cashMovementMeta.setText(context.getString(
                    R.string.cash_movement_meta_format,
                    Formats.formatDateTime(entity.timestamp),
                    CashUiLabels.typeLabel(context, type),
                    CashUiLabels.methodLabel(context, entity.method),
                    entity.operator));
            binding.cashMovementReversedTag.setVisibility(View.GONE);
            binding.btnCashReverseMovement.setVisibility(View.GONE);
        }

        private static CashMovementType parseType(String raw) {
            try {
                return CashMovementType.valueOf(raw);
            } catch (IllegalArgumentException ex) {
                return CashMovementType.OTHER_INCOME;
            }
        }
    }

    private static final DiffUtil.ItemCallback<CashMovementEntity> DIFF =
            new DiffUtil.ItemCallback<CashMovementEntity>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull CashMovementEntity oldItem,
                        @NonNull CashMovementEntity newItem) {
                    return oldItem.id.equals(newItem.id);
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull CashMovementEntity oldItem,
                        @NonNull CashMovementEntity newItem) {
                    return oldItem.amountCents == newItem.amountCents
                            && Objects.equals(oldItem.type, newItem.type)
                            && Objects.equals(oldItem.method, newItem.method)
                            && Objects.equals(oldItem.concept, newItem.concept)
                            && oldItem.timestamp == newItem.timestamp;
                }
            };
}
