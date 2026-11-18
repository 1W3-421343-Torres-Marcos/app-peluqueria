package com.metanoia.app.ui.agenda;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.metanoia.app.R;
import com.metanoia.app.data.db.entity.ScheduleBlockEntity;
import com.metanoia.app.data.db.projection.AppointmentCardProjection;
import com.metanoia.app.ui.common.AppointmentCardView;
import com.metanoia.app.util.Formats;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Adaptador cronológico diario que combina bloqueos activos y tarjetas de turnos.
 */
public class DayTimelineAdapter extends
        ListAdapter<DayTimelineAdapter.TimelineRow, RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_BLOCK = 0;
    private static final int VIEW_TYPE_APPOINTMENT = 1;

    public interface Listener {
        void onAppointmentClicked(@NonNull AppointmentCardProjection card);
    }

    private final Listener listener;

    public DayTimelineAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    public static List<TimelineRow> buildRows(
            @Nullable List<ScheduleBlockEntity> blocks,
            @Nullable List<AppointmentCardProjection> appointments,
            @Nullable String filterProfessionalId,
            @NonNull Map<String, String> professionalNames
    ) {
        List<TimelineRow> rows = new ArrayList<>();
        if (blocks != null) {
            for (ScheduleBlockEntity block : blocks) {
                if (filterProfessionalId != null
                        && block.professionalId != null
                        && !filterProfessionalId.equals(block.professionalId)) {
                    continue;
                }
                String scopeName = block.professionalId == null
                        ? null
                        : professionalNames.get(block.professionalId);
                rows.add(TimelineRow.forBlock(block, scopeName));
            }
        }
        if (appointments != null) {
            for (AppointmentCardProjection apt : appointments) {
                rows.add(TimelineRow.forAppointment(apt));
            }
        }
        return rows;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).block != null ? VIEW_TYPE_BLOCK : VIEW_TYPE_APPOINTMENT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_BLOCK) {
            return new BlockHolder(inflater.inflate(
                    R.layout.item_schedule_block_card, parent, false));
        }
        return new AppointmentHolder(inflater.inflate(
                R.layout.item_appointment_card_row, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        TimelineRow row = getItem(position);
        if (holder instanceof BlockHolder && row.block != null) {
            ((BlockHolder) holder).bind(row.block, row.blockScopeLabel);
        } else if (holder instanceof AppointmentHolder && row.appointment != null) {
            ((AppointmentHolder) holder).bind(row.appointment, listener);
        }
    }

    static final class BlockHolder extends RecyclerView.ViewHolder {
        private final TextView titleView;
        private final TextView reasonView;

        BlockHolder(@NonNull View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.blockCardTitle);
            reasonView = itemView.findViewById(R.id.blockCardReason);
        }

        void bind(@NonNull ScheduleBlockEntity block, @Nullable String professionalName) {
            Context context = itemView.getContext();
            String scope = professionalName != null
                    ? professionalName
                    : context.getString(R.string.agenda_block_scope_all);
            String timeLabel = (block.startMin == null || block.endMin == null)
                    ? context.getString(R.string.agenda_block_full_day)
                    : context.getString(
                            R.string.agenda_time_inline,
                            Formats.formatTime(block.startMin),
                            Formats.formatTime(block.endMin));
            titleView.setText(context.getString(R.string.agenda_block_header, scope, timeLabel));
            reasonView.setText(block.reason);
        }
    }

    static final class AppointmentHolder extends RecyclerView.ViewHolder {
        private final AppointmentCardView cardView;

        AppointmentHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.appointmentCardView);
        }

        void bind(@NonNull AppointmentCardProjection card, @NonNull Listener listener) {
            cardView.bind(card);
            cardView.setOnClickListener(v -> listener.onAppointmentClicked(card));
        }
    }

    public static final class TimelineRow {
        @Nullable
        public final ScheduleBlockEntity block;
        @Nullable
        public final String blockScopeLabel;
        @Nullable
        public final AppointmentCardProjection appointment;

        private TimelineRow(
                @Nullable ScheduleBlockEntity block,
                @Nullable String blockScopeLabel,
                @Nullable AppointmentCardProjection appointment
        ) {
            this.block = block;
            this.blockScopeLabel = blockScopeLabel;
            this.appointment = appointment;
        }

        static TimelineRow forBlock(
                @NonNull ScheduleBlockEntity block,
                @Nullable String scopeLabel
        ) {
            return new TimelineRow(block, scopeLabel, null);
        }

        static TimelineRow forAppointment(@NonNull AppointmentCardProjection appointment) {
            return new TimelineRow(null, null, appointment);
        }
    }

    private static final DiffUtil.ItemCallback<TimelineRow> DIFF =
            new DiffUtil.ItemCallback<TimelineRow>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull TimelineRow oldItem,
                        @NonNull TimelineRow newItem) {
                    if (oldItem.block != null && newItem.block != null) {
                        return oldItem.block.id.equals(newItem.block.id);
                    }
                    if (oldItem.appointment != null && newItem.appointment != null) {
                        return oldItem.appointment.id.equals(newItem.appointment.id);
                    }
                    return false;
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull TimelineRow oldItem,
                        @NonNull TimelineRow newItem) {
                    if (oldItem.block != null && newItem.block != null) {
                        return oldItem.block.updatedAt == newItem.block.updatedAt
                                && Objects.equals(oldItem.blockScopeLabel, newItem.blockScopeLabel)
                                && oldItem.block.reason.equals(newItem.block.reason);
                    }
                    if (oldItem.appointment != null && newItem.appointment != null) {
                        AppointmentCardProjection a = oldItem.appointment;
                        AppointmentCardProjection b = newItem.appointment;
                        return a.startMin == b.startMin
                                && a.endMin == b.endMin
                                && a.status.equals(b.status)
                                && a.clientName.equals(b.clientName)
                                && a.professionalName.equals(b.professionalName)
                                && a.servicesSummary.equals(b.servicesSummary)
                                && a.totalCents() == b.totalCents();
                    }
                    return false;
                }
            };
}
