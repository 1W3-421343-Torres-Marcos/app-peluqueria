package com.metanoia.app.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.metanoia.app.data.db.entity.ProductEntity;
import com.metanoia.app.data.db.entity.StockMovementEntity;

import java.util.List;

/**
 * DAO para catálogo de productos, conteo de alertas y movimientos inmutables de stock.
 */
@Dao
public interface StockDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertProductIgnore(ProductEntity entity);

    @Update
    void updateProduct(ProductEntity entity);

    @Transaction
    default void upsertProduct(ProductEntity entity) {
        if (insertProductIgnore(entity) == -1L) {
            updateProduct(entity);
        }
    }

    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insertMovement(StockMovementEntity entity);

    @Transaction
    default void insertMovementAndUpdateProduct(
            StockMovementEntity movement,
            ProductEntity product
    ) {
        insertMovement(movement);
        upsertProduct(product);
    }

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    ProductEntity findProductByIdSync(String id);

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    LiveData<ProductEntity> observeProductById(String id);

    @Query("SELECT * FROM products "
            + "WHERE (:includeInactive = 1 OR active = 1) "
            + "ORDER BY active DESC, "
            + "CASE WHEN active = 1 AND current_stock <= min_stock THEN 0 ELSE 1 END ASC, "
            + "name_normalized ASC")
    List<ProductEntity> findAllProductsSync(boolean includeInactive);

    @Query("SELECT * FROM products "
            + "WHERE (:includeInactive = 1 OR active = 1) "
            + "ORDER BY active DESC, "
            + "CASE WHEN active = 1 AND current_stock <= min_stock THEN 0 ELSE 1 END ASC, "
            + "name_normalized ASC")
    LiveData<List<ProductEntity>> observeProducts(boolean includeInactive);

    @Query("SELECT * FROM products "
            + "WHERE active = 1 AND type IN ('SALE', 'BOTH') "
            + "ORDER BY name_normalized ASC")
    List<ProductEntity> findActiveSaleProductsSync();

    @Query("SELECT * FROM products "
            + "WHERE active = 1 AND type IN ('SALE', 'BOTH') "
            + "ORDER BY name_normalized ASC")
    LiveData<List<ProductEntity>> observeActiveSaleProducts();

    @Query("SELECT COUNT(*) FROM products WHERE active = 1 AND current_stock <= min_stock")
    LiveData<Integer> observeLowStockCount();

    @Query("SELECT * FROM stock_movements WHERE product_id = :productId ORDER BY timestamp DESC")
    List<StockMovementEntity> findMovementsByProductIdSync(String productId);

    @Query("SELECT * FROM stock_movements WHERE product_id = :productId ORDER BY timestamp DESC")
    LiveData<List<StockMovementEntity>> observeMovementsByProductId(String productId);

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM stock_movements WHERE product_id = :productId")
    int sumMovementsByProductIdSync(String productId);
}
