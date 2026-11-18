package com.metanoia.domain.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.metanoia.domain.rules.BackupRetentionPolicy.BackupFileEntry;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BackupRetentionPolicyTest {

    @Test
    public void keepsLatestPerDayAndDeletesMultipleOlderBackupsSameDay() {
        List<BackupFileEntry> files = Arrays.asList(
                entry("metanoia-backup-20261118-090000-ON_BACKGROUND.zip"),
                entry("metanoia-backup-20261118-130000-MANUAL.zip"),
                entry("metanoia-backup-20261118-210000-CASH_CLOSE.zip")
        );

        List<BackupFileEntry> toDelete = BackupRetentionPolicy.selectFilesToDelete(
                files, 7, 3, "metanoia-backup-20261118-210000-CASH_CLOSE.zip");

        Set<String> deletedNames = namesOf(toDelete);
        assertEquals(2, deletedNames.size());
        assertTrue(deletedNames.contains("metanoia-backup-20261118-090000-ON_BACKGROUND.zip"));
        assertTrue(deletedNames.contains("metanoia-backup-20261118-130000-MANUAL.zip"));
    }

    @Test
    public void keepsLatestOfLastNDistinctDaysIgnoringGapDays() {
        // 4 días con backups separados por días sin actividad, N = 3 -> borra solo 20261101
        List<BackupFileEntry> files = Arrays.asList(
                entry("metanoia-backup-20261101-190000-AUTO.zip"),
                entry("metanoia-backup-20261105-200000-CASH_CLOSE.zip"),
                entry("metanoia-backup-20261110-180000-ON_BACKGROUND.zip"),
                entry("metanoia-backup-20261118-203000-AUTO.zip")
        );

        List<BackupFileEntry> toDelete = BackupRetentionPolicy.selectFilesToDelete(
                files, 3, 3, "metanoia-backup-20261118-203000-AUTO.zip");

        assertEquals(1, toDelete.size());
        assertEquals("metanoia-backup-20261101-190000-AUTO.zip", toDelete.get(0).fileName());
    }

    @Test
    public void handlesPreRestoreSeparatelyKeepingLastThree() {
        List<BackupFileEntry> files = Arrays.asList(
                entry("metanoia-backup-20261110-100000-PRE_RESTORE.zip"),
                entry("metanoia-backup-20261112-100000-PRE_RESTORE.zip"),
                entry("metanoia-backup-20261114-100000-PRE_RESTORE.zip"),
                entry("metanoia-backup-20261116-100000-PRE_RESTORE.zip"),
                entry("metanoia-backup-20261118-100000-PRE_RESTORE.zip"),
                entry("metanoia-backup-20261118-120000-AUTO.zip")
        );

        List<BackupFileEntry> toDelete = BackupRetentionPolicy.selectFilesToDelete(
                files, 7, 3, "metanoia-backup-20261118-120000-AUTO.zip");

        Set<String> deletedNames = namesOf(toDelete);
        assertEquals(2, deletedNames.size());
        assertTrue(deletedNames.contains("metanoia-backup-20261110-100000-PRE_RESTORE.zip"));
        assertTrue(deletedNames.contains("metanoia-backup-20261112-100000-PRE_RESTORE.zip"));
        assertFalse(deletedNames.contains("metanoia-backup-20261118-120000-AUTO.zip"));
    }

    @Test
    public void neverDeletesNewlyCreatedOrForeignFiles() {
        List<BackupFileEntry> files = Arrays.asList(
                entry("notas-salon.txt"),
                entry("metanoia-backup-invalido.zip"),
                entry("metanoia-backup-20261118-090000-MANUAL.zip"),
                entry("metanoia-backup-20261118-100000-AUTO.zip")
        );

        // Simulamos archivo recién creado con nombre igual al más viejo (no debería borrarse igual)
        List<BackupFileEntry> toDelete = BackupRetentionPolicy.selectFilesToDelete(
                files, 1, 3, "metanoia-backup-20261118-090000-MANUAL.zip");

        assertEquals(1, toDelete.size());
        assertEquals("metanoia-backup-20261118-100000-AUTO.zip", toDelete.get(0).fileName());
    }

    @Test
    public void evaluatesOnBackgroundThresholdAndOverdueDays() {
        long t0 = 1_700_000_000_000L;
        long fiveHours = 5L * 3_600_000L;
        long sixHours = 6L * 3_600_000L;

        assertTrue(BackupRetentionPolicy.shouldTriggerOnBackground(0L, t0, 6));
        assertFalse(BackupRetentionPolicy.shouldTriggerOnBackground(t0, t0 + fiveHours, 6));
        assertTrue(BackupRetentionPolicy.shouldTriggerOnBackground(t0, t0 + sixHours, 6));

        long threeDays = 3L * 86_400_000L;
        assertTrue(BackupRetentionPolicy.isBackupOverdue(false, t0, t0, 3));
        assertTrue(BackupRetentionPolicy.isBackupOverdue(true, 0L, t0, 3));
        assertFalse(BackupRetentionPolicy.isBackupOverdue(true, t0, t0 + threeDays, 3));
        assertTrue(BackupRetentionPolicy.isBackupOverdue(true, t0, t0 + threeDays + 1_000L, 3));
    }

    private static BackupFileEntry entry(String name) {
        return new BackupFileEntry(name, name, 0L);
    }

    private static Set<String> namesOf(List<BackupFileEntry> entries) {
        if (entries.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> set = new HashSet<>();
        for (BackupFileEntry item : entries) {
            set.add(item.fileName());
        }
        return set;
    }
}
