package com.metanoia.app.data.db.migration;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Crea las tablas {@code commission_settlements} y {@code commission_settlement_entries}
 * con FK RESTRICT e índices.
 */
public final class Migration_12_13 extends Migration {

    public static final Migration_12_13 INSTANCE = new Migration_12_13();

    private Migration_12_13() {
        super(12, 13);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `commission_settlements` ("
                + "`id` TEXT NOT NULL, "
                + "`professional_id` TEXT NOT NULL, "
                + "`from_day` INTEGER NOT NULL, "
                + "`to_day` INTEGER NOT NULL, "
                + "`base_cents` INTEGER NOT NULL, "
                + "`commission_cents` INTEGER NOT NULL, "
                + "`cash_movement_id` TEXT NOT NULL, "
                + "`settled_at` INTEGER NOT NULL, "
                + "`operator` TEXT NOT NULL, "
                + "PRIMARY KEY(`id`), "
                + "FOREIGN KEY(`professional_id`) REFERENCES `professionals`(`id`) "
                + "ON UPDATE NO ACTION ON DELETE RESTRICT , "
                + "FOREIGN KEY(`cash_movement_id`) REFERENCES `cash_movements`(`id`) "
                + "ON UPDATE NO ACTION ON DELETE RESTRICT )");

        db.execSQL("CREATE INDEX IF NOT EXISTS "
                + "`index_commission_settlements_professional_id_from_day_to_day` "
                + "ON `commission_settlements` (`professional_id`, `from_day`, `to_day`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS "
                + "`index_commission_settlements_cash_movement_id` "
                + "ON `commission_settlements` (`cash_movement_id`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS "
                + "`index_commission_settlements_settled_at` "
                + "ON `commission_settlements` (`settled_at`)");

        db.execSQL("CREATE TABLE IF NOT EXISTS `commission_settlement_entries` ("
                + "`id` TEXT NOT NULL, "
                + "`settlement_id` TEXT NOT NULL, "
                + "`appointment_id` TEXT NOT NULL, "
                + "`type` TEXT NOT NULL, "
                + "`base_cents` INTEGER NOT NULL, "
                + "`commission_cents` INTEGER NOT NULL, "
                + "PRIMARY KEY(`id`), "
                + "FOREIGN KEY(`settlement_id`) REFERENCES `commission_settlements`(`id`) "
                + "ON UPDATE NO ACTION ON DELETE RESTRICT , "
                + "FOREIGN KEY(`appointment_id`) REFERENCES `appointments`(`id`) "
                + "ON UPDATE NO ACTION ON DELETE RESTRICT )");

        db.execSQL("CREATE INDEX IF NOT EXISTS "
                + "`index_commission_settlement_entries_settlement_id` "
                + "ON `commission_settlement_entries` (`settlement_id`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS "
                + "`index_commission_settlement_entries_appointment_id` "
                + "ON `commission_settlement_entries` (`appointment_id`)");
    }
}
