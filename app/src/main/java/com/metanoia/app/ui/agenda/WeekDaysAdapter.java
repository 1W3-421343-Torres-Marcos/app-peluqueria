package com.metanoia.app.ui.agenda;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.metanoia.app.R;
import com.metanoia.app.data.db.entity.ScheduleBlockEntity;
import com.metanoia.app.data.db.projection.AppointmentCardProjection;
import com.metanoia.app.util.Formats;
import com.metanoia.domain.model.AppointmentStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Adaptador para los 7 días (lunes a domingo) de la vista semanal con ocupación y bloqueos.
 */
public class WeekDaysAdapter extends
        ListAdapter<WeekDaysAdapter.WeekDaySummary, WeekDaysAdapter.Holder> {

    public interface Listener {
        void onDaySelected(long epochDay);
    }

    private final Listener listener;

    public WeekDaysAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    public static List<WeekDaySummary> buildWeek(
            long mondayEpoch,
            long selectedDayEpoch,
            long todayEpoch,
            @Nullable List<AppointmentCardProjection> weekAppointments,
            @Nullable List<ScheduleBlockEntity> weekBlocks,
            @Nullable String filterProfessionalId
    ) {
        List<WeekDaySummary> days = new ArrayList<>(7);
        for (int offset = 0; offset < 7; offset++) {
            long day = mondayEpoch + offset;
            int count = 0;
            int occupiedMin = 0;
            if (weekAppointments != null) {
                for (AppointmentCardProjection apt : weekAppointments) {
                    if (apt.day != day) {
                        continue;
                    }
                    AppointmentStatus status = AppointmentStatus.valueOf(apt.status);
                    if (status.occupiesSchedule()) {
                        count++;
                        occupiedMin += Math.max(0, apt.endMin - apt.startMin);
                    }
                }
            }
            boolean hasBlock = false;
            if (weekBlocks != null) {
                for (ScheduleBlockEntity block : weekBlocks) {
                    if (block.startDay <= day && day <= block.endDay
                            && (filterProfessionalId == null
                            || block.professionalId == null
                            || filterProfessionalId.equals(block.professionalId))) {
                        hasBlock = true;
                        break;
                    }
                }
            }
            days.add(new WeekDaySummary(
                    day,
                    count,
                    occupiedMin,
                    hasBlock,
                    day == todayEpoch,
                    day == selectedDayEpoch
            ));
        }
        return days;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_week_day_row, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static final class Holder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView titleView;
        private final TextView subtitleView;
        private final TextView countBadge;

        Holder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.weekDayCard);
            titleView = itemView.findViewById(R.id.weekDayTitle);
            subtitleView = itemView.findViewById(R.id.weekDaySubtitle);
            countBadge = itemView.findViewById(R.id.weekDayCountBadge);
        }

        void bind(@NonNull WeekDaySummary summary, @NonNull Listener listener) {
            Context context = itemView.getContext();
            String dateTitle = Formats.formatWeekdayAndDayMonth(summary.dayEpoch);
            titleView.setText(summary.today
                    ? context.getString(R.string.agenda_week_day_today, dateTitle)
                    : dateTitle);

            String countLabel = summary.appointmentCount == 0
                    ? context.getString(R.string.agenda_week_no_appointments)
                    : context.getResources().getQuantityString(
                            R.plurals.agenda_week_appointments_count,
                            summary.appointmentCount,
                            summary.appointmentCount);
            countBadge.setText(countLabel);

            String occupationText = formatOccupation(context, summary.occupiedMinutes);
            if (summary.hasBlocks) {
                String blockNote = context.getString(R.string.agenda_week_has_blocks);
                subtitleView.setText(summary.occupiedMinutes > 0
                        ? context.getString(
                                R.string.agenda_week_summary_with_block,
                                occupationText,
                                blockNote)
                        : blockNote);
            } else {
                subtitleView.setText(summary.occupiedMinutes > 0
                        ? occupationText
                        : context.getString(R.string.agenda_week_no_appointments));
            }

            if (summary.today) {
                cardView.setStrokeColor(ContextCompat.getColor(context, R.color.accent));
            } else if (summary.selected) {
                cardView.setStrokeColor(MaterialColors.getColor(
                        cardView, androidx.appcompat.R.attr.colorPrimary, Color.BLACK));
            } else {
                cardView.setStrokeColor(MaterialColors.getColor(
                        cardView, com.google.android.material.R.attr.colorOutline, Color.LTGRAY));
            }

            cardView.setOnClickListener(v -> listener.onDaySelected(summary.dayEpoch));
        }

        private static String formatOccupation(Context context, int minutes) {
            if (minutes <= 0) {
                return "";
            }
            if (minutes < 60) {
                return context.getString(R.string.agenda_week_occupied_minutes, minutes);
            }
            return context.getString(
                    R.string.agenda_week_occupied_hours, minutes / 60, minutes % 60);
        }
    }

    public static final class WeekDaySummary {
        public final long dayEpoch;
        public final int appointmentCount;
        public final int occupiedMinutes;
        public final boolean hasBlocks;
        public final boolean today;
        public final boolean selected;

        public WeekDaySummary(
                long dayEpoch,
                int appointmentCount,
                int occupiedMinutes,
                boolean hasBlocks,
                boolean today,
                boolean selected
        ) {
            this.dayEpoch = dayEpoch;
            this.appointmentCount = appointmentCount;
            this.occupiedMinutes = occupiedMinutes;
            this.hasBlocks = hasBlocks;
            this.today = today;
            this.selected = selected;
        }
    }

    private static final DiffUtil.ItemCallback<WeekDaySummary> DIFF =
            new DiffUtil.ItemCallback<WeekDaySummary>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull WeekDaySummary oldItem,
                        @NonNull WeekDaySummary newItem) {
                    return oldItem.dayEpoch == newItem.dayEpoch;
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull WeekDaySummary oldItem,
                        @NonNull WeekDaySummary newItem) {
                    return oldItem.appointmentCount == newItem.appointmentCount
                            && oldItem.occupiedMinutes == newItem.occupiedMinutes
                            && oldItem.hasBlocks == newItem.hasBlocks
                            && oldItem.today == newItem.today
                            && oldItem.selected == newItem.selected;
                }
            };
}
