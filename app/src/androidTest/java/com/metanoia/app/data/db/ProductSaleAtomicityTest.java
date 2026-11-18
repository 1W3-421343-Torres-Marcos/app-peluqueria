package com.metanoia.app.data.db;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.metanoia.app.data.db.entity.ProductEntity;
import com.metanoia.app.data.db.entity.StockMovementEntity;
import com.metanoia.app.data.db.migration.Migration_1_2;
import com.metanoia.app.data.repository.AuditLogRepositoryImpl;
import com.metanoia.app.data.repository.CashRepositoryImpl;
import com.metanoia.app.data.repository.SettingsRepositoryImpl;
import com.metanoia.app.data.repository.StockRepositoryImpl;
import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.model.PaymentMethod;
import com.metanoia.domain.model.Product;
import com.metanoia.domain.model.StockMovement;
import com.metanoia.domain.repository.StockRepository;
import com.metanoia.domain.usecase.SellProductUseCase;
import com.metanoia.domain.usecase.SellProductUseCase.Request;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Optional;

@RunWith(AndroidJUnit4.class)
public class ProductSaleAtomicityTest {

    private AppDatabase db;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();

        Migration_1_2.seedDefaults(db.getOpenHelper().getWritableDatabase());
        db.stockDao().upsertProduct(new ProductEntity(
                "prod-1", "Shampoo Nutritivo", "shampoo nutritivo", "Kérastase",
                "SALE", "UNIT", 1_500_000L, 2, 5, true, 1_000L, 1_000L));
        db.stockDao().insertMovement(new StockMovementEntity(
                "sm-init", "prod-1", 1_000L, "IN", 5, "Stock inicial", null, "Dueño"));
    }

    @After
    public void tearDown() {
        db.close();
    }

    @Test
    public void failedStockMovement_rollsBackCashMovementAndProductStock() {
        StockRepository realStock = new StockRepositoryImpl(db);
        StockRepository failingOnMoveStock = new StockRepository() {
            @Override
            public Optional<Product> findProductById(String id) {
                return realStock.findProductById(id);
            }

            @Override
            public List<Product> findAllProducts(boolean includeInactive) {
                return realStock.findAllProducts(includeInactive);
            }

            @Override
            public List<Product> findActiveSaleProducts() {
                return realStock.findActiveSaleProducts();
            }

            @Override
            public List<StockMovement> findMovementsByProductId(String productId) {
                return realStock.findMovementsByProductId(productId);
            }

            @Override
            public int sumMovementsByProductId(String productId) {
                return realStock.sumMovementsByProductId(productId);
            }

            @Override
            public void saveProduct(Product product) {
                realStock.saveProduct(product);
            }

            @Override
            public void saveMovement(StockMovement movement) {
                realStock.saveMovement(movement);
            }

            @Override
            public void recordMovementAndUpdateProduct(StockMovement movement, Product updatedProduct) {
                throw new IllegalStateException("Fallo simulado al descontar stock");
            }

            @Override
            public void runInTransaction(Runnable action) {
                realStock.runInTransaction(action);
            }
        };

        Clock clock = Clock.systemArgentina();
        SellProductUseCase useCase = new SellProductUseCase(
                failingOnMoveStock,
                new CashRepositoryImpl(db),
                new SettingsRepositoryImpl(db.settingsDao(), clock),
                new AuditLogRepositoryImpl(db.auditLogDao()),
                clock,
                IdGenerator.uuid()
        );

        try {
            useCase.execute(new Request("prod-1", 2, PaymentMethod.CASH, null, null));
        } catch (IllegalStateException expected) {
            // Rollback esperado en Room
        }

        assertEquals(0, db.cashDao().findByDaySync(clock.todayEpochDay()).size());
        assertEquals(5, db.stockDao().findProductByIdSync("prod-1").currentStock);
        assertEquals(1, db.stockDao().findMovementsByProductIdSync("prod-1").size());
    }
}
