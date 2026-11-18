package com.metanoia.domain.usecase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.AuditRecord;
import com.metanoia.domain.model.CashClosing;
import com.metanoia.domain.model.CashMovement;
import com.metanoia.domain.model.CashMovementType;
import com.metanoia.domain.model.Money;
import com.metanoia.domain.model.PaymentMethod;
import com.metanoia.domain.model.Product;
import com.metanoia.domain.model.ProductType;
import com.metanoia.domain.model.ProductUnit;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.model.StockMovement;
import com.metanoia.domain.model.StockMovementType;
import com.metanoia.domain.repository.CashRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.repository.StockRepository;
import com.metanoia.domain.rules.LowStockRule;
import com.metanoia.domain.usecase.SellProductUseCase.Outcome;
import com.metanoia.domain.usecase.SellProductUseCase.Request;

import org.junit.Test;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SellProductUseCaseTest {

    @Test
    public void saleOfSaleAndBothProducts_recordsCashAndStockInSameTransaction() {
        TransactionalStore store = new TransactionalStore();
        seedProduct(store, "p-sale", "Shampoo Brillo", ProductType.SALE, 1_200_000L, 5);
        SellProductUseCase useCase = buildUseCase(store);

        Result<Outcome> result = useCase.execute(new Request(
                "p-sale", 2, PaymentMethod.MERCADO_PAGO, null, null));

        assertTrue(result.isSuccess());
        Outcome out = result.getValue();
        assertEquals(CashMovementType.PRODUCT_SALE, out.cashMovement().type());
        assertEquals(2_400_000L, out.cashMovement().amount().cents());
        assertEquals("p-sale", out.cashMovement().productId().orElse(""));
        assertEquals(Integer.valueOf(2), out.cashMovement().quantity().orElse(0));

        assertEquals(StockMovementType.OUT_SALE, out.stockMovement().type());
        assertEquals(-2, out.stockMovement().quantity());
        assertEquals(out.cashMovement().id(), out.stockMovement().cashMovementId().orElse(""));
        assertEquals(3, out.updatedProduct().currentStock());
        assertFalse(out.negativeStockWarning());

        assertEquals(1, store.cashMovements.size());
        assertEquals(2, store.stockMovements.size());
        assertEquals(3, store.sumMovementsByProductId("p-sale"));
        assertEquals("CASH_PRODUCT_SALE", store.audits.get(0).action());
    }

    @Test
    public void useOnlyProductAndClosedDay_areRejectedWithoutWriting() {
        TransactionalStore store = new TransactionalStore();
        seedProduct(store, "p-use", "Tintura 7.1", ProductType.USE, null, 5);
        seedProduct(store, "p-both", "Ampolla", ProductType.BOTH, 900_000L, 4);
        SellProductUseCase useCase = buildUseCase(store);

        Result<Outcome> useRes = useCase.execute(new Request(
                "p-use", 1, PaymentMethod.CASH, Money.ofCents(500_000L), null));
        assertFalse(useRes.isSuccess());
        assertEquals(DomainError.VALIDATION_ERROR, useRes.getError());
        assertTrue(store.cashMovements.isEmpty());

        store.closedDays.put(20_500L, Boolean.TRUE);
        Result<Outcome> closedRes = useCase.execute(new Request(
                "p-both", 1, PaymentMethod.CASH, null, null));
        assertFalse(closedRes.isSuccess());
        assertEquals(DomainError.CONFLICT, closedRes.getError());
        assertTrue(store.cashMovements.isEmpty());
    }

    @Test
    public void failedStockPersistence_rollsBackCashAndAuditAtomically() {
        TransactionalStore store = new TransactionalStore();
        seedProduct(store, "p-sale", "Sérum Capilar", ProductType.SALE, 1_500_000L, 3);
        store.failOnStockRecord = true;
        SellProductUseCase useCase = buildUseCase(store);

        try {
            useCase.execute(new Request("p-sale", 2, PaymentMethod.CASH, null, null));
            fail("Se esperaba excepción simulada de persistencia de stock");
        } catch (IllegalStateException expected) {
            // Rollback transaccional esperado
        }

        assertTrue(store.cashMovements.isEmpty());
        assertTrue(store.audits.isEmpty());
        assertEquals(3, store.products.get("p-sale").currentStock());
        assertEquals(1, store.stockMovements.size());
    }

    @Test
    public void saleLeadingToNegativeStock_succeedsWithWarningFlag() {
        TransactionalStore store = new TransactionalStore();
        seedProduct(store, "p-sale", "Aceite Argán", ProductType.BOTH, 1_100_000L, 1);
        SellProductUseCase useCase = buildUseCase(store);

        Result<Outcome> result = useCase.execute(new Request(
                "p-sale", 3, PaymentMethod.TRANSFER, null, null));

        assertTrue(result.isSuccess());
        assertTrue(result.getValue().negativeStockWarning());
        assertEquals(-2, result.getValue().updatedProduct().currentStock());
    }

    private static void seedProduct(
            TransactionalStore store,
            String id,
            String name,
            ProductType type,
            Long salePriceCents,
            int initialStock
    ) {
        Product product = new Product.Builder()
                .id(id)
                .name(name)
                .type(type)
                .unit(ProductUnit.UNIT)
                .salePrice(salePriceCents == null ? null : Money.ofCents(salePriceCents))
                .minStock(1)
                .currentStock(initialStock)
                .active(true)
                .createdAt(1_000L)
                .updatedAt(1_000L)
                .build();
        store.products.put(id, product);
        if (initialStock > 0) {
            store.stockMovements.add(new StockMovement.Builder()
                    .id("init-" + id)
                    .productId(id)
                    .timestamp(1_000L)
                    .type(StockMovementType.IN)
                    .quantity(initialStock)
                    .reason("Stock inicial")
                    .operator("Dueño")
                    .build());
        }
    }

    private static SellProductUseCase buildUseCase(TransactionalStore store) {
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
                return 30_000L;
            }

            @Override
            public long todayEpochDay() {
                return 20_500L;
            }

            @Override
            public int currentMinuteOfDay() {
                return 800;
            }

            @Override
            public ZoneId zoneId() {
                return ARGENTINA_ZONE;
            }
        };
        int[] seq = {0};
        IdGenerator ids = () -> "tx-" + (++seq[0]);
        return new SellProductUseCase(
                store, store, settingsRepo, store.audits::add, clock, ids);
    }

    private static final class TransactionalStore implements StockRepository, CashRepository {
        Map<String, Product> products = new LinkedHashMap<>();
        List<StockMovement> stockMovements = new ArrayList<>();
        Map<String, CashMovement> cashMovements = new LinkedHashMap<>();
        Map<Long, Boolean> closedDays = new LinkedHashMap<>();
        List<AuditRecord> audits = new ArrayList<>();
        boolean failOnStockRecord;

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
            return Collections.emptyList();
        }

        @Override
        public List<StockMovement> findMovementsByProductId(String productId) {
            List<StockMovement> list = new ArrayList<>();
            for (StockMovement m : stockMovements) {
                if (productId.equals(m.productId())) {
                    list.add(m);
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
            stockMovements.add(movement);
        }

        @Override
        public void recordMovementAndUpdateProduct(StockMovement movement, Product updatedProduct) {
            if (failOnStockRecord) {
                throw new IllegalStateException("Fallo simulado de stock");
            }
            stockMovements.add(movement);
            products.put(updatedProduct.id(), updatedProduct);
        }

        @Override
        public Optional<CashMovement> findMovementById(String id) {
            return Optional.ofNullable(cashMovements.get(id));
        }

        @Override
        public List<CashMovement> findMovementsByDay(long dayEpoch) {
            return new ArrayList<>(cashMovements.values());
        }

        @Override
        public List<CashMovement> findMovementsByAppointmentId(String appointmentId) {
            return Collections.emptyList();
        }

        @Override
        public List<CashMovement> findMovementsByClientId(String clientId) {
            return Collections.emptyList();
        }

        @Override
        public boolean existsReversalForMovement(String movementId) {
            return false;
        }

        @Override
        public boolean isDayClosed(long dayEpoch) {
            return Boolean.TRUE.equals(closedDays.get(dayEpoch));
        }

        @Override
        public Optional<CashClosing> findClosingByDay(long dayEpoch) {
            return Optional.empty();
        }

        @Override
        public Optional<Long> findLatestUnclosedDayWithMovementsBefore(long dayEpoch) {
            return Optional.empty();
        }

        @Override
        public void saveMovement(CashMovement movement) {
            cashMovements.put(movement.id(), movement);
        }

        @Override
        public void saveMovements(List<CashMovement> movements) {
            for (CashMovement m : movements) {
                saveMovement(m);
            }
        }

        @Override
        public void saveClosingAndLockDay(CashClosing closing) {
            closedDays.put(closing.day(), Boolean.TRUE);
        }

        @Override
        public void runInTransaction(Runnable action) {
            Map<String, Product> snapProducts = new LinkedHashMap<>(products);
            List<StockMovement> snapStock = new ArrayList<>(stockMovements);
            Map<String, CashMovement> snapCash = new LinkedHashMap<>(cashMovements);
            List<AuditRecord> snapAudits = new ArrayList<>(audits);
            try {
                action.run();
            } catch (RuntimeException ex) {
                products = snapProducts;
                stockMovements = snapStock;
                cashMovements = snapCash;
                audits.clear();
                audits.addAll(snapAudits);
                throw ex;
            }
        }
    }
}
