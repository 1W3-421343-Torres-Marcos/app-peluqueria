package com.metanoia.domain.usecase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.AuditRecord;
import com.metanoia.domain.model.Money;
import com.metanoia.domain.model.Product;
import com.metanoia.domain.model.ProductType;
import com.metanoia.domain.model.ProductUnit;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.model.StockMovement;
import com.metanoia.domain.model.StockMovementType;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.repository.StockRepository;
import com.metanoia.domain.rules.LowStockRule;

import org.junit.Test;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SaveProductUseCaseTest {

    @Test
    public void createUseProduct_clearsPriceAndRecordsInitialMovementWhenStockPositive() {
        InMemoryStockRepo stockRepo = new InMemoryStockRepo();
        List<AuditRecord> audits = new ArrayList<>();
        SaveProductUseCase saveUseCase = buildSaveUseCase(stockRepo, audits);

        Result<Product> created = saveUseCase.execute(new SaveProductUseCase.Request(
                null,
                "Tintura 7.1",
                "L'Oréal",
                ProductType.USE,
                ProductUnit.GRAM,
                Money.ofCents(1_200_000L),
                3,
                5
        ));

        assertTrue(created.isSuccess());
        Product product = created.getValue();
        assertFalse(product.salePrice().isPresent());
        assertEquals(5, product.currentStock());
        assertEquals(5, stockRepo.sumMovementsByProductId(product.id()));
        List<StockMovement> movements = stockRepo.findMovementsByProductId(product.id());
        assertEquals(1, movements.size());
        assertEquals(StockMovementType.IN, movements.get(0).type());
        assertEquals(5, movements.get(0).quantity());
        assertEquals(1, audits.size());
        assertEquals("PRODUCT_CREATE", audits.get(0).action());
    }

    @Test
    public void createSaleOrBothProduct_requiresPositiveSalePrice() {
        InMemoryStockRepo stockRepo = new InMemoryStockRepo();
        SaveProductUseCase saveUseCase = buildSaveUseCase(stockRepo, new ArrayList<>());

        Result<Product> missingPrice = saveUseCase.execute(new SaveProductUseCase.Request(
                null,
                "Shampoo Matizador",
                "Kérastase",
                ProductType.SALE,
                ProductUnit.MILLILITER,
                null,
                2,
                0
        ));
        assertFalse(missingPrice.isSuccess());
        assertEquals(DomainError.VALIDATION_ERROR, missingPrice.getError());

        Result<Product> zeroPrice = saveUseCase.execute(new SaveProductUseCase.Request(
                null,
                "Ampolla Nutritiva",
                null,
                ProductType.BOTH,
                ProductUnit.UNIT,
                Money.zero(),
                2,
                0
        ));
        assertFalse(zeroPrice.isSuccess());
        assertEquals(DomainError.VALIDATION_ERROR, zeroPrice.getError());

        Result<Product> validBoth = saveUseCase.execute(new SaveProductUseCase.Request(
                null,
                "Ampolla Nutritiva",
                null,
                ProductType.BOTH,
                ProductUnit.UNIT,
                Money.ofCents(850_000L),
                2,
                0
        ));
        assertTrue(validBoth.isSuccess());
        assertTrue(validBoth.getValue().salePrice().isPresent());
        assertEquals(850_000L, validBoth.getValue().salePrice().get().cents());
    }

    @Test
    public void editAndDeactivateProduct_preserveCurrentStockAndMovementHistory() {
        InMemoryStockRepo stockRepo = new InMemoryStockRepo();
        List<AuditRecord> audits = new ArrayList<>();
        SaveProductUseCase saveUseCase = buildSaveUseCase(stockRepo, audits);
        SetProductActiveUseCase activeUseCase = buildActiveUseCase(stockRepo, audits);

        Result<Product> created = saveUseCase.execute(new SaveProductUseCase.Request(
                null,
                "Mascarilla Reparadora",
                "Alfaparf",
                ProductType.BOTH,
                ProductUnit.UNIT,
                Money.ofCents(1_500_000L),
                2,
                4
        ));
        assertTrue(created.isSuccess());
        String productId = created.getValue().id();

        Result<Product> updated = saveUseCase.execute(new SaveProductUseCase.Request(
                productId,
                "Mascarilla Reparadora Intensiva",
                "Alfaparf",
                ProductType.SALE,
                ProductUnit.UNIT,
                Money.ofCents(1_800_000L),
                3,
                99
        ));
        assertTrue(updated.isSuccess());
        assertEquals(4, updated.getValue().currentStock());
        assertEquals("Mascarilla Reparadora Intensiva", updated.getValue().name());
        assertEquals(3, updated.getValue().minStock());

        Result<Product> deactivated = activeUseCase.execute(productId, false);
        assertTrue(deactivated.isSuccess());
        assertFalse(deactivated.getValue().active());
        assertEquals(1, stockRepo.findMovementsByProductId(productId).size());
        assertEquals(4, stockRepo.sumMovementsByProductId(productId));
        assertTrue(stockRepo.findActiveSaleProducts().isEmpty());
        assertEquals("PRODUCT_DEACTIVATE", audits.get(audits.size() - 1).action());
    }

    private static SaveProductUseCase buildSaveUseCase(
            InMemoryStockRepo stockRepo,
            List<AuditRecord> audits
    ) {
        return new SaveProductUseCase(
                stockRepo,
                settingsRepo(),
                audits::add,
                fixedClock(),
                seqIds()
        );
    }

    private static SetProductActiveUseCase buildActiveUseCase(
            InMemoryStockRepo stockRepo,
            List<AuditRecord> audits
    ) {
        return new SetProductActiveUseCase(
                stockRepo,
                settingsRepo(),
                audits::add,
                fixedClock(),
                seqIds()
        );
    }

    private static SettingsRepository settingsRepo() {
        return new SettingsRepository() {
            @Override
            public Settings getSettings() {
                return Settings.defaultSettings();
            }

            @Override
            public void saveSettings(Settings settings) {
            }
        };
    }

    private static Clock fixedClock() {
        return new Clock() {
            @Override
            public long nowMillis() {
                return 10_000L;
            }

            @Override
            public long todayEpochDay() {
                return 20_500L;
            }

            @Override
            public int currentMinuteOfDay() {
                return 600;
            }

            @Override
            public ZoneId zoneId() {
                return ARGENTINA_ZONE;
            }
        };
    }

    private static IdGenerator seqIds() {
        int[] seq = {0};
        return () -> "stk-" + (++seq[0]);
    }

    private static final class InMemoryStockRepo implements StockRepository {
        final Map<String, Product> products = new LinkedHashMap<>();
        final List<StockMovement> movements = new ArrayList<>();

        @Override
        public Optional<Product> findProductById(String id) {
            return Optional.ofNullable(products.get(id));
        }

        @Override
        public List<Product> findAllProducts(boolean includeInactive) {
            List<Product> result = new ArrayList<>();
            for (Product product : products.values()) {
                if (includeInactive || product.active()) {
                    result.add(product);
                }
            }
            return LowStockRule.sortWithLowStockFirst(result);
        }

        @Override
        public List<Product> findActiveSaleProducts() {
            List<Product> result = new ArrayList<>();
            for (Product product : products.values()) {
                if (product.active() && product.type().isForSale()) {
                    result.add(product);
                }
            }
            return result;
        }

        @Override
        public List<StockMovement> findMovementsByProductId(String productId) {
            List<StockMovement> result = new ArrayList<>();
            for (StockMovement movement : movements) {
                if (productId.equals(movement.productId())) {
                    result.add(movement);
                }
            }
            return result;
        }

        @Override
        public int sumMovementsByProductId(String productId) {
            return LowStockRule.calculateStock(findMovementsByProductId(productId));
        }

        @Override
        public void saveProduct(Product product) {
            products.put(product.id(), product);
        }

        @Override
        public void saveMovement(StockMovement movement) {
            movements.add(movement);
        }

        @Override
        public void recordMovementAndUpdateProduct(StockMovement movement, Product updatedProduct) {
            movements.add(movement);
            products.put(updatedProduct.id(), updatedProduct);
        }

        @Override
        public void runInTransaction(Runnable action) {
            action.run();
        }
    }
}
