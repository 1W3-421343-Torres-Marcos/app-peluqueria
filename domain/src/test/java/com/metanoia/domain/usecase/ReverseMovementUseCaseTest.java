package com.metanoia.domain.usecase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.DomainError;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.common.Result;
import com.metanoia.domain.model.Appointment;
import com.metanoia.domain.model.AppointmentItem;
import com.metanoia.domain.model.AppointmentStatus;
import com.metanoia.domain.model.AuditRecord;
import com.metanoia.domain.model.CashClosing;
import com.metanoia.domain.model.CashMovement;
import com.metanoia.domain.model.CashMovementType;
import com.metanoia.domain.model.Money;
import com.metanoia.domain.model.PaymentMethod;
import com.metanoia.domain.model.Product;
import com.metanoia.domain.model.ProductType;
import com.metanoia.domain.model.ProductUnit;
import com.metanoia.domain.model.RecurrenceRule;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.model.StockMovement;
import com.metanoia.domain.model.StockMovementType;
import com.metanoia.domain.repository.AppointmentRepository;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.CashRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.repository.StockRepository;
import com.metanoia.domain.rules.LowStockRule;

import org.junit.Test;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ReverseMovementUseCaseTest {

    @Test
    public void reversalOfPaidAppointment_revertsStatusToCompletedAndLogsAudit() {
        InMemoryCashRepo cashRepo = new InMemoryCashRepo();
        InMemoryAptRepo aptRepo = new InMemoryAptRepo();
        List<AuditRecord> audits = new ArrayList<>();
        ReverseMovementUseCase useCase = buildUseCase(cashRepo, aptRepo, audits, 20_500L);

        aptRepo.save(paidAppointment("apt-1", 15_000L));
        cashRepo.saveMovement(payment("mov-1", 20_500L, "apt-1", 15_000L));

        Result<CashMovement> result = useCase.execute("mov-1", "Error de método");

        assertTrue(result.isSuccess());
        assertEquals(CashMovementType.REVERSAL, result.getValue().type());
        assertEquals("mov-1", result.getValue().reversesMovementId().orElse(""));
        assertEquals(AppointmentStatus.COMPLETED, aptRepo.findById("apt-1").get().status());
        assertEquals(1, audits.size());
        assertEquals("CASH_REVERSAL", audits.get(0).action());
    }

    @Test
    public void preventsDoubleReversalAndReversingAReversal() {
        InMemoryCashRepo cashRepo = new InMemoryCashRepo();
        InMemoryAptRepo aptRepo = new InMemoryAptRepo();
        ReverseMovementUseCase useCase = buildUseCase(cashRepo, aptRepo, new ArrayList<>(), 20_500L);

        aptRepo.save(paidAppointment("apt-1", 15_000L));
        cashRepo.saveMovement(payment("mov-1", 20_500L, "apt-1", 15_000L));

        Result<CashMovement> first = useCase.execute("mov-1", "Corrección");
        assertTrue(first.isSuccess());

        Result<CashMovement> duplicate = useCase.execute("mov-1", "Otra vez");
        assertFalse(duplicate.isSuccess());
        assertEquals(DomainError.CONFLICT, duplicate.getError());

        Result<CashMovement> reverseOfReversal = useCase.execute(
                first.getValue().id(), "Revertir reverso");
        assertFalse(reverseOfReversal.isSuccess());
        assertEquals(DomainError.VALIDATION_ERROR, reverseOfReversal.getError());
    }

    @Test
    public void reversalOnClosedOriginalDay_recordsReversalOnCurrentOpenDay() {
        InMemoryCashRepo cashRepo = new InMemoryCashRepo();
        InMemoryAptRepo aptRepo = new InMemoryAptRepo();
        ReverseMovementUseCase useCase = buildUseCase(cashRepo, aptRepo, new ArrayList<>(), 20_501L);

        aptRepo.save(paidAppointment("apt-1", 10_000L));
        cashRepo.saveMovement(payment("mov-1", 20_500L, "apt-1", 10_000L));
        cashRepo.closedDays.put(20_500L, Boolean.TRUE);

        Result<CashMovement> result = useCase.execute("mov-1", "Anulación posterior al cierre");

        assertTrue(result.isSuccess());
        assertEquals(20_501L, result.getValue().day());
        assertEquals(AppointmentStatus.COMPLETED, aptRepo.findById("apt-1").get().status());
    }

    @Test
    public void reversalOfProductSale_restoresStockAtomically() {
        InMemoryCashRepo cashRepo = new InMemoryCashRepo();
        InMemoryAptRepo aptRepo = new InMemoryAptRepo();
        InMemoryStockRepo stockRepo = new InMemoryStockRepo();
        stockRepo.saveProduct(new Product.Builder()
                .id("prod-1")
                .name("Shampoo")
                .type(ProductType.SALE)
                .unit(ProductUnit.UNIT)
                .salePrice(Money.ofCents(1_000_000L))
                .minStock(1)
                .currentStock(3)
                .active(true)
                .createdAt(1_000L)
                .updatedAt(1_000L)
                .build());
        stockRepo.saveMovement(new StockMovement.Builder()
                .id("sm-init")
                .productId("prod-1")
                .timestamp(1_000L)
                .type(StockMovementType.IN)
                .quantity(5)
                .operator("Dueño")
                .build());
        stockRepo.saveMovement(new StockMovement.Builder()
                .id("sm-sale")
                .productId("prod-1")
                .timestamp(2_000L)
                .type(StockMovementType.OUT_SALE)
                .quantity(-2)
                .cashMovementId("mov-sale")
                .operator("Dueño")
                .build());
        cashRepo.saveMovement(new CashMovement.Builder()
                .id("mov-sale")
                .day(20_500L)
                .timestamp(2_000L)
                .type(CashMovementType.PRODUCT_SALE)
                .method(PaymentMethod.CASH)
                .amount(Money.ofCents(2_000_000L))
                .concept("Venta: Shampoo x2")
                .productId("prod-1")
                .quantity(2)
                .operator("Dueño")
                .build());

        ReverseMovementUseCase useCase = buildUseCaseWithStock(
                cashRepo, aptRepo, stockRepo, new ArrayList<>(), 20_500L);
        Result<CashMovement> reversal = useCase.execute("mov-sale", "Cliente cambió de producto");

        assertTrue(reversal.isSuccess());
        assertEquals(5, stockRepo.findProductById("prod-1").get().currentStock());
        assertEquals(5, stockRepo.sumMovementsByProductId("prod-1"));
        assertEquals(3, stockRepo.movements.size());
    }

    private static ReverseMovementUseCase buildUseCase(
            InMemoryCashRepo cashRepo,
            InMemoryAptRepo aptRepo,
            List<AuditRecord> audits,
            long todayEpoch
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
        AuditLogRepository auditRepo = audits::add;
        Clock fixedClock = new Clock() {
            @Override
            public long nowMillis() {
                return 5_000L;
            }

            @Override
            public long todayEpochDay() {
                return todayEpoch;
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
        int[] seq = {0};
        IdGenerator ids = () -> "gen-" + (++seq[0]);
        return new ReverseMovementUseCase(
                cashRepo, aptRepo, settingsRepo, auditRepo, fixedClock, ids);
    }

    private static ReverseMovementUseCase buildUseCaseWithStock(
            InMemoryCashRepo cashRepo,
            InMemoryAptRepo aptRepo,
            StockRepository stockRepo,
            List<AuditRecord> audits,
            long todayEpoch
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
        AuditLogRepository auditRepo = audits::add;
        Clock fixedClock = new Clock() {
            @Override
            public long nowMillis() {
                return 5_000L;
            }

            @Override
            public long todayEpochDay() {
                return todayEpoch;
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
        int[] seq = {0};
        IdGenerator ids = () -> "gen-" + (++seq[0]);
        return new ReverseMovementUseCase(
                cashRepo, aptRepo, stockRepo, settingsRepo, auditRepo, fixedClock, ids);
    }

    private static Appointment paidAppointment(String id, long cents) {
        AppointmentItem item = new AppointmentItem.Builder()
                .id("item-" + id)
                .appointmentId(id)
                .serviceId("svc-1")
                .serviceNameSnapshot("Corte")
                .priceSnapshot(Money.ofCents(cents))
                .durationMinSnapshot(30)
                .position(0)
                .build();
        return new Appointment.Builder()
                .id(id)
                .clientId("cli-1")
                .professionalId("pro-1")
                .day(20_500L)
                .startMin(600)
                .endMin(630)
                .status(AppointmentStatus.PAID)
                .items(Collections.singletonList(item))
                .createdAt(1_000L)
                .updatedAt(1_000L)
                .build();
    }

    private static CashMovement payment(String id, long day, String aptId, long cents) {
        return new CashMovement.Builder()
                .id(id)
                .day(day)
                .timestamp(2_000L)
                .type(CashMovementType.APPOINTMENT_PAYMENT)
                .method(PaymentMethod.CASH)
                .amount(Money.ofCents(cents))
                .concept("Cobro turno")
                .appointmentId(aptId)
                .clientId("cli-1")
                .operator("Dueño")
                .build();
    }

    private static final class InMemoryCashRepo implements CashRepository {
        final Map<String, CashMovement> movements = new LinkedHashMap<>();
        final Map<Long, Boolean> closedDays = new LinkedHashMap<>();

        @Override
        public Optional<CashMovement> findMovementById(String id) {
            return Optional.ofNullable(movements.get(id));
        }

        @Override
        public List<CashMovement> findMovementsByDay(long dayEpoch) {
            List<CashMovement> list = new ArrayList<>();
            for (CashMovement m : movements.values()) {
                if (m.day() == dayEpoch) {
                    list.add(m);
                }
            }
            return list;
        }

        @Override
        public List<CashMovement> findMovementsByAppointmentId(String appointmentId) {
            List<CashMovement> list = new ArrayList<>();
            for (CashMovement m : movements.values()) {
                if (appointmentId.equals(m.appointmentId().orElse(null))) {
                    list.add(m);
                }
            }
            return list;
        }

        @Override
        public List<CashMovement> findMovementsByClientId(String clientId) {
            return Collections.emptyList();
        }

        @Override
        public boolean existsReversalForMovement(String movementId) {
            for (CashMovement m : movements.values()) {
                if (movementId.equals(m.reversesMovementId().orElse(null))) {
                    return true;
                }
            }
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
            movements.put(movement.id(), movement);
        }

        @Override
        public void saveMovements(List<CashMovement> list) {
            for (CashMovement m : list) {
                movements.put(m.id(), m);
            }
        }

        @Override
        public void saveClosingAndLockDay(CashClosing closing) {
            closedDays.put(closing.day(), Boolean.TRUE);
        }

        @Override
        public void runInTransaction(Runnable action) {
            action.run();
        }
    }

    private static final class InMemoryAptRepo implements AppointmentRepository {
        final Map<String, Appointment> map = new LinkedHashMap<>();

        @Override
        public Optional<Appointment> findById(String id) {
            return Optional.ofNullable(map.get(id));
        }

        @Override
        public List<Appointment> findByDayAndProfessional(long dayEpoch, String professionalId) {
            return Collections.emptyList();
        }

        @Override
        public List<Appointment> findByDayRange(long startDayEpoch, long endDayEpoch) {
            return Collections.emptyList();
        }

        @Override
        public List<Appointment> findFutureActiveFromSeries(
                String recurrenceRuleId, long fromDayEpoch, int fromStartMin) {
            return Collections.emptyList();
        }

        @Override
        public boolean hasFutureActiveForProfessional(String professionalId, long fromDayEpoch) {
            return false;
        }

        @Override
        public void save(Appointment appointment) {
            map.put(appointment.id(), appointment);
        }

        @Override
        public void saveAll(List<Appointment> appointments) {
            for (Appointment apt : appointments) {
                save(apt);
            }
        }

        @Override
        public void saveRecurrenceRule(RecurrenceRule rule) {
        }
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
            return Collections.emptyList();
        }

        @Override
        public List<StockMovement> findMovementsByProductId(String productId) {
            List<StockMovement> list = new ArrayList<>();
            for (StockMovement m : movements) {
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
