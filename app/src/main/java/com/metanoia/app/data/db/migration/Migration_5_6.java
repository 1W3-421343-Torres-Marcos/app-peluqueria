package com.metanoia.app.data.db.migration;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Crea las tablas {@code recurrence_rules}, {@code appointments}, {@code appointment_items}
 * y {@code schedule_blocks} con índices y claves foráneas RESTRICT.
 */
public final class Migration_5_6 extends Migration {

    public static final Migration_5_6 INSTANCE = new Migration_5_6();

    private Migration_5_6() {
        super(5, 6);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `recurrence_rules` ("
                + "`id` TEXT NOT NULL, "
                + "`interval_weeks` INTEGER NOT NULL, "
                + "`until_day` INTEGER, "
                + "`max_occurrences` INTEGER, "
                + "`created_at` INTEGER NOT NULL, "
                + "PRIMARY KEY(`id`))");

        db.execSQL("CREATE TABLE IF NOT EXISTS `appointments` ("
                + "`id` TEXT NOT NULL, "
                + "`client_id` TEXT NOT NULL, "
                + "`professional_id` TEXT NOT NULL, "
                + "`day` INTEGER NOT NULL, "
                + "`start_min` INTEGER NOT NULL, "
                + "`end_min` INTEGER NOT NULL, "
                + "`status` TEXT NOT NULL, "
                + "`adjustment_cents` INTEGER NOT NULL, "
                + "`notes` TEXT NOT NULL, "
                + "`recurrence_rule_id` TEXT, "
                + "`cancel_reason` TEXT, "
                + "`created_at` INTEGER NOT NULL, "
                + "`updated_at` INTEGER NOT NULL, "
                + "`created_by` TEXT NOT NULL, "
                + "PRIMARY KEY(`id`), "
                + "FOREIGN KEY(`client_id`) REFERENCES `clients`(`id`) "
                + "ON UPDATE NO ACTION ON DELETE RESTRICT , "
                + "FOREIGN KEY(`professional_id`) REFERENCES `professionals`(`id`) "
                + "ON UPDATE NO ACTION ON DELETE RESTRICT )");

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_appointments_professional_id_day` "
                + "ON `appointments` (`professional_id`, `day`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_appointments_client_id_day` "
                + "ON `appointments` (`client_id`, `day`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_appointments_day_start_min` "
                + "ON `appointments` (`day`, `start_min`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_appointments_recurrence_rule_id` "
                + "ON `appointments` (`recurrence_rule_id`)");

        db.execSQL("CREATE TABLE IF NOT EXISTS `appointment_items` ("
                + "`id` TEXT NOT NULL, "
                + "`appointment_id` TEXT NOT NULL, "
                + "`service_id` TEXT NOT NULL, "
                + "`service_name_snapshot` TEXT NOT NULL, "
                + "`price_cents_snapshot` INTEGER NOT NULL, "
                + "`duration_min_snapshot` INTEGER NOT NULL, "
                + "`commission_bp_snapshot` INTEGER, "
                + "`color_formula_id` TEXT, "
                + "`position` INTEGER NOT NULL, "
                + "PRIMARY KEY(`id`), "
                + "FOREIGN KEY(`appointment_id`) REFERENCES `appointments`(`id`) "
                + "ON UPDATE NO ACTION ON DELETE RESTRICT , "
                + "FOREIGN KEY(`service_id`) REFERENCES `salon_services`(`id`) "
                + "ON UPDATE NO ACTION ON DELETE RESTRICT )");

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_appointment_items_appointment_id` "
                + "ON `appointment_items` (`appointment_id`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_appointment_items_service_id` "
                + "ON `appointment_items` (`service_id`)");

        db.execSQL("CREATE TABLE IF NOT EXISTS `schedule_blocks` ("
                + "`id` TEXT NOT NULL, "
                + "`professional_id` TEXT, "
                + "`start_day` INTEGER NOT NULL, "
                + "`end_day` INTEGER NOT NULL, "
                + "`start_min` INTEGER, "
                + "`end_min` INTEGER, "
                + "`type` TEXT NOT NULL, "
                + "`reason` TEXT NOT NULL, "
                + "`active` INTEGER NOT NULL, "
                + "`created_at` INTEGER NOT NULL, "
                + "`updated_at` INTEGER NOT NULL, "
                + "PRIMARY KEY(`id`), "
                + "FOREIGN KEY(`professional_id`) REFERENCES `professionals`(`id`) "
                + "ON UPDATE NO ACTION ON DELETE RESTRICT )");

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_schedule_blocks_start_day_end_day` "
                + "ON `schedule_blocks` (`start_day`, `end_day`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_schedule_blocks_professional_id` "
                + "ON `schedule_blocks` (`professional_id`)");
    }
}
