package com.metanoia.app.data.db.migration;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Crea la tabla {@code reminders} con claves foráneas RESTRICT e índices para turnos y cumpleaños.
 */
public final class Migration_8_9 extends Migration {

    public static final Migration_8_9 INSTANCE = new Migration_8_9();

    private Migration_8_9() {
        super(8, 9);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `reminders` ("
                + "`id` TEXT NOT NULL, "
                + "`type` TEXT NOT NULL, "
                + "`appointment_id` TEXT, "
                + "`client_id` TEXT NOT NULL, "
                + "`year` INTEGER, "
                + "`sent_at` INTEGER NOT NULL, "
                + "`sent_by` TEXT NOT NULL, "
                + "PRIMARY KEY(`id`), "
                + "FOREIGN KEY(`appointment_id`) REFERENCES `appointments`(`id`) "
                + "ON UPDATE NO ACTION ON DELETE RESTRICT , "
                + "FOREIGN KEY(`client_id`) REFERENCES `clients`(`id`) "
                + "ON UPDATE NO ACTION ON DELETE RESTRICT )");

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_appointment_id` "
                + "ON `reminders` (`appointment_id`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_client_id` "
                + "ON `reminders` (`client_id`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_type_appointment_id` "
                + "ON `reminders` (`type`, `appointment_id`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_type_client_id_year` "
                + "ON `reminders` (`type`, `client_id`, `year`)");
    }
}
