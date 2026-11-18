package com.metanoia.app.ui.cash.receivables;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.metanoia.app.R;
import com.metanoia.app.data.db.projection.AppointmentCardProjection;
import com.metanoia.app.ui.common.AppointmentCardView;

/**
 * Adaptador para listar turnos {@code COMPLETED} con saldo pendiente por cobrar.
 */
public class ReceivablesAdapter extends
        ListAdapter<AppointmentCardProjection, ReceivablesAdapter.Holder> {

    public interface Listener {
        void onReceivableClicked(@NonNull AppointmentCardProjection card);
    }

    private final Listener listener;

    public ReceivablesAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        AppointmentCardView cardView = (AppointmentCardView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_appointment_card_row, parent, false);
        return new Holder(cardView);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        AppointmentCardProjection card = getItem(position);
        holder.cardView.bindForReceivable(card);
        holder.cardView.setOnClickListener(v -> listener.onReceivableClicked(card));
    }

    static final class Holder extends RecyclerView.ViewHolder {

        final AppointmentCardView cardView;

        Holder(@NonNull AppointmentCardView cardView) {
            super(cardView);
            this.cardView = cardView;
        }
    }

    private static final DiffUtil.ItemCallback<AppointmentCardProjection> DIFF =
            new DiffUtil.ItemCallback<AppointmentCardProjection>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull AppointmentCardProjection oldItem,
                        @NonNull AppointmentCardProjection newItem) {
                    return oldItem.id.equals(newItem.id);
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull AppointmentCardProjection oldItem,
                        @NonNull AppointmentCardProjection newItem) {
                    return oldItem.paidCents == newItem.paidCents
                            && oldItem.adjustmentCents == newItem.adjustmentCents
                            && oldItem.itemsSubtotalCents == newItem.itemsSubtotalCents
                            && oldItem.day == newItem.day
                            && oldItem.startMin == newItem.startMin
                            && oldItem.status.equals(newItem.status)
                            && oldItem.clientName.equals(newItem.clientName)
                            && oldItem.professionalName.equals(newItem.professionalName)
                            && oldItem.servicesSummary.equals(newItem.servicesSummary);
                }
            };
}
