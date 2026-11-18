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
import com.metanoia.domain.model.Client;
import com.metanoia.domain.model.ClientSummary;
import com.metanoia.domain.model.Money;
import com.metanoia.domain.model.Professional;
import com.metanoia.domain.model.RecurrenceRule;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.repository.AppointmentRepository;
import com.metanoia.domain.repository.AuditLogRepository;
import com.metanoia.domain.repository.ClientRepository;
import com.metanoia.domain.repository.ProfessionalRepository;
import com.metanoia.domain.repository.SettingsRepository;

import org.junit.Before;
import org.junit.Test;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ChangeAppointmentStatusUseCaseTest {

    private FakeAppointmentRepo appointmentRepo;
    private FakeClientRepo clientRepo;
    private FakeAuditRepo auditRepo;
    private ChangeAppointmentStatusUseCase useCase;

    @Before
    public void setUp() {
        appointmentRepo = new FakeAppointmentRepo();
        clientRepo = new FakeClientRepo();
        auditRepo = new FakeAuditRepo();

        clientRepo.save(new Client.Builder()
                .id("cli-1")
                .name("Sofía")
                .nameNormalized("sofia")
                .phone("5491122334455")
                .noShowCount(1)
                .build());

        ProfessionalRepository proRepo = new ProfessionalRepository() {
            private final Professional pro = new Professional.Builder()
                    .id("pro-1")
                    .name("Ana")
                    .commissionBasisPoints(4500) // 45,00 %
                    .build();

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
            public boolean hasFutureActiveAppointments(String p, long d) {
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
                return 5000L;
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
        IdGenerator ids = new IdGenerator() {
            private int seq = 1;

            @Override
            public String nextId() {
                return "aud-" + seq++;
            }
        };

        appointmentRepo.save(sample("apt-1", AppointmentStatus.PENDING));
        useCase = new ChangeAppointmentStatusUseCase(
                appointmentRepo, clientRepo, proRepo, settingsRepo, auditRepo, clock, ids);
    }

    @Test
    public void completeAndRevertWithoutPayments_freezesThenClearsCommissionAndAudits() {
        assertTrue(useCase.execute("apt-1", AppointmentStatus.CONFIRMED, null).isSuccess());

        Result<Appointment> completed = useCase.execute(
                "apt-1", AppointmentStatus.COMPLETED, null);
        assertTrue(completed.isSuccess());
        assertEquals(AppointmentStatus.COMPLETED, completed.getValue().status());
        assertEquals(Integer.valueOf(4500), completed.getValue().items().get(0)
                .commissionBasisPointsSnapshot().orElseThrow());

        // Con cobros no puede revertirse
        Result<Appointment> blockedRevert = useCase.execute(
                "apt-1", AppointmentStatus.CONFIRMED, null, 10_000L);
        assertFalse(blockedRevert.isSuccess());
        assertEquals(DomainError.CONFLICT, blockedRevert.getError());

        // Sin cobros sí revierte, libera comisión y registra auditoría
        Result<Appointment> reverted = useCase.execute(
                "apt-1", AppointmentStatus.CONFIRMED, null, 0L);
        assertTrue(reverted.isSuccess());
        assertEquals(AppointmentStatus.CONFIRMED, reverted.getValue().status());
        assertFalse(reverted.getValue().items().get(0)
                .commissionBasisPointsSnapshot().isPresent());
        assertEquals("APPOINTMENT_REVERT_COMPLETED",
                auditRepo.records.get(auditRepo.records.size() - 1).action());
    }

    @Test
    public void noShow_incrementsClientNoShowCountAndBlocksFurtherTransitions() {
        Result<Appointment> result = useCase.execute("apt-1", AppointmentStatus.NO_SHOW, null);
        assertTrue(result.isSuccess());
        assertEquals(2, clientRepo.findById("cli-1").orElseThrow().noShowCount());

        Result<Appointment> afterFinal = useCase.execute("apt-1", AppointmentStatus.CONFIRMED, null);
        assertEquals(DomainError.CONFLICT, afterFinal.getError());
    }

    @Test
    public void cancel_requiresReasonAndRejectsManualPaid() {
        Result<Appointment> missingReason = useCase.execute(
                "apt-1", AppointmentStatus.CANCELLED, " ");
        assertEquals(DomainError.VALIDATION_ERROR, missingReason.getError());

        Result<Appointment> manualPaid = useCase.execute(
                "apt-1", AppointmentStatus.PAID, null);
        assertEquals(DomainError.CONFLICT, manualPaid.getError());

        Result<Appointment> cancelled = useCase.execute(
                "apt-1", AppointmentStatus.CANCELLED, "Reprogramación solicitada");
        assertTrue(cancelled.isSuccess());
        assertEquals("Reprogramación solicitada",
                cancelled.getValue().cancelReason().orElseThrow());
    }

    private static Appointment sample(String id, AppointmentStatus status) {
        return new Appointment.Builder()
                .id(id)
                .clientId("cli-1")
                .professionalId("pro-1")
                .day(20_500L)
                .startMin(600)
                .endMin(630)
                .status(status)
                .items(Collections.singletonList(new AppointmentItem.Builder()
                        .id("i1")
                        .appointmentId(id)
                        .serviceId("svc-1")
                        .serviceNameSnapshot("Corte")
                        .priceSnapshot(Money.ofCents(1_500_000L))
                        .durationMinSnapshot(30)
                        .position(0)
                        .build()))
                .createdAt(1000L)
                .updatedAt(1000L)
                .createdBy("Dueño")
                .build();
    }

    private static final class FakeAppointmentRepo implements AppointmentRepository {
        final Map<String, Appointment> store = new LinkedHashMap<>();

        @Override
        public Optional<Appointment> findById(String id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<Appointment> findByDayAndProfessional(long d, String p) {
            return Collections.emptyList();
        }

        @Override
        public List<Appointment> findByDayRange(long s, long e) {
            return Collections.emptyList();
        }

        @Override
        public List<Appointment> findFutureActiveFromSeries(String r, long d, int m) {
            return Collections.emptyList();
        }

        @Override
        public boolean hasFutureActiveForProfessional(String p, long d) {
            return false;
        }

        @Override
        public void save(Appointment appointment) {
            store.put(appointment.id(), appointment);
        }

        @Override
        public void saveAll(List<Appointment> appointments) {
        }

        @Override
        public void saveRecurrenceRule(RecurrenceRule rule) {
        }
    }

    private static final class FakeClientRepo implements ClientRepository {
        private final Map<String, Client> store = new LinkedHashMap<>();

        @Override
        public Optional<Client> findById(String id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<Client> findActiveByPhone(String p) {
            return Optional.empty();
        }

        @Override
        public List<ClientSummary> searchSummaries(String q, boolean inc) {
            return Collections.emptyList();
        }

        @Override
        public void save(Client client) {
            store.put(client.id(), client);
        }
    }

    private static final class FakeAuditRepo implements AuditLogRepository {
        final List<AuditRecord> records = new ArrayList<>();

        @Override
        public void record(AuditRecord record) {
            records.add(record);
        }
    }
}
