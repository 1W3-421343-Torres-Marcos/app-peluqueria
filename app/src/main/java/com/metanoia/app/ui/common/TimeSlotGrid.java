package com.metanoia.app.ui.common;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.TextViewCompat;

import com.google.android.material.color.MaterialColors;
import com.metanoia.app.R;
import com.metanoia.app.util.Formats;

import java.util.List;

/**
 * Grilla reutilizable de bloques horarios según {@code docs/design.md} §7:
 * libre con borde, superpuesto con otro turno marcado con advertencia y seleccionado en {@code colorPrimary}.
 */
public class TimeSlotGrid extends LinearLayout {

    private static final int COLUMNS = 4;

    public static final class SlotOption {
        public final int startMin;
        public final boolean available;
        public final boolean overlapsExisting;

        public SlotOption(int startMin, boolean available) {
            this(startMin, available, false);
        }

        public SlotOption(int startMin, boolean available, boolean overlapsExisting) {
            this.startMin = startMin;
            this.available = available;
            this.overlapsExisting = overlapsExisting;
        }
    }

    public interface OnSlotSelectedListener {
        void onSlotSelected(int startMin);
    }

    @Nullable
    private OnSlotSelectedListener listener;
    @Nullable
    private Integer selectedStartMin;

    public TimeSlotGrid(@NonNull Context context) {
        this(context, null);
    }

    public TimeSlotGrid(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TimeSlotGrid(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);
    }

    public void setOnSlotSelectedListener(@Nullable OnSlotSelectedListener listener) {
        this.listener = listener;
    }

    @Nullable
    public Integer getSelectedStartMin() {
        return selectedStartMin;
    }

    public void setSlots(@NonNull List<SlotOption> slots, @Nullable Integer selectedStartMin) {
        this.selectedStartMin = selectedStartMin;
        removeAllViews();
        if (slots.isEmpty()) {
            return;
        }
        int rowSpacing = getResources().getDimensionPixelSize(R.dimen.space_s);
        int cellSpacing = getResources().getDimensionPixelSize(R.dimen.space_xs);

        int index = 0;
        while (index < slots.size()) {
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(HORIZONTAL);
            LayoutParams rowParams = new LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            if (index > 0) {
                rowParams.topMargin = rowSpacing;
            }
            row.setLayoutParams(rowParams);

            for (int col = 0; col < COLUMNS; col++) {
                LayoutParams cellParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
                if (col > 0) {
                    cellParams.setMarginStart(cellSpacing);
                }
                if (index < slots.size()) {
                    SlotOption slot = slots.get(index++);
                    TextView pill = buildSlotView(slot,
                            selectedStartMin != null && selectedStartMin == slot.startMin);
                    pill.setLayoutParams(cellParams);
                    row.addView(pill);
                } else {
                    android.view.View spacer = new android.view.View(getContext());
                    spacer.setLayoutParams(cellParams);
                    row.addView(spacer);
                }
            }
            addView(row);
        }
    }

    private TextView buildSlotView(@NonNull SlotOption slot, boolean selected) {
        Context context = getContext();
        TextView view = new TextView(context);
        view.setText(slot.overlapsExisting
                ? Formats.formatTime(slot.startMin) + " !"
                : Formats.formatTime(slot.startMin));
        view.setGravity(Gravity.CENTER);
        view.setMinHeight(getResources().getDimensionPixelSize(R.dimen.touch_target_min));
        TextViewCompat.setTextAppearance(view, R.style.TextAppearance_Metanoia_Money);
        view.setFontFeatureSettings("tnum");

        int colorPrimary = MaterialColors.getColor(
                this, androidx.appcompat.R.attr.colorPrimary, Color.BLACK);
        int colorOnPrimary = MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorOnPrimary, Color.WHITE);
        int colorSurface = MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorSurface, Color.WHITE);
        int colorSurfaceVariant = MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorSurfaceVariant, Color.LTGRAY);
        int colorOutline = MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorOutline, Color.GRAY);
        int colorOnSurface = MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorOnSurface, Color.BLACK);
        int colorOnSurfaceVariant = MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.DKGRAY);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(getResources().getDimension(R.dimen.radius_s));
        int stroke = getResources().getDimensionPixelSize(R.dimen.stroke_width);

        if (selected && slot.available) {
            bg.setColor(colorPrimary);
            view.setTextColor(colorOnPrimary);
            view.setAlpha(1f);
        } else if (slot.available) {
            if (slot.overlapsExisting) {
                bg.setColor(context.getColor(R.color.accent_container));
                bg.setStroke(stroke, context.getColor(R.color.accent));
                view.setTextColor(context.getColor(R.color.accent));
            } else {
                bg.setColor(colorSurface);
                bg.setStroke(stroke, colorOutline);
                view.setTextColor(colorOnSurface);
            }
            view.setAlpha(1f);
        } else {
            bg.setColor(colorSurfaceVariant);
            view.setTextColor(colorOnSurfaceVariant);
            view.setAlpha(0.55f);
        }
        view.setBackground(bg);

        if (slot.available) {
            TypedValue out = new TypedValue();
            if (context.getTheme().resolveAttribute(
                    android.R.attr.selectableItemBackground, out, true)) {
                view.setForeground(androidx.core.content.ContextCompat.getDrawable(
                        context, out.resourceId));
            }
            view.setClickable(true);
            view.setFocusable(true);
            view.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSlotSelected(slot.startMin);
                }
            });
        } else {
            view.setEnabled(false);
        }
        return view;
    }
}
