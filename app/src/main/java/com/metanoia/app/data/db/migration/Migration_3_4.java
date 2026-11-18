package com.metanoia.app.data.db.migration;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Crea las tablas {@code professionals}, {@code work_schedules}, {@code professional_days_off}
 * y {@code professional_services} con claves foráneas RESTRICT.
 */
public final class Migration_3_4 extends Migration {

    public static final Migration_3_4 INSTANCE = new Migration_3_4();

    private Migration_3_4() {
        super(3, 4);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `professionals` ("
                + "`id` TEXT NOT NULL, "
                + "`name` TEXT NOT NULL, "
                + "`phone` TEXT NOT NULL, "
                + "`color_tag` TEXT NOT NULL, "
                + "`commission_basis_points` INTEGER NOT NULL, "
                + "`active` INTEGER NOT NULL, "
                + "`created_at` INTEGER NOT NULL, "
                + "`updated_at` INTEGER NOT NULL, "
                + "PRIMARY KEY(`id`))");

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_professionals_active_name` "
                + "ON `professionals` (`active`, `name`)");

        db.execSQL("CREATE TABLE IF NOT EXISTS `work_schedules` ("
                + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                + "`professional_id` TEXT NOT NULL, "
                + "`day_of_week` INTEGER NOT NULL, "
                + "`start_min` INTEGER NOT NULL, "
                + "`end_min` INTEGER NOT NULL, "
                + "FOREIGN KEY(`professional_id`) REFERENCES `professionals`(`id`) "
                + "ON UPDATE NO ACTION ON DELETE RESTRICT )");

        db.execSQL("CREATE INDEX IF NOT EXISTS "
                + "`index_work_schedules_professional_id_day_of_week` "
                + "ON `work_schedules` (`professional_id`, `day_of_week`)");

        db.execSQL("CREATE TABLE IF NOT EXISTS `professional_days_off` ("
                + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                + "`professional_id` TEXT NOT NULL, "
                + "`day_epoch` INTEGER NOT NULL, "
                + "FOREIGN KEY(`professional_id`) REFERENCES `professionals`(`id`) "
                + "ON UPDATE NO ACTION ON DELETE RESTRICT )");

        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS "
                + "`index_professional_days_off_professional_id_day_epoch` "
                + "ON `professional_days_off` (`professional_id`, `day_epoch`)");

        db.execSQL("CREATE TABLE IF NOT EXISTS `professional_services` ("
                + "`professional_id` TEXT NOT NULL, "
                + "`service_id` TEXT NOT NULL, "
                + "PRIMARY KEY(`professional_id`, `service_id`), "
                + "FOREIGN KEY(`professional_id`) REFERENCES `professionals`(`id`) "
                + "ON UPDATE NO ACTION ON DELETE RESTRICT , "
                + "FOREIGN KEY(`service_id`) REFERENCES `salon_services`(`id`) "
                + "ON UPDATE NO ACTION ON DELETE RESTRICT )");

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_professional_services_service_id` "
                + "ON `professional_services` (`service_id`)");
    }
}
