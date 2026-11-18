package com.metanoia.app.data.db;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class SchemaExportTest {

    @Test
    public void roomSchemaV1_isExportedWithAuditLogsTable() throws Exception {
        File schemaFile = resolveSchemaFile(
                "schemas/com.metanoia.app.data.db.AppDatabase/1.json");
        assertTrue("Falta el esquema exportado v1 de Room", schemaFile.exists());

        String content = new String(Files.readAllBytes(schemaFile.toPath()), StandardCharsets.UTF_8);
        assertTrue(content.contains("\"tableName\": \"audit_logs\""));
        assertTrue(content.contains("\"identityHash\""));
    }

    private static File resolveSchemaFile(String relativePath) {
        File direct = new File(relativePath);
        if (direct.exists()) {
            return direct;
        }
        return new File("app", relativePath);
    }
}
