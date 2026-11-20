package com.metanoia.app.data.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.metanoia.app.data.backup.RestoreManager.ValidatedBackup;
import com.metanoia.app.data.db.AppDatabase;
import com.metanoia.app.data.db.entity.ClientEntity;
import com.metanoia.app.data.prefs.DevicePrefs;
import com.metanoia.app.data.repository.AuditLogRepositoryImpl;
import com.metanoia.app.data.repository.SettingsRepositoryImpl;
import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.BackupManifest;
import com.metanoia.domain.model.BackupTrigger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

@RunWith(AndroidJUnit4.class)
public class BackupRestoreRoundTripTest {

    private Context context;
    private AppDatabase db;
    private DevicePrefs prefs;
    private BackupManager backupManager;
    private RestoreManager restoreManager;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase(AppDatabase.DATABASE_NAME);
        prefs = new DevicePrefs(context);
        prefs.setBackupTreeUri(null);
        db = Room.databaseBuilder(context, AppDatabase.class, AppDatabase.DATABASE_NAME)
                .addMigrations(AppDatabase.allMigrations())
                .allowMainThreadQueries()
                .build();
        Clock clock = Clock.systemArgentina();
        IdGenerator ids = IdGenerator.uuid();
        backupManager = new BackupManager(
                context,
                db,
                prefs,
                new AuditLogRepositoryImpl(db.auditLogDao()),
                new SettingsRepositoryImpl(db.settingsDao(), clock),
                clock,
                ids
        );
        restoreManager = new RestoreManager(context, db, prefs, backupManager);
    }

    @After
    public void tearDown() {
        if (db != null && db.isOpen()) {
            db.close();
        }
        context.deleteDatabase(AppDatabase.DATABASE_NAME);
    }

    @Test
    public void backupAndRestore_preservesClientsAfterDatabaseMutation() {
        db.clientDao().upsert(new ClientEntity(
                "cli-backup-1", "Sofía Gómez", "sofia gomez", "5491144556677",
                12, 5, null, "Sin amoníaco", 0, true, 1_000L, 1_000L));
        db.clientDao().upsert(new ClientEntity(
                "cli-sync-1", "Lucía Contacto", "lucia contacto", "5491133445566",
                21, 8, null, "", 0, true, 1_500L, 1_500L,
                "lk-lucia-1", "CONTACT_SYNC", 1_500L));

        Result<File> zipResult = backupManager.createBackupInCacheDir(
                "test_roundtrip", BackupTrigger.MANUAL);
        assertTrue(zipResult.isSuccess());

        db.clientDao().upsert(new ClientEntity(
                "cli-extra-temp", "Temporal", "temporal", "5491199999999",
                null, null, null, "", 0, true, 2_000L, 2_000L));

        Result<ValidatedBackup> inspected = restoreManager.inspectBackupFile(zipResult.getValue());
        assertTrue(inspected.isSuccess());
        assertEquals(2, inspected.getValue().manifest.clientsCount());
        assertEquals(14, inspected.getValue().manifest.schemaVersion());

        Result<BackupManifest> restored = restoreManager.performRestore(inspected.getValue());
        assertTrue(restored.isSuccess());

        db = Room.databaseBuilder(context, AppDatabase.class, AppDatabase.DATABASE_NAME)
                .addMigrations(AppDatabase.allMigrations())
                .allowMainThreadQueries()
                .build();
        ClientEntity sofia = db.clientDao().findByIdSync("cli-backup-1");
        ClientEntity lucia = db.clientDao().findByIdSync("cli-sync-1");
        assertEquals("Sofía Gómez", sofia.name);
        assertEquals("MANUAL", sofia.origin);
        assertEquals("Lucía Contacto", lucia.name);
        assertEquals("CONTACT_SYNC", lucia.origin);
        assertEquals("lk-lucia-1", lucia.contactLookupKey);
        assertEquals(Long.valueOf(1_500L), lucia.createdFromContactAt);
        assertEquals(null, db.clientDao().findByIdSync("cli-extra-temp"));
    }
}
