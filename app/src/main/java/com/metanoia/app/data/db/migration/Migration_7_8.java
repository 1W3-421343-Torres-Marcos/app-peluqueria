package com.metanoia.app.data.db.migration;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.metanoia.domain.model.Settings;

/**
 * Agrega columnas de plantillas editables de WhatsApp sobre {@code settings}.
 */
public final class Migration_7_8 extends Migration {

    public static final Migration_7_8 INSTANCE = new Migration_7_8();

    private Migration_7_8() {
        super(7, 8);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase db) {
        db.execSQL("ALTER TABLE `settings` ADD COLUMN `reminder_template` TEXT NOT NULL DEFAULT '"
                + escapeSql(Settings.DEFAULT_REMINDER_TEMPLATE) + "'");
        db.execSQL("ALTER TABLE `settings` ADD COLUMN `birthday_template` TEXT NOT NULL DEFAULT '"
                + escapeSql(Settings.DEFAULT_BIRTHDAY_TEMPLATE) + "'");
        db.execSQL("ALTER TABLE `settings` ADD COLUMN `waitlist_offer_template` TEXT NOT NULL DEFAULT '"
                + escapeSql(Settings.DEFAULT_WAITLIST_OFFER_TEMPLATE) + "'");
    }

    private static String escapeSql(String value) {
        return value.replace("'", "''");
    }
}
