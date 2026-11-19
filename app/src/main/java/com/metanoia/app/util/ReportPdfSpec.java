package com.metanoia.app.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Especificación tabular y paginación determinista para reportes PDF A4 blanco y negro.
 */
public final class ReportPdfSpec {

    public static final int FIRST_PAGE_CONTENT_HEIGHT_PT = 570;
    public static final int NEXT_PAGE_CONTENT_HEIGHT_PT = 670;
    public static final int SECTION_TITLE_HEIGHT_PT = 22;
    public static final int TABLE_HEADER_HEIGHT_PT = 22;
    public static final int TABLE_ROW_HEIGHT_PT = 20;
    public static final int SECTION_GAP_PT = 14;

    public static final class KpiPair {
        @NonNull
        public final String label;
        @NonNull
        public final String value;

        public KpiPair(@NonNull String label, @NonNull String value) {
            this.label = Objects.requireNonNull(label, "label");
            this.value = Objects.requireNonNull(value, "value");
        }
    }

    public static final class TableColumn {
        @NonNull
        public final String title;
        public final float widthWeight;
        public final boolean alignRight;

        public TableColumn(@NonNull String title, float widthWeight, boolean alignRight) {
            this.title = Objects.requireNonNull(title, "title");
            this.widthWeight = widthWeight;
            this.alignRight = alignRight;
        }
    }

    public static final class TableSection {
        @Nullable
        public final String sectionTitle;
        @NonNull
        public final List<TableColumn> columns;
        @NonNull
        public final List<List<String>> rows;

        public TableSection(
                @Nullable String sectionTitle,
                @NonNull List<TableColumn> columns,
                @NonNull List<List<String>> rows
        ) {
            this.sectionTitle = sectionTitle;
            this.columns = Collections.unmodifiableList(new ArrayList<>(columns));
            this.rows = Collections.unmodifiableList(new ArrayList<>(rows));
        }
    }

    @NonNull
    public final String title;
    @NonNull
    public final String filterSummary;
    @NonNull
    public final List<KpiPair> kpis;
    @NonNull
    public final List<TableSection> sections;
    @NonNull
    public final List<String> notesFooter;

    public ReportPdfSpec(
            @NonNull String title,
            @NonNull String filterSummary,
            @NonNull List<KpiPair> kpis,
            @NonNull List<TableSection> sections,
            @NonNull List<String> notesFooter
    ) {
        this.title = Objects.requireNonNull(title, "title");
        this.filterSummary = Objects.requireNonNull(filterSummary, "filterSummary");
        this.kpis = Collections.unmodifiableList(new ArrayList<>(kpis));
        this.sections = Collections.unmodifiableList(new ArrayList<>(sections));
        this.notesFooter = Collections.unmodifiableList(new ArrayList<>(notesFooter));
    }

    public int estimatePageCount() {
        return paginate().size();
    }

    /**
     * Divide las secciones y filas en páginas discretas para poder imprimir "Página X de Y"
     * en una única pasada hacia adelante sobre {@code PdfDocument}.
     */
    @NonNull
    public List<List<TableSection>> paginate() {
        List<List<TableSection>> pages = new ArrayList<>();
        List<TableSection> currentPage = new ArrayList<>();
        int remainingPt = FIRST_PAGE_CONTENT_HEIGHT_PT;

        for (TableSection section : sections) {
            int headerCost = (section.sectionTitle == null ? 0 : SECTION_TITLE_HEIGHT_PT)
                    + TABLE_HEADER_HEIGHT_PT;
            if (section.rows.isEmpty()) {
                if (!currentPage.isEmpty() && remainingPt < headerCost + TABLE_ROW_HEIGHT_PT) {
                    pages.add(currentPage);
                    currentPage = new ArrayList<>();
                    remainingPt = NEXT_PAGE_CONTENT_HEIGHT_PT;
                }
                currentPage.add(section);
                remainingPt -= headerCost + SECTION_GAP_PT;
                continue;
            }

            int idx = 0;
            boolean firstChunk = true;
            while (idx < section.rows.size()) {
                int chunkHeader = (firstChunk && section.sectionTitle != null
                        ? SECTION_TITLE_HEIGHT_PT : 0) + TABLE_HEADER_HEIGHT_PT;
                if (!currentPage.isEmpty() && remainingPt < chunkHeader + TABLE_ROW_HEIGHT_PT) {
                    pages.add(currentPage);
                    currentPage = new ArrayList<>();
                    remainingPt = NEXT_PAGE_CONTENT_HEIGHT_PT;
                }
                int maxRows = Math.max(1, (remainingPt - chunkHeader) / TABLE_ROW_HEIGHT_PT);
                int end = Math.min(section.rows.size(), idx + maxRows);
                currentPage.add(new TableSection(
                        firstChunk ? section.sectionTitle : null,
                        section.columns,
                        section.rows.subList(idx, end)));
                remainingPt -= chunkHeader + (end - idx) * TABLE_ROW_HEIGHT_PT + SECTION_GAP_PT;
                idx = end;
                firstChunk = false;
            }
        }

        if (!currentPage.isEmpty() || pages.isEmpty()) {
            pages.add(currentPage);
        }
        return pages;
    }
}
