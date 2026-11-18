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
import com.metanoia.domain.usecase.RecordCashMovementUseCase.Request;

import org.junit.Test;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RecordCashMovementUseCaseTest {

    @Test
    public void looseSaleAndOtherIncome_saveMovementAndAuditLog() {
        InMemoryCashRepo cashRepo = new InMemoryCashRepo();
        List<AuditRecord> audits = new ArrayList<>();
        RecordCashMovementUseCase useCase = buildUseCase(cashRepo, audits, 20_500L);

        Result<CashMovement> saleResult = useCase.execute(new Request(
                CashMovementType.PRODUCT_SALE,
                PaymentMethod.MERCADO_PAGO,
                Money.ofCents(450_000L),
                "Shampoo matizador"));

        assertTrue(saleResult.isSuccess());
        CashMovement saved = saleResult.getValue();
        assertEquals(CashMovementType.PRODUCT_SALE, saved.type());
        assertEquals(PaymentMethod.MERCADO_PAGO, saved.method());
        assertEquals(450_000L, saved.amount().cents());
        assertFalse(saved.appointmentId().isPresent());
        assertFalse(saved.productId().isPresent());
        assertEquals(1, cashRepo.findMovementsByDay(20_500L).size());
        assertEquals(1, audits.size());
        assertEquals("CASH_PRODUCT_SALE", audits.get(0).action());

        Result<CashMovement> incomeResult = useCase.execute(new Request(
                CashMovementType.OTHER_INCOME,
                PaymentMethod.CASH,
                Money.ofCents(100_000L),
                "Ajuste caja inicial"));
        assertTrue(incomeResult.isSuccess());
        assertEquals(2, cashRepo.findMovementsByDay(20_500L).size());
        assertEquals("CASH_OTHER_INCOME", audits.get(1).action());
    }

    @Test
    public void expense_savesOutflowOnOpenDay() {
        InMemoryCashRepo cashRepo = new InMemoryCashRepo();
        List<AuditRecord> audits = new ArrayList<>();
        RecordCashMovementUseCase useCase = buildUseCase(cashRepo, audits, 20_500L);

        Result<CashMovement> result = useCase.execute(new Request(
                CashMovementType.EXPENSE,
                PaymentMethod.TRANSFER,
                Money.ofCents(320_000L),
                "Compra de tinturas"));

        assertTrue(result.isSuccess());
        assertEquals(CashMovementType.EXPENSE, result.getValue().type());
        assertEquals("CASH_EXPENSE", audits.get(0).action());
    }

    @Test
    public void closedDay_rejectsNewMovement() {
        InMemoryCashRepo cashRepo = new InMemoryCashRepo();
        cashRepo.closeDay(20_500L);
        List<AuditRecord> audits = new ArrayList<>();
        RecordCashMovementUseCase useCase = buildUseCase(cashRepo, audits, 20_500L);

        Result<CashMovement> result = useCase.execute(new Request(
                CashMovementType.EXPENSE,
                PaymentMethod.CASH,
                Money.ofCents(50_000L),
                "Café"));

        assertFalse(result.isSuccess());
        assertEquals(DomainError.CONFLICT, result.getError());
        assertTrue(cashRepo.findMovementsByDay(20_500L).isEmpty());
        assertTrue(audits.isEmpty());
    }

    @Test
    public void rejectsInvalidAmountBlankConceptOrAppointmentType() {
        InMemoryCashRepo cashRepo = new InMemoryCashRepo();
        RecordCashMovementUseCase useCase = buildUseCase(cashRepo, new ArrayList<>(), 20_500L);

        Result<CashMovement> zeroAmount = useCase.execute(new Request(
                CashMovementType.EXPENSE, PaymentMethod.CASH, Money.zero(), "Insumos"));
        assertEquals(DomainError.VALIDATION_ERROR, zeroAmount.getError());

        Result<CashMovement> blankConcept = useCase.execute(new Request(
                CashMovementType.PRODUCT_SALE, PaymentMethod.CASH, Money.ofCents(10_000L), " "));
        assertEquals(DomainError.VALIDATION_ERROR, blankConcept.getError());

        Result<CashMovement> unsupportedType = useCase.execute(new Request(
                CashMovementType.APPOINTMENT_PAYMENT, PaymentMethod.CASH,
                Money.ofCents(10_000L), "Turno"));
        assertEquals(DomainError.VALIDATION_ERROR, unsupportedType.getError());
    }

    private static RecordCashMovementUseCase buildUseCase(
            CashRepository cashRepo, List<AuditRecord> audits, long today) {
        Clock clock = new Clock() {
            @Override
            public long nowMillis() {
                return 5_000L;
            }

            @Override
            public long todayEpochDay() {
                return today;
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
        final int[] seq = {0};
        IdGenerator idGen = () -> "id-" + (++seq[0]);
        return new RecordCashMovementUseCase(cashRepo, settingsRepo, auditRepo, clock, idGen);
    }

    private static final class InMemoryCashRepo implements CashRepository {
        private final Map<String, CashMovement> movements = new LinkedHashMap<>();
        private final Map<Long, Boolean> closedDays = new LinkedHashMap<>();

        void closeDay(long day) {
            closedDays.put(day, Boolean.TRUE);
        }

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
            return Boolean.TRUE.equals(closedDays.get(dayEpoch));
        }

        @Override
        public Optional<CashClosing> findClosingByDay(long dayEpoch) {
            return Optional.empty();
        }

        @Override
        public Optional<Long> findLatestUnclosedDayWithMovementsBefore(long beforeDayEpoch) {
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
            closedDays.put(closing.day(), Boolean.TRUE);
        }

        @Override
        public void runInTransaction(Runnable work) {
            work.run();
        }
    }
}
