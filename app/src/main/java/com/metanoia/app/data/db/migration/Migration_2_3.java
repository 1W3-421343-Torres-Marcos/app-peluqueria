package com.metanoia.app.data.db.migration;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Crea la tabla {@code salon_services} y su índice de categoría/estado/orden.
 */
public final class Migration_2_3 extends Migration {

    public static final Migration_2_3 INSTANCE = new Migration_2_3();

    private Migration_2_3() {
        super(2, 3);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `salon_services` ("
                + "`id` TEXT NOT NULL, "
                + "`name` TEXT NOT NULL, "
                + "`name_normalized` TEXT NOT NULL, "
                + "`category` TEXT NOT NULL, "
                + "`category_normalized` TEXT NOT NULL, "
                + "`price_cents` INTEGER NOT NULL, "
                + "`duration_min` INTEGER NOT NULL, "
                + "`is_color_service` INTEGER NOT NULL, "
                + "`active` INTEGER NOT NULL, "
                + "`sort_order` INTEGER NOT NULL, "
                + "`created_at` INTEGER NOT NULL, "
                + "`updated_at` INTEGER NOT NULL, "
                + "PRIMARY KEY(`id`))");

        db.execSQL("CREATE INDEX IF NOT EXISTS "
                + "`index_salon_services_category_normalized_active_sort_order` "
                + "ON `salon_services` (`category_normalized`, `active`, `sort_order`)");
    }
}
