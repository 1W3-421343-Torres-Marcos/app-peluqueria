package com.metanoia.domain.rules;

import com.metanoia.domain.model.Settings;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renderiza plantillas de WhatsApp reemplazando variables soportadas, dejando literales las
 * desconocidas (y reportándolas) y agregando la firma del negocio si se omitió {@code {negocio}}.
 */
public final class TemplateRenderer {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^{}]+)\\}");
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");

    public static final class TemplateContext {
        private final String clientFullName;
        private final long epochDay;
        private final int startMin;
        private final String servicesSummary;
        private final String professionalName;
        private final String businessDisplayName;

        public TemplateContext(
                String clientFullName,
                long epochDay,
                int startMin,
                String servicesSummary,
                String professionalName,
                String businessDisplayName
        ) {
            this.clientFullName = clientFullName == null ? "" : clientFullName.trim();
            this.epochDay = epochDay;
            this.startMin = Math.max(0, Math.min(1439, startMin));
            this.servicesSummary = servicesSummary == null ? "" : servicesSummary.trim();
            this.professionalName = professionalName == null ? "" : professionalName.trim();
            this.businessDisplayName =
                    businessDisplayName == null ? "" : businessDisplayName.trim();
        }
    }

    public static final class RenderResult {
        private final String renderedText;
        private final List<String> unknownVariables;

        RenderResult(String renderedText, List<String> unknownVariables) {
            this.renderedText = renderedText;
            this.unknownVariables = Collections.unmodifiableList(unknownVariables);
        }

        public String renderedText() {
            return renderedText;
        }

        public List<String> unknownVariables() {
            return unknownVariables;
        }
    }

    private TemplateRenderer() {
    }

    public static RenderResult render(String template, TemplateContext context) {
        Objects.requireNonNull(context, "context");
        String source = template == null ? "" : template;
        Map<String, String> values = buildVariableMap(context);

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(source);
        StringBuffer out = new StringBuffer();
        List<String> unknown = new ArrayList<>();
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = values.get(key);
            if (replacement != null) {
                matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
            } else {
                String literalToken = "{" + key + "}";
                if (!unknown.contains(literalToken)) {
                    unknown.add(literalToken);
                }
                matcher.appendReplacement(out, Matcher.quoteReplacement(literalToken));
            }
        }
        matcher.appendTail(out);

        String rendered = out.toString().trim();
        if (!source.contains("{negocio}")
                && !rendered.isEmpty()
                && !context.businessDisplayName.isEmpty()) {
            rendered = rendered + "\n— " + context.businessDisplayName;
        }
        return new RenderResult(rendered, unknown);
    }

    public static List<String> findUnknownVariables(String template) {
        return render(template, new TemplateContext("", 20_500L, 600, "", "", ""))
                .unknownVariables();
    }

    public static TemplateContext sampleContext(Settings settings) {
        Settings effective = settings == null ? Settings.defaultSettings() : settings;
        long sampleDay = LocalDate.of(2026, 11, 19).toEpochDay();
        return new TemplateContext(
                "Sofía Martínez",
                sampleDay,
                15 * 60 + 30,
                "Corte, Brushing",
                "Ana",
                effective.businessDisplayName()
        );
    }

    private static Map<String, String> buildVariableMap(TemplateContext context) {
        LocalDate date = LocalDate.ofEpochDay(context.epochDay);
        LocalTime time = LocalTime.of(context.startMin / 60, context.startMin % 60);

        Map<String, String> values = new LinkedHashMap<>();
        values.put("nombre", extractFirstName(context.clientFullName));
        values.put("fecha", date.format(DATE_FORMATTER));
        values.put("dia", spanishWeekday(date.getDayOfWeek()));
        values.put("hora", time.format(TIME_FORMATTER));
        values.put("servicios", context.servicesSummary);
        values.put("profesional", context.professionalName);
        values.put("negocio", context.businessDisplayName);
        return values;
    }

    public static String extractFirstName(String fullName) {
        if (fullName == null) {
            return "";
        }
        String trimmed = fullName.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        int spaceIdx = trimmed.indexOf(' ');
        return spaceIdx < 0 ? trimmed : trimmed.substring(0, spaceIdx);
    }

    private static String spanishWeekday(DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case MONDAY:
                return "lunes";
            case TUESDAY:
                return "martes";
            case WEDNESDAY:
                return "miércoles";
            case THURSDAY:
                return "jueves";
            case FRIDAY:
                return "viernes";
            case SATURDAY:
                return "sábado";
            case SUNDAY:
            default:
                return "domingo";
        }
    }
}
