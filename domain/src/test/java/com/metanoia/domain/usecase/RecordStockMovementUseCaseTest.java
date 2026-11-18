package com.metanoia.domain.usecase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.AuditRecord;
import com.metanoia.domain.model.Product;
import com.metanoia.domain.model.ProductType;
import com.metanoia.domain.model.ProductUnit;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.model.StockMovement;
import com.metanoia.domain.model.StockMovementType;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.repository.StockRepository;
import com.metanoia.domain.rules.LowStockRule;
import com.metanoia.domain.usecase.RecordStockMovementUseCase.Outcome;
import com.metanoia.domain.usecase.RecordStockMovementUseCase.Request;

import org.junit.Test;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RecordStockMovementUseCaseTest {

    @Test
    public void stockInOutAndAdjustmentWithReason_updatesStockFromMovementSumAndAudits() {
        InMemoryStockRepo repo = new InMemoryStockRepo();
        List<AuditRecord> audits = new ArrayList<>();
        RecordStockMovementUseCase useCase = buildUseCase(repo, audits);

        Product initial = product("p-1", 0, true);
        repo.saveProduct(initial);

        Result<Outcome> resIn = useCase.execute(Request.stockIn("p-1", 10, "Compra proveedor"));
        assertTrue(resIn.isSuccess());
        assertEquals(10, resIn.getValue().updatedProduct().currentStock());
        assertFalse(resIn.getValue().negativeStockWarning());

        Result<Outcome> resUse = useCase.execute(Request.stockOutUse("p-1", 4, "Servicio color"));
        assertTrue(resUse.isSuccess());
        assertEquals(6, resUse.getValue().updatedProduct().currentStock());

        Result<Outcome> missingReason = useCase.execute(Request.adjustment("p-1", -2, " "));
        assertFalse(missingReason.isSuccess());
        assertEquals(DomainError.VALIDATION_ERROR, missingReason.getError());

        Result<Outcome> resAdjust = useCase.execute(
                Request.adjustment("p-1", 3, "Recuento físico mensual"));
        assertTrue(resAdjust.isSuccess());
        assertEquals(9, resAdjust.getValue().updatedProduct().currentStock());
        assertEquals(9, repo.sumMovementsByProductId("p-1"));
        assertEquals(3, audits.size());
        assertEquals("STOCK_ADJUSTMENT", audits.get(2).action());
    }

    @Test
    public void outExceedingStock_allowsNegativeStockWithWarningFlag() {
        InMemoryStockRepo repo = new InMemoryStockRepo();
        RecordStockMovementUseCase useCase = buildUseCase(repo, new ArrayList<>());

        repo.saveProduct(product("p-1", 2, true));
        repo.saveMovement(new StockMovement.Builder()
                .id("seed-1")
                .productId("p-1")
                .timestamp(500L)
                .type(StockMovementType.IN)
                .quantity(2)
                .operator("Dueño")
                .build());

        Result<Outcome> result = useCase.execute(Request.stockOutUse("p-1", 5, null));
        assertTrue(result.isSuccess());
        assertTrue(result.getValue().negativeStockWarning());
        assertEquals(-3, result.getValue().updatedProduct().currentStock());
        assertTrue(result.getValue().updatedProduct().isNegativeStock());
    }

    @Test
    public void rejectsInactiveProductAndOutSaleType() {
        InMemoryStockRepo repo = new InMemoryStockRepo();
        RecordStockMovementUseCase useCase = buildUseCase(repo, new ArrayList<>());

        repo.saveProduct(product("p-inactive", 5, false));

        Result<Outcome> inactiveRes = useCase.execute(Request.stockIn("p-inactive", 3, null));
        assertFalse(inactiveRes.isSuccess());
        assertEquals(DomainError.CONFLICT, inactiveRes.getError());

        Result<Outcome> saleRes = useCase.execute(
                new Request("p-inactive", StockMovementType.OUT_SALE, -1, "Venta"));
        assertFalse(saleRes.isSuccess());
        assertEquals(DomainError.VALIDATION_ERROR, saleRes.getError());
    }

    private static RecordStockMovementUseCase buildUseCase(
            InMemoryStockRepo repo,
            List<AuditRecord> audits
    ) {
        SettingsRepository settingsRepo = new SettingsRepository() {
            @Override
            public Settings getSettings() {
                return Settings.defaultSettings();
            }

            @Override
            public void saveSettings(Settings settings) {
            }
        };
        Clock clock = new Clock() {
            @Override
            public long nowMillis() {
                return 20_000L;
            }

            @Override
            public long todayEpochDay() {
                return 20_500L;
            }

            @Override
            public int currentMinuteOfDay() {
                return 720;
            }

            @Override
            public ZoneId zoneId() {
                return ARGENTINA_ZONE;
            }
        };
        int[] seq = {0};
        IdGenerator ids = () -> "sm-" + (++seq[0]);
        return new RecordStockMovementUseCase(repo, settingsRepo, audits::add, clock, ids);
    }

    private static Product product(String id, int stock, boolean active) {
        return new Product.Builder()
                .id(id)
                .name("Oxidante 20 vol")
                .type(ProductType.USE)
                .unit(ProductUnit.MILLILITER)
                .minStock(2)
                .currentStock(stock)
                .active(active)
                .createdAt(1_000L)
                .updatedAt(1_000L)
                .build();
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
            return new ArrayList<>(products.values());
        }

        @Override
        public List<Product> findActiveSaleProducts() {
            return new ArrayList<>();
        }

        @Override
        public List<StockMovement> findMovementsByProductId(String productId) {
            List<StockMovement> list = new ArrayList<>();
            for (StockMovement movement : movements) {
                if (productId.equals(movement.productId())) {
                    list.add(movement);
                }
            }
            return list;
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
