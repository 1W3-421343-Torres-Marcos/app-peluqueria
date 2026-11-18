package com.metanoia.domain.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.BackupManifest;
import com.metanoia.domain.model.BackupTrigger;

import org.junit.Test;

public class BackupManifestCodecTest {

    @Test
    public void roundTrip_preservesAllFieldsAndTriggers() {
        for (BackupTrigger trigger : BackupTrigger.values()) {
            BackupManifest original = new BackupManifest(
                    BackupManifest.CURRENT_FORMAT,
                    6,
                    4,
                    "2026-11-18T20:15:00Z",
                    "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789",
                    125,
                    430,
                    0,
                    trigger
            );

            String json = BackupManifestCodec.toJson(original);
            Result<BackupManifest> parsed = BackupManifestCodec.fromJson(json);

            assertTrue(parsed.isSuccess());
            BackupManifest restored = parsed.getValue();
            assertEquals(original.format(), restored.format());
            assertEquals(original.schemaVersion(), restored.schemaVersion());
            assertEquals(original.appVersionCode(), restored.appVersionCode());
            assertEquals(original.createdAt(), restored.createdAt());
            assertEquals(original.sha256(), restored.sha256());
            assertEquals(original.clientsCount(), restored.clientsCount());
            assertEquals(original.appointmentsCount(), restored.appointmentsCount());
            assertEquals(original.cashMovementsCount(), restored.cashMovementsCount());
            assertEquals(original.trigger(), restored.trigger());
        }
    }

    @Test
    public void fromJson_rejectsMissingFieldsAndInvalidJson() {
        Result<BackupManifest> empty = BackupManifestCodec.fromJson("");
        assertFalse(empty.isSuccess());
        assertEquals(DomainError.VALIDATION_ERROR, empty.getError());

        Result<BackupManifest> missingSha = BackupManifestCodec.fromJson(
                "{\"format\":1,\"schemaVersion\":6,\"appVersionCode\":3,"
                        + "\"createdAt\":\"2026-11-18T10:00:00Z\",\"clientsCount\":1,"
                        + "\"appointmentsCount\":2,\"cashMovementsCount\":0,\"trigger\":\"MANUAL\"}");
        assertFalse(missingSha.isSuccess());
        assertEquals(DomainError.VALIDATION_ERROR, missingSha.getError());

        Result<BackupManifest> unknownTrigger = BackupManifestCodec.fromJson(
                "{\"format\":1,\"schemaVersion\":6,\"appVersionCode\":3,"
                        + "\"createdAt\":\"2026-11-18T10:00:00Z\",\"sha256\":\"abc\","
                        + "\"clientsCount\":1,\"appointmentsCount\":2,\"cashMovementsCount\":0,"
                        + "\"trigger\":\"UNKNOWN\"}");
        assertFalse(unknownTrigger.isSuccess());
        assertEquals(DomainError.VALIDATION_ERROR, unknownTrigger.getError());
    }
}
