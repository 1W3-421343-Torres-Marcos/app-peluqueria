package com.metanoia.app.ui.clients;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.metanoia.app.R;
import com.metanoia.app.data.db.projection.AppointmentCardProjection;
import com.metanoia.app.ui.common.AppointmentCardView;

/**
 * Adaptador para listar el historial de visitas (turnos COMPLETED/PAID) en la ficha de cliente.
 */
public class ClientVisitsAdapter
        extends ListAdapter<AppointmentCardProjection, ClientVisitsAdapter.Holder> {

    public interface Listener {
        void onVisitClicked(@NonNull AppointmentCardProjection card);
    }

    private final Listener listener;

    public ClientVisitsAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_appointment_card_row, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static final class Holder extends RecyclerView.ViewHolder {
        private final AppointmentCardView cardView;

        Holder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.appointmentCardView);
        }

        void bind(@NonNull AppointmentCardProjection card, @NonNull Listener listener) {
            cardView.bindForClientHistory(card);
            cardView.setOnClickListener(v -> listener.onVisitClicked(card));
        }
    }

    private static final DiffUtil.ItemCallback<AppointmentCardProjection> DIFF =
            new DiffUtil.ItemCallback<AppointmentCardProjection>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull AppointmentCardProjection oldItem,
                        @NonNull AppointmentCardProjection newItem
                ) {
                    return oldItem.id.equals(newItem.id);
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull AppointmentCardProjection oldItem,
                        @NonNull AppointmentCardProjection newItem
                ) {
                    return oldItem.day == newItem.day
                            && oldItem.startMin == newItem.startMin
                            && oldItem.endMin == newItem.endMin
                            && oldItem.status.equals(newItem.status)
                            && oldItem.professionalName.equals(newItem.professionalName)
                            && oldItem.servicesSummary.equals(newItem.servicesSummary)
                            && oldItem.totalCents() == newItem.totalCents();
                }
            };
}
