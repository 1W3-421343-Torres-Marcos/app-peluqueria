package com.metanoia.app.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.pdf.PdfDocument;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.metanoia.app.R;
import com.metanoia.app.ui.cash.CashUiLabels;
import com.metanoia.app.ui.common.LogoView;
import com.metanoia.domain.model.Appointment;
import com.metanoia.domain.model.AppointmentBalance;
import com.metanoia.domain.model.AppointmentItem;
import com.metanoia.domain.model.CashMovement;
import com.metanoia.domain.model.CashMovementType;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.rules.AppointmentPaymentCalculator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Genera un recibo PDF de una página para turnos completados o pagados, con logo reemplazable
 * ({@link LogoView#LOGO_DRAWABLE_NAME}) y marca "METANOIA Hair Salon".
 */
public final class ReceiptPdfBuilder {

    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final int MARGIN = 44;
    private static final int LOGO_SIZE = 44;
    private static final int LINE_HEIGHT = 18;
    private static final String DEFAULT_BRAND = "METANOIA";
    private static final String DEFAULT_SUBTITLE = "Hair Salon";

    private ReceiptPdfBuilder() {
    }

    @NonNull
    public static File buildPdfFile(
            @NonNull Context context,
            @NonNull Settings settings,
            @NonNull Appointment appointment,
            @NonNull String clientName,
            @NonNull String professionalName,
            @NonNull List<CashMovement> movements
    ) throws IOException {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
                PAGE_WIDTH, PAGE_HEIGHT, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        try {
            drawPage(context, page.getCanvas(), settings, appointment,
                    clientName, professionalName, movements);
            document.finishPage(page);

            File cacheFolder = new File(context.getCacheDir(), "shared_backups");
            if (!cacheFolder.exists() && !cacheFolder.mkdirs()) {
                throw new IOException("No se pudo crear la carpeta temporal de recibos");
            }
            File outFile = new File(cacheFolder, "recibo_metanoia_" + appointment.id() + ".pdf");
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                document.writeTo(fos);
            }
            return outFile;
        } finally {
            document.close();
        }
    }

    private static void drawPage(
            @NonNull Context context,
            @NonNull Canvas canvas,
            @NonNull Settings settings,
            @NonNull Appointment appointment,
            @NonNull String clientName,
            @NonNull String professionalName,
            @NonNull List<CashMovement> movements
    ) {
        canvas.drawColor(Color.WHITE);

        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.BLACK);
        titlePaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        titlePaint.setTextSize(20f);
        titlePaint.setLetterSpacing(0.08f);

        Paint subtitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        subtitlePaint.setColor(Color.DKGRAY);
        subtitlePaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
        subtitlePaint.setTextSize(11f);

        Paint sectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sectionPaint.setColor(Color.BLACK);
        sectionPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        sectionPaint.setTextSize(12f);

        Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bodyPaint.setColor(Color.BLACK);
        bodyPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
        bodyPaint.setTextSize(11f);

        Paint amountPaint = new Paint(bodyPaint);
        amountPaint.setTextAlign(Paint.Align.RIGHT);

        Paint boldAmountPaint = new Paint(sectionPaint);
        boldAmountPaint.setTextAlign(Paint.Align.RIGHT);

        Paint dividerPaint = new Paint();
        dividerPaint.setColor(Color.LTGRAY);
        dividerPaint.setStrokeWidth(1f);

        int y = MARGIN;
        int headerTextX = MARGIN;
        int logoResId = context.getResources().getIdentifier(
                LogoView.LOGO_DRAWABLE_NAME, "drawable", context.getPackageName());
        if (logoResId != 0) {
            Drawable logo = ContextCompat.getDrawable(context, logoResId);
            if (logo != null) {
                Drawable wrapped = DrawableCompat.wrap(logo.mutate());
                DrawableCompat.setTint(wrapped, Color.BLACK);
                wrapped.setBounds(MARGIN, y, MARGIN + LOGO_SIZE, y + LOGO_SIZE);
                wrapped.draw(canvas);
                headerTextX = MARGIN + LOGO_SIZE + 14;
            }
        }

        String businessName = settings.businessName() == null
                || settings.businessName().trim().isEmpty()
                ? DEFAULT_BRAND
                : settings.businessName().trim().toUpperCase(Locale.ROOT);
        String subtitle = settings.subtitle() == null || settings.subtitle().trim().isEmpty()
                ? DEFAULT_SUBTITLE
                : settings.subtitle().trim();

        canvas.drawText(businessName + " " + subtitle, headerTextX, y + 20, titlePaint);
        int contactY = y + 36;
        StringBuilder contactLine = new StringBuilder();
        if (settings.address() != null && !settings.address().trim().isEmpty()) {
            contactLine.append(settings.address().trim());
        }
        if (settings.phone() != null && !settings.phone().trim().isEmpty()) {
            if (contactLine.length() > 0) {
                contactLine.append(" · ");
            }
            contactLine.append(settings.phone().trim());
        }
        if (settings.cuit() != null && !settings.cuit().trim().isEmpty()) {
            if (contactLine.length() > 0) {
                contactLine.append(" · ");
            }
            contactLine.append("CUIT ").append(settings.cuit().trim());
        }
        if (contactLine.length() > 0) {
            canvas.drawText(contactLine.toString(), headerTextX, contactY, subtitlePaint);
            contactY += 16;
        }

        y = Math.max(y + LOGO_SIZE + 12, contactY + 8);
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, dividerPaint);
        y += 22;

        canvas.drawText(context.getString(R.string.receipt_title), MARGIN, y, sectionPaint);
        y += LINE_HEIGHT + 4;
        y = drawKeyValue(canvas, bodyPaint,
                context.getString(R.string.receipt_date_label),
                Formats.formatDate(appointment.day()) + " · "
                        + Formats.formatTime(appointment.startMin()),
                y);
        y = drawKeyValue(canvas, bodyPaint,
                context.getString(R.string.receipt_client_label), clientName, y);
        y = drawKeyValue(canvas, bodyPaint,
                context.getString(R.string.receipt_professional_label), professionalName, y);

        y += 8;
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, dividerPaint);
        y += 20;

        canvas.drawText(context.getString(R.string.receipt_services_header), MARGIN, y, sectionPaint);
        y += LINE_HEIGHT;
        int rightX = PAGE_WIDTH - MARGIN;
        for (AppointmentItem item : appointment.items()) {
            canvas.drawText(item.serviceNameSnapshot(), MARGIN, y, bodyPaint);
            canvas.drawText(Formats.formatMoney(item.priceSnapshot()), rightX, y, amountPaint);
            y += LINE_HEIGHT;
        }

        AppointmentBalance balance = AppointmentPaymentCalculator.balanceOf(appointment, movements);
        y += 6;
        canvas.drawLine(MARGIN, y, rightX, y, dividerPaint);
        y += LINE_HEIGHT;
        canvas.drawText(context.getString(R.string.receipt_subtotal_label), MARGIN, y, bodyPaint);
        canvas.drawText(Formats.formatMoney(balance.itemsSubtotal()), rightX, y, amountPaint);
        y += LINE_HEIGHT;

        if (!balance.adjustment().isZero()) {
            canvas.drawText(
                    context.getString(R.string.receipt_adjustment_label), MARGIN, y, bodyPaint);
            canvas.drawText(Formats.formatMoney(balance.adjustment()), rightX, y, amountPaint);
            y += LINE_HEIGHT;
        }

        canvas.drawText(context.getString(R.string.receipt_total_label), MARGIN, y, sectionPaint);
        canvas.drawText(Formats.formatMoney(balance.total()), rightX, y, boldAmountPaint);
        y += LINE_HEIGHT + 8;

        if (!movements.isEmpty()) {
            canvas.drawLine(MARGIN, y, rightX, y, dividerPaint);
            y += 20;
            canvas.drawText(
                    context.getString(R.string.receipt_payments_header), MARGIN, y, sectionPaint);
            y += LINE_HEIGHT;
            for (CashMovement movement : movements) {
                String methodLabel = CashUiLabels.methodLabel(context, movement.method());
                String rowText = movement.type() == CashMovementType.REVERSAL
                        ? context.getString(
                                R.string.appointment_detail_reversal_row_prefix, methodLabel)
                        : context.getString(
                                R.string.appointment_detail_payment_line_format,
                                methodLabel,
                                Formats.formatDate(movement.day()));
                long signed = movement.type() == CashMovementType.REVERSAL
                        ? -movement.amount().cents()
                        : movement.amount().cents();
                canvas.drawText(rowText, MARGIN, y, bodyPaint);
                canvas.drawText(Formats.formatMoney(signed), rightX, y, amountPaint);
                y += LINE_HEIGHT;
            }
        }

        y += 6;
        canvas.drawLine(MARGIN, y, rightX, y, dividerPaint);
        y += LINE_HEIGHT;
        canvas.drawText(context.getString(R.string.receipt_paid_label), MARGIN, y, bodyPaint);
        canvas.drawText(Formats.formatMoney(balance.paid()), rightX, y, amountPaint);
        y += LINE_HEIGHT;
        canvas.drawText(context.getString(R.string.receipt_balance_label), MARGIN, y, sectionPaint);
        canvas.drawText(Formats.formatMoney(balance.balance()), rightX, y, boldAmountPaint);

        y += 32;
        canvas.drawText(context.getString(R.string.receipt_footer), MARGIN, y, subtitlePaint);
    }

    private static int drawKeyValue(
            @NonNull Canvas canvas,
            @NonNull Paint paint,
            @NonNull String label,
            @NonNull String value,
            int y) {
        canvas.drawText(label + ": " + value, MARGIN, y, paint);
        return y + LINE_HEIGHT;
    }
}
