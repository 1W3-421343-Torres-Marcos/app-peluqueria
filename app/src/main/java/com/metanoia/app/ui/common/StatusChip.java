package com.metanoia.app.ui.common;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.google.android.material.color.MaterialColors;
import com.metanoia.app.R;

/**
 * Chip píldora reutilizable para estados de turno y alertas de stock ({@code docs/design.md} §7).
 */
public class StatusChip extends LinearLayout {

    public enum Variant {
        PENDING(R.string.status_pending, R.drawable.ic_clock),
        CONFIRMED(R.string.status_confirmed, R.drawable.ic_check),
        COMPLETED(R.string.status_completed, R.drawable.ic_check),
        PAID(R.string.status_paid, R.drawable.ic_paid),
        CANCELLED(R.string.status_cancelled, R.drawable.ic_close),
        NO_SHOW(R.string.status_no_show, R.drawable.ic_person_off),
        LOW_STOCK(R.string.status_low_stock, R.drawable.ic_warning);

        @StringRes final int labelRes;
        @DrawableRes final int iconRes;

        Variant(@StringRes int labelRes, @DrawableRes int iconRes) {
            this.labelRes = labelRes;
            this.iconRes = iconRes;
        }
    }

    private final ImageView iconView;
    private final TextView labelView;
    private final GradientDrawable backgroundDrawable;
    private final int strokeWidth;
    private final int colorPrimary;
    private final int colorOnPrimary;
    private final int colorOnSurface;
    private final int colorOnSurfaceVariant;
    private final int gray400;
    private final int accent;
    private final int onAccent;
    private final int accentContainer;
    private final int absentBg;
    private Variant currentVariant = Variant.PENDING;
    private boolean styleInitialized;

    public StatusChip(@NonNull Context context) {
        this(context, null);
    }

    public StatusChip(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StatusChip(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.chip_height));
        int horizontalPad = getResources().getDimensionPixelSize(R.dimen.space_s);
        int verticalPad = getResources().getDimensionPixelSize(R.dimen.space_xs);
        setPadding(horizontalPad, verticalPad, horizontalPad, verticalPad);

        LayoutInflater.from(context).inflate(R.layout.view_status_chip, this, true);
        iconView = findViewById(R.id.statusChipIcon);
        labelView = findViewById(R.id.statusChipLabel);

        strokeWidth = getResources().getDimensionPixelSize(R.dimen.stroke_width);
        colorPrimary = MaterialColors.getColor(
                this, androidx.appcompat.R.attr.colorPrimary, Color.BLACK);
        colorOnPrimary = MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorOnPrimary, Color.WHITE);
        colorOnSurface = MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorOnSurface, Color.BLACK);
        colorOnSurfaceVariant = MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.DKGRAY);
        gray400 = ContextCompat.getColor(context, R.color.gray400);
        accent = ContextCompat.getColor(context, R.color.accent);
        onAccent = ContextCompat.getColor(context, R.color.on_accent);
        accentContainer = ContextCompat.getColor(context, R.color.accent_container);
        absentBg = ContextCompat.getColor(context, R.color.absent_chip_bg);

        backgroundDrawable = new GradientDrawable();
        backgroundDrawable.setShape(GradientDrawable.RECTANGLE);
        backgroundDrawable.setCornerRadius(getResources().getDimension(R.dimen.radius_pill));
        setBackground(backgroundDrawable);

        Variant initial = readVariant(context, attrs);
        setVariant(initial);
    }

    private Variant readVariant(Context context, @Nullable AttributeSet attrs) {
        if (attrs == null) {
            return Variant.PENDING;
        }
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.StatusChip);
        try {
            int ordinal = array.getInt(R.styleable.StatusChip_statusVariant, 0);
            Variant[] values = Variant.values();
            if (ordinal >= 0 && ordinal < values.length) {
                return values[ordinal];
            }
            return Variant.PENDING;
        } finally {
            array.recycle();
        }
    }

    public Variant getVariant() {
        return currentVariant;
    }

    public void setVariant(@NonNull Variant variant) {
        if (styleInitialized && this.currentVariant == variant) {
            return;
        }
        this.styleInitialized = true;
        this.currentVariant = variant;
        labelView.setText(variant.labelRes);
        iconView.setImageResource(variant.iconRes);
        setContentDescription(getContext().getString(variant.labelRes));

        int flags = labelView.getPaintFlags();
        if (variant == Variant.CANCELLED) {
            labelView.setPaintFlags(flags | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            labelView.setPaintFlags(flags & ~Paint.STRIKE_THRU_TEXT_FLAG);
        }

        applyStyle(variant);
    }

    private void applyStyle(Variant variant) {
        switch (variant) {
            case PENDING:
                applyColors(Color.TRANSPARENT, strokeWidth, gray400, colorOnSurfaceVariant);
                break;
            case CONFIRMED:
                applyColors(accent, 0, Color.TRANSPARENT, onAccent);
                break;
            case COMPLETED:
                applyColors(Color.TRANSPARENT, strokeWidth, colorPrimary, colorPrimary);
                break;
            case PAID:
                applyColors(colorPrimary, 0, Color.TRANSPARENT, colorOnPrimary);
                break;
            case CANCELLED:
                applyColors(Color.TRANSPARENT, strokeWidth, accent, accent);
                break;
            case NO_SHOW:
                applyColors(absentBg, 0, Color.TRANSPARENT, colorOnSurface);
                break;
            case LOW_STOCK:
                applyColors(accentContainer, 0, Color.TRANSPARENT, accent);
                break;
        }
    }

    private void applyColors(
            @ColorInt int fillColor,
            int strokePx,
            @ColorInt int strokeColor,
            @ColorInt int contentColor
    ) {
        backgroundDrawable.setColor(fillColor);
        backgroundDrawable.setStroke(strokePx, strokeColor);
        labelView.setTextColor(contentColor);
        ImageViewCompat.setImageTintList(iconView, ColorStateList.valueOf(contentColor));
    }
}
