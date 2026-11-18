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

    @Test
    public void roomSchemaV4_isExportedWithProfessionalTables() throws Exception {
        String content = readSchema(4);
        assertTrue(content.contains("\"tableName\": \"professionals\""));
        assertTrue(content.contains("\"tableName\": \"work_schedules\""));
        assertTrue(content.contains("\"tableName\": \"professional_days_off\""));
        assertTrue(content.contains("\"tableName\": \"professional_services\""));
    }

    @Test
    public void roomSchemaV5_isExportedWithClientsTableAndIndices() throws Exception {
        String content = readSchema(5);
        assertTrue(content.contains("\"tableName\": \"clients\""));
        assertTrue(content.contains("index_clients_name_normalized"));
        assertTrue(content.contains("index_clients_phone"));
    }

    @Test
    public void roomSchemaV6_isExportedWithAgendaTablesAndIndices() throws Exception {
        String content = readSchema(6);
        assertTrue(content.contains("\"tableName\": \"appointments\""));
        assertTrue(content.contains("\"tableName\": \"appointment_items\""));
        assertTrue(content.contains("\"tableName\": \"schedule_blocks\""));
        assertTrue(content.contains("\"tableName\": \"recurrence_rules\""));
        assertTrue(content.contains("index_appointments_professional_id_day"));
        assertTrue(content.contains("index_appointments_client_id_day"));
    }

    @Test
    public void roomSchemaV7_isExportedWithCashTablesAndIndices() throws Exception {
        String content = readSchema(7);
        assertTrue(content.contains("\"tableName\": \"cash_closings\""));
        assertTrue(content.contains("\"tableName\": \"cash_movements\""));
        assertTrue(content.contains("index_cash_closings_day"));
        assertTrue(content.contains("index_cash_movements_day"));
        assertTrue(content.contains("index_cash_movements_method"));
        assertTrue(content.contains("index_cash_movements_appointment_id"));
    }

    @Test
    public void roomSchemaV8_isExportedWithTemplateColumnsOnSettings() throws Exception {
        String content = readSchema(8);
        assertTrue(content.contains("\"tableName\": \"settings\""));
        assertTrue(content.contains("reminder_template"));
        assertTrue(content.contains("birthday_template"));
        assertTrue(content.contains("waitlist_offer_template"));
    }

    @Test
    public void roomSchemaV9_isExportedWithRemindersTableAndIndices() throws Exception {
        String content = readSchema(9);
        assertTrue(content.contains("\"tableName\": \"reminders\""));
        assertTrue(content.contains("index_reminders_appointment_id"));
        assertTrue(content.contains("index_reminders_client_id"));
        assertTrue(content.contains("index_reminders_type_appointment_id"));
        assertTrue(content.contains("index_reminders_type_client_id_year"));
    }

    @Test
    public void roomSchemaV10_isExportedWithColorFormulaTablesAndIndices() throws Exception {
        String content = readSchema(10);
        assertTrue(content.contains("\"tableName\": \"color_formulas\""));
        assertTrue(content.contains("\"tableName\": \"color_formula_components\""));
        assertTrue(content.contains("index_color_formulas_client_id_day"));
        assertTrue(content.contains("index_color_formulas_appointment_id"));
        assertTrue(content.contains("index_color_formulas_professional_id"));
        assertTrue(content.contains("index_color_formula_components_formula_id_position"));
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
