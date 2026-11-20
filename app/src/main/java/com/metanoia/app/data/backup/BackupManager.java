package com.metanoia.app.data.backup;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.metanoia.app.BuildConfig;
import com.metanoia.app.data.backup.BackupFolderAccess.FolderState;
import com.metanoia.app.data.backup.BackupFolderAccess.FolderStatus;
import com.metanoia.app.data.db.AppDatabase;
import com.metanoia.app.data.prefs.DevicePrefs;
import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.AuditRecord;
import com.metanoia.domain.model.BackupManifest;
import com.metanoia.domain.model.BackupTrigger;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.rules.BackupManifestCodec;
import com.metanoia.domain.rules.BackupRetentionPolicy;
import com.metanoia.domain.rules.BackupRetentionPolicy.BackupFileEntry;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Generador consistente de backups ZIP con {@code PRAGMA wal_checkpoint(TRUNCATE)},
 * hash SHA-256 del archivo SQLite y manifiesto JSON.
 */
public final class BackupManager {

    public static final ReentrantLock BACKUP_LOCK = new ReentrantLock();
    public static final int CURRENT_SCHEMA_VERSION = 14;
    public static final String FILE_PREFIX = "metanoia-backup-";
    public static final String ENTRY_DATABASE = AppDatabase.DATABASE_NAME;
    public static final String ENTRY_MANIFEST = "manifest.json";

