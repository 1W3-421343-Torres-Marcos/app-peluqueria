package com.metanoia.domain.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.BackupManifest;
import com.metanoia.domain.model.BackupTrigger;

import org.junit.Test;

public class BackupValidatorTest {

    private static final String SAMPLE_SHA =
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Test
    public void acceptsMatchingShaWithSameOrOlderSchemaVersion() {
        BackupManifest same = manifest(BackupManifest.CURRENT_FORMAT, 6, SAMPLE_SHA);
        Result<BackupManifest> resSame = BackupValidator.validate(same, SAMPLE_SHA, 6);
        assertTrue(resSame.isSuccess());

        BackupManifest older = manifest(BackupManifest.CURRENT_FORMAT, 5, SAMPLE_SHA);
        Result<BackupManifest> resOlder = BackupValidator.validate(
                older, SAMPLE_SHA.toUpperCase(), 6);
        assertTrue(resOlder.isSuccess());
    }

    @Test
    public void rejectsNewerSchemaVersionWithConflict() {
        BackupManifest newer = manifest(BackupManifest.CURRENT_FORMAT, 7, SAMPLE_SHA);
        Result<BackupManifest> result = BackupValidator.validate(newer, SAMPLE_SHA, 6);

        assertFalse(result.isSuccess());
        assertEquals(DomainError.CONFLICT, result.getError());
    }

    @Test
    public void rejectsHashMismatchUnsupportedFormatAndNullInputs() {
        BackupManifest valid = manifest(BackupManifest.CURRENT_FORMAT, 6, SAMPLE_SHA);
        Result<BackupManifest> wrongSha = BackupValidator.validate(
                valid, "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 6);
        assertFalse(wrongSha.isSuccess());
        assertEquals(DomainError.VALIDATION_ERROR, wrongSha.getError());

        BackupManifest badFormat = manifest(2, 6, SAMPLE_SHA);
        Result<BackupManifest> wrongFormat = BackupValidator.validate(badFormat, SAMPLE_SHA, 6);
        assertFalse(wrongFormat.isSuccess());
        assertEquals(DomainError.VALIDATION_ERROR, wrongFormat.getError());

        assertFalse(BackupValidator.validate(null, SAMPLE_SHA, 6).isSuccess());
        assertFalse(BackupValidator.validate(valid, "", 6).isSuccess());
    }

    private static BackupManifest manifest(int format, int schemaVersion, String sha) {
        return new BackupManifest(
                format,
                schemaVersion,
                4,
                "2026-11-18T18:00:00Z",
                sha,
                10,
                25,
                0,
                BackupTrigger.MANUAL
        );
    }
}
