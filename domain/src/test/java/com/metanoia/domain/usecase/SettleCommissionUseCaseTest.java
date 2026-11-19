package com.metanoia.domain.usecase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
import com.metanoia.domain.model.CommissionSettlement;
import com.metanoia.domain.model.CommissionSettlementEntryType;
import com.metanoia.domain.model.Money;
import com.metanoia.domain.model.PaymentMethod;
import com.metanoia.domain.model.Professional;
import com.metanoia.domain.model.RecurrenceRule;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.repository.AppointmentRepository;
import com.metanoia.domain.repository.CashRepository;
import com.metanoia.domain.repository.CommissionRepository;
import com.metanoia.domain.repository.ProfessionalRepository;
import com.metanoia.domain.repository.SettingsRepository;
import com.metanoia.domain.usecase.SettleCommissionUseCase.Outcome;
import com.metanoia.domain.usecase.SettleCommissionUseCase.Request;

import org.junit.Test;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SettleCommissionUseCaseTest {

    @Test
    public void settlePeriod_createsCashOutflowSettlementEntriesAndPreventsDoubleSettlement() {
        TransactionalStore store = new TransactionalStore();
        seedProfessional(store, "pro-1", "Ana", 4_000);
        store.appointments.put("apt-1", appointment(
                "apt-1", "pro-1", 20_500L, AppointmentStatus.PAID, 1_000_000L, 4_000));
        store.appointments.put("apt-2", appointment(
                "apt-2", "pro-1", 20_501L, AppointmentStatus.COMPLETED, 2_000_000L, 4_000));

        SettleCommissionUseCase useCase = buildUseCase(store);
        Result<Outcome> res = useCase.execute(new Request(
                "pro-1", 20_500L, 20_506L, PaymentMethod.TRANSFER));

        assertTrue(res.isSuccess());
        Outcome out = res.getValue();
        assertEquals(CashMovementType.COMMISSION_PAYOUT, out.cashMovement().type());
        assertEquals(400_000L, out.cashMovement().amount().cents());
        assertEquals(PaymentMethod.TRANSFER, out.cashMovement().method());
        assertEquals(out.cashMovement().id(), out.settlement().cashMovementId());
        assertEquals(1, out.settlement().entries().size());
        assertEquals("apt-1", out.settlement().entries().get(0).appointmentId());
        assertEquals(1, store.audits.size());
        assertEquals("COMMISSION_SETTLE", store.audits.get(0).action());

        // No permite volver a liquidar período solapado
        Result<Outcome> overlapRes = useCase.execute(new Request(
                "pro-1", 20_505L, 20_510L, PaymentMethod.CASH));
        assertFalse(overlapRes.isSuccess());
        assertEquals(DomainError.CONFLICT, overlapRes.getError());
        assertEquals(1, store.settlements.size());
        assertEquals(1, store.cashMovements.size());
    }

    @Test
    public void closedCashDay_isRejectedWithoutPersistingAnything() {
        TransactionalStore store = new TransactionalStore();
        seedProfessional(store, "pro-1", "Ana", 4_000);
        store.appointments.put("apt-1", appointment(
                "apt-1", "pro-1", 20_500L, AppointmentStatus.PAID, 1_000_000L, 4_000));
        store.closedDays.put(20_510L, Boolean.TRUE);

        SettleCommissionUseCase useCase = buildUseCase(store);
        Result<Outcome> res = useCase.execute(new Request(
                "pro-1", 20_500L, 20_506L, PaymentMethod.CASH));

        assertFalse(res.isSuccess());
        assertEquals(DomainError.CONFLICT, res.getError());
        assertTrue(store.cashMovements.isEmpty());
        assertTrue(store.settlements.isEmpty());
    }

    @Test
    public void reversalClawbackAndSubsequentRepayment_areSettledCorrectly() {
        TransactionalStore store = new TransactionalStore();
        seedProfessional(store, "pro-1", "Ana", 5_000);
        store.appointments.put("apt-1", appointment(
                "apt-1", "pro-1", 20_500L, AppointmentStatus.PAID, 1_000_000L, 5_000));

        SettleCommissionUseCase useCase = buildUseCase(store);
        assertTrue(useCase.execute(new Request("pro-1", 20_500L, 20_506L, PaymentMethod.CASH))
                .isSuccess());

        // Turno 1 revertido a COMPLETED; turno 2 pagado en período siguiente (20_507..20_513)
        store.appointments.put("apt-1", appointment(
                "apt-1", "pro-1", 20_500L, AppointmentStatus.COMPLETED, 1_000_000L, 5_000));
        store.appointments.put("apt-2", appointment(
                "apt-2", "pro-1", 20_508L, AppointmentStatus.PAID, 2_000_000L, 5_000));

        Result<Outcome> nextRes = useCase.execute(new Request(
                "pro-1", 20_507L, 20_513L, PaymentMethod.CASH));
        assertTrue(nextRes.isSuccess());
        // 1.000.000 - 500.000 = 500.000
        assertEquals(500_000L, nextRes.getValue().cashMovement().amount().cents());
        assertEquals(2, nextRes.getValue().settlement().entries().size());
        assertEquals(CommissionSettlementEntryType.REVERSAL_ADJUSTMENT,
                nextRes.getValue().settlement().entries().get(1).type());

        // Si luego vuelve a PAID, se vuelve a liquidar en el 3er período
        store.appointments.put("apt-1", appointment(
                "apt-1", "pro-1", 20_500L, AppointmentStatus.PAID, 1_000_000L, 5_000));
        Result<Outcome> thirdRes = useCase.execute(new Request(
                "pro-1", 20_514L, 20_520L, PaymentMethod.CASH));
        assertTrue(thirdRes.isSuccess());
        assertEquals(500_000L, thirdRes.getValue().cashMovement().amount().cents());
    }

    @Test
    public void failedSettlementSave_rollsBackCashOutflowAndAudit() {
        TransactionalStore store = new TransactionalStore();
        seedProfessional(store, "pro-1", "Ana", 4_000);
        store.appointments.put("apt-1", appointment(
                "apt-1", "pro-1", 20_500L, AppointmentStatus.PAID, 1_000_000L, 4_000));
        store.failOnSettlementSave = true;

        SettleCommissionUseCase useCase = buildUseCase(store);
        try {
            useCase.execute(new Request("pro-1", 20_500L, 20_506L, PaymentMethod.CASH));
            fail("Se esperaba excepción simulada al persistir liquidación");
        } catch (IllegalStateException expected) {
            // Rollback transaccional esperado
        }

        assertTrue(store.cashMovements.isEmpty());
        assertTrue(store.settlements.isEmpty());
        assertTrue(store.audits.isEmpty());
    }

    private static void seedProfessional(
            TransactionalStore store,
            String id,
            String name,
            int commissionBp
    ) {
        store.professionals.put(id, new Professional.Builder()
                .id(id)
                .name(name)
                .commissionBasisPoints(commissionBp)
                .active(true)
                .createdAt(1_000L)
                .updatedAt(1_000L)
                .build());
    }

    private static Appointment appointment(
            String id,
            String proId,
            long day,
            AppointmentStatus status,
            long priceCents,
            int bp
    ) {
        return new Appointment.Builder()
                .id(id)
                .clientId("cli-1")
                .professionalId(proId)
                .day(day)
                .startMin(600)
                .endMin(630)
                .status(status)
                .items(Collections.singletonList(new AppointmentItem.Builder()
                        .id("it-" + id)
                        .appointmentId(id)
                        .serviceId("svc-1")
                        .serviceNameSnapshot("Corte")
                        .priceSnapshot(Money.ofCents(priceCents))
                        .durationMinSnapshot(30)
                        .commissionBasisPointsSnapshot(bp)
                        .position(0)
                        .build()))
                .createdAt(1_000L)
                .updatedAt(1_000L)
                .build();
    }

    private static SettleCommissionUseCase buildUseCase(TransactionalStore store) {
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
                return 50_000L;
            }

            @Override
            public long todayEpochDay() {
                return 20_510L;
            }

            @Override
            public int currentMinuteOfDay() {
                return 900;
            }

            @Override
            public ZoneId zoneId() {
                return ARGENTINA_ZONE;
            }
        };
        int[] seq = {0};
        IdGenerator ids = () -> "set-" + (++seq[0]);
        return new SettleCommissionUseCase(
                store, store, store.professionalRepo, store,
                settingsRepo, store.audits::add, clock, ids);
    }

    private static final class TransactionalStore implements
            CommissionRepository, AppointmentRepository, CashRepository {

        Map<String, Professional> professionals = new LinkedHashMap<>();
        final ProfessionalRepository professionalRepo = new ProfessionalRepository() {
            @Override
            public Optional<Professional> findById(String id) {
                return Optional.ofNullable(professionals.get(id));
            }

            @Override
            public List<Professional> findAll(boolean includeInactive) {
                return new ArrayList<>(professionals.values());
            }

            @Override
            public void save(Professional professional) {
                professionals.put(professional.id(), professional);
            }

            @Override
            public boolean hasFutureActiveAppointments(String professionalId, long fromDayEpoch) {
                return false;
            }
        };
        Map<String, Appointment> appointments = new LinkedHashMap<>();
        List<CommissionSettlement> settlements = new ArrayList<>();
        Map<String, CashMovement> cashMovements = new LinkedHashMap<>();
        Map<Long, Boolean> closedDays = new LinkedHashMap<>();
        List<AuditRecord> audits = new ArrayList<>();
        boolean failOnSettlementSave;

        @Override
        public List<CommissionSettlement> findSettlementsByProfessional(String professionalId) {
            List<CommissionSettlement> list = new ArrayList<>();
            for (CommissionSettlement s : settlements) {
                if (professionalId.equals(s.professionalId())) {
                    list.add(s);
                }
            }
            return list;
        }

        @Override
        public boolean hasOverlappingSettlement(String professionalId, long fromDay, long toDay) {
            for (CommissionSettlement s : settlements) {
                if (professionalId.equals(s.professionalId()) && s.overlaps(fromDay, toDay)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public List<Appointment> findPaidAppointmentsInPreviousSettledPeriods(
                String professionalId,
                long beforeDayEpoch
        ) {
            List<Appointment> result = new ArrayList<>();
            for (Appointment apt : appointments.values()) {
                if (professionalId.equals(apt.professionalId())
                        && apt.status() == AppointmentStatus.PAID
                        && apt.day() < beforeDayEpoch) {
                    for (CommissionSettlement s : settlements) {
                        if (professionalId.equals(s.professionalId()) && s.coversDay(apt.day())) {
                            result.add(apt);
                            break;
                        }
                    }
                }
            }
            return result;
        }

        @Override
        public void saveSettlement(CommissionSettlement settlement) {
            if (failOnSettlementSave) {
                throw new IllegalStateException("Fallo simulado de liquidación");
            }
            settlements.add(settlement);
        }

        @Override
        public Optional<Appointment> findById(String id) {
            return Optional.ofNullable(appointments.get(id));
        }

        @Override
        public List<Appointment> findByDayAndProfessional(long dayEpoch, String professionalId) {
            return Collections.emptyList();
        }

        @Override
        public List<Appointment> findByDayRange(long startDayEpoch, long endDayEpoch) {
            List<Appointment> result = new ArrayList<>();
            for (Appointment apt : appointments.values()) {
                if (apt.day() >= startDayEpoch && apt.day() <= endDayEpoch) {
                    result.add(apt);
                }
            }
            return result;
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
            appointments.put(appointment.id(), appointment);
        }

        @Override
        public void saveAll(List<Appointment> list) {
            for (Appointment apt : list) {
                save(apt);
            }
        }

        @Override
        public void saveRecurrenceRule(RecurrenceRule rule) {
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
            List<CommissionSettlement> snapSet = new ArrayList<>(settlements);
            Map<String, CashMovement> snapCash = new LinkedHashMap<>(cashMovements);
            List<AuditRecord> snapAudits = new ArrayList<>(audits);
            try {
                action.run();
            } catch (RuntimeException ex) {
                settlements = snapSet;
                cashMovements = snapCash;
                audits.clear();
                audits.addAll(snapAudits);
                throw ex;
            }
        }
    }
}
