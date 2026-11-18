package com.metanoia.domain.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.BackupManifest;
import com.metanoia.domain.model.BackupTrigger;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class BackupPackageRoundTripTest {

    @Test
    public void zipManifestAndPayload_roundTripValidatesAndDetectsTampering()
            throws IOException, NoSuchAlgorithmException {
        byte[] payload = "SQLite format 3\000test-payload".getBytes(StandardCharsets.UTF_8);
        String sha256 = sha256Hex(payload);
        BackupManifest manifest = new BackupManifest(
                BackupManifest.CURRENT_FORMAT,
                6,
                4,
                "2026-11-18T18:30:00Z",
                sha256,
                15,
                42,
                0,
                BackupTrigger.CASH_CLOSE
        );

        ByteArrayOutputStream zipBytes = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(zipBytes)) {
            zos.putNextEntry(new ZipEntry("metanoia.db"));
            zos.write(payload);
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("manifest.json"));
            zos.write(BackupManifestCodec.toJson(manifest).getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        byte[] readDb = null;
        String readManifest = null;
        try (ZipInputStream zis = new ZipInputStream(
                new ByteArrayInputStream(zipBytes.toByteArray()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if ("metanoia.db".equals(entry.getName())) {
                    readDb = readAll(zis);
                } else if ("manifest.json".equals(entry.getName())) {
                    readManifest = new String(readAll(zis), StandardCharsets.UTF_8);
                }
                zis.closeEntry();
            }
        }

        Result<BackupManifest> parsed = BackupManifestCodec.fromJson(readManifest);
        assertTrue(parsed.isSuccess());
        Result<BackupManifest> valid = BackupValidator.validate(
                parsed.getValue(), sha256Hex(readDb), 6);
        assertTrue(valid.isSuccess());
        assertEquals(15, valid.getValue().clientsCount());
        assertEquals(42, valid.getValue().appointmentsCount());
        assertEquals(BackupTrigger.CASH_CLOSE, valid.getValue().trigger());

        byte[] tamperedDb = readDb.clone();
        tamperedDb[tamperedDb.length - 1] ^= 0x01;
        Result<BackupManifest> tamperedResult = BackupValidator.validate(
                parsed.getValue(), sha256Hex(tamperedDb), 6);
        assertFalse(tamperedResult.isSuccess());
        assertEquals(DomainError.VALIDATION_ERROR, tamperedResult.getError());
    }

    private static byte[] readAll(ZipInputStream zis) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int read;
        while ((read = zis.read(buf)) != -1) {
            out.write(buf, 0, read);
        }
        return out.toByteArray();
    }

    private static String sha256Hex(byte[] data) throws NoSuchAlgorithmException {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
        char[] chars = new char[digest.length * 2];
        char[] hex = "0123456789abcdef".toCharArray();
        for (int i = 0; i < digest.length; i++) {
            int v = digest[i] & 0xFF;
            chars[i * 2] = hex[v >>> 4];
            chars[i * 2 + 1] = hex[v & 0x0F];
        }
        return new String(chars);
    }
}
