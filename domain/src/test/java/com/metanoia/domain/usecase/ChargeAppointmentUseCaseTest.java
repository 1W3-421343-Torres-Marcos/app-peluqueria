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
import com.metanoia.domain.model.Money;
import com.metanoia.domain.model.PaymentMethod;
import com.metanoia.domain.model.RecurrenceRule;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.repository.AppointmentRepository;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.CashRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.rules.CashPaymentSupport.PaymentSplit;
import com.metanoia.domain.usecase.ChargeAppointmentUseCase.ChargeResult;
import com.metanoia.domain.usecase.ChargeAppointmentUseCase.Request;

import org.junit.Test;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ChargeAppointmentUseCaseTest {

    @Test
    public void partialThenFinalAndSplitPayments_transitionCompletedToPaidAutomatically() {
        InMemoryCashRepo cashRepo = new InMemoryCashRepo();
        InMemoryAptRepo aptRepo = new InMemoryAptRepo();
        List<AuditRecord> audits = new ArrayList<>();
        ChargeAppointmentUseCase useCase = buildUseCase(cashRepo, aptRepo, audits, 20_500L);

        aptRepo.save(appointment("apt-1", AppointmentStatus.COMPLETED, 20_000L));

        // 1. Pago parcial en efectivo -> sigue COMPLETED con saldo 8.000
        Result<ChargeResult> partial = useCase.execute(new Request(
                "apt-1", null, null,
                Collections.singletonList(new PaymentSplit(PaymentMethod.CASH, Money.ofCents(12_000L)))));
        assertTrue(partial.isSuccess());
        assertEquals(AppointmentStatus.COMPLETED, partial.getValue().appointment().status());
        assertEquals(8_000L, partial.getValue().balance().balance().cents());

        // 2. Pago dividido del resto -> pasa automáticamente a PAID
        Result<ChargeResult> finalSplit = useCase.execute(new Request(
                "apt-1", null, null,
                Arrays.asList(
                        new PaymentSplit(PaymentMethod.TRANSFER, Money.ofCents(5_000L)),
                        new PaymentSplit(PaymentMethod.MERCADO_PAGO, Money.ofCents(3_000L)))));
        assertTrue(finalSplit.isSuccess());
        assertEquals(AppointmentStatus.PAID, finalSplit.getValue().appointment().status());
        assertEquals(0L, finalSplit.getValue().balance().balance().cents());
        assertEquals(3, cashRepo.findMovementsByAppointmentId("apt-1").size());
        assertEquals(3, audits.size());
    }

    @Test
    public void adjustmentRequiresReason_andUpdatesPayableTotal() {
        InMemoryCashRepo cashRepo = new InMemoryCashRepo();
        InMemoryAptRepo aptRepo = new InMemoryAptRepo();
        List<AuditRecord> audits = new ArrayList<>();
        ChargeAppointmentUseCase useCase = buildUseCase(cashRepo, aptRepo, audits, 20_500L);
        aptRepo.save(appointment("apt-1", AppointmentStatus.COMPLETED, 10_000L));

        Result<ChargeResult> noReason = useCase.execute(new Request(
                "apt-1", Money.ofCents(-2_000L), "",
                Collections.singletonList(new PaymentSplit(PaymentMethod.CASH, Money.ofCents(8_000L)))));
        assertFalse(noReason.isSuccess());
        assertEquals(DomainError.VALIDATION_ERROR, noReason.getError());

        Result<ChargeResult> withReason = useCase.execute(new Request(
                "apt-1", Money.ofCents(-2_000L), "Descuento efectivo",
                Collections.singletonList(new PaymentSplit(PaymentMethod.CASH, Money.ofCents(8_000L)))));
        assertTrue(withReason.isSuccess());
        assertEquals(AppointmentStatus.PAID, withReason.getValue().appointment().status());
        assertEquals(-2_000L, withReason.getValue().appointment().adjustment().cents());
        assertEquals("APPOINTMENT_ADJUSTMENT", audits.get(0).action());
    }

    @Test
    public void rejectsNonCompletedStatusesAndClosedDay() {
        InMemoryCashRepo cashRepo = new InMemoryCashRepo();
        InMemoryAptRepo aptRepo = new InMemoryAptRepo();
        ChargeAppointmentUseCase useCase = buildUseCase(cashRepo, aptRepo, new ArrayList<>(), 20_500L);

        AppointmentStatus[] forbidden = {
                AppointmentStatus.PENDING,
                AppointmentStatus.CONFIRMED,
                AppointmentStatus.PAID,
                AppointmentStatus.CANCELLED,
                AppointmentStatus.NO_SHOW
        };
        for (AppointmentStatus status : forbidden) {
            String id = "apt-" + status.name();
            aptRepo.save(appointment(id, status, 10_000L));
            Result<ChargeResult> res = useCase.execute(new Request(
                    id, null, null,
                    Collections.singletonList(
                            new PaymentSplit(PaymentMethod.CASH, Money.ofCents(10_000L)))));
            assertFalse(res.isSuccess());
            assertEquals(DomainError.CONFLICT, res.getError());
        }

        aptRepo.save(appointment("apt-ok", AppointmentStatus.COMPLETED, 10_000L));
        cashRepo.closedDays.put(20_500L, Boolean.TRUE);
        Result<ChargeResult> closedRes = useCase.execute(new Request(
                "apt-ok", null, null,
                Collections.singletonList(
                        new PaymentSplit(PaymentMethod.CASH, Money.ofCents(10_000L)))));
        assertFalse(closedRes.isSuccess());
        assertEquals(DomainError.CONFLICT, closedRes.getError());
    }

    private static ChargeAppointmentUseCase buildUseCase(
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
        Clock clock = new Clock() {
            @Override
            public long nowMillis() {
                return 10_000L;
            }

            @Override
            public long todayEpochDay() {
                return todayEpoch;
            }

            @Override
            public int currentMinuteOfDay() {
                return 700;
            }

            @Override
            public ZoneId zoneId() {
                return ARGENTINA_ZONE;
            }
        };
        int[] seq = {0};
        return new ChargeAppointmentUseCase(
                aptRepo, cashRepo, settingsRepo, auditRepo, clock, () -> "id-" + (++seq[0]));
    }

    private static Appointment appointment(String id, AppointmentStatus status, long priceCents) {
        AppointmentItem item = new AppointmentItem.Builder()
                .id("item-" + id)
                .appointmentId(id)
                .serviceId("svc-1")
                .serviceNameSnapshot("Corte")
                .priceSnapshot(Money.ofCents(priceCents))
                .durationMinSnapshot(30)
                .commissionBasisPointsSnapshot(4000)
                .position(0)
                .build();
        return new Appointment.Builder()
                .id(id)
                .clientId("cli-1")
                .professionalId("pro-1")
                .day(20_500L)
                .startMin(600)
                .endMin(630)
                .status(status)
                .items(Collections.singletonList(item))
                .createdAt(1_000L)
                .updatedAt(1_000L)
                .build();
    }

    private static final class InMemoryCashRepo implements CashRepository {
        final Map<String, CashMovement> map = new LinkedHashMap<>();
        final Map<Long, Boolean> closedDays = new LinkedHashMap<>();

        @Override
        public Optional<CashMovement> findMovementById(String id) {
            return Optional.ofNullable(map.get(id));
        }

        @Override
        public List<CashMovement> findMovementsByDay(long dayEpoch) {
            return Collections.emptyList();
        }

        @Override
        public List<CashMovement> findMovementsByAppointmentId(String appointmentId) {
            List<CashMovement> out = new ArrayList<>();
            for (CashMovement m : map.values()) {
                if (appointmentId.equals(m.appointmentId().orElse(null))) {
                    out.add(m);
                }
            }
            return out;
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
            map.put(movement.id(), movement);
        }

        @Override
        public void saveMovements(List<CashMovement> movements) {
            for (CashMovement m : movements) {
                map.put(m.id(), m);
            }
        }

        @Override
        public void saveClosingAndLockDay(CashClosing closing) {
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
}
