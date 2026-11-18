package com.metanoia.app.data.db.migration;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Crea las tablas {@code cash_closings} y {@code cash_movements} con índices y claves RESTRICT.
 */
public final class Migration_6_7 extends Migration {

    public static final Migration_6_7 INSTANCE = new Migration_6_7();

    private Migration_6_7() {
        super(6, 7);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `cash_closings` ("
                + "`id` TEXT NOT NULL, "
                + "`day` INTEGER NOT NULL, "
                + "`expected_cash_cents` INTEGER NOT NULL, "
                + "`expected_transfer_cents` INTEGER NOT NULL, "
                + "`expected_card_cents` INTEGER NOT NULL, "
                + "`expected_mp_cents` INTEGER NOT NULL, "
                + "`counted_cash_cents` INTEGER NOT NULL, "
                + "`counted_transfer_cents` INTEGER NOT NULL, "
                + "`counted_card_cents` INTEGER NOT NULL, "
                + "`counted_mp_cents` INTEGER NOT NULL, "
                + "`difference_cents` INTEGER NOT NULL, "
                + "`notes` TEXT NOT NULL, "
                + "`closed_at` INTEGER NOT NULL, "
                + "`operator` TEXT NOT NULL, "
                + "PRIMARY KEY(`id`))");

        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_cash_closings_day` "
                + "ON `cash_closings` (`day`)");

        db.execSQL("CREATE TABLE IF NOT EXISTS `cash_movements` ("
                + "`id` TEXT NOT NULL, "
                + "`day` INTEGER NOT NULL, "
                + "`timestamp` INTEGER NOT NULL, "
                + "`type` TEXT NOT NULL, "
                + "`method` TEXT NOT NULL, "
                + "`amount_cents` INTEGER NOT NULL, "
                + "`concept` TEXT NOT NULL, "
                + "`appointment_id` TEXT, "
                + "`client_id` TEXT, "
                + "`product_id` TEXT, "
                + "`quantity` INTEGER, "
                + "`reverses_movement_id` TEXT, "
                + "`operator` TEXT NOT NULL, "
                + "`closing_id` TEXT, "
                + "PRIMARY KEY(`id`), "
                + "FOREIGN KEY(`appointment_id`) REFERENCES `appointments`(`id`) "
                + "ON UPDATE NO ACTION ON DELETE RESTRICT , "
                + "FOREIGN KEY(`client_id`) REFERENCES `clients`(`id`) "
                + "ON UPDATE NO ACTION ON DELETE RESTRICT , "
                + "FOREIGN KEY(`reverses_movement_id`) REFERENCES `cash_movements`(`id`) "
                + "ON UPDATE NO ACTION ON DELETE RESTRICT , "
                + "FOREIGN KEY(`closing_id`) REFERENCES `cash_closings`(`id`) "
                + "ON UPDATE NO ACTION ON DELETE RESTRICT )");

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cash_movements_day` "
                + "ON `cash_movements` (`day`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cash_movements_method` "
                + "ON `cash_movements` (`method`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cash_movements_appointment_id` "
                + "ON `cash_movements` (`appointment_id`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cash_movements_client_id` "
                + "ON `cash_movements` (`client_id`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cash_movements_reverses_movement_id` "
                + "ON `cash_movements` (`reverses_movement_id`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cash_movements_closing_id` "
                + "ON `cash_movements` (`closing_id`)");
    }
}
