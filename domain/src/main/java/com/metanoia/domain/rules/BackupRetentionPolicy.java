package com.metanoia.domain.rules;

import com.metanoia.domain.model.BackupTrigger;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Reglas puras de retención de copias de seguridad, umbral {@code ON_BACKGROUND}
 * y detección de backup vencido.
 */
public final class BackupRetentionPolicy {

    public static final int DEFAULT_KEEP_PRE_RESTORE_COUNT = 3;
    private static final String FILE_PREFIX = "metanoia-backup-";
    private static final String FILE_SUFFIX = ".zip";
    private static final long MILLIS_PER_HOUR = 3_600_000L;
    private static final long MILLIS_PER_DAY = 86_400_000L;

    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter
            .ofPattern("uuuuMMdd", Locale.US)
            .withResolverStyle(ResolverStyle.STRICT);

    public static final class BackupFileEntry {
        private final String id;
        private final String fileName;
        private final long fallbackTimestampMillis;

        public BackupFileEntry(String id, String fileName, long fallbackTimestampMillis) {
            this.id = Objects.requireNonNull(id, "id");
            this.fileName = Objects.requireNonNull(fileName, "fileName");
            this.fallbackTimestampMillis = fallbackTimestampMillis;
        }

        public String id() {
            return id;
        }

        public String fileName() {
            return fileName;
        }

        public long fallbackTimestampMillis() {
            return fallbackTimestampMillis;
        }
    }

    private BackupRetentionPolicy() {
    }

    public static List<BackupFileEntry> selectFilesToDelete(
            List<BackupFileEntry> existingFiles,
            int keepDaysN,
            int keepPreRestoreCount,
            String newlyCreatedFileName
    ) {
        if (existingFiles == null || existingFiles.isEmpty()) {
            return Collections.emptyList();
        }
        int safeKeepDays = Math.max(1, keepDaysN);
        int safePreRestore = Math.max(1, keepPreRestoreCount);

        List<ParsedFile> regularFiles = new ArrayList<>();
        List<ParsedFile> preRestoreFiles = new ArrayList<>();

        for (BackupFileEntry entry : existingFiles) {
            if (entry == null) {
                continue;
            }
            ParsedFile parsed = parse(entry);
            if (parsed == null) {
                continue;
            }
            if (parsed.trigger == BackupTrigger.PRE_RESTORE) {
                preRestoreFiles.add(parsed);
            } else {
                regularFiles.add(parsed);
            }
        }

        Comparator<ParsedFile> newestFirst = (a, b) -> {
            int cmp = Long.compare(b.sortKey, a.sortKey);
            if (cmp != 0) {
                return cmp;
            }
            cmp = Long.compare(
                    b.entry.fallbackTimestampMillis(), a.entry.fallbackTimestampMillis());
            if (cmp != 0) {
                return cmp;
            }
            return b.entry.fileName().compareTo(a.entry.fileName());
        };

        List<BackupFileEntry> toDelete = new ArrayList<>();

        preRestoreFiles.sort(newestFirst);
        for (int i = safePreRestore; i < preRestoreFiles.size(); i++) {
            BackupFileEntry candidate = preRestoreFiles.get(i).entry;
            if (!isProtectedNewFile(candidate, newlyCreatedFileName)) {
                toDelete.add(candidate);
            }
        }

        regularFiles.sort(newestFirst);
        Map<String, List<ParsedFile>> byDayNewestFirst = new LinkedHashMap<>();
        for (ParsedFile file : regularFiles) {
            byDayNewestFirst
                    .computeIfAbsent(file.dayKey, ignored -> new ArrayList<>())
                    .add(file);
        }

        int dayIndex = 0;
        for (List<ParsedFile> sameDay : byDayNewestFirst.values()) {
            sameDay.sort(newestFirst);
            if (dayIndex < safeKeepDays) {
                ParsedFile keeper = selectKeeperForDay(sameDay, newlyCreatedFileName);
                for (ParsedFile item : sameDay) {
                    if (item != keeper && !isProtectedNewFile(item.entry, newlyCreatedFileName)) {
                        toDelete.add(item.entry);
                    }
                }
            } else {
                for (ParsedFile item : sameDay) {
                    if (!isProtectedNewFile(item.entry, newlyCreatedFileName)) {
                        toDelete.add(item.entry);
                    }
                }
            }
            dayIndex++;
        }

        return Collections.unmodifiableList(toDelete);
    }

