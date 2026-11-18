package com.metanoia.domain.rules;

import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.BackupManifest;
import com.metanoia.domain.model.BackupTrigger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Serializador y analizador puro en Java para {@code manifest.json} sin dependencias externas.
 */
public final class BackupManifestCodec {

    private BackupManifestCodec() {
    }

    public static String toJson(BackupManifest manifest) {
        Objects.requireNonNull(manifest, "manifest");
        StringBuilder sb = new StringBuilder(256);
        sb.append("{\n");
        appendIntField(sb, "format", manifest.format(), true);
        appendIntField(sb, "schemaVersion", manifest.schemaVersion(), true);
        appendIntField(sb, "appVersionCode", manifest.appVersionCode(), true);
        appendStringField(sb, "createdAt", manifest.createdAt(), true);
        appendStringField(sb, "sha256", manifest.sha256(), true);
        appendIntField(sb, "clientsCount", manifest.clientsCount(), true);
        appendIntField(sb, "appointmentsCount", manifest.appointmentsCount(), true);
        appendIntField(sb, "cashMovementsCount", manifest.cashMovementsCount(), true);
        appendStringField(sb, "trigger", manifest.trigger().name(), false);
        sb.append("}\n");
        return sb.toString();
    }

    public static Result<BackupManifest> fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Result.failure(DomainError.VALIDATION_ERROR, "Manifiesto vacío");
        }
        try {
            Map<String, Object> fields = parseFlatObject(json);
            int format = requireInt(fields, "format");
            int schemaVersion = requireInt(fields, "schemaVersion");
            int appVersionCode = requireInt(fields, "appVersionCode");
            String createdAt = requireString(fields, "createdAt");
            String sha256 = requireString(fields, "sha256");
            int clientsCount = requireInt(fields, "clientsCount");
            int appointmentsCount = requireInt(fields, "appointmentsCount");
            int cashMovementsCount = requireInt(fields, "cashMovementsCount");
            String triggerRaw = requireString(fields, "trigger");
            BackupTrigger trigger = BackupTrigger.valueOf(triggerRaw);

            return Result.success(new BackupManifest(
                    format,
                    schemaVersion,
                    appVersionCode,
                    createdAt,
                    sha256,
                    clientsCount,
                    appointmentsCount,
                    cashMovementsCount,
                    trigger
            ));
        } catch (IllegalArgumentException ex) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "Manifiesto de backup inválido: " + ex.getMessage());
        }
    }

    private static void appendIntField(StringBuilder sb, String key, int value, boolean comma) {
        sb.append("  \"").append(key).append("\": ").append(value);
        if (comma) {
            sb.append(',');
        }
        sb.append('\n');
    }

    private static void appendStringField(
            StringBuilder sb,
            String key,
            String value,
            boolean comma
    ) {
        sb.append("  \"").append(key).append("\": \"");
        appendEscaped(sb, value);
        sb.append('"');
        if (comma) {
            sb.append(',');
        }
        sb.append('\n');
    }

    private static void appendEscaped(StringBuilder sb, String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\':
                    sb.append("\\\\");
                    break;
                case '"':
                    sb.append("\\\"");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
    }

    private static int requireInt(Map<String, Object> fields, String key) {
        Object value = fields.get(key);
        if (!(value instanceof Long)) {
            throw new IllegalArgumentException("Falta campo numérico: " + key);
        }
        long num = (Long) value;
        if (num < Integer.MIN_VALUE || num > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Fuera de rango: " + key);
        }
        return (int) num;
    }

    private static String requireString(Map<String, Object> fields, String key) {
        Object value = fields.get(key);
        if (!(value instanceof String)) {
            throw new IllegalArgumentException("Falta campo de texto: " + key);
        }
        String str = ((String) value).trim();
        if (str.isEmpty()) {
            throw new IllegalArgumentException("Campo vacío: " + key);
        }
        return str;
    }

    private static Map<String, Object> parseFlatObject(String raw) {
        String text = raw.trim();
        int len = text.length();
        if (len < 2 || text.charAt(0) != '{') {
            throw new IllegalArgumentException("Se esperaba objeto JSON");
        }
        Map<String, Object> map = new LinkedHashMap<>();
        int idx = skipWhitespace(text, 1);
        if (idx < len && text.charAt(idx) == '}') {
            if (skipWhitespace(text, idx + 1) != len) {
                throw new IllegalArgumentException("Contenido extra tras '}'");
            }
            return map;
        }
        while (idx < len) {
            idx = skipWhitespace(text, idx);
            if (idx >= len || text.charAt(idx) != '"') {
                throw new IllegalArgumentException("Se esperaba clave entre comillas");
            }
            StringBuilder keyOut = new StringBuilder();
            idx = readString(text, idx + 1, keyOut);
            idx = skipWhitespace(text, idx);
            if (idx >= len || text.charAt(idx) != ':') {
                throw new IllegalArgumentException("Se esperaba ':' tras clave");
            }
            idx = skipWhitespace(text, idx + 1);
            if (idx >= len) {
                throw new IllegalArgumentException("Falta valor para clave");
            }
            Object val;
            if (text.charAt(idx) == '"') {
                StringBuilder valOut = new StringBuilder();
                idx = readString(text, idx + 1, valOut);
                val = valOut.toString();
            } else {
                int start = idx;
                while (idx < len) {
                    char c = text.charAt(idx);
                    if (c == ',' || c == '}' || Character.isWhitespace(c)) {
                        break;
                    }
                    idx++;
                }
                String token = text.substring(start, idx);
                try {
                    val = Long.parseLong(token);
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Valor entero inválido: " + token);
                }
            }
            map.put(keyOut.toString(), val);
            idx = skipWhitespace(text, idx);
            if (idx >= len) {
                throw new IllegalArgumentException("Objeto JSON sin cierre");
            }
            char next = text.charAt(idx);
            if (next == ',') {
                idx++;
            } else if (next == '}') {
                if (skipWhitespace(text, idx + 1) != len) {
                    throw new IllegalArgumentException("Contenido extra tras '}'");
                }
                return map;
            } else {
                throw new IllegalArgumentException("Separador JSON inesperado: " + next);
            }
        }
        throw new IllegalArgumentException("Objeto JSON incompleto");
    }

    private static int readString(String text, int startAfterQuote, StringBuilder out) {
        int i = startAfterQuote;
        int len = text.length();
        while (i < len) {
            char c = text.charAt(i++);
            if (c == '"') {
                return i;
            }
            if (c == '\\') {
                if (i >= len) {
                    throw new IllegalArgumentException("Escape JSON incompleto");
                }
                char esc = text.charAt(i++);
                switch (esc) {
                    case '"':
                        out.append('"');
                        break;
                    case '\\':
                        out.append('\\');
                        break;
                    case '/':
                        out.append('/');
                        break;
                    case 'n':
                        out.append('\n');
                        break;
                    case 'r':
                        out.append('\r');
                        break;
                    case 't':
                        out.append('\t');
                        break;
                    default:
                        out.append(esc);
                        break;
                }
            } else {
                out.append(c);
            }
        }
        throw new IllegalArgumentException("Cadena JSON sin comilla de cierre");
    }

    private static int skipWhitespace(String text, int idx) {
        int len = text.length();
        while (idx < len && Character.isWhitespace(text.charAt(idx))) {
            idx++;
        }
        return idx;
    }
}
