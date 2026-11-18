package com.metanoia.app.data.repository;

import androidx.annotation.NonNull;

import com.metanoia.app.data.db.AppDatabase;
import com.metanoia.app.data.db.dao.StockDao;
import com.metanoia.app.data.db.entity.ProductEntity;
import com.metanoia.app.data.db.entity.StockMovementEntity;
import com.metanoia.app.data.mapper.StockMapper;
import com.metanoia.domain.model.Product;
import com.metanoia.domain.model.StockMovement;
import com.metanoia.domain.repository.StockRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Implementación Room de {@link StockRepository} con soporte transaccional compartido.
 */
public final class StockRepositoryImpl implements StockRepository {

    private final AppDatabase database;
    private final StockDao stockDao;

    public StockRepositoryImpl(@NonNull AppDatabase database) {
        this.database = Objects.requireNonNull(database, "database");
        this.stockDao = database.stockDao();
    }

    @Override
    public Optional<Product> findProductById(String id) {
        ProductEntity entity = stockDao.findProductByIdSync(id);
        return entity == null ? Optional.empty() : Optional.of(StockMapper.toDomain(entity));
    }

    @Override
    public List<Product> findAllProducts(boolean includeInactive) {
        return mapProducts(stockDao.findAllProductsSync(includeInactive));
    }

    @Override
    public List<Product> findActiveSaleProducts() {
        return mapProducts(stockDao.findActiveSaleProductsSync());
    }

    @Override
    public List<StockMovement> findMovementsByProductId(String productId) {
        List<StockMovementEntity> entities = stockDao.findMovementsByProductIdSync(productId);
        List<StockMovement> list = new ArrayList<>(entities.size());
        for (StockMovementEntity entity : entities) {
            list.add(StockMapper.toDomain(entity));
        }
        return list;
    }

    @Override
    public int sumMovementsByProductId(String productId) {
        return stockDao.sumMovementsByProductIdSync(productId);
    }

    @Override
    public void saveProduct(Product product) {
        stockDao.upsertProduct(StockMapper.toEntity(product));
    }

    @Override
    public void saveMovement(StockMovement movement) {
        stockDao.insertMovement(StockMapper.toEntity(movement));
    }

    @Override
    public void recordMovementAndUpdateProduct(StockMovement movement, Product updatedProduct) {
        stockDao.insertMovementAndUpdateProduct(
                StockMapper.toEntity(movement),
                StockMapper.toEntity(updatedProduct)
        );
    }

    @Override
    public void runInTransaction(Runnable action) {
        database.runInTransaction(action);
    }

    private static List<Product> mapProducts(List<ProductEntity> entities) {
        List<Product> list = new ArrayList<>(entities.size());
        for (ProductEntity entity : entities) {
            list.add(StockMapper.toDomain(entity));
        }
        return list;
    }
}
