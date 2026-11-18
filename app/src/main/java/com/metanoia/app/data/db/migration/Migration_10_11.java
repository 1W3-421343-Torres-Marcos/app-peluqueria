package com.metanoia.app.data.db.migration;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Crea las tablas {@code waitlist_entries} y {@code waitlist_entry_services} con FK RESTRICT e índices.
 */
public final class Migration_10_11 extends Migration {

    public static final Migration_10_11 INSTANCE = new Migration_10_11();

    private Migration_10_11() {
        super(10, 11);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `waitlist_entries` ("
                + "`id` TEXT NOT NULL, "
                + "`client_id` TEXT NOT NULL, "
                + "`professional_id` TEXT, "
                + "`specific_day` INTEGER, "
                + "`preferred_weekdays` TEXT NOT NULL, "
                + "`from_min` INTEGER NOT NULL, "
                + "`to_min` INTEGER NOT NULL, "
                + "`valid_until_day` INTEGER NOT NULL, "
                + "`status` TEXT NOT NULL, "
                + "`notes` TEXT NOT NULL, "
                + "`created_at` INTEGER NOT NULL, "
                + "`offered_at` INTEGER, "
                + "PRIMARY KEY(`id`), "
                + "FOREIGN KEY(`client_id`) REFERENCES `clients`(`id`) "
                + "ON UPDATE NO ACTION ON DELETE RESTRICT , "
                + "FOREIGN KEY(`professional_id`) REFERENCES `professionals`(`id`) "
                + "ON UPDATE NO ACTION ON DELETE RESTRICT )");

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_waitlist_entries_status_valid_until_day` "
                + "ON `waitlist_entries` (`status`, `valid_until_day`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_waitlist_entries_client_id_status` "
                + "ON `waitlist_entries` (`client_id`, `status`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_waitlist_entries_professional_id` "
                + "ON `waitlist_entries` (`professional_id`)");

        db.execSQL("CREATE TABLE IF NOT EXISTS `waitlist_entry_services` ("
                + "`entry_id` TEXT NOT NULL, "
                + "`service_id` TEXT NOT NULL, "
                + "PRIMARY KEY(`entry_id`, `service_id`), "
                + "FOREIGN KEY(`entry_id`) REFERENCES `waitlist_entries`(`id`) "
                + "ON UPDATE NO ACTION ON DELETE RESTRICT , "
                + "FOREIGN KEY(`service_id`) REFERENCES `salon_services`(`id`) "
                + "ON UPDATE NO ACTION ON DELETE RESTRICT )");

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_waitlist_entry_services_service_id` "
                + "ON `waitlist_entry_services` (`service_id`)");
    }
}
