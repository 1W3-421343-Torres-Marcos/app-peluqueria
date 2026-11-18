package com.metanoia.app.ui.professionals;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.metanoia.app.R;
import com.metanoia.app.data.db.pojo.ProfessionalWithDetails;
import com.metanoia.app.ui.common.ListRowView;
import com.metanoia.app.util.Formats;

/**
 * Adaptador para el listado de profesionales con porcentaje de comisión y servicios asignados.
 */
public class ProfessionalsAdapter extends
        ListAdapter<ProfessionalWithDetails, ProfessionalsAdapter.Holder> {

    public interface Listener {
        void onProfessionalClicked(ProfessionalWithDetails details);
    }

    private final Listener listener;

    public ProfessionalsAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_professional_row, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static final class Holder extends RecyclerView.ViewHolder {
        private final ListRowView rowView;

        Holder(@NonNull View itemView) {
            super(itemView);
            rowView = itemView.findViewById(R.id.professionalListRow);
        }

        void bind(ProfessionalWithDetails details, Listener listener) {
            Context context = itemView.getContext();
            int serviceCount = details.serviceRefs == null ? 0 : details.serviceRefs.size();
            rowView.setTitle(details.professional.name);
            rowView.setSubtitle(context.getString(
                    details.professional.active
                            ? R.string.professional_row_meta_active
                            : R.string.professional_row_meta_inactive,
                    serviceCount
            ));
            rowView.setTrailingText(
                    Formats.formatPercent(details.professional.commissionBasisPoints));
            rowView.setAlpha(details.professional.active ? 1f : 0.6f);
            rowView.setOnClickListener(v -> listener.onProfessionalClicked(details));
        }
    }

    private static final DiffUtil.ItemCallback<ProfessionalWithDetails> DIFF =
            new DiffUtil.ItemCallback<ProfessionalWithDetails>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull ProfessionalWithDetails oldItem,
                        @NonNull ProfessionalWithDetails newItem) {
                    return oldItem.professional.id.equals(newItem.professional.id);
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull ProfessionalWithDetails oldItem,
                        @NonNull ProfessionalWithDetails newItem) {
                    int oldServices = oldItem.serviceRefs == null ? 0 : oldItem.serviceRefs.size();
                    int newServices = newItem.serviceRefs == null ? 0 : newItem.serviceRefs.size();
                    return oldItem.professional.name.equals(newItem.professional.name)
                            && oldItem.professional.commissionBasisPoints
                                    == newItem.professional.commissionBasisPoints
                            && oldItem.professional.active == newItem.professional.active
                            && oldServices == newServices;
                }
            };
}