    private static final int BUFFER_SIZE = 16_384;
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.US);

    public static final class BackupCreated {
        public final BackupManifest manifest;
        public final String fileName;
        public final long createdAtMillis;

        public BackupCreated(
                @NonNull BackupManifest manifest,
                @NonNull String fileName,
                long createdAtMillis
        ) {
            this.manifest = manifest;
            this.fileName = fileName;
            this.createdAtMillis = createdAtMillis;
        }
    }

    private final Context context;
    private final AppDatabase database;
    private final DevicePrefs devicePrefs;
    private final AuditLogRepository auditLogRepository;
    private final SettingsRepository settingsRepository;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public BackupManager(
            @NonNull Context context,
            @NonNull AppDatabase database,
            @NonNull DevicePrefs devicePrefs,
            @NonNull AuditLogRepository auditLogRepository,
            @NonNull SettingsRepository settingsRepository,
            @NonNull Clock clock,
            @NonNull IdGenerator idGenerator
    ) {
        this.context = Objects.requireNonNull(context, "context").getApplicationContext();
        this.database = Objects.requireNonNull(database, "database");
        this.devicePrefs = Objects.requireNonNull(devicePrefs, "devicePrefs");
        this.auditLogRepository = Objects.requireNonNull(auditLogRepository, "auditLogRepository");
        this.settingsRepository = Objects.requireNonNull(settingsRepository, "settingsRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    }

    @NonNull
    public static String buildBackupFileName(long epochMillis, @NonNull BackupTrigger trigger) {
        ZonedDateTime zdt = Instant.ofEpochMilli(epochMillis).atZone(Clock.ARGENTINA_ZONE);
        return FILE_PREFIX + zdt.format(FILE_TIMESTAMP_FORMATTER) + "-" + trigger.name() + ".zip";
    }

    @NonNull
    public static String computeSha256Hex(@NonNull File file) throws IOException {
        MessageDigest digest = createSha256Digest();
        byte[] buffer = new byte[BUFFER_SIZE];
        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return toHex(digest.digest());
    }

    @NonNull
    public Result<BackupCreated> createBackupToConfiguredFolder(@NonNull BackupTrigger trigger) {
        Objects.requireNonNull(trigger, "trigger");
        FolderStatus folderStatus = BackupFolderAccess.inspect(context, devicePrefs);
        if (folderStatus.state != FolderState.READY || folderStatus.treeUri == null) {
            String msg = folderStatus.state == FolderState.PERMISSION_LOST
                    ? "Permiso perdido sobre carpeta de backup"
                    : "Carpeta de backup no configurada";
            devicePrefs.recordBackupError(msg);
            return Result.failure(DomainError.CONFLICT, msg);
        }

        BACKUP_LOCK.lock();
        File stagingDir = new File(context.getCacheDir(), "backup_staging");
        File stagedZip = null;
        try {
            StagedPackage staged = buildStagedZipLocked(stagingDir, trigger);
            stagedZip = staged.zipFile;

            DocumentFile folderDoc = DocumentFile.fromTreeUri(context, folderStatus.treeUri);
            if (folderDoc == null || !folderDoc.canWrite()) {
                String msg = "No se puede escribir en la carpeta de backup";
                devicePrefs.recordBackupError(msg);
                return Result.failure(DomainError.CONFLICT, msg);
            }
            DocumentFile existing = folderDoc.findFile(staged.fileName);
            if (existing != null) {
                existing.delete();
            }
            DocumentFile targetFile = folderDoc.createFile("application/zip", staged.fileName);
            if (targetFile == null) {
                String msg = "No se pudo crear el archivo ZIP en la carpeta destino";
                devicePrefs.recordBackupError(msg);
                return Result.failure(DomainError.UNEXPECTED, msg);
            }
            try (OutputStream out = context.getContentResolver()
                    .openOutputStream(targetFile.getUri())) {
                if (out == null) {
                    targetFile.delete();
                    String msg = "No se pudo abrir flujo de escritura SAF";
                    devicePrefs.recordBackupError(msg);
                    return Result.failure(DomainError.UNEXPECTED, msg);
                }
                copyFileToStream(stagedZip, out);
            } catch (IOException writeEx) {
                targetFile.delete();
                throw writeEx;
            }

            devicePrefs.recordBackupSuccess(
                    staged.createdAtMillis, staged.fileName, trigger.name());
            recordBackupAudit(staged.fileName, staged.manifest);
            applyRetentionPolicy(folderDoc, staged.fileName);
            return Result.success(
                    new BackupCreated(staged.manifest, staged.fileName, staged.createdAtMillis));
        } catch (IOException | RuntimeException ex) {
            String err = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            devicePrefs.recordBackupError(err);
            return Result.failure(DomainError.UNEXPECTED, "Error al generar backup: " + err);
        } finally {
            deleteQuietly(stagedZip);
            BACKUP_LOCK.unlock();
        }
    }

    @NonNull
    public Result<BackupCreated> exportBackupToUri(
            @NonNull Uri targetUri,
            @NonNull BackupTrigger trigger
    ) {
        Objects.requireNonNull(targetUri, "targetUri");
        Objects.requireNonNull(trigger, "trigger");
        BACKUP_LOCK.lock();
        File stagingDir = new File(context.getCacheDir(), "backup_staging");
        File stagedZip = null;
        try {
            StagedPackage staged = buildStagedZipLocked(stagingDir, trigger);
            stagedZip = staged.zipFile;
            try (OutputStream out = context.getContentResolver().openOutputStream(targetUri)) {
                if (out == null) {
                    return Result.failure(DomainError.UNEXPECTED,
                            "No se pudo escribir en el archivo seleccionado");
                }
                copyFileToStream(stagedZip, out);
            }
            devicePrefs.recordBackupSuccess(
                    staged.createdAtMillis, staged.fileName, trigger.name());
            recordBackupAudit(staged.fileName, staged.manifest);
            return Result.success(
                    new BackupCreated(staged.manifest, staged.fileName, staged.createdAtMillis));
        } catch (IOException | RuntimeException ex) {
            String err = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            return Result.failure(DomainError.UNEXPECTED, "Error al exportar backup: " + err);
        } finally {
            deleteQuietly(stagedZip);
            BACKUP_LOCK.unlock();
        }
    }

    @NonNull
    public Result<File> createBackupInCacheDir(
            @NonNull String subDirName,
            @NonNull BackupTrigger trigger
    ) {
        Objects.requireNonNull(subDirName, "subDirName");
        Objects.requireNonNull(trigger, "trigger");
        BACKUP_LOCK.lock();
        File stagingDir = new File(context.getCacheDir(), "backup_staging");
        File stagedZip = null;
        try {
            StagedPackage staged = buildStagedZipLocked(stagingDir, trigger);
            stagedZip = staged.zipFile;

            File outDir = new File(context.getCacheDir(), subDirName);
            ensureDirectory(outDir);
            File destination = new File(outDir, staged.fileName);
            try (OutputStream out = new BufferedOutputStream(new FileOutputStream(destination))) {
                copyFileToStream(stagedZip, out);
            }
            if (trigger != BackupTrigger.PRE_RESTORE) {
                devicePrefs.recordBackupSuccess(
                        staged.createdAtMillis, staged.fileName, trigger.name());
            }
            recordBackupAudit(staged.fileName, staged.manifest);
            return Result.success(destination);
        } catch (IOException | RuntimeException ex) {
            String err = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            return Result.failure(DomainError.UNEXPECTED, "Error al preparar backup: " + err);
        } finally {
            deleteQuietly(stagedZip);
            BACKUP_LOCK.unlock();
        }
    }

    private StagedPackage buildStagedZipLocked(
            @NonNull File stagingDir,
            @NonNull BackupTrigger trigger
    ) throws IOException {
        ensureDirectory(stagingDir);
        SupportSQLiteDatabase writableDb = database.getOpenHelper().getWritableDatabase();
        checkpointWal(writableDb);
        int clientsCount = querySingleInt(writableDb, "SELECT COUNT(*) FROM clients");
        int appointmentsCount = querySingleInt(writableDb, "SELECT COUNT(*) FROM appointments");
        int cashMovementsCount = querySingleInt(writableDb, "SELECT COUNT(*) FROM cash_movements");

        File dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME);
        if (!dbFile.exists() || !dbFile.isFile()) {
            throw new IOException("No se encontró el archivo de base de datos");
        }

        File stagedDb = new File(stagingDir, "temp_" + ENTRY_DATABASE);
        String sha256 = copyAndHashSha256(dbFile, stagedDb);

        long nowMillis = clock.nowMillis();
        String fileName = buildBackupFileName(nowMillis, trigger);
        BackupManifest manifest = new BackupManifest(
                BackupManifest.CURRENT_FORMAT,
                CURRENT_SCHEMA_VERSION,
                BuildConfig.VERSION_CODE,
                Instant.ofEpochMilli(nowMillis).toString(),
                sha256,
                clientsCount,
                appointmentsCount,
                cashMovementsCount,
                trigger
        );
        byte[] manifestBytes = BackupManifestCodec.toJson(manifest)
                .getBytes(StandardCharsets.UTF_8);

        File stagedZip = new File(stagingDir, fileName);
        try {
            writeZip(stagedDb, manifestBytes, stagedZip);
            return new StagedPackage(manifest, fileName, nowMillis, stagedZip);
        } finally {
            deleteQuietly(stagedDb);
        }
    }

    private void applyRetentionPolicy(
            @NonNull DocumentFile folderDoc,
            @NonNull String newlyCreatedFileName
    ) {
        DocumentFile[] children = folderDoc.listFiles();
        if (children.length == 0) {
            return;
        }
        Map<String, DocumentFile> byId = new LinkedHashMap<>();
        List<BackupFileEntry> entries = new ArrayList<>();
        for (DocumentFile child : children) {
            if (!child.isFile()) {
                continue;
            }
            String name = child.getName();
            if (name == null || name.trim().isEmpty()) {
                continue;
            }
            String id = child.getUri().toString();
            byId.put(id, child);
            entries.add(new BackupFileEntry(id, name, child.lastModified()));
        }
        List<BackupFileEntry> toDelete = BackupRetentionPolicy.selectFilesToDelete(
                entries,
                devicePrefs.getRetentionDays(),
                BackupRetentionPolicy.DEFAULT_KEEP_PRE_RESTORE_COUNT,
                newlyCreatedFileName
        );
        for (BackupFileEntry item : toDelete) {
            DocumentFile doc = byId.get(item.id());
            if (doc != null) {
                doc.delete();
            }
        }
    }

    private void recordBackupAudit(@NonNull String fileName, @NonNull BackupManifest manifest) {
        Settings settings = settingsRepository.getSettings();
        auditLogRepository.record(new AuditRecord.Builder()
                .id(idGenerator.nextId())
                .timestamp(clock.nowMillis())
                .operator(settings.currentOperator())
                .action("BACKUP_CREATE")
                .entityType("BACKUP")
                .entityId(fileName)
                .concept(String.format(
                        Locale.US,
                        "Backup %s (clientes=%d, turnos=%d)",
                        manifest.trigger().name(),
                        manifest.clientsCount(),
                        manifest.appointmentsCount()))
                .build());
    }

    private static void checkpointWal(@NonNull SupportSQLiteDatabase writableDb) {
        try (Cursor cursor = writableDb.query("PRAGMA wal_checkpoint(TRUNCATE)")) {
            cursor.moveToFirst();
        }
    }

    private static int querySingleInt(
            @NonNull SupportSQLiteDatabase db,
            @NonNull String sql
    ) {
        try (Cursor cursor = db.query(sql)) {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
            return 0;
        }
    }

    private static String copyAndHashSha256(@NonNull File source, @NonNull File target)
            throws IOException {
        MessageDigest digest = createSha256Digest();
        byte[] buffer = new byte[BUFFER_SIZE];
        try (
                InputStream in = new BufferedInputStream(new FileInputStream(source));
                OutputStream out = new BufferedOutputStream(new FileOutputStream(target))
        ) {
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
                out.write(buffer, 0, read);
            }
            out.flush();
        }
        return toHex(digest.digest());
    }

    private static void writeZip(
            @NonNull File dbFile,
            @NonNull byte[] manifestBytes,
            @NonNull File zipOutFile
    ) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        try (ZipOutputStream zos = new ZipOutputStream(
                new BufferedOutputStream(new FileOutputStream(zipOutFile)))) {
            ZipEntry dbEntry = new ZipEntry(ENTRY_DATABASE);
            zos.putNextEntry(dbEntry);
            try (InputStream in = new BufferedInputStream(new FileInputStream(dbFile))) {
                int read;
                while ((read = in.read(buffer)) != -1) {
                    zos.write(buffer, 0, read);
                }
            }
            zos.closeEntry();

            ZipEntry manifestEntry = new ZipEntry(ENTRY_MANIFEST);
            zos.putNextEntry(manifestEntry);
            zos.write(manifestBytes);
            zos.closeEntry();
            zos.finish();
        }
    }

    private static void copyFileToStream(@NonNull File source, @NonNull OutputStream out)
            throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        try (InputStream in = new BufferedInputStream(new FileInputStream(source))) {
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
        }
    }

    private static void ensureDirectory(@NonNull File dir) throws IOException {
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("No se pudo crear directorio temporal: " + dir.getAbsolutePath());
        }
    }

    private static void deleteQuietly(File file) {
        if (file != null && file.exists()) {
            file.delete();
        }
    }

    private static MessageDigest createSha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }

    private static String toHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        final char[] digits = "0123456789abcdef".toCharArray();
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hexChars[i * 2] = digits[v >>> 4];
            hexChars[i * 2 + 1] = digits[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static final class StagedPackage {
        final BackupManifest manifest;
        final String fileName;
        final long createdAtMillis;
        final File zipFile;

        StagedPackage(
                BackupManifest manifest,
                String fileName,
                long createdAtMillis,
                File zipFile
        ) {
            this.manifest = manifest;
            this.fileName = fileName;
            this.createdAtMillis = createdAtMillis;
            this.zipFile = zipFile;
        }
    }
}
