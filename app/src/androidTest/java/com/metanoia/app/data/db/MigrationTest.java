package com.metanoia.app.data.db;

import static org.junit.Assert.assertEquals;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.metanoia.app.data.db.migration.Migration_1_2;
import com.metanoia.app.data.db.migration.Migration_2_3;
import com.metanoia.app.data.db.migration.Migration_3_4;
import com.metanoia.app.data.db.migration.Migration_4_5;
import com.metanoia.app.data.db.migration.Migration_5_6;
import com.metanoia.app.data.db.migration.Migration_6_7;
import com.metanoia.app.data.db.migration.Migration_7_8;
import com.metanoia.app.data.db.migration.Migration_8_9;
import com.metanoia.app.data.db.migration.Migration_9_10;
import com.metanoia.app.data.db.migration.Migration_10_11;
import com.metanoia.app.data.db.migration.Migration_11_12;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(AndroidJUnit4.class)
public class MigrationTest {

    private static final String TEST_DB = "metanoia-migration-test";

    @Rule
    public final MigrationTestHelper helper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AppDatabase.class
    );

    @Test
    public void createV1_insertsAndReadsAuditLog() throws IOException {
        try (SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 1)) {
            ContentValues values = new ContentValues();
            values.put("id", "log-1");
            values.put("timestamp", 1700000000000L);
            values.put("operator", "Dueño");
            values.put("action", "CREATE");
            values.put("entity_type", "SETTINGS");
            values.put("entity_id", "default");
            values.put("amount_cents", 150000L);
            values.put("concept", "Prueba inicial");
            db.insert("audit_logs", SQLiteDatabase.CONFLICT_ABORT, values);

            try (Cursor cursor = db.query("SELECT operator, concept FROM audit_logs WHERE id = ?",
                    new Object[]{"log-1"})) {
                assertEquals(1, cursor.getCount());
                cursor.moveToFirst();
                assertEquals("Dueño", cursor.getString(0));
                assertEquals("Prueba inicial", cursor.getString(1));
            }
        }
    }

    @Test
    public void migrate1To2_seedsDefaultSettingsAndHours() throws IOException {
        try (SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 1)) {
            db.execSQL("INSERT INTO audit_logs VALUES ('log-1', 1000, 'Dueño', 'INIT', 'APP', '1', NULL, 'Init')");
        }

        try (SupportSQLiteDatabase migrated = helper.runMigrationsAndValidate(
                TEST_DB, 2, true, Migration_1_2.INSTANCE)) {
            try (Cursor cursor = migrated.query(
                    "SELECT business_name, slot_minutes, current_operator FROM settings WHERE id = 'default'")) {
                assertEquals(1, cursor.getCount());
                cursor.moveToFirst();
                assertEquals("Metanoia", cursor.getString(0));
                assertEquals(15, cursor.getInt(1));
                assertEquals("Dueño", cursor.getString(2));
            }
            try (Cursor hours = migrated.query("SELECT COUNT(*) FROM salon_hours")) {
                hours.moveToFirst();
                assertEquals(6, hours.getInt(0));
            }
        }
    }

    @Test
    public void migrate2To3_createsSalonServicesTable() throws IOException {
        try (SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 2)) {
            db.execSQL("INSERT INTO settings VALUES ('default', 'Metanoia', 'Hair Salon', '', '', '', 15, 'Dueño', 'Dueño', 30, 0)");
        }

        try (SupportSQLiteDatabase migrated = helper.runMigrationsAndValidate(
                TEST_DB, 3, true, Migration_2_3.INSTANCE)) {
            migrated.execSQL("INSERT INTO salon_services VALUES ("
                    + "'svc-1', 'Corte', 'corte', 'Corte', 'corte', 1500000, 30, 0, 1, 0, 1000, 1000)");
            try (Cursor cursor = migrated.query(
                    "SELECT name, price_cents FROM salon_services WHERE id = 'svc-1'")) {
                assertEquals(1, cursor.getCount());
                cursor.moveToFirst();
                assertEquals("Corte", cursor.getString(0));
                assertEquals(1500000L, cursor.getLong(1));
            }
        }
    }

    @Test
    public void migrate3To4_createsProfessionalTablesWithForeignKeys() throws IOException {
        try (SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 3)) {
            db.execSQL("INSERT INTO salon_services VALUES ("
                    + "'svc-1', 'Corte', 'corte', 'Corte', 'corte', 1500000, 30, 0, 1, 0, 1000, 1000)");
        }

        try (SupportSQLiteDatabase migrated = helper.runMigrationsAndValidate(
                TEST_DB, 4, true, Migration_3_4.INSTANCE)) {
            migrated.execSQL("INSERT INTO professionals VALUES ("
                    + "'pro-1', 'Ana', '3515551234', 'gray600', 4000, 1, 1000, 1000)");
            migrated.execSQL("INSERT INTO work_schedules (`professional_id`, `day_of_week`, `start_min`, `end_min`) "
                    + "VALUES ('pro-1', 1, 540, 1140)");
            migrated.execSQL("INSERT INTO professional_services VALUES ('pro-1', 'svc-1')");
            migrated.execSQL("INSERT INTO professional_days_off (`professional_id`, `day_epoch`) "
                    + "VALUES ('pro-1', 20010)");

            try (Cursor cursor = migrated.query(
                    "SELECT name, commission_basis_points FROM professionals WHERE id = 'pro-1'")) {
                assertEquals(1, cursor.getCount());
                cursor.moveToFirst();
                assertEquals("Ana", cursor.getString(0));
                assertEquals(4000, cursor.getInt(1));
            }
        }
    }

    @Test
    public void migrate4To5_createsClientsTableWithIndices() throws IOException {
        try (SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 4)) {
            db.execSQL("INSERT INTO professionals VALUES ("
                    + "'pro-1', 'Ana', '3515551234', '', 4000, 1, 1000, 1000)");
        }

        try (SupportSQLiteDatabase migrated = helper.runMigrationsAndValidate(
                TEST_DB, 5, true, Migration_4_5.INSTANCE)) {
            migrated.execSQL("INSERT INTO clients VALUES ("
                    + "'cli-1', 'María José', 'maria jose', '5491122334455', 14, 7, NULL, "
                    + "'', 0, 1, 1000, 1000)");

            try (Cursor cursor = migrated.query(
                    "SELECT name, phone, no_show_count FROM clients WHERE name_normalized LIKE 'maria%'")) {
                assertEquals(1, cursor.getCount());
                cursor.moveToFirst();
                assertEquals("María José", cursor.getString(0));
                assertEquals("5491122334455", cursor.getString(1));
                assertEquals(0, cursor.getInt(2));
            }
        }
    }

    @Test
    public void migrate5To6_createsAgendaTablesWithForeignKeys() throws IOException {
        try (SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 5)) {
            db.execSQL("INSERT INTO salon_services VALUES ("
                    + "'svc-1', 'Corte', 'corte', 'Corte', 'corte', 1500000, 30, 0, 1, 0, 1000, 1000)");
            db.execSQL("INSERT INTO professionals VALUES ("
                    + "'pro-1', 'Ana', '', '', 4000, 1, 1000, 1000)");
            db.execSQL("INSERT INTO clients VALUES ("
                    + "'cli-1', 'Sofía', 'sofia', '5491122334455', NULL, NULL, NULL, "
                    + "'', 0, 1, 1000, 1000)");
        }

        try (SupportSQLiteDatabase migrated = helper.runMigrationsAndValidate(
                TEST_DB, 6, true, Migration_5_6.INSTANCE)) {
            migrated.execSQL("INSERT INTO appointments VALUES ("
                    + "'apt-1', 'cli-1', 'pro-1', 20500, 540, 570, 'CONFIRMED', 0, '', "
                    + "NULL, NULL, 1000, 1000, 'Dueño')");
            migrated.execSQL("INSERT INTO appointment_items VALUES ("
                    + "'item-1', 'apt-1', 'svc-1', 'Corte', 1500000, 30, NULL, NULL, 0)");
            migrated.execSQL("INSERT INTO schedule_blocks VALUES ("
                    + "'blk-1', NULL, 20501, 20501, NULL, NULL, 'HOLIDAY', 'Feriado', 1, 1000, 1000)");

            try (Cursor cursor = migrated.query(
                    "SELECT status, start_min FROM appointments WHERE professional_id = 'pro-1' AND day = 20500")) {
                assertEquals(1, cursor.getCount());
                cursor.moveToFirst();
                assertEquals("CONFIRMED", cursor.getString(0));
                assertEquals(540, cursor.getInt(1));
            }
        }
    }

    @Test
    public void migrate6To7_createsCashTablesAndPreservesAppointments() throws IOException {
        try (SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 6)) {
            db.execSQL("INSERT INTO salon_services VALUES ("
                    + "'svc-1', 'Corte', 'corte', 'Corte', 'corte', 1500000, 30, 0, 1, 0, 1000, 1000)");
            db.execSQL("INSERT INTO professionals VALUES ("
                    + "'pro-1', 'Ana', '', '', 4000, 1, 1000, 1000)");
            db.execSQL("INSERT INTO clients VALUES ("
                    + "'cli-1', 'Sofía', 'sofia', '5491122334455', NULL, NULL, NULL, "
                    + "'', 0, 1, 1000, 1000)");
            db.execSQL("INSERT INTO appointments VALUES ("
                    + "'apt-1', 'cli-1', 'pro-1', 20500, 540, 570, 'COMPLETED', 0, '', "
                    + "NULL, NULL, 1000, 1000, 'Dueño')");
        }

        try (SupportSQLiteDatabase migrated = helper.runMigrationsAndValidate(
                TEST_DB, 7, true, Migration_6_7.INSTANCE)) {
            migrated.execSQL("INSERT INTO cash_movements VALUES ("
                    + "'mov-1', 20500, 2000, 'APPOINTMENT_PAYMENT', 'CASH', 1500000, "
                    + "'Cobro turno', 'apt-1', 'cli-1', NULL, NULL, NULL, 'Dueño', NULL)");
            migrated.execSQL("INSERT INTO cash_closings VALUES ("
                    + "'close-1', 20500, 1500000, 0, 0, 0, 1500000, 0, 0, 0, 0, '', 3000, 'Dueño')");

            try (Cursor cursor = migrated.query(
                    "SELECT amount_cents, method FROM cash_movements WHERE appointment_id = 'apt-1'")) {
                assertEquals(1, cursor.getCount());
                cursor.moveToFirst();
                assertEquals(1_500_000L, cursor.getLong(0));
                assertEquals("CASH", cursor.getString(1));
            }
        }
    }

    @Test
    public void migrate7To8_addsDefaultMessageTemplates() throws IOException {
        try (SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 7)) {
            db.execSQL("INSERT INTO settings VALUES ("
                    + "'default', 'Metanoia', 'Hair Salon', '', '', '', 15, 'Dueño', 'Dueño', 30, 0)");
        }

        try (SupportSQLiteDatabase migrated = helper.runMigrationsAndValidate(
                TEST_DB, 8, true, Migration_7_8.INSTANCE)) {
            try (Cursor cursor = migrated.query(
                    "SELECT reminder_template, birthday_template, waitlist_offer_template "
                            + "FROM settings WHERE id = 'default'")) {
                assertEquals(1, cursor.getCount());
                cursor.moveToFirst();
                assertEquals(com.metanoia.domain.model.Settings.DEFAULT_REMINDER_TEMPLATE,
                        cursor.getString(0));
                assertEquals(com.metanoia.domain.model.Settings.DEFAULT_BIRTHDAY_TEMPLATE,
                        cursor.getString(1));
                assertEquals(com.metanoia.domain.model.Settings.DEFAULT_WAITLIST_OFFER_TEMPLATE,
                        cursor.getString(2));
            }
        }
    }

    @Test
    public void migrate8To9_createsRemindersTableWithIndices() throws IOException {
        try (SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 8)) {
            db.execSQL("INSERT INTO professionals VALUES ("
                    + "'pro-1', 'Ana', '', '', 4000, 1, 1000, 1000)");
            db.execSQL("INSERT INTO clients VALUES ("
                    + "'cli-1', 'Sofía', 'sofia', '5491122334455', 18, 11, 1995, "
                    + "'', 0, 1, 1000, 1000)");
            db.execSQL("INSERT INTO appointments VALUES ("
                    + "'apt-1', 'cli-1', 'pro-1', 20501, 540, 570, 'CONFIRMED', 0, '', "
                    + "NULL, NULL, 1000, 1000, 'Dueño')");
        }

        try (SupportSQLiteDatabase migrated = helper.runMigrationsAndValidate(
                TEST_DB, 9, true, Migration_8_9.INSTANCE)) {
            migrated.execSQL("INSERT INTO reminders VALUES ("
                    + "'rem-1', 'APPOINTMENT', 'apt-1', 'cli-1', NULL, 2000, 'Dueño')");
            try (Cursor cursor = migrated.query(
                    "SELECT type, client_id FROM reminders WHERE appointment_id = 'apt-1'")) {
                assertEquals(1, cursor.getCount());
                cursor.moveToFirst();
                assertEquals("APPOINTMENT", cursor.getString(0));
                assertEquals("cli-1", cursor.getString(1));
            }
        }
    }

    @Test
    public void migrate9To10_createsColorFormulaTablesWithIndices() throws IOException {
        try (SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 9)) {
            db.execSQL("INSERT INTO professionals VALUES ("
                    + "'pro-1', 'Ana', '', '', 4000, 1, 1000, 1000)");
            db.execSQL("INSERT INTO clients VALUES ("
                    + "'cli-1', 'Sofía', 'sofia', '5491122334455', 18, 11, 1995, "
                    + "'', 0, 1, 1000, 1000)");
            db.execSQL("INSERT INTO appointments VALUES ("
                    + "'apt-1', 'cli-1', 'pro-1', 20501, 540, 570, 'CONFIRMED', 0, '', "
                    + "NULL, NULL, 1000, 1000, 'Dueño')");
        }

        try (SupportSQLiteDatabase migrated = helper.runMigrationsAndValidate(
                TEST_DB, 10, true, Migration_9_10.INSTANCE)) {
            migrated.execSQL("INSERT INTO color_formulas VALUES ("
                    + "'cf-1', 'cli-1', 'apt-1', 'pro-1', 20501, 35, '20 vol', "
                    + "'Raíces', 'Parejo', NULL, 2000)");
            migrated.execSQL("INSERT INTO color_formula_components VALUES ("
                    + "'cfc-1', 'cf-1', 'Wella', 'Koleston', '7.1', 300, 'GRAMS', 0)");
            try (Cursor cursor = migrated.query(
                    "SELECT shade, amount_tenths FROM color_formula_components WHERE formula_id = 'cf-1'")) {
                assertEquals(1, cursor.getCount());
                cursor.moveToFirst();
                assertEquals("7.1", cursor.getString(0));
                assertEquals(300, cursor.getInt(1));
            }
        }
    }

    @Test
    public void migrate10To11_createsWaitlistTablesWithIndices() throws IOException {
        try (SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 10)) {
            db.execSQL("INSERT INTO salon_services VALUES ("
                    + "'svc-1', 'Corte', 'corte', 'Corte', 'corte', 1500000, 30, 0, 1, 0, 1000, 1000)");
            db.execSQL("INSERT INTO professionals VALUES ("
                    + "'pro-1', 'Ana', '', '', 4000, 1, 1000, 1000)");
            db.execSQL("INSERT INTO clients VALUES ("
                    + "'cli-1', 'Sofía', 'sofia', '5491122334455', 18, 11, 1995, "
                    + "'', 0, 1, 1000, 1000)");
        }

        try (SupportSQLiteDatabase migrated = helper.runMigrationsAndValidate(
                TEST_DB, 11, true, Migration_10_11.INSTANCE)) {
            migrated.execSQL("INSERT INTO waitlist_entries VALUES ("
                    + "'wl-1', 'cli-1', 'pro-1', NULL, '5,6', 540, 780, 20550, "
                    + "'ACTIVE', '', 2000, NULL)");
            migrated.execSQL("INSERT INTO waitlist_entry_services VALUES ('wl-1', 'svc-1')");
            try (Cursor cursor = migrated.query(
                    "SELECT status, preferred_weekdays FROM waitlist_entries WHERE client_id = 'cli-1'")) {
                assertEquals(1, cursor.getCount());
                cursor.moveToFirst();
                assertEquals("ACTIVE", cursor.getString(0));
                assertEquals("5,6", cursor.getString(1));
            }
        }
    }

    @Test
    public void migrate11To12_createsStockTablesWithIndices() throws IOException {
        try (SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 11)) {
            db.execSQL("INSERT INTO cash_movements VALUES ("
                    + "'mov-1', 20500, 2000, 'PRODUCT_SALE', 'CASH', 1500000, "
                    + "'Venta shampoo', NULL, NULL, 'prod-1', 1, NULL, 'Dueño', NULL)");
        }

        try (SupportSQLiteDatabase migrated = helper.runMigrationsAndValidate(
                TEST_DB, 12, true, Migration_11_12.INSTANCE)) {
            migrated.execSQL("INSERT INTO products VALUES ("
                    + "'prod-1', 'Shampoo Nutritivo', 'shampoo nutritivo', 'Kérastase', "
                    + "'BOTH', 'MILLILITER', 1500000, 2, 4, 1, 1000, 1000)");
            migrated.execSQL("INSERT INTO stock_movements VALUES ("
                    + "'sm-1', 'prod-1', 2000, 'OUT_SALE', -1, 'Venta: Shampoo Nutritivo x1', "
                    + "'mov-1', 'Dueño')");
            try (Cursor cursor = migrated.query(
                    "SELECT current_stock, sale_price_cents FROM products WHERE id = 'prod-1'")) {
                assertEquals(1, cursor.getCount());
                cursor.moveToFirst();
                assertEquals(4, cursor.getInt(0));
                assertEquals(1_500_000L, cursor.getLong(1));
            }
        }
    }

    @Test
    public void migrate1To12_allMigrations_passValidation() throws IOException {
        try (SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 1)) {
            db.execSQL("INSERT INTO audit_logs VALUES ('log-1', 1000, 'Dueño', 'INIT', 'APP', '1', NULL, 'Init')");
        }

        try (SupportSQLiteDatabase migrated = helper.runMigrationsAndValidate(
                TEST_DB,
                12,
                true,
                AppDatabase.allMigrations()
        )) {
            migrated.execSQL("INSERT INTO salon_services VALUES ("
                    + "'svc-all', 'Corte', 'corte', 'Corte', 'corte', 1500000, 30, 0, 1, 0, 1000, 1000)");
            migrated.execSQL("INSERT INTO clients VALUES ("
                    + "'cli-all', 'Sofía Gómez', 'sofia gomez', '5491144556677', NULL, NULL, NULL, "
                    + "'Notas', 1, 1, 1000, 1000)");
            migrated.execSQL("INSERT INTO waitlist_entries VALUES ("
                    + "'wl-all', 'cli-all', NULL, 20520, '', 600, 900, 20530, 'ACTIVE', '', 1000, NULL)");
            migrated.execSQL("INSERT INTO waitlist_entry_services VALUES ('wl-all', 'svc-all')");
            migrated.execSQL("INSERT INTO products VALUES ("
                    + "'prod-all', 'Tintura 7.1', 'tintura 7.1', NULL, 'USE', 'GRAM', NULL, 3, 5, 1, 1000, 1000)");
            migrated.execSQL("INSERT INTO stock_movements VALUES ("
                    + "'sm-all', 'prod-all', 1000, 'IN', 5, 'Stock inicial', NULL, 'Dueño')");
            try (Cursor cursor = migrated.query("SELECT COUNT(*) FROM products")) {
                cursor.moveToFirst();
                assertEquals(1, cursor.getInt(0));
            }
        }
    }
}
