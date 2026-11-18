package com.metanoia.app.ui.agenda.blocks;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.metanoia.app.R;
import com.metanoia.app.data.db.entity.ScheduleBlockEntity;
import com.metanoia.app.ui.common.ListRowView;
import com.metanoia.app.util.Formats;
import com.metanoia.domain.model.ScheduleBlockType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adaptador para listar bloqueos activos con rango de fechas, horario, alcance y tipo.
 */
public class ScheduleBlocksAdapter extends
        ListAdapter<ScheduleBlockEntity, ScheduleBlocksAdapter.Holder> {

    public interface Listener {
        void onBlockClicked(@NonNull ScheduleBlockEntity block);
    }

    private final Listener listener;
    private Map<String, String> professionalNames = Collections.emptyMap();

    public ScheduleBlocksAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    public void setProfessionalNames(@NonNull Map<String, String> names) {
        this.professionalNames = new LinkedHashMap<>(names);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule_block_row, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position), professionalNames, listener);
    }

    static final class Holder extends RecyclerView.ViewHolder {
        private final ListRowView rowView;

        Holder(@NonNull View itemView) {
            super(itemView);
            rowView = itemView.findViewById(R.id.scheduleBlockListRow);
        }

        void bind(
                @NonNull ScheduleBlockEntity block,
                @NonNull Map<String, String> professionalNames,
                @NonNull Listener listener
        ) {
            Context context = itemView.getContext();
            String scope = block.professionalId == null
                    ? context.getString(R.string.agenda_block_scope_all)
                    : professionalNames.getOrDefault(
                            block.professionalId,
                            context.getString(R.string.agenda_block_scope_all));

            String dateLabel = block.startDay == block.endDay
                    ? Formats.formatDate(block.startDay)
                    : context.getString(
                            R.string.schedule_block_date_range,
                            Formats.formatDate(block.startDay),
                            Formats.formatDate(block.endDay));

            String timeLabel = (block.startMin == null || block.endMin == null)
                    ? context.getString(R.string.agenda_block_full_day)
                    : context.getString(
                            R.string.agenda_time_inline,
                            Formats.formatTime(block.startMin),
                            Formats.formatTime(block.endMin));

            rowView.setTitle(context.getString(R.string.agenda_block_header, scope, block.reason));
            rowView.setSubtitle(context.getString(R.string.agenda_block_header, dateLabel, timeLabel));
            rowView.setTrailingText(typeLabel(context, block.type));
            rowView.setOnClickListener(v -> listener.onBlockClicked(block));
        }

        private static String typeLabel(Context context, String rawType) {
            ScheduleBlockType type = ScheduleBlockType.valueOf(rawType);
            switch (type) {
                case VACATION:
                    return context.getString(R.string.block_type_vacation);
                case LUNCH:
                    return context.getString(R.string.block_type_lunch);
                case HOLIDAY:
                    return context.getString(R.string.block_type_holiday);
                case OTHER:
                default:
                    return context.getString(R.string.block_type_other);
            }
        }
    }

    private static final DiffUtil.ItemCallback<ScheduleBlockEntity> DIFF =
            new DiffUtil.ItemCallback<ScheduleBlockEntity>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull ScheduleBlockEntity oldItem,
                        @NonNull ScheduleBlockEntity newItem) {
                    return oldItem.id.equals(newItem.id);
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull ScheduleBlockEntity oldItem,
                        @NonNull ScheduleBlockEntity newItem) {
                    return oldItem.updatedAt == newItem.updatedAt
                            && oldItem.active == newItem.active
                            && oldItem.reason.equals(newItem.reason);
                }
            };
}
