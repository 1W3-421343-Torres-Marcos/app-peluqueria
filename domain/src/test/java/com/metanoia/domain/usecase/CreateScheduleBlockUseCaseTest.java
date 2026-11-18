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
import com.metanoia.domain.model.Money;
import com.metanoia.domain.model.Professional;
import com.metanoia.domain.model.RecurrenceRule;
import com.metanoia.domain.model.ScheduleBlock;
import com.metanoia.domain.model.ScheduleBlockType;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.repository.AppointmentRepository;
import com.metanoia.domain.repository.ProfessionalRepository;
import com.metanoia.domain.repository.ScheduleBlockRepository;
import com.metanoia.domain.repository.SettingsRepository;

import org.junit.Before;
import org.junit.Test;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CreateScheduleBlockUseCaseTest {

    private static final long DAY = 20_500L;

    private FakeBlockRepo blockRepo;
    private CreateScheduleBlockUseCase createUseCase;
    private DeactivateScheduleBlockUseCase deactivateUseCase;

    @Before
    public void setUp() {
        blockRepo = new FakeBlockRepo();
        AppointmentRepository appointmentRepo = new AppointmentRepository() {
            private final List<Appointment> items = Arrays.asList(
                    apt("apt-hit", "pro-1", DAY, 600, 660, AppointmentStatus.CONFIRMED),
                    apt("apt-cancelled", "pro-1", DAY, 600, 660, AppointmentStatus.CANCELLED),
                    apt("apt-other-time", "pro-1", DAY, 720, 750, AppointmentStatus.PENDING)
            );

            @Override
            public Optional<Appointment> findById(String id) {
                return Optional.empty();
            }

            @Override
            public List<Appointment> findByDayAndProfessional(long d, String p) {
                return Collections.emptyList();
            }

            @Override
            public List<Appointment> findByDayRange(long s, long e) {
                return items;
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
            }

            @Override
            public void saveAll(List<Appointment> appointments) {
            }

            @Override
            public void saveRecurrenceRule(RecurrenceRule rule) {
            }
        };

        ProfessionalRepository proRepo = new ProfessionalRepository() {
            private final Professional pro1 = new Professional.Builder()
                    .id("pro-1").name("Ana").build();

            @Override
            public List<Professional> findAll(boolean inc) {
                return Collections.singletonList(pro1);
            }

            @Override
            public Optional<Professional> findById(String id) {
                return "pro-1".equals(id) ? Optional.of(pro1) : Optional.empty();
            }

            @Override
            public void save(Professional p) {
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
                return 10_000L;
            }

            @Override
            public long todayEpochDay() {
                return DAY;
            }

            @Override
            public int currentMinuteOfDay() {
                return 540;
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
                return "blk-" + seq++;
            }
        };

        createUseCase = new CreateScheduleBlockUseCase(
                blockRepo, appointmentRepo, proRepo, settingsRepo, r -> { }, clock, ids);
        deactivateUseCase = new DeactivateScheduleBlockUseCase(
                blockRepo, settingsRepo, r -> { }, clock, ids);
    }

    @Test
    public void execute_partialBlockOverlappingAppointment_savesAndReportsAffected() {
        Result<CreateScheduleBlockUseCase.CreateResult> result = createUseCase.execute(
                new CreateScheduleBlockUseCase.Request(
                        "pro-1", DAY, DAY, 630, 690,
                        ScheduleBlockType.LUNCH, "Almuerzo especial"));

        assertTrue(result.isSuccess());
        CreateScheduleBlockUseCase.CreateResult created = result.getValue();
        assertEquals(1, created.affectedAppointments().size());
        assertEquals("apt-hit", created.affectedAppointments().get(0).id());

        Result<ScheduleBlock> deactivated = deactivateUseCase.execute(created.block().id());
        assertTrue(deactivated.isSuccess());
        assertFalse(deactivated.getValue().isActive());
    }

    @Test
    public void execute_invalidDatesOrShortReason_failsValidation() {
        Result<CreateScheduleBlockUseCase.CreateResult> invertedDays = createUseCase.execute(
                new CreateScheduleBlockUseCase.Request(
                        null, DAY + 2, DAY, null, null, ScheduleBlockType.HOLIDAY, "Feriado"));
        assertEquals(DomainError.VALIDATION_ERROR, invertedDays.getError());

        Result<CreateScheduleBlockUseCase.CreateResult> shortReason = createUseCase.execute(
                new CreateScheduleBlockUseCase.Request(
                        null, DAY, DAY, null, null, ScheduleBlockType.OTHER, "A"));
        assertEquals(DomainError.VALIDATION_ERROR, shortReason.getError());
    }

    private static Appointment apt(
            String id, String proId, long day, int start, int end, AppointmentStatus status) {
        return new Appointment.Builder()
                .id(id)
                .clientId("cli-1")
                .professionalId(proId)
                .day(day)
                .startMin(start)
                .endMin(end)
                .status(status)
                .items(Collections.singletonList(new AppointmentItem.Builder()
                        .id("i-" + id)
                        .appointmentId(id)
                        .serviceId("svc-1")
                        .serviceNameSnapshot("Corte")
                        .priceSnapshot(Money.ofCents(1_000_000L))
                        .durationMinSnapshot(30)
                        .position(0)
                        .build()))
                .build();
    }

    private static final class FakeBlockRepo implements ScheduleBlockRepository {
        private final Map<String, ScheduleBlock> store = new LinkedHashMap<>();

        @Override
        public Optional<ScheduleBlock> findById(String id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<ScheduleBlock> findActiveOverlappingRange(long s, long e) {
            return new ArrayList<>(store.values());
        }

        @Override
        public void save(ScheduleBlock block) {
            store.put(block.id(), block);
        }
    }
}
