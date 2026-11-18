package com.metanoia.app.data.db.migration;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Crea las tablas {@code color_formulas} y {@code color_formula_components} con FK RESTRICT e índices.
 */
public final class Migration_9_10 extends Migration {

    public static final Migration_9_10 INSTANCE = new Migration_9_10();

    private Migration_9_10() {
        super(9, 10);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `color_formulas` ("
                + "`id` TEXT NOT NULL, "
                + "`client_id` TEXT NOT NULL, "
                + "`appointment_id` TEXT, "
                + "`professional_id` TEXT, "
                + "`day` INTEGER NOT NULL, "
                + "`exposure_min` INTEGER NOT NULL, "
                + "`developer_volume` TEXT, "
                + "`notes` TEXT NOT NULL, "
                + "`result` TEXT, "
                + "`deleted_at` INTEGER, "
                + "`created_at` INTEGER NOT NULL, "
                + "PRIMARY KEY(`id`), "
                + "FOREIGN KEY(`client_id`) REFERENCES `clients`(`id`) "
                + "ON UPDATE NO ACTION ON DELETE RESTRICT , "
                + "FOREIGN KEY(`appointment_id`) REFERENCES `appointments`(`id`) "
                + "ON UPDATE NO ACTION ON DELETE RESTRICT , "
                + "FOREIGN KEY(`professional_id`) REFERENCES `professionals`(`id`) "
                + "ON UPDATE NO ACTION ON DELETE RESTRICT )");

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_color_formulas_client_id_day` "
                + "ON `color_formulas` (`client_id`, `day`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_color_formulas_appointment_id` "
                + "ON `color_formulas` (`appointment_id`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_color_formulas_professional_id` "
                + "ON `color_formulas` (`professional_id`)");

        db.execSQL("CREATE TABLE IF NOT EXISTS `color_formula_components` ("
                + "`id` TEXT NOT NULL, "
                + "`formula_id` TEXT NOT NULL, "
                + "`brand` TEXT NOT NULL, "
                + "`product_line` TEXT, "
                + "`shade` TEXT NOT NULL, "
                + "`amount_tenths` INTEGER NOT NULL, "
                + "`unit` TEXT NOT NULL, "
                + "`position` INTEGER NOT NULL, "
                + "PRIMARY KEY(`id`), "
                + "FOREIGN KEY(`formula_id`) REFERENCES `color_formulas`(`id`) "
                + "ON UPDATE NO ACTION ON DELETE RESTRICT )");

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_color_formula_components_formula_id_position` "
                + "ON `color_formula_components` (`formula_id`, `position`)");
    }
}
