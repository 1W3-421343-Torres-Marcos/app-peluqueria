package com.metanoia.domain.rules;

import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.BackupManifest;

/**
 * Validaciones puras previas a restaurar un backup: formato, versión de esquema y SHA-256.
 */
public final class BackupValidator {

    private BackupValidator() {
    }

    public static Result<BackupManifest> validate(
            BackupManifest manifest,
            String actualDbSha256,
            int currentSchemaVersion
    ) {
        if (manifest == null) {
            return Result.failure(DomainError.VALIDATION_ERROR, "Falta el manifiesto del backup");
        }
        if (actualDbSha256 == null || actualDbSha256.trim().isEmpty()) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "No se pudo verificar la huella SHA-256 de la base de datos");
        }
        if (manifest.format() != BackupManifest.CURRENT_FORMAT) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "Formato de backup no soportado: " + manifest.format());
        }
        if (manifest.schemaVersion() <= 0 || manifest.schemaVersion() > currentSchemaVersion) {
            return Result.failure(DomainError.CONFLICT,
                    "El backup pertenece a una versión más nueva de la aplicación (esquema "
                            + manifest.schemaVersion() + ")");
        }
        if (!manifest.sha256().equalsIgnoreCase(actualDbSha256.trim())) {
            return Result.failure(DomainError.VALIDATION_ERROR,
                    "El archivo de base de datos está dañado o no coincide con el manifiesto");
        }
        return Result.success(manifest);
    }
}
