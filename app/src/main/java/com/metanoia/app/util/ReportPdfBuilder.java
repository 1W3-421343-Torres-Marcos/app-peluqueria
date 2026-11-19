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
import com.metanoia.app.ui.common.LogoView;
import com.metanoia.app.util.ReportPdfSpec.KpiPair;
import com.metanoia.app.util.ReportPdfSpec.TableColumn;
import com.metanoia.app.util.ReportPdfSpec.TableSection;
import com.metanoia.domain.model.Settings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;

/**
 * Constructor nativo de reportes PDF A4 multipágina (blanco y negro, identidad Metanoia).
 */
public final class ReportPdfBuilder {

    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final int MARGIN = 40;
    private static final int CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2;
    private static final int LOGO_SIZE = 38;
    private static final int COLOR_GRAY_50 = 0xFFF5F5F5;
    private static final int COLOR_GRAY_200 = 0xFFD6D6D6;
    private static final int COLOR_GRAY_600 = 0xFF616161;
    private static final float CELL_PAD = 6f;

    private ReportPdfBuilder() {
    }

    public static void writePdf(
            @NonNull Context context,
            @NonNull Settings settings,
            @NonNull ReportPdfSpec spec,
            long generatedAtMillis,
            @NonNull OutputStream outputStream
    ) throws IOException {
        PdfDocument document = renderDocument(context, settings, spec, generatedAtMillis);
        try {
            document.writeTo(outputStream);
            outputStream.flush();
        } finally {
            document.close();
        }
    }

