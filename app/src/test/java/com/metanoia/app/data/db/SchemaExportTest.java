package com.metanoia.app.data.db;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class SchemaExportTest {

    @Test
    public void roomSchemaV1_isExportedWithAuditLogsTable() throws Exception {
        String content = readSchema(1);
        assertTrue(content.contains("\"tableName\": \"audit_logs\""));
        assertTrue(content.contains("\"identityHash\""));
    }

    @Test
    public void roomSchemaV2_isExportedWithSettingsAndSalonHours() throws Exception {
        String content = readSchema(2);
        assertTrue(content.contains("\"tableName\": \"settings\""));
        assertTrue(content.contains("\"tableName\": \"salon_hours\""));
    }

    @Test
    public void roomSchemaV3_isExportedWithSalonServices() throws Exception {
        String content = readSchema(3);
        assertTrue(content.contains("\"tableName\": \"salon_services\""));
    }

    private static String readSchema(int version) throws Exception {
        File schemaFile = resolveSchemaFile(
                "schemas/com.metanoia.app.data.db.AppDatabase/" + version + ".json");
        assertTrue("Falta el esquema exportado v" + version + " de Room", schemaFile.exists());
        return new String(Files.readAllBytes(schemaFile.toPath()), StandardCharsets.UTF_8);
    }

    private static File resolveSchemaFile(String relativePath) {
        File direct = new File(relativePath);
        if (direct.exists()) {
            return direct;
        }
        return new File("app", relativePath);
    }
}
