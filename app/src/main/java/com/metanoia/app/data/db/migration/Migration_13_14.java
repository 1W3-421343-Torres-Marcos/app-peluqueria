package com.metanoia.app.data.db.migration;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Agrega columnas de vínculo, origen y fecha de importación desde contactos sobre {@code clients}.
 */
public final class Migration_13_14 extends Migration {

    public static final Migration_13_14 INSTANCE = new Migration_13_14();

    private Migration_13_14() {
        super(13, 14);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase db) {
        db.execSQL("ALTER TABLE `clients` ADD COLUMN `contact_lookup_key` TEXT");
        db.execSQL("ALTER TABLE `clients` ADD COLUMN `origin` TEXT NOT NULL DEFAULT 'MANUAL'");
        db.execSQL("ALTER TABLE `clients` ADD COLUMN `created_from_contact_at` INTEGER");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_clients_contact_lookup_key` "
                + "ON `clients` (`contact_lookup_key`)");
    }
}
