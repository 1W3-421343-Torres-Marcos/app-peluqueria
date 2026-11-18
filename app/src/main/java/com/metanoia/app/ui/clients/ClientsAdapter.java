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
import com.metanoia.app.data.db.projection.ClientRowProjection;
import com.metanoia.app.ui.common.ListRowView;
import com.metanoia.domain.rules.PhoneNormalizer;

/**
 * Adaptador de clientes respaldado por {@link ClientRowProjection} y {@link DiffUtil}.
 */
public class ClientsAdapter extends ListAdapter<ClientRowProjection, ClientsAdapter.Holder> {

    public interface Listener {
        void onClientClicked(@NonNull ClientRowProjection client);
    }

    private final Listener listener;

    public ClientsAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_client_row, parent, false);
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
            rowView = itemView.findViewById(R.id.clientListRow);
        }

        void bind(@NonNull ClientRowProjection client, @NonNull Listener listener) {
            Context context = itemView.getContext();
            rowView.setTitle(client.name);
            rowView.setSubtitle(PhoneNormalizer.formatForDisplay(client.phone));
            rowView.setTrailingText(buildTrailingText(context, client));
            rowView.setAlpha(client.active ? 1f : 0.6f);
            rowView.setOnClickListener(v -> listener.onClientClicked(client));
        }

        private static String buildTrailingText(Context context, ClientRowProjection client) {
            String absencesText = client.noShowCount > 0
                    ? context.getResources().getQuantityString(
                            R.plurals.client_no_shows_count,
                            client.noShowCount,
                            client.noShowCount)
                    : null;
            if (!client.active) {
                String inactiveText = context.getString(R.string.client_inactive_tag);
                if (absencesText != null) {
                    return context.getString(
                            R.string.client_row_inactive_with_absences,
                            inactiveText,
                            absencesText);
                }
                return inactiveText;
            }
            return absencesText;
        }
    }

    private static final DiffUtil.ItemCallback<ClientRowProjection> DIFF =
            new DiffUtil.ItemCallback<ClientRowProjection>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull ClientRowProjection oldItem,
                        @NonNull ClientRowProjection newItem) {
                    return oldItem.id.equals(newItem.id);
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull ClientRowProjection oldItem,
                        @NonNull ClientRowProjection newItem) {
                    return oldItem.name.equals(newItem.name)
                            && oldItem.phone.equals(newItem.phone)
                            && oldItem.noShowCount == newItem.noShowCount
                            && oldItem.active == newItem.active;
                }
            };
}
