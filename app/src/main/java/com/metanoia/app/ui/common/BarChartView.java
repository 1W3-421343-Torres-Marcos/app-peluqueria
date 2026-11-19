package com.metanoia.app.ui.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;

import com.google.android.material.color.MaterialColors;
import com.metanoia.app.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Gráfico de barras monocromático propio sin librerías externas, compatible con Claro/Oscuro.
 */
public class BarChartView extends View {

    private static final int ALPHA_MUTED_FILL = 72;
    private static final int ALPHA_GUIDE = 90;
    private static final float STROKE_HAIRLINE = 1.5f;

    public static final class Entry {
        @NonNull
        public final String label;
        public final long value;
        @NonNull
        public final String formattedValue;
        public final boolean muted;

        public Entry(
                @NonNull String label,
                long value,
                @NonNull String formattedValue,
                boolean muted
        ) {
            this.label = Objects.requireNonNull(label, "label");
            this.value = value;
            this.formattedValue = Objects.requireNonNull(formattedValue, "formattedValue");
            this.muted = muted;
        }
    }

    public static final class AxisScale {
        public final long minValue;
        public final long maxValue;
        public final float baselineFraction;

        public AxisScale(long minValue, long maxValue, float baselineFraction) {
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.baselineFraction = baselineFraction;
        }
    }

    private final Paint solidBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mutedBarFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mutedBarStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint baselinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint guidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF barRect = new RectF();

    private List<Entry> entries = Collections.emptyList();
    @Nullable
    private Long maxValueOverride;
    private AxisScale axisScale = new AxisScale(0L, 1L, 1f);
    private float paddingPx;
    private float barRadiusPx;
    private float minSlotWidthPx;

    public BarChartView(@NonNull Context context) {
        this(context, null);
    }