    @NonNull
    public static File buildTempPdfFile(
            @NonNull Context context,
            @NonNull Settings settings,
            @NonNull ReportPdfSpec spec,
            long generatedAtMillis,
            @NonNull String fileName
    ) throws IOException {
        File dir = new File(context.getCacheDir(), "shared_reports");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("No se pudo crear la carpeta temporal de PDF");
        }
        File outFile = new File(dir, fileName);
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            writePdf(context, settings, spec, generatedAtMillis, fos);
        }
        return outFile;
    }

    @NonNull
    private static PdfDocument renderDocument(
            @NonNull Context context,
            @NonNull Settings settings,
            @NonNull ReportPdfSpec spec,
            long generatedAtMillis
    ) {
        PdfDocument document = new PdfDocument();
        PdfPaints paints = new PdfPaints();
        List<List<TableSection>> pages = spec.paginate();
        int totalPages = pages.size();
        String generatedLabel = Formats.formatDateTime(generatedAtMillis);

        for (int i = 0; i < totalPages; i++) {
            int pageNumber = i + 1;
            PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(
                    PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create();
            PdfDocument.Page page = document.startPage(info);
            try {
                drawPage(context, page.getCanvas(), paints, settings, spec,
                        pages.get(i), generatedLabel, pageNumber, totalPages);
            } finally {
                document.finishPage(page);
            }
        }
        return document;
    }

    private static void drawPage(
            @NonNull Context context,
            @NonNull Canvas canvas,
            @NonNull PdfPaints paints,
            @NonNull Settings settings,
            @NonNull ReportPdfSpec spec,
            @NonNull List<TableSection> sections,
            @NonNull String generatedLabel,
            int pageNumber,
            int totalPages
    ) {
        canvas.drawColor(Color.WHITE);
        int y = pageNumber == 1
                ? drawFullHeader(context, canvas, paints, settings, spec, generatedLabel)
                : drawCompactHeader(canvas, paints, spec, generatedLabel);

        if (pageNumber == 1 && !spec.kpis.isEmpty()) {
            y = drawKpiRow(canvas, paints, spec.kpis, y);
        }

        for (TableSection section : sections) {
            y = drawTableSection(canvas, paints, section, y);
        }

        if (pageNumber == totalPages && !spec.notesFooter.isEmpty()) {
            y += 6;
            for (String note : spec.notesFooter) {
                canvas.drawText(truncateToWidth(note, paints.muted, CONTENT_WIDTH),
                        MARGIN, y, paints.muted);
                y += 14;
            }
        }

        drawPageFooter(canvas, paints, pageNumber, totalPages);
    }

    private static int drawFullHeader(
            @NonNull Context context,
            @NonNull Canvas canvas,
            @NonNull PdfPaints paints,
            @NonNull Settings settings,
            @NonNull ReportPdfSpec spec,
            @NonNull String generatedLabel
    ) {
        int y = MARGIN;
        int textX = MARGIN;
        int logoId = context.getResources().getIdentifier(
                LogoView.LOGO_DRAWABLE_NAME, "drawable", context.getPackageName());
        if (logoId != 0) {
            Drawable logo = ContextCompat.getDrawable(context, logoId);
            if (logo != null) {
                Drawable wrapped = DrawableCompat.wrap(logo.mutate());
                DrawableCompat.setTint(wrapped, Color.BLACK);
                wrapped.setBounds(MARGIN, y, MARGIN + LOGO_SIZE, y + LOGO_SIZE);
                wrapped.draw(canvas);
                textX = MARGIN + LOGO_SIZE + 12;
            }
        }

        String rawName = settings.businessName() == null || settings.businessName().trim().isEmpty()
                ? "METANOIA"
                : settings.businessName().trim().toUpperCase(Locale.ROOT);
        String rawSub = settings.subtitle() == null || settings.subtitle().trim().isEmpty()
                ? "Hair Salon"
                : settings.subtitle().trim();

        canvas.drawText(rawName + " " + rawSub, textX, y + 18, paints.brandTitle);
        canvas.drawText("Emitido: " + generatedLabel, textX, y + 34, paints.muted);

        y = Math.max(y + LOGO_SIZE + 10, y + 46);
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paints.border);
        y += 20;

        canvas.drawText(spec.title, MARGIN, y, paints.reportTitle);
        y += 16;
        canvas.drawText(truncateToWidth(spec.filterSummary, paints.muted, CONTENT_WIDTH),
                MARGIN, y, paints.muted);
        return y + 16;
    }

    private static int drawCompactHeader(
            @NonNull Canvas canvas,
            @NonNull PdfPaints paints,
            @NonNull ReportPdfSpec spec,
            @NonNull String generatedLabel
    ) {
        int y = MARGIN + 10;
        canvas.drawText(spec.title, MARGIN, y, paints.sectionTitle);
        canvas.drawText(generatedLabel, PAGE_WIDTH - MARGIN, y, paints.mutedRight);
        y += 8;
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paints.border);
        return y + 18;
    }

    private static int drawKpiRow(
            @NonNull Canvas canvas,
            @NonNull PdfPaints paints,
            @NonNull List<KpiPair> kpis,
            int startY
    ) {
        int boxHeight = 44;
        int gap = 8;
        float boxWidth = (CONTENT_WIDTH - gap * (kpis.size() - 1)) / (float) kpis.size();
        for (int i = 0; i < kpis.size(); i++) {
            KpiPair kpi = kpis.get(i);
            float x = MARGIN + i * (boxWidth + gap);
            canvas.drawRect(x, startY, x + boxWidth, startY + boxHeight, paints.headerFill);
            canvas.drawRect(x, startY, x + boxWidth, startY + boxHeight, paints.border);
            canvas.drawText(truncateToWidth(kpi.label, paints.muted, boxWidth - 12f),
                    x + 8f, startY + 16f, paints.muted);
            canvas.drawText(truncateToWidth(kpi.value, paints.sectionTitle, boxWidth - 12f),
                    x + 8f, startY + 34f, paints.sectionTitle);
        }
        return startY + boxHeight + 16;
    }

    private static int drawTableSection(
            @NonNull Canvas canvas,
            @NonNull PdfPaints paints,
            @NonNull TableSection section,
            int startY
    ) {
        int y = startY;
        if (section.sectionTitle != null && !section.sectionTitle.isEmpty()) {
            canvas.drawText(section.sectionTitle, MARGIN, y + 14, paints.sectionTitle);
            y += ReportPdfSpec.SECTION_TITLE_HEIGHT_PT;
        }

        float[] colWidths = computeColumnWidths(section.columns);
        y = drawTableRow(canvas, paints, section.columns, colWidths, null, y, true);
        for (List<String> row : section.rows) {
            y = drawTableRow(canvas, paints, section.columns, colWidths, row, y, false);
        }
        return y + ReportPdfSpec.SECTION_GAP_PT;
    }

    private static int drawTableRow(
            @NonNull Canvas canvas,
            @NonNull PdfPaints paints,
            @NonNull List<TableColumn> columns,
            @NonNull float[] colWidths,
            List<String> rowValues,
            int y,
            boolean isHeader
    ) {
        int height = isHeader
                ? ReportPdfSpec.TABLE_HEADER_HEIGHT_PT
                : ReportPdfSpec.TABLE_ROW_HEIGHT_PT;
        if (isHeader) {
            canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + height, paints.headerFill);
        }
        canvas.drawLine(MARGIN, y + height, PAGE_WIDTH - MARGIN, y + height, paints.border);
        if (isHeader) {
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paints.border);
        }

        float x = MARGIN;
        float textY = y + height - 6f;
        for (int i = 0; i < columns.size(); i++) {
            TableColumn col = columns.get(i);
            float w = colWidths[i];
            String text = isHeader
                    ? col.title
                    : (rowValues != null && i < rowValues.size() ? rowValues.get(i) : "");
            Paint basePaint = isHeader ? paints.tableHeader : paints.body;
            String clipped = truncateToWidth(text, basePaint, w - CELL_PAD * 2f);
            if (col.alignRight) {
                Paint rightPaint = isHeader ? paints.tableHeaderRight : paints.bodyRight;
                canvas.drawText(clipped, x + w - CELL_PAD, textY, rightPaint);
            } else {
                canvas.drawText(clipped, x + CELL_PAD, textY, basePaint);
            }
            x += w;
        }
        return y + height;
    }

    private static void drawPageFooter(
            @NonNull Canvas canvas,
            @NonNull PdfPaints paints,
            int pageNumber,
            int totalPages
    ) {
        int footerY = PAGE_HEIGHT - 24;
        canvas.drawLine(MARGIN, footerY - 12, PAGE_WIDTH - MARGIN, footerY - 12, paints.border);
        canvas.drawText("Metanoia Hair Salon", MARGIN, footerY, paints.muted);
        canvas.drawText("Página " + pageNumber + " de " + totalPages,
                PAGE_WIDTH - MARGIN, footerY, paints.mutedRight);
    }

    @NonNull
    private static float[] computeColumnWidths(@NonNull List<TableColumn> columns) {
        float sum = 0f;
        for (TableColumn c : columns) {
            sum += c.widthWeight;
        }
        float[] widths = new float[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            widths[i] = sum <= 0f
                    ? (CONTENT_WIDTH / (float) columns.size())
                    : (CONTENT_WIDTH * (columns.get(i).widthWeight / sum));
        }
        return widths;
    }

    @NonNull
    private static String truncateToWidth(@NonNull String text, @NonNull Paint paint, float maxWidth) {
        if (maxWidth <= 4f || text.isEmpty() || paint.measureText(text) <= maxWidth) {
            return text;
        }
        int count = paint.breakText(text, true, Math.max(0f, maxWidth - 8f), null);
        if (count <= 0) {
            return "…";
        }
        return text.substring(0, count).trim() + "…";
    }

    private static final class PdfPaints {
        final Paint brandTitle = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint reportTitle = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint sectionTitle = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint tableHeader = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint tableHeaderRight = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint body = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint bodyRight = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint muted = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint mutedRight = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint border = new Paint();
        final Paint headerFill = new Paint();

        PdfPaints() {
            brandTitle.setColor(Color.BLACK);
            brandTitle.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
            brandTitle.setTextSize(16f);
            brandTitle.setLetterSpacing(0.06f);

            reportTitle.setColor(Color.BLACK);
            reportTitle.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
            reportTitle.setTextSize(15f);

            sectionTitle.setColor(Color.BLACK);
            sectionTitle.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
            sectionTitle.setTextSize(11f);

            tableHeader.setColor(Color.BLACK);
            tableHeader.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
            tableHeader.setTextSize(9.5f);
            tableHeaderRight.set(tableHeader);
            tableHeaderRight.setTextAlign(Paint.Align.RIGHT);

            body.setColor(Color.BLACK);
            body.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
            body.setTextSize(9.5f);
            bodyRight.set(body);
            bodyRight.setTextAlign(Paint.Align.RIGHT);

            muted.setColor(COLOR_GRAY_600);
            muted.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
            muted.setTextSize(9f);
            mutedRight.set(muted);
            mutedRight.setTextAlign(Paint.Align.RIGHT);

            border.setColor(COLOR_GRAY_200);
            border.setStyle(Paint.Style.STROKE);
            border.setStrokeWidth(0.8f);

            headerFill.setColor(COLOR_GRAY_50);
            headerFill.setStyle(Paint.Style.FILL);
        }
    }
}
