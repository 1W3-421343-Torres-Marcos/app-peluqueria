package com.metanoia.app.data.backup;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.metanoia.app.data.backup.BackupFolderAccess.FolderState;
import com.metanoia.app.data.db.AppDatabase;
import com.metanoia.app.data.prefs.DevicePrefs;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.BackupManifest;
import com.metanoia.domain.model.BackupTrigger;
import com.metanoia.domain.rules.BackupManifestCodec;
import com.metanoia.domain.rules.BackupValidator;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Inspecciona, valida integridad ({@code SHA-256}, esquema y {@code PRAGMA integrity_check})
 * y ejecuta la restauración segura de backups con copia previa {@code PRE_RESTORE} y rollback.
 */
public final class RestoreManager {

    private static final int BUFFER_SIZE = 16_384;
    private static final int MAX_MANIFEST_BYTES = 1_048_576;

    public static final class ValidatedBackup {
        public final BackupManifest manifest;
        public final File extractedDbFile;

        public ValidatedBackup(
                @NonNull BackupManifest manifest,
                @NonNull File extractedDbFile
        ) {
            this.manifest = manifest;
            this.extractedDbFile = extractedDbFile;
        }
    }

    private final Context context;
    private final AppDatabase database;
    private final DevicePrefs devicePrefs;
    private final BackupManager backupManager;

    public RestoreManager(
            @NonNull Context context,
            @NonNull AppDatabase database,
            @NonNull DevicePrefs devicePrefs,
            @NonNull BackupManager backupManager
    ) {
        this.context = Objects.requireNonNull(context, "context").getApplicationContext();
        this.database = Objects.requireNonNull(database, "database");
        this.devicePrefs = Objects.requireNonNull(devicePrefs, "devicePrefs");
        this.backupManager = Objects.requireNonNull(backupManager, "backupManager");
    }

