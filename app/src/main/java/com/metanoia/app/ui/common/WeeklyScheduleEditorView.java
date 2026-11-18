package com.metanoia.app.ui.common;

import android.app.TimePickerDialog;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.metanoia.app.R;
import com.metanoia.app.util.Formats;
import com.metanoia.domain.model.TimeRange;

import java.util.ArrayList;
import java.util.List;

/**
 * Editor semanal reutilizable de tramos horarios (lunes a domingo, hasta dos tramos por día).
 */
public class WeeklyScheduleEditorView extends LinearLayout {

    private static final int DEFAULT_START_1 = 9 * 60;
    private static final int DEFAULT_END_1 = 20 * 60;
    private static final int DEFAULT_START_2 = 15 * 60;
    private static final int DEFAULT_END_2 = 20 * 60;

    private static final int[] DAY_NAME_RES = {
            R.string.day_monday,
            R.string.day_tuesday,
            R.string.day_wednesday,
            R.string.day_thursday,
            R.string.day_friday,
            R.string.day_saturday,
            R.string.day_sunday
    };

    private final DayRowHolder[] dayRows = new DayRowHolder[7];

    public WeeklyScheduleEditorView(@NonNull Context context) {
        this(context, null);
    }

    public WeeklyScheduleEditorView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WeeklyScheduleEditorView(
            @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);
        buildRows(context);
    }

    private void buildRows(Context context) {
        MaterialButton copyButton = new MaterialButton(
                context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        copyButton.setText(R.string.schedule_copy_mon_to_sat);
        copyButton.setOnClickListener(v -> copyMondayToSaturday());
        addView(copyButton, new LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        LayoutInflater inflater = LayoutInflater.from(context);
        for (int i = 0; i < 7; i++) {
            View rowView = inflater.inflate(R.layout.view_schedule_day_row, this, false);
            dayRows[i] = new DayRowHolder(rowView, i + 1, DAY_NAME_RES[i]);
            addView(rowView);
        }
    }

    public void setSchedule(@NonNull List<TimeRange> ranges) {
        for (DayRowHolder holder : dayRows) {
            holder.clear();
        }
        for (TimeRange range : ranges) {
            int idx = range.dayOfWeek() - 1;
            if (idx >= 0 && idx < dayRows.length) {
                dayRows[idx].addRange(range.startMin(), range.endMin());
            }
        }
        for (DayRowHolder holder : dayRows) {
            holder.render();
        }
    }

    public List<TimeRange> getSchedule() {
        List<TimeRange> result = new ArrayList<>();
        for (DayRowHolder holder : dayRows) {
            holder.appendRangesTo(result);
        }
        return result;
    }

    public boolean hasInvalidRange() {
        for (DayRowHolder holder : dayRows) {
            if (holder.hasInvalidRange()) {
                return true;
            }
        }
        return false;
    }

    private void copyMondayToSaturday() {
        DayRowHolder monday = dayRows[0];
        for (int dayIndex = 1; dayIndex <= 5; dayIndex++) {
            dayRows[dayIndex].copyFrom(monday);
        }
    }

    private final class DayRowHolder {
        private final int dayOfWeek;
        private final MaterialSwitch openSwitch;
        private final TextView closedLabel;
        private final View shiftsContainer;
        private final MaterialButton btnStart1;
        private final MaterialButton btnEnd1;
        private final MaterialButton btnAddShift2;
        private final View shift2Row;
        private final MaterialButton btnStart2;
        private final MaterialButton btnEnd2;

        private boolean open;
        private int start1 = DEFAULT_START_1;
        private int end1 = DEFAULT_END_1;
        private boolean hasShift2;
        private int start2 = DEFAULT_START_2;
        private int end2 = DEFAULT_END_2;
        private boolean binding;

        DayRowHolder(View root, int dayOfWeek, int dayLabelRes) {
            this.dayOfWeek = dayOfWeek;
            TextView dayName = root.findViewById(R.id.dayName);
            dayName.setText(dayLabelRes);
            closedLabel = root.findViewById(R.id.dayClosedLabel);
            openSwitch = root.findViewById(R.id.dayOpenSwitch);
            shiftsContainer = root.findViewById(R.id.shiftsContainer);
            btnStart1 = root.findViewById(R.id.btnShift1Start);
            btnEnd1 = root.findViewById(R.id.btnShift1End);
            btnAddShift2 = root.findViewById(R.id.btnAddShift2);
            shift2Row = root.findViewById(R.id.shift2Row);
            btnStart2 = root.findViewById(R.id.btnShift2Start);
            btnEnd2 = root.findViewById(R.id.btnShift2End);
            ImageButton btnRemoveShift2 = root.findViewById(R.id.btnRemoveShift2);

            openSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (binding) {
                    return;
                }
                open = isChecked;
                render();
            });
            btnStart1.setOnClickListener(v -> pickTime(start1, m -> {
                start1 = m;
                if (end1 <= start1) {
                    end1 = Math.min(1440, start1 + 60);
                }
                render();
            }));
            btnEnd1.setOnClickListener(v -> pickTime(end1, m -> {
                end1 = m == 0 ? 1440 : m;
                render();
            }));
            btnAddShift2.setOnClickListener(v -> {
                hasShift2 = true;
                if (start2 < end1) {
                    start2 = Math.min(1380, end1 + 60);
                    end2 = Math.min(1440, start2 + 180);
                }
                render();
            });
            btnStart2.setOnClickListener(v -> pickTime(start2, m -> {
                start2 = m;
                if (end2 <= start2) {
                    end2 = Math.min(1440, start2 + 60);
                }
                render();
            }));
            btnEnd2.setOnClickListener(v -> pickTime(end2, m -> {
                end2 = m == 0 ? 1440 : m;
                render();
            }));
            btnRemoveShift2.setOnClickListener(v -> {
                hasShift2 = false;
                render();
            });
        }

        void clear() {
            open = false;
            start1 = DEFAULT_START_1;
            end1 = DEFAULT_END_1;
            hasShift2 = false;
            start2 = DEFAULT_START_2;
            end2 = DEFAULT_END_2;
        }

        void addRange(int startMin, int endMin) {
            if (!open) {
                open = true;
                start1 = startMin;
                end1 = endMin;
            } else if (!hasShift2) {
                hasShift2 = true;
                start2 = startMin;
                end2 = endMin;
            }
        }

        void copyFrom(DayRowHolder source) {
            open = source.open;
            start1 = source.start1;
            end1 = source.end1;
            hasShift2 = source.hasShift2;
            start2 = source.start2;
            end2 = source.end2;
            render();
        }

        boolean hasInvalidRange() {
            return open && ((start1 >= end1) || (hasShift2 && start2 >= end2));
        }

        void appendRangesTo(List<TimeRange> out) {
            if (!open) {
                return;
            }
            if (start1 < end1) {
                out.add(new TimeRange(dayOfWeek, start1, end1));
            }
            if (hasShift2 && start2 < end2) {
                out.add(new TimeRange(dayOfWeek, start2, end2));
            }
        }

        void render() {
            binding = true;
            openSwitch.setChecked(open);
            binding = false;
            closedLabel.setVisibility(open ? View.GONE : View.VISIBLE);
            shiftsContainer.setVisibility(open ? View.VISIBLE : View.GONE);
            btnStart1.setText(formatTimeEdge(start1));
            btnEnd1.setText(formatTimeEdge(end1));
            btnAddShift2.setVisibility(hasShift2 ? View.GONE : View.VISIBLE);
            shift2Row.setVisibility(hasShift2 ? View.VISIBLE : View.GONE);
            btnStart2.setText(formatTimeEdge(start2));
            btnEnd2.setText(formatTimeEdge(end2));
        }

        private String formatTimeEdge(int min) {
            return min == 1440 ? "24:00" : Formats.formatTime(min);
        }

        private void pickTime(int initialMin, TimeSelectedCallback callback) {
            int safe = Math.min(initialMin, 1439);
            new TimePickerDialog(
                    getContext(),
                    (picker, hour, minute) -> callback.onSelected(hour * 60 + minute),
                    safe / 60,
                    safe % 60,
                    true
            ).show();
        }
    }

    private interface TimeSelectedCallback {
        void onSelected(int minutesFromMidnight);
    }
}