    public BarChartView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BarChartView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(@NonNull Context context) {
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        paddingPx = getResources().getDimension(R.dimen.bar_chart_padding);
        barRadiusPx = getResources().getDimension(R.dimen.bar_chart_bar_radius);
        minSlotWidthPx = getResources().getDimension(R.dimen.bar_chart_min_slot_width);

        int primary = MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorPrimary, Color.BLACK);
        int onSurface = MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorOnSurface, Color.BLACK);
        int onSurfaceVariant = MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.DKGRAY);
        int outline = MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorOutline, Color.LTGRAY);

        solidBarPaint.setStyle(Paint.Style.FILL);
        solidBarPaint.setColor(primary);

        mutedBarFillPaint.setStyle(Paint.Style.FILL);
        mutedBarFillPaint.setColor(ColorUtils.setAlphaComponent(onSurfaceVariant, ALPHA_MUTED_FILL));

        mutedBarStrokePaint.setStyle(Paint.Style.STROKE);
        mutedBarStrokePaint.setStrokeWidth(STROKE_HAIRLINE);
        mutedBarStrokePaint.setColor(onSurfaceVariant);

        baselinePaint.setStyle(Paint.Style.STROKE);
        baselinePaint.setStrokeWidth(STROKE_HAIRLINE);
        baselinePaint.setColor(onSurfaceVariant);

        guidePaint.setStyle(Paint.Style.STROKE);
        guidePaint.setStrokeWidth(1f);
        guidePaint.setColor(ColorUtils.setAlphaComponent(outline, ALPHA_GUIDE));

        labelPaint.setColor(onSurfaceVariant);
        labelPaint.setTextSize(getResources().getDimension(R.dimen.bar_chart_label_text_size));
        labelPaint.setTextAlign(Paint.Align.CENTER);

        valuePaint.setColor(onSurface);
        valuePaint.setTextSize(getResources().getDimension(R.dimen.bar_chart_value_text_size));
        valuePaint.setTextAlign(Paint.Align.CENTER);

        setContentDescription(context.getString(R.string.reports_chart_cd_empty));
    }

    public void setData(@NonNull List<Entry> newEntries, @Nullable Long maxValueOverride) {
        this.entries = new ArrayList<>(Objects.requireNonNull(newEntries, "newEntries"));
        this.maxValueOverride = maxValueOverride;
        this.axisScale = computeAxisScale(this.entries, maxValueOverride);
        updateContentDescription();
        invalidate();
    }

    public static AxisScale computeAxisScale(
            @NonNull List<Entry> entries,
            @Nullable Long maxValueOverride
    ) {
        long minVal = 0L;
        long maxVal = maxValueOverride != null && maxValueOverride > 0L ? maxValueOverride : 0L;
        for (Entry entry : entries) {
            minVal = Math.min(minVal, entry.value);
            if (maxValueOverride == null) {
                maxVal = Math.max(maxVal, entry.value);
            } else {
                maxVal = Math.max(maxVal, entry.value);
            }
        }
        if (minVal == maxVal) {
            maxVal = minVal + 1L;
        }
        long span = maxVal - minVal;
        float baselineFraction = span <= 0L ? 1f : (float) maxVal / (float) span;
        return new AxisScale(minVal, maxVal, baselineFraction);
    }

    public static int computeLabelStep(int count, float availableWidthPx, float minSlotPx) {
        if (count <= 0 || availableWidthPx <= 0f || minSlotPx <= 0f) {
            return 1;
        }
        int maxLabels = Math.max(1, (int) Math.floor(availableWidthPx / minSlotPx));
        return count <= maxLabels ? 1 : (int) Math.ceil((double) count / (double) maxLabels);
    }

    private void updateContentDescription() {
        if (entries.isEmpty()) {
            setContentDescription(getContext().getString(R.string.reports_chart_cd_empty));
            return;
        }
        int previewCount = Math.min(4, entries.size());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < previewCount; i++) {
            Entry e = entries.get(i);
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(getContext().getString(
                    R.string.reports_chart_cd_entry_format, e.label, e.formattedValue));
        }
        setContentDescription(getContext().getString(
                R.string.reports_chart_cd_format, entries.size(), sb.toString()));
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (entries.isEmpty()) {
            return;
        }
        float left = getPaddingLeft() + paddingPx;
        float right = getWidth() - getPaddingRight() - paddingPx;
        float top = getPaddingTop() + paddingPx + valuePaint.getTextSize() * 1.4f;
        float bottom = getHeight() - getPaddingBottom() - paddingPx - labelPaint.getTextSize() * 1.8f;
        if (right <= left || bottom <= top) {
            return;
        }

        float plotHeight = bottom - top;
        float baselineY = top + plotHeight * axisScale.baselineFraction;
        canvas.drawLine(left, top, right, top, guidePaint);
        canvas.drawLine(left, (top + bottom) * 0.5f, right, (top + bottom) * 0.5f, guidePaint);
        canvas.drawLine(left, baselineY, right, baselineY, baselinePaint);

        drawBarsAndLabels(canvas, left, right, top, bottom, baselineY);
    }

    private void drawBarsAndLabels(
            @NonNull Canvas canvas,
            float left,
            float right,
            float top,
            float bottom,
            float baselineY
    ) {
        int count = entries.size();
        float plotWidth = right - left;
        float slotWidth = plotWidth / count;
        float barWidth = Math.max(4f, Math.min(slotWidth * 0.62f, paddingPx * 4f));
        long span = Math.max(1L, axisScale.maxValue - axisScale.minValue);
        float pxPerUnit = (bottom - top) / (float) span;

        int step = computeLabelStep(count, plotWidth, minSlotWidthPx);
        boolean drawTopValues = count <= 10 && slotWidth >= minSlotWidthPx * 0.85f;
        float labelY = getHeight() - getPaddingBottom() - paddingPx * 0.4f;

        for (int i = 0; i < count; i++) {
            Entry entry = entries.get(i);
            float centerX = left + slotWidth * (i + 0.5f);
            float barLeft = centerX - barWidth * 0.5f;
            float barRight = centerX + barWidth * 0.5f;
            float targetY = baselineY - (entry.value * pxPerUnit);

            float rectTop = entry.value >= 0L ? targetY : baselineY;
            float rectBottom = entry.value >= 0L ? baselineY : targetY;
            if (Math.abs(rectBottom - rectTop) < 2f && entry.value != 0L) {
                if (entry.value > 0L) {
                    rectTop = rectBottom - 2f;
                } else {
                    rectBottom = rectTop + 2f;
                }
            }
            barRect.set(barLeft, rectTop, barRight, rectBottom);
            if (entry.value != 0L || entry.muted) {
                if (entry.muted || entry.value < 0L) {
                    canvas.drawRoundRect(barRect, barRadiusPx, barRadiusPx, mutedBarFillPaint);
                    canvas.drawRoundRect(barRect, barRadiusPx, barRadiusPx, mutedBarStrokePaint);
                } else {
                    canvas.drawRoundRect(barRect, barRadiusPx, barRadiusPx, solidBarPaint);
                }
            }

            if (i % step == 0 || i == count - 1) {
                canvas.drawText(truncateText(entry.label, 8), centerX, labelY, labelPaint);
            }
            if (drawTopValues) {
                float valueY = entry.value >= 0L
                        ? Math.max(top - 4f, rectTop - 6f)
                        : Math.min(bottom + valuePaint.getTextSize(), rectBottom + valuePaint.getTextSize());
                canvas.drawText(truncateText(entry.formattedValue, 10), centerX, valueY, valuePaint);
            }
        }
    }

    @NonNull
    private static String truncateText(@NonNull String text, int maxChars) {
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars - 1) + "…";
    }
}
