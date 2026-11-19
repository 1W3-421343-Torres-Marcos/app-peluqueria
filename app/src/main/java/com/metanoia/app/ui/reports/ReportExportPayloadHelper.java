package com.metanoia.app.ui.reports;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.metanoia.app.data.db.pojo.ProfessionalWithDetails;
import com.metanoia.app.ui.reports.ReportsUiModels.LoadedPayload;
import com.metanoia.app.ui.reports.ReportsUiModels.ReportsFilterState;
import com.metanoia.app.util.CsvExporter;
import com.metanoia.app.util.Formats;
import com.metanoia.app.util.ReportPdfBuilder;
import com.metanoia.app.util.ReportPdfSpec;
import com.metanoia.app.util.ReportPdfSpecFactory;
import com.metanoia.domain.common.Clock;
import com.metanoia.domain.repository.SettingsRepository;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * Construye y escribe payloads de exportación CSV y PDF a partir del estado cargado de reportes.
 */
final class ReportExportPayloadHelper {

    private ReportExportPayloadHelper() {
    }

    static void writeExportToUri(
            @NonNull Context context,
            @NonNull SettingsRepository settingsRepository,
            @NonNull Clock clock,
            @NonNull ReportsFilterState filter,
            @NonNull LoadedPayload payload,
            @Nullable List<ProfessionalWithDetails> professionals,
            @NonNull Uri destinationUri,
            boolean isPdf
    ) throws IOException {
        try (OutputStream out = context.getContentResolver().openOutputStream(destinationUri)) {
            if (out == null) {
                throw new IOException("OutputStream nulo");
            }
            if (isPdf) {
                ReportPdfSpec spec = buildPdfSpec(filter, payload, professionals);
                ReportPdfBuilder.writePdf(
                        context, settingsRepository.getSettings(), spec, clock.nowMillis(), out);
            } else {
                CsvExporter.writeBytes(buildCsvBytes(filter, payload), out);
            }
        }
    }

    @NonNull
    static File buildTempShareFile(
            @NonNull Context context,
            @NonNull SettingsRepository settingsRepository,
            @NonNull Clock clock,
            @NonNull ReportsFilterState filter,
            @NonNull LoadedPayload payload,
            @Nullable List<ProfessionalWithDetails> professionals,
            boolean isPdf
    ) throws IOException {
        String ext = isPdf ? "pdf" : "csv";
        String fileName = CsvExporter.suggestFileName(
                filter.reportType, filter.fromDay, filter.toDay, ext);
        if (isPdf) {
            ReportPdfSpec spec = buildPdfSpec(filter, payload, professionals);
            return ReportPdfBuilder.buildTempPdfFile(
                    context, settingsRepository.getSettings(), spec, clock.nowMillis(), fileName);
        }
        return CsvExporter.writeTempShareFile(
                context, fileName, buildCsvBytes(filter, payload));
    }

    @NonNull
    static byte[] buildCsvBytes(
            @NonNull ReportsFilterState filter,
            @NonNull LoadedPayload payload
    ) {
        switch (filter.reportType) {
            case TOP_SERVICES:
                return CsvExporter.exportTopServices(payload.topServices, filter.serviceGrouping);
            case FREQUENT_CLIENTS:
                return CsvExporter.exportFrequentClients(
                        payload.frequentClients,
                        filter.clientRankingMode,
                        filter.includeClientPhone);
            case OCCUPANCY:
                return CsvExporter.exportOccupancy(payload.occupancy, filter.occupancyGrouping);
            case PROFESSIONAL_PERFORMANCE:
                return CsvExporter.exportProfessionalPerformance(payload.performance);
            case RECEIVABLES:
                return CsvExporter.exportReceivables(payload.receivables);
            case INCOME:
            default:
                return CsvExporter.exportIncome(payload.income, filter.incomeGranularity);
        }
    }

    @NonNull
    static ReportPdfSpec buildPdfSpec(
            @NonNull ReportsFilterState filter,
            @NonNull LoadedPayload payload,
            @Nullable List<ProfessionalWithDetails> professionals
    ) {
        String proName = resolveProfessionalName(filter.professionalId, professionals);
        String summary = "Período: " + Formats.formatDate(filter.fromDay)
                + " – " + Formats.formatDate(filter.toDay)
                + " · Profesional: " + proName;
        switch (filter.reportType) {
            case TOP_SERVICES:
                return ReportPdfSpecFactory.forTopServices(
                        summary, payload.topServices, filter.serviceGrouping);
            case FREQUENT_CLIENTS:
                return ReportPdfSpecFactory.forFrequentClients(
                        summary, payload.frequentClients,
                        filter.clientRankingMode, filter.includeClientPhone);
            case OCCUPANCY:
                return ReportPdfSpecFactory.forOccupancy(
                        summary, payload.occupancy, filter.occupancyGrouping);
            case PROFESSIONAL_PERFORMANCE:
                return ReportPdfSpecFactory.forProfessionalPerformance(
                        summary, payload.performance);
            case RECEIVABLES:
                return ReportPdfSpecFactory.forReceivables(summary, payload.receivables);
            case INCOME:
            default:
                return ReportPdfSpecFactory.forIncome(
                        summary, payload.income, filter.incomeGranularity);
        }
    }

    @NonNull
    private static String resolveProfessionalName(
            @Nullable String proId,
            @Nullable List<ProfessionalWithDetails> professionals
    ) {
        if (proId == null) {
            return "Todos";
        }
        if (professionals != null) {
            for (ProfessionalWithDetails item : professionals) {
                if (item.professional.id.equals(proId)) {
                    return item.professional.name;
                }
            }
        }
        return "Seleccionado";
    }
}
