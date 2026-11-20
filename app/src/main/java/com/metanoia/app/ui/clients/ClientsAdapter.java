package com.metanoia.app.ui.clients;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.metanoia.app.R;
import com.metanoia.app.data.db.projection.ClientRowProjection;
import com.metanoia.app.ui.clients.ClientsViewModel.ClientFilterMode;
import com.metanoia.app.ui.common.ListRowView;
import com.metanoia.app.util.Formats;
import com.metanoia.domain.common.Clock;
import com.metanoia.domain.rules.PhoneNormalizer;

import java.time.Instant;
import java.util.Objects;

/**
 * Adaptador de clientes respaldado por {@link ClientRowProjection} y {@link DiffUtil}.
 */
public class ClientsAdapter extends ListAdapter<ClientRowProjection, ClientsAdapter.Holder> {

    public interface Listener {
        void onClientClicked(@NonNull ClientRowProjection client);
    }

    private final Listener listener;
    private ClientFilterMode filterMode = ClientFilterMode.ALL;

    public ClientsAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    public void setFilterMode(@NonNull ClientFilterMode mode) {
        if (this.filterMode != mode) {
            this.filterMode = mode;
            notifyDataSetChanged();
        }
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
        holder.bind(getItem(position), filterMode, listener);
    }

    static final class Holder extends RecyclerView.ViewHolder {
        private final ListRowView rowView;

        Holder(@NonNull View itemView) {
            super(itemView);
            rowView = itemView.findViewById(R.id.clientListRow);
        }

        void bind(
                @NonNull ClientRowProjection client,
                @NonNull ClientFilterMode filterMode,
                @NonNull Listener listener
        ) {
            Context context = itemView.getContext();
            rowView.setTitle(client.name);
            rowView.setSubtitle(PhoneNormalizer.formatForDisplay(client.phone));
            rowView.setTrailingText(buildTrailingText(context, client, filterMode));
            rowView.setAlpha(client.active ? 1f : 0.6f);
            rowView.setOnClickListener(v -> listener.onClientClicked(client));
        }

        @Nullable
        private static String buildTrailingText(
                Context context,
                ClientRowProjection client,
                ClientFilterMode filterMode
        ) {
            if (filterMode == ClientFilterMode.MISSING_BIRTHDAY && !client.hasBirthday()) {
                return context.getString(R.string.client_missing_birthday_tag);
            }
            if (filterMode == ClientFilterMode.RECENT_CONTACT
                    && client.createdFromContactAt != null) {
                long epochDay = Instant.ofEpochMilli(client.createdFromContactAt)
                        .atZone(Clock.ARGENTINA_ZONE)
                        .toLocalDate()
                        .toEpochDay();
                return context.getString(
                        R.string.client_recent_contact_tag,
                        Formats.formatDate(epochDay));
            }
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
                    return Objects.equals(oldItem.name, newItem.name)
                            && Objects.equals(oldItem.phone, newItem.phone)
                            && Objects.equals(oldItem.birthDay, newItem.birthDay)
                            && Objects.equals(oldItem.birthMonth, newItem.birthMonth)
                            && Objects.equals(oldItem.origin, newItem.origin)
                            && Objects.equals(
                                    oldItem.createdFromContactAt, newItem.createdFromContactAt)
                            && oldItem.noShowCount == newItem.noShowCount
                            && oldItem.active == newItem.active;
                }
            };
}
