package com.metanoia.app.data.db.migration;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Crea las tablas {@code products} y {@code stock_movements} con FK RESTRICT e índices.
 */
public final class Migration_11_12 extends Migration {

    public static final Migration_11_12 INSTANCE = new Migration_11_12();

    private Migration_11_12() {
        super(11, 12);
    }

    @Override
    public void migrate(@NonNull SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `products` ("
                + "`id` TEXT NOT NULL, "
                + "`name` TEXT NOT NULL, "
                + "`name_normalized` TEXT NOT NULL, "
                + "`brand` TEXT, "
                + "`type` TEXT NOT NULL, "
                + "`unit` TEXT NOT NULL, "
                + "`sale_price_cents` INTEGER, "
                + "`min_stock` INTEGER NOT NULL, "
                + "`current_stock` INTEGER NOT NULL, "
                + "`active` INTEGER NOT NULL, "
                + "`created_at` INTEGER NOT NULL, "
                + "`updated_at` INTEGER NOT NULL, "
                + "PRIMARY KEY(`id`))");

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_active_type_name_normalized` "
                + "ON `products` (`active`, `type`, `name_normalized`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_active_current_stock_min_stock` "
                + "ON `products` (`active`, `current_stock`, `min_stock`)");

        db.execSQL("CREATE TABLE IF NOT EXISTS `stock_movements` ("
                + "`id` TEXT NOT NULL, "
                + "`product_id` TEXT NOT NULL, "
                + "`timestamp` INTEGER NOT NULL, "
                + "`type` TEXT NOT NULL, "
                + "`quantity` INTEGER NOT NULL, "
                + "`reason` TEXT, "
                + "`cash_movement_id` TEXT, "
                + "`operator` TEXT NOT NULL, "
                + "PRIMARY KEY(`id`), "
                + "FOREIGN KEY(`product_id`) REFERENCES `products`(`id`) "
                + "ON UPDATE NO ACTION ON DELETE RESTRICT , "
                + "FOREIGN KEY(`cash_movement_id`) REFERENCES `cash_movements`(`id`) "
                + "ON UPDATE NO ACTION ON DELETE RESTRICT )");

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_movements_product_id_timestamp` "
                + "ON `stock_movements` (`product_id`, `timestamp`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_movements_cash_movement_id` "
                + "ON `stock_movements` (`cash_movement_id`)");
    }
}
