package com.metanoia.app.data.db.migration;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Crea la tabla {@code clients} y sus índices de búsqueda sobre {@code name_normalized} y {@code phone}.
 */
public final class Migration_4_5 extends Migration {

    public static final Migration_4_5 INSTANCE = new Migration_4_5();

    private Migration_4_5() {
        super(4, 5);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `clients` ("
                + "`id` TEXT NOT NULL, "
                + "`name` TEXT NOT NULL, "
                + "`name_normalized` TEXT NOT NULL, "
                + "`phone` TEXT NOT NULL, "
                + "`birth_day` INTEGER, "
                + "`birth_month` INTEGER, "
                + "`birth_year` INTEGER, "
                + "`notes` TEXT NOT NULL, "
                + "`no_show_count` INTEGER NOT NULL, "
                + "`active` INTEGER NOT NULL, "
                + "`created_at` INTEGER NOT NULL, "
                + "`updated_at` INTEGER NOT NULL, "
                + "PRIMARY KEY(`id`))");

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_clients_name_normalized` "
                + "ON `clients` (`name_normalized`)");

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_clients_phone` "
                + "ON `clients` (`phone`)");
    }
}
