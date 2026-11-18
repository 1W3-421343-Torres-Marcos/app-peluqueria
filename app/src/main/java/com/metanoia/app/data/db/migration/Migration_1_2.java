package com.metanoia.app.data.db.migration;

import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Crea las tablas {@code settings} y {@code salon_hours} e inserta los valores iniciales del salón.
 */
public final class Migration_1_2 extends Migration {

    public static final Migration_1_2 INSTANCE = new Migration_1_2();

    private Migration_1_2() {
        super(1, 2);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `settings` ("
                + "`id` TEXT NOT NULL, "
                + "`business_name` TEXT NOT NULL, "
                + "`subtitle` TEXT NOT NULL, "
                + "`address` TEXT NOT NULL, "
                + "`phone` TEXT NOT NULL, "
                + "`cuit` TEXT NOT NULL, "
                + "`slot_minutes` INTEGER NOT NULL, "
                + "`current_operator` TEXT NOT NULL, "
                + "`operators_csv` TEXT NOT NULL, "
                + "`waitlist_expiry_days` INTEGER NOT NULL, "
                + "`updated_at` INTEGER NOT NULL, "
                + "PRIMARY KEY(`id`))");

        db.execSQL("CREATE TABLE IF NOT EXISTS `salon_hours` ("
                + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                + "`day_of_week` INTEGER NOT NULL, "
                + "`start_min` INTEGER NOT NULL, "
                + "`end_min` INTEGER NOT NULL)");

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_salon_hours_day_of_week` "
                + "ON `salon_hours` (`day_of_week`)");

        seedDefaults(db);
    }

    /**
     * Inserta la fila única por defecto y el horario base lunes–sábado 09:00–20:00 si aún no existen.
     */
    public static void seedDefaults(@NonNull SupportSQLiteDatabase db) {
        db.execSQL("INSERT OR IGNORE INTO `settings` ("
                + "`id`, `business_name`, `subtitle`, `address`, `phone`, `cuit`, "
                + "`slot_minutes`, `current_operator`, `operators_csv`, "
                + "`waitlist_expiry_days`, `updated_at`) "
                + "VALUES ('default', 'Metanoia', 'Hair Salon', '', '', '', "
                + "15, 'Dueño', 'Dueño', 30, 0)");

        try (Cursor cursor = db.query("SELECT COUNT(*) FROM `salon_hours`")) {
            if (cursor.moveToFirst() && cursor.getInt(0) == 0) {
                for (int day = 1; day <= 6; day++) {
                    db.execSQL("INSERT INTO `salon_hours` (`day_of_week`, `start_min`, `end_min`) "
                            + "VALUES (" + day + ", 540, 1200)");
                }
            }
        }
    }
}