    public static boolean shouldTriggerOnBackground(
            long lastSuccessMillis,
            long nowMillis,
            int thresholdHours
    ) {
        if (lastSuccessMillis <= 0L || thresholdHours <= 0) {
            return true;
        }
        if (nowMillis < lastSuccessMillis) {
            return false;
        }
        long elapsed = nowMillis - lastSuccessMillis;
        return elapsed >= (long) thresholdHours * MILLIS_PER_HOUR;
    }

    public static boolean isBackupOverdue(
            boolean hasValidFolder,
            long lastSuccessMillis,
            long nowMillis,
            int warningDays
    ) {
        if (!hasValidFolder || lastSuccessMillis <= 0L) {
            return true;
        }
        if (nowMillis <= lastSuccessMillis) {
            return false;
        }
        long safeDays = Math.max(1L, warningDays);
        return (nowMillis - lastSuccessMillis) > safeDays * MILLIS_PER_DAY;
    }

    private static ParsedFile selectKeeperForDay(
            List<ParsedFile> sortedSameDay,
            String newlyCreatedFileName
    ) {
        for (ParsedFile item : sortedSameDay) {
            if (isProtectedNewFile(item.entry, newlyCreatedFileName)) {
                return item;
            }
        }
        return sortedSameDay.get(0);
    }

    private static boolean isProtectedNewFile(
            BackupFileEntry candidate,
            String newlyCreatedFileName
    ) {
        return newlyCreatedFileName != null
                && newlyCreatedFileName.equals(candidate.fileName());
    }

    private static ParsedFile parse(BackupFileEntry entry) {
        String name = entry.fileName();
        if (!name.startsWith(FILE_PREFIX) || !name.endsWith(FILE_SUFFIX)) {
            return null;
        }
        String core = name.substring(FILE_PREFIX.length(), name.length() - FILE_SUFFIX.length());
        // Esperado: yyyyMMdd-HHmmss-TRIGGER (8 + 1 + 6 + 1 + trigger)
        if (core.length() < 17 || core.charAt(8) != '-' || core.charAt(15) != '-') {
            return null;
        }
        String dayPart = core.substring(0, 8);
        String timePart = core.substring(9, 15);
        String triggerPart = core.substring(16);
        if (!isValidDatePart(dayPart) || !isValidTimePart(timePart)) {
            return null;
        }
        BackupTrigger trigger;
        try {
            trigger = BackupTrigger.valueOf(triggerPart);
        } catch (IllegalArgumentException ex) {
            return null;
        }
        long sortKey = Long.parseLong(dayPart + timePart);
        return new ParsedFile(entry, dayPart, sortKey, trigger);
    }

    private static boolean isValidDatePart(String dayPart) {
        for (int i = 0; i < dayPart.length(); i++) {
            if (!Character.isDigit(dayPart.charAt(i))) {
                return false;
            }
        }
        try {
            LocalDate.parse(dayPart, BASIC_DATE);
            return true;
        } catch (DateTimeParseException ex) {
            return false;
        }
    }

    private static boolean isValidTimePart(String timePart) {
        for (int i = 0; i < timePart.length(); i++) {
            if (!Character.isDigit(timePart.charAt(i))) {
                return false;
            }
        }
        int hh = Integer.parseInt(timePart.substring(0, 2));
        int mm = Integer.parseInt(timePart.substring(2, 4));
        int ss = Integer.parseInt(timePart.substring(4, 6));
        return hh <= 23 && mm <= 59 && ss <= 59;
    }

    private static final class ParsedFile {
        final BackupFileEntry entry;
        final String dayKey;
        final long sortKey;
        final BackupTrigger trigger;

        ParsedFile(
                BackupFileEntry entry,
                String dayKey,
                long sortKey,
                BackupTrigger trigger
        ) {
            this.entry = entry;
            this.dayKey = dayKey;
            this.sortKey = sortKey;
            this.trigger = trigger;
        }
    }
}
