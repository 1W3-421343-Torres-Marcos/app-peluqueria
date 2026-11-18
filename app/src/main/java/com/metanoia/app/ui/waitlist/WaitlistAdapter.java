package com.metanoia.app.ui.waitlist;

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
import com.metanoia.app.databinding.ItemWaitlistEntryBinding;
import com.metanoia.app.util.Formats;
import com.metanoia.domain.model.WaitlistEntry;
import com.metanoia.domain.model.WaitlistStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Adaptador para listar solicitudes de lista de espera con estado, franja y acciones.
 */
public class WaitlistAdapter
        extends ListAdapter<WaitlistAdapter.WaitlistRowItem, WaitlistAdapter.WaitlistViewHolder> {

    public interface OnWaitlistActionListener {
        void onEdit(@NonNull WaitlistEntry entry);

        void onCancel(@NonNull WaitlistEntry entry);
    }

    private final OnWaitlistActionListener listener;

    public WaitlistAdapter(@NonNull OnWaitlistActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = Objects.requireNonNull(listener, "listener");
    }

    @NonNull
    @Override
    public WaitlistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemWaitlistEntryBinding binding = ItemWaitlistEntryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new WaitlistViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull WaitlistViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static final class WaitlistViewHolder extends RecyclerView.ViewHolder {

        private final ItemWaitlistEntryBinding binding;

        WaitlistViewHolder(@NonNull ItemWaitlistEntryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull WaitlistRowItem item, @NonNull OnWaitlistActionListener listener) {
            Context context = binding.getRoot().getContext();
            WaitlistEntry entry = item.entry;

            binding.waitlistRowClient.setText(item.clientName);
            binding.waitlistRowStatus.setText(statusLabel(context, entry.status()));
            binding.waitlistRowServices.setText(item.servicesSummary);
            binding.waitlistRowProfessional.setText(item.professionalName != null
                    ? item.professionalName
                    : context.getString(R.string.waitlist_any_professional));
            binding.waitlistRowWindow.setText(formatWindow(context, entry));
            binding.waitlistRowValidity.setText(context.getString(
                    R.string.waitlist_valid_until_format,
                    Formats.formatDate(entry.validUntilDay())));

            if (entry.notes().isEmpty()) {
                binding.waitlistRowNotes.setVisibility(View.GONE);
            } else {
                binding.waitlistRowNotes.setVisibility(View.VISIBLE);
                binding.waitlistRowNotes.setText(context.getString(
                        R.string.waitlist_notes_format, entry.notes()));
            }

            boolean open = entry.status().isOpen();
            binding.waitlistRowActions.setVisibility(open ? View.VISIBLE : View.GONE);
            binding.btnWaitlistEditEntry.setOnClickListener(v -> listener.onEdit(entry));
            binding.btnWaitlistCancelEntry.setOnClickListener(v -> listener.onCancel(entry));
        }

        private static String statusLabel(
                @NonNull Context context,
                @NonNull WaitlistStatus status
        ) {
            switch (status) {
                case ACTIVE:
                    return context.getString(R.string.waitlist_status_active);
                case OFFERED:
                    return context.getString(R.string.waitlist_status_offered);
                case FULFILLED:
                    return context.getString(R.string.waitlist_status_fulfilled);
                case EXPIRED:
                    return context.getString(R.string.waitlist_status_expired);
                case CANCELLED:
                default:
                    return context.getString(R.string.waitlist_status_cancelled);
            }
        }

        private static String formatWindow(
                @NonNull Context context,
                @NonNull WaitlistEntry entry
        ) {
            String dayLabel;
            if (entry.specificDay().isPresent()) {
                dayLabel = Formats.formatDayWithShortWeekday(entry.specificDay().get());
            } else {
                List<String> dayNames = new ArrayList<>();
                for (Integer day : entry.preferredDaysOfWeek()) {
                    dayNames.add(weekdayShortLabel(context, day));
                }
                dayLabel = String.join(", ", dayNames);
            }
            return context.getString(
                    R.string.waitlist_window_format,
                    dayLabel,
                    Formats.formatTime(entry.fromMin()),
                    Formats.formatTime(entry.toMin()));
        }

        private static String weekdayShortLabel(@NonNull Context context, int isoDay) {
            switch (isoDay) {
                case 1:
                    return context.getString(R.string.waitlist_weekday_mon);
                case 2:
                    return context.getString(R.string.waitlist_weekday_tue);
                case 3:
                    return context.getString(R.string.waitlist_weekday_wed);
                case 4:
                    return context.getString(R.string.waitlist_weekday_thu);
                case 5:
                    return context.getString(R.string.waitlist_weekday_fri);
                case 6:
                    return context.getString(R.string.waitlist_weekday_sat);
                case 7:
                default:
                    return context.getString(R.string.waitlist_weekday_sun);
            }
        }
    }

    /**
     * Modelo liviano de fila combinando entrada y etiquetas ya resueltas.
     */
    public static final class WaitlistRowItem {
        public final WaitlistEntry entry;
        public final String clientName;
        public final String servicesSummary;
        @Nullable
        public final String professionalName;

        public WaitlistRowItem(
                @NonNull WaitlistEntry entry,
                @NonNull String clientName,
                @NonNull String servicesSummary,
                @Nullable String professionalName
        ) {
            this.entry = Objects.requireNonNull(entry, "entry");
            this.clientName = Objects.requireNonNull(clientName, "clientName");
            this.servicesSummary = Objects.requireNonNull(servicesSummary, "servicesSummary");
            this.professionalName = professionalName;
        }
    }

    private static final DiffUtil.ItemCallback<WaitlistRowItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<WaitlistRowItem>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull WaitlistRowItem oldItem,
                        @NonNull WaitlistRowItem newItem
                ) {
                    return oldItem.entry.id().equals(newItem.entry.id());
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull WaitlistRowItem oldItem,
                        @NonNull WaitlistRowItem newItem
                ) {
                    return oldItem.entry.equals(newItem.entry)
                            && oldItem.clientName.equals(newItem.clientName)
                            && oldItem.servicesSummary.equals(newItem.servicesSummary)
                            && Objects.equals(oldItem.professionalName, newItem.professionalName);
                }
            };
}
