package com.metanoia.app.data.db;

import static org.junit.Assert.assertEquals;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

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
}
