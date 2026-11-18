package com.metanoia.app.ui.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.metanoia.app.R;
import com.metanoia.app.data.db.projection.AppointmentCardProjection;
import com.metanoia.app.util.Formats;
import com.metanoia.domain.model.AppointmentStatus;

/**
 * Tarjeta estándar de turno con barra lateral según estado ({@code docs/design.md} §7).
 */
public class AppointmentCardView extends MaterialCardView {

    private static final int CANCELLED_BAR_ALPHA = 102; // 40 % de opacidad

    private final View statusBar;
    private final TextView timeView;
    private final TextView clientView;
    private final TextView servicesView;
    private final TextView professionalView;
    private final StatusChip statusChip;
    private final MoneyTextView pendingBalanceView;

    public AppointmentCardView(@NonNull Context context) {
        this(context, null);
    }

    public AppointmentCardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, com.google.android.material.R.attr.materialCardViewStyle);
    }

    public AppointmentCardView(
            @NonNull Context context,
            @Nullable AttributeSet attrs,
            int defStyleAttr
    ) {
        super(context, attrs, defStyleAttr);
        setRadius(getResources().getDimension(R.dimen.radius_m));
        setCardElevation(0f);
        setStrokeWidth(getResources().getDimensionPixelSize(R.dimen.stroke_width));
        setStrokeColor(MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorOutline, Color.LTGRAY));
        setCardBackgroundColor(MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorSurface, Color.WHITE));
        setClickable(true);
        setFocusable(true);

        LayoutInflater.from(context).inflate(R.layout.view_appointment_card, this, true);
        statusBar = findViewById(R.id.appointmentCardBar);
        timeView = findViewById(R.id.appointmentCardTime);
        clientView = findViewById(R.id.appointmentCardClient);
        servicesView = findViewById(R.id.appointmentCardServices);
        professionalView = findViewById(R.id.appointmentCardProfessional);
        statusChip = findViewById(R.id.appointmentCardStatusChip);
        pendingBalanceView = findViewById(R.id.appointmentCardPendingBalance);
    }

    public void bind(@NonNull AppointmentCardProjection card) {
        Context context = getContext();
        String start = Formats.formatTime(card.startMin);
        String end = Formats.formatTime(card.endMin);
        timeView.setText(context.getString(R.string.agenda_time_stacked, start, end));
        clientView.setText(card.clientName);
        servicesView.setText(card.servicesSummary);
        professionalView.setText(card.professionalName);

        AppointmentStatus status = AppointmentStatus.valueOf(card.status);
        statusChip.setVariant(toChipVariant(status));
        statusBar.setBackground(buildSideBarDrawable(status));

        long pending = card.pendingBalanceCents();
        if (status == AppointmentStatus.COMPLETED && pending > 0L) {
            pendingBalanceView.setMoneyCents(pending);
            pendingBalanceView.setVisibility(View.VISIBLE);
        } else {
            pendingBalanceView.setVisibility(View.GONE);
        }
    }

    private Drawable buildSideBarDrawable(AppointmentStatus status) {
        Context context = getContext();
        int colorPrimary = MaterialColors.getColor(
                this, androidx.appcompat.R.attr.colorPrimary, Color.BLACK);
        switch (status) {
            case PENDING:
                return new ColorDrawable(ContextCompat.getColor(context, R.color.gray400));
            case CONFIRMED:
                return new ColorDrawable(ContextCompat.getColor(context, R.color.accent));
            case COMPLETED:
                return new StripedBarDrawable(colorPrimary);
            case PAID:
                return new ColorDrawable(colorPrimary);
            case CANCELLED:
                int base = ContextCompat.getColor(context, R.color.accent_cancelled);
                int translucent = Color.argb(
                        CANCELLED_BAR_ALPHA,
                        Color.red(base),
                        Color.green(base),
                        Color.blue(base));
                return new ColorDrawable(translucent);
            case NO_SHOW:
            default:
                return new ColorDrawable(ContextCompat.getColor(context, R.color.absent_bar));
        }
    }

    private static StatusChip.Variant toChipVariant(AppointmentStatus status) {
        switch (status) {
            case CONFIRMED:
                return StatusChip.Variant.CONFIRMED;
            case COMPLETED:
                return StatusChip.Variant.COMPLETED;
            case PAID:
                return StatusChip.Variant.PAID;
            case CANCELLED:
                return StatusChip.Variant.CANCELLED;
            case NO_SHOW:
                return StatusChip.Variant.NO_SHOW;
            case PENDING:
            default:
                return StatusChip.Variant.PENDING;
        }
    }

    private static final class StripedBarDrawable extends Drawable {

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        StripedBarDrawable(@ColorInt int color) {
            paint.setColor(color);
            paint.setStyle(Paint.Style.STROKE);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            Rect bounds = getBounds();
            if (bounds.isEmpty()) {
                return;
            }
            canvas.save();
            canvas.clipRect(bounds);
            float step = Math.max(4f, bounds.width() * 2.5f);
            paint.setStrokeWidth(Math.max(2f, bounds.width()));
            for (float y = -bounds.width(); y < bounds.height() + step; y += step) {
                canvas.drawLine(
                        bounds.left - bounds.width(),
                        y + bounds.width(),
                        bounds.right + bounds.width(),
                        y - bounds.width(),
                        paint);
            }
            canvas.restore();
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
        }

        @Override
        @SuppressWarnings("deprecation")
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
