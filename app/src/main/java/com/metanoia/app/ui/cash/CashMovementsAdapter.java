package com.metanoia.app.ui.cash;

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
import com.metanoia.app.ui.cash.CashViewModel.CashMovementRow;
import com.metanoia.app.util.Formats;

import java.util.Objects;

/**
 * Adaptador de movimientos diarios de caja con estado de reverso y acción de revertir.
 */
public class CashMovementsAdapter extends ListAdapter<CashMovementRow, CashMovementsAdapter.Holder> {

    public interface Listener {
        void onReverseClicked(@NonNull CashMovementEntity movement);
    }

    private final Listener listener;

    public CashMovementsAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
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
        holder.bind(getItem(position), listener);
    }

    static final class Holder extends RecyclerView.ViewHolder {

        private final ItemCashMovementRowBinding binding;

        Holder(@NonNull ItemCashMovementRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull CashMovementRow row, @NonNull Listener listener) {
            Context context = binding.getRoot().getContext();
            CashMovementEntity entity = row.entity;
            binding.cashMovementConcept.setText(entity.concept);
            binding.cashMovementAmount.setMoneyCents(row.signedAmountCents);
            binding.cashMovementMeta.setText(context.getString(
                    R.string.cash_movement_meta_format,
                    Formats.formatTimeMillis(entity.timestamp),
                    CashUiLabels.typeLabel(context, entity.type),
                    CashUiLabels.methodLabel(context, entity.method),
                    entity.operator));

            binding.cashMovementReversedTag.setVisibility(
                    row.reversed ? View.VISIBLE : View.GONE);
            binding.btnCashReverseMovement.setVisibility(
                    row.canReverse ? View.VISIBLE : View.GONE);
            binding.btnCashReverseMovement.setOnClickListener(
                    v -> listener.onReverseClicked(entity));
        }
    }

    private static final DiffUtil.ItemCallback<CashMovementRow> DIFF =
            new DiffUtil.ItemCallback<CashMovementRow>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull CashMovementRow oldItem,
                        @NonNull CashMovementRow newItem) {
                    return oldItem.entity.id.equals(newItem.entity.id);
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull CashMovementRow oldItem,
                        @NonNull CashMovementRow newItem) {
                    return oldItem.signedAmountCents == newItem.signedAmountCents
                            && oldItem.reversed == newItem.reversed
                            && oldItem.canReverse == newItem.canReverse
                            && Objects.equals(oldItem.entity.concept, newItem.entity.concept)
                            && Objects.equals(oldItem.entity.method, newItem.entity.method)
                            && Objects.equals(oldItem.entity.type, newItem.entity.type);
                }
            };
}
