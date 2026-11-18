package com.metanoia.domain.usecase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.CashRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.usecase.CloseCashDayUseCase.Request;

import org.junit.Test;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CloseCashDayUseCaseTest {

    @Test
    public void closeOpenDay_locksMovementsAndRecordsDifferenceAndAudit() {
        InMemoryCashRepo cashRepo = new InMemoryCashRepo();
        cashRepo.saveMovement(movement("m1", 20_500L, CashMovementType.APPOINTMENT_PAYMENT,
                PaymentMethod.CASH, 1_000_000L, null));
        cashRepo.saveMovement(movement("m2", 20_500L, CashMovementType.EXPENSE,
                PaymentMethod.CASH, 250_000L, null));
        List<AuditRecord> audits = new ArrayList<>();
        CloseCashDayUseCase useCase = buildUseCase(cashRepo, audits);

        Map<PaymentMethod, Money> counted = new EnumMap<>(PaymentMethod.class);
        counted.put(PaymentMethod.CASH, Money.ofCents(740_000L));
        Result<CashClosing> result = useCase.execute(new Request(20_500L, counted, "Faltan $100"));

        assertTrue(result.isSuccess());
        CashClosing closing = result.getValue();
        assertEquals(750_000L, closing.totalExpected().cents());
        assertEquals(740_000L, closing.totalCounted().cents());
        assertEquals(-10_000L, closing.difference().cents());
        assertTrue(cashRepo.isDayClosed(20_500L));
        assertEquals(closing.id(), cashRepo.findMovementById("m1").get().closingId().orElse(""));
        assertEquals(1, audits.size());
        assertEquals("CASH_CLOSE", audits.get(0).action());
    }

    @Test
    public void alreadyClosedDay_returnsConflictWithoutSecondAudit() {
        InMemoryCashRepo cashRepo = new InMemoryCashRepo();
        List<AuditRecord> audits = new ArrayList<>();
        CloseCashDayUseCase useCase = buildUseCase(cashRepo, audits);

        Map<PaymentMethod, Money> counted = new EnumMap<>(PaymentMethod.class);
        assertTrue(useCase.execute(new Request(20_500L, counted, "")).isSuccess());

        Result<CashClosing> second = useCase.execute(new Request(20_500L, counted, "Duplicado"));
        assertFalse(second.isSuccess());
        assertEquals(DomainError.CONFLICT, second.getError());
        assertEquals(1, audits.size());
    }

    @Test
    public void negativeCountedAmount_returnsValidationError() {
        InMemoryCashRepo cashRepo = new InMemoryCashRepo();
        CloseCashDayUseCase useCase = buildUseCase(cashRepo, new ArrayList<>());

        Map<PaymentMethod, Money> counted = new EnumMap<>(PaymentMethod.class);
        counted.put(PaymentMethod.CASH, Money.ofCents(-100L));
        Result<CashClosing> result = useCase.execute(new Request(20_500L, counted, ""));

        assertFalse(result.isSuccess());
        assertEquals(DomainError.VALIDATION_ERROR, result.getError());
        assertFalse(cashRepo.isDayClosed(20_500L));
    }

    private static CloseCashDayUseCase buildUseCase(
            CashRepository cashRepo, List<AuditRecord> audits) {
        Clock clock = new Clock() {
            @Override
            public long nowMillis() {
                return 9_000L;
            }

            @Override
            public long todayEpochDay() {
                return 20_500L;
            }

            @Override
            public int currentMinuteOfDay() {
                return 1_100;
            }

            @Override
            public ZoneId zoneId() {
                return ARGENTINA_ZONE;
            }
        };
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
        int[] seq = {0};
        IdGenerator idGen = () -> "close-" + (++seq[0]);
        return new CloseCashDayUseCase(cashRepo, settingsRepo, auditRepo, clock, idGen);
    }

    private static CashMovement movement(
            String id, long day, CashMovementType type, PaymentMethod method,
            long cents, String revId) {
        return new CashMovement.Builder()
                .id(id)
                .day(day)
                .timestamp(1_000L)
                .type(type)
                .method(method)
                .amount(Money.ofCents(cents))
                .concept("Mov " + id)
                .reversesMovementId(revId)
                .operator("Dueño")
                .build();
    }

    private static final class InMemoryCashRepo implements CashRepository {
        private final Map<String, CashMovement> movements = new LinkedHashMap<>();
        private final Map<Long, CashClosing> closings = new LinkedHashMap<>();

        @Override
        public Optional<CashMovement> findMovementById(String id) {
            return Optional.ofNullable(movements.get(id));
        }

        @Override
        public List<CashMovement> findMovementsByDay(long dayEpoch) {
            List<CashMovement> out = new ArrayList<>();
            for (CashMovement m : movements.values()) {
                if (m.day() == dayEpoch) {
                    out.add(m);
                }
            }
            return out;
        }

        @Override
        public List<CashMovement> findMovementsByAppointmentId(String appointmentId) {
            return new ArrayList<>();
        }

        @Override
        public List<CashMovement> findMovementsByClientId(String clientId) {
            return new ArrayList<>();
        }

        @Override
        public boolean existsReversalForMovement(String movementId) {
            return false;
        }

        @Override
        public boolean isDayClosed(long dayEpoch) {
            return closings.containsKey(dayEpoch);
        }

        @Override
        public Optional<CashClosing> findClosingByDay(long dayEpoch) {
            return Optional.ofNullable(closings.get(dayEpoch));
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
                saveMovement(m);
            }
        }

        @Override
        public void saveClosingAndLockDay(CashClosing closing) {
            closings.put(closing.day(), closing);
            for (Map.Entry<String, CashMovement> entry : movements.entrySet()) {
                CashMovement m = entry.getValue();
                if (m.day() == closing.day() && !m.closingId().isPresent()) {
                    entry.setValue(new CashMovement.Builder()
                            .id(m.id())
                            .day(m.day())
                            .timestamp(m.timestamp())
                            .type(m.type())
                            .method(m.method())
                            .amount(m.amount())
                            .concept(m.concept())
                            .appointmentId(m.appointmentId().orElse(null))
                            .clientId(m.clientId().orElse(null))
                            .productId(m.productId().orElse(null))
                            .quantity(m.quantity().orElse(null))
                            .reversesMovementId(m.reversesMovementId().orElse(null))
                            .operator(m.operator())
                            .closingId(closing.id())
                            .build());
                }
            }
        }

        @Override
        public void runInTransaction(Runnable work) {
            work.run();
        }
    }
}
