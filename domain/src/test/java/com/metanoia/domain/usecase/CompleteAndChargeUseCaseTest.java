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
import com.metanoia.domain.model.Professional;
import com.metanoia.domain.model.RecurrenceRule;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.model.TimeRange;
import com.metanoia.domain.repository.AppointmentRepository;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.CashRepository;
import com.metanoia.domain.repository.ProfessionalRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.rules.CashPaymentSupport.PaymentSplit;
import com.metanoia.domain.usecase.ChargeAppointmentUseCase.ChargeResult;
import com.metanoia.domain.usecase.CompleteAndChargeUseCase.Request;

import org.junit.Test;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CompleteAndChargeUseCaseTest {

    @Test
    public void fullPayment_freezesCommissionAndMarksPaidAtomically() {
        InMemoryCashRepo cashRepo = new InMemoryCashRepo();
        InMemoryAptRepo aptRepo = new InMemoryAptRepo();
        List<AuditRecord> audits = new ArrayList<>();
        CompleteAndChargeUseCase useCase = buildUseCase(cashRepo, aptRepo, audits, 20_500L);

        aptRepo.save(appointment("apt-1", AppointmentStatus.CONFIRMED, 18_000L));

        Result<ChargeResult> result = useCase.execute(new Request(
                "apt-1",
                null,
                null,
                Collections.singletonList(
                        new PaymentSplit(PaymentMethod.CASH, Money.ofCents(18_000L)))));

        assertTrue(result.isSuccess());
        Appointment saved = aptRepo.findById("apt-1").get();
        assertEquals(AppointmentStatus.PAID, saved.status());
        assertEquals(Integer.valueOf(4_500),
                saved.items().get(0).commissionBasisPointsSnapshot().orElse(null));
        assertEquals(1, cashRepo.findMovementsByAppointmentId("apt-1").size());
        assertEquals(2, audits.size());
    }

    @Test
    public void partialPayment_leavesCompletedWithRemainingBalanceAndFrozenCommission() {
        InMemoryCashRepo cashRepo = new InMemoryCashRepo();
        InMemoryAptRepo aptRepo = new InMemoryAptRepo();
        CompleteAndChargeUseCase useCase = buildUseCase(
                cashRepo, aptRepo, new ArrayList<>(), 20_500L);

        aptRepo.save(appointment("apt-1", AppointmentStatus.PENDING, 20_000L));

        Result<ChargeResult> result = useCase.execute(new Request(
                "apt-1",
                null,
                null,
                Collections.singletonList(
                        new PaymentSplit(PaymentMethod.TRANSFER, Money.ofCents(10_000L)))));

        assertTrue(result.isSuccess());
        Appointment saved = aptRepo.findById("apt-1").get();
        assertEquals(AppointmentStatus.COMPLETED, saved.status());
        assertEquals(10_000L, result.getValue().balance().balance().cents());
        assertTrue(saved.items().get(0).commissionBasisPointsSnapshot().isPresent());
    }

    @Test
    public void invalidOverpayment_leavesAppointmentUntouchedInPendingState() {
        InMemoryCashRepo cashRepo = new InMemoryCashRepo();
        InMemoryAptRepo aptRepo = new InMemoryAptRepo();
        CompleteAndChargeUseCase useCase = buildUseCase(
                cashRepo, aptRepo, new ArrayList<>(), 20_500L);

        aptRepo.save(appointment("apt-1", AppointmentStatus.PENDING, 15_000L));

        Result<ChargeResult> result = useCase.execute(new Request(
                "apt-1",
                null,
                null,
                Collections.singletonList(
                        new PaymentSplit(PaymentMethod.CASH, Money.ofCents(20_000L)))));

        assertFalse(result.isSuccess());
        assertEquals(DomainError.VALIDATION_ERROR, result.getError());
        Appointment untouched = aptRepo.findById("apt-1").get();
        assertEquals(AppointmentStatus.PENDING, untouched.status());
        assertFalse(untouched.items().get(0).commissionBasisPointsSnapshot().isPresent());
        assertTrue(cashRepo.findMovementsByAppointmentId("apt-1").isEmpty());
    }

    private static CompleteAndChargeUseCase buildUseCase(
            InMemoryCashRepo cashRepo,
            InMemoryAptRepo aptRepo,
            List<AuditRecord> audits,
            long todayEpoch
    ) {
        Professional pro = new Professional.Builder()
                .id("pro-1")
                .name("Ana")
                .commissionBasisPoints(4_500)
                .workSchedules(Collections.singletonList(new TimeRange(1, 540, 1200)))
                .serviceIds(Collections.singleton("svc-1"))
                .createdAt(1_000L)
                .updatedAt(1_000L)
                .build();
        ProfessionalRepository proRepo = new ProfessionalRepository() {
            @Override
            public List<Professional> findAll(boolean includeInactive) {
                return Collections.singletonList(pro);
            }

            @Override
            public Optional<Professional> findById(String id) {
                return "pro-1".equals(id) ? Optional.of(pro) : Optional.empty();
            }

            @Override
            public void save(Professional professional) {
            }

            @Override
            public boolean hasFutureActiveAppointments(String professionalId, long fromEpochDay) {
                return false;
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
        Clock clock = new Clock() {
            @Override
            public long nowMillis() {
                return 9_000L;
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
        return new CompleteAndChargeUseCase(
                aptRepo,
                proRepo,
                cashRepo,
                settingsRepo,
                audits::add,
                clock,
                () -> "gen-" + (++seq[0]));
    }

    private static Appointment appointment(String id, AppointmentStatus status, long priceCents) {
        AppointmentItem item = new AppointmentItem.Builder()
                .id("item-" + id)
                .appointmentId(id)
                .serviceId("svc-1")
                .serviceNameSnapshot("Corte")
                .priceSnapshot(Money.ofCents(priceCents))
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
                .status(status)
                .items(Collections.singletonList(item))
                .createdAt(1_000L)
                .updatedAt(1_000L)
                .build();
    }

    private static final class InMemoryCashRepo implements CashRepository {
        final Map<String, CashMovement> map = new LinkedHashMap<>();

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
            return false;
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
        public boolean hasFutureActiveForProfessional(String professionalId, long fromEpochDay) {
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