    @NonNull
    public Result<ValidatedBackup> inspectBackup(@NonNull Uri sourceUri) {
        Objects.requireNonNull(sourceUri, "sourceUri");
        try (InputStream rawIn = context.getContentResolver().openInputStream(sourceUri)) {
            if (rawIn == null) {
                return Result.failure(DomainError.VALIDATION_ERROR,
                        "No se pudo abrir el archivo seleccionado");
            }
            return inspectBackupStream(rawIn);
        } catch (IOException | RuntimeException ex) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "No se pudo leer el backup: " + ex.getMessage());
        }
    }

    @NonNull
    public Result<ValidatedBackup> inspectBackupFile(@NonNull File zipFile) {
        Objects.requireNonNull(zipFile, "zipFile");
        try (InputStream in = new BufferedInputStream(new FileInputStream(zipFile))) {
            return inspectBackupStream(in);
        } catch (IOException | RuntimeException ex) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "No se pudo leer el backup: " + ex.getMessage());
        }
    }

    @NonNull
    public Result<BackupManifest> performRestore(@NonNull ValidatedBackup validated) {
        Objects.requireNonNull(validated, "validated");
        BackupManager.BACKUP_LOCK.lock();
        File stagingDir = new File(context.getCacheDir(), "restore_staging");
        File rollbackFile = new File(stagingDir, "rollback_" + AppDatabase.DATABASE_NAME);
        try {
            if (!validated.extractedDbFile.exists() || !validated.extractedDbFile.isFile()) {
                return Result.failure(DomainError.VALIDATION_ERROR,
                        "El archivo temporal de restauración ya no está disponible");
            }
            String actualSha = BackupManager.computeSha256Hex(validated.extractedDbFile);
            Result<BackupManifest> recheck = BackupValidator.validate(
                    validated.manifest, actualSha, BackupManager.CURRENT_SCHEMA_VERSION);
            if (!recheck.isSuccess()) {
                return recheck;
            }

            boolean preRestoreOk = createPreRestoreBackup();
            if (!preRestoreOk) {
                return Result.failure(DomainError.UNEXPECTED,
                        "No se pudo generar el backup previo antes de restaurar");
            }

            if (database.isOpen()) {
                SupportSQLiteDatabase writable = database.getOpenHelper().getWritableDatabase();
                try (Cursor c = writable.query("PRAGMA wal_checkpoint(TRUNCATE)")) {
                    c.moveToFirst();
                }
                database.close();
            }

            File activeDbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME);
            File parentDir = activeDbFile.getParentFile();
            if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                return Result.failure(DomainError.UNEXPECTED,
                        "No se pudo preparar la carpeta de base de datos");
            }

            ensureDirectory(stagingDir);
            boolean hasRollback = false;
            if (activeDbFile.exists()) {
                copyFile(activeDbFile, rollbackFile);
                hasRollback = true;
            }

            try {
                copyFile(validated.extractedDbFile, activeDbFile);
                deleteSidecarWalFiles(activeDbFile);
            } catch (IOException replaceEx) {
                if (hasRollback && rollbackFile.exists()) {
                    try {
                        copyFile(rollbackFile, activeDbFile);
                        deleteSidecarWalFiles(activeDbFile);
                    } catch (IOException ignored) {
                        // Mejor esfuerzo de rollback en disco
                    }
                }
                return Result.failure(DomainError.UNEXPECTED,
                        "Falló el reemplazo de la base de datos: " + replaceEx.getMessage());
            }

            return Result.success(validated.manifest);
        } catch (IOException | RuntimeException ex) {
            return Result.failure(DomainError.UNEXPECTED,
                    "Error al restaurar backup: " + ex.getMessage());
        } finally {
            deleteQuietly(rollbackFile);
            deleteQuietly(validated.extractedDbFile);
            BackupManager.BACKUP_LOCK.unlock();
        }
    }

    @NonNull
    private Result<ValidatedBackup> inspectBackupStream(@NonNull InputStream rawIn)
            throws IOException {
        File stagingDir = new File(context.getCacheDir(), "restore_staging");
        clearDirectory(stagingDir);
        ensureDirectory(stagingDir);

        File extractedDb = new File(stagingDir, "extracted_" + AppDatabase.DATABASE_NAME);
        String manifestJson = null;

        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(rawIn))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                try {
                    if (entry.isDirectory()) {
                        continue;
                    }
                    String name = entry.getName();
                    if (name.contains("..") || name.contains("/") || name.contains("\\")) {
                        deleteQuietly(extractedDb);
                        return Result.failure(DomainError.VALIDATION_ERROR,
                                "El archivo ZIP contiene rutas no permitidas");
                    }
                    if (BackupManager.ENTRY_MANIFEST.equals(name)) {
                        manifestJson = readBoundedUtf8(zis, MAX_MANIFEST_BYTES);
                    } else if (BackupManager.ENTRY_DATABASE.equals(name)) {
                        copyStreamToFile(zis, extractedDb);
                    }
                } finally {
                    zis.closeEntry();
                }
            }
        } catch (IOException | IllegalArgumentException ex) {
            deleteQuietly(extractedDb);
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "El archivo ZIP está corrupto o es ilegible");
        }

        if (manifestJson == null || !extractedDb.exists() || extractedDb.length() == 0L) {
            deleteQuietly(extractedDb);
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "El ZIP no contiene una copia válida de Metanoia");
        }

        Result<BackupManifest> parsed = BackupManifestCodec.fromJson(manifestJson);
        if (!parsed.isSuccess()) {
            deleteQuietly(extractedDb);
            return Result.failure(parsed.getError(), parsed.getErrorMessage());
        }

        String actualSha = BackupManager.computeSha256Hex(extractedDb);
        Result<BackupManifest> validation = BackupValidator.validate(
                parsed.getValue(), actualSha, BackupManager.CURRENT_SCHEMA_VERSION);
        if (!validation.isSuccess()) {
            deleteQuietly(extractedDb);
            return Result.failure(validation.getError(), validation.getErrorMessage());
        }

        if (!passesSqliteIntegrityCheck(extractedDb)) {
            deleteQuietly(extractedDb);
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "La base de datos dentro del backup no superó integrity_check");
        }

        return Result.success(new ValidatedBackup(validation.getValue(), extractedDb));
    }

    private boolean createPreRestoreBackup() {
        File currentDb = context.getDatabasePath(AppDatabase.DATABASE_NAME);
        if (!currentDb.exists()) {
            return true;
        }
        if (BackupFolderAccess.inspect(context, devicePrefs).state == FolderState.READY) {
            return backupManager
                    .createBackupToConfiguredFolder(BackupTrigger.PRE_RESTORE)
                    .isSuccess();
        }
        return backupManager
                .createBackupInCacheDir("pre_restore_backups", BackupTrigger.PRE_RESTORE)
                .isSuccess();
    }

    private static boolean passesSqliteIntegrityCheck(@NonNull File dbFile) {
        try (SQLiteDatabase sqlite = SQLiteDatabase.openDatabase(
                dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
                Cursor cursor = sqlite.rawQuery("PRAGMA integrity_check", null)) {
            if (!cursor.moveToFirst()) {
                return false;
            }
            String result = cursor.getString(0);
            boolean singleRow = !cursor.moveToNext();
            return singleRow && "ok".equalsIgnoreCase(result == null ? "" : result.trim());
        } catch (SQLiteException ex) {
            return false;
        }
    }

    private static void deleteSidecarWalFiles(@NonNull File dbFile) {
        deleteQuietly(new File(dbFile.getPath() + "-wal"));
        deleteQuietly(new File(dbFile.getPath() + "-shm"));
        deleteQuietly(new File(dbFile.getPath() + "-journal"));
    }

    private static String readBoundedUtf8(@NonNull InputStream in, int maxBytes)
            throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4_096];
        int total = 0;
        int read;
        while ((read = in.read(buffer)) != -1) {
            total += read;
            if (total > maxBytes) {
                throw new IllegalArgumentException("Manifiesto demasiado grande");
            }
            out.write(buffer, 0, read);
        }
        return out.toString(StandardCharsets.UTF_8.name());
    }

    private static void copyStreamToFile(@NonNull InputStream in, @NonNull File target)
            throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(target))) {
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
        }
    }

    private static void copyFile(@NonNull File source, @NonNull File target) throws IOException {
        try (InputStream in = new BufferedInputStream(new FileInputStream(source))) {
            copyStreamToFile(in, target);
        }
    }

    private static void ensureDirectory(@NonNull File dir) throws IOException {
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("No se pudo crear carpeta temporal: " + dir.getAbsolutePath());
        }
    }

    private static void clearDirectory(@NonNull File dir) {
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                if (!child.isDirectory()) {
                    child.delete();
                }
            }
        }
    }

    private static void deleteQuietly(File file) {
        if (file != null && file.exists()) {
            file.delete();
        }
    }
}
