package com.metanoia.app.ui.debug;

import androidx.annotation.NonNull;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteStatement;

import com.metanoia.app.AppContainer;
import com.metanoia.domain.model.AppointmentStatus;
import com.metanoia.domain.model.Money;
import com.metanoia.domain.model.Professional;
import com.metanoia.domain.model.SalonService;
import com.metanoia.domain.model.Settings;
import com.metanoia.domain.model.TimeRange;
import com.metanoia.domain.rules.TextNormalizer;
import com.metanoia.domain.usecase.SaveProfessionalUseCase;
import com.metanoia.domain.usecase.SaveSalonServiceUseCase;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Generador por lotes en SQLite para pruebas de volumen (hasta 5.000 clientes y 50.000 turnos).
 */
public final class DebugSeeder {

    public static final int DEFAULT_CLIENT_COUNT = 5_000;
    public static final int DEFAULT_APPOINTMENT_COUNT = 50_000;

    private static final String[] FIRST_NAMES = {
            "Sofía", "Valentina", "Camila", "Lucía", "Martina",
            "Paula", "Agustina", "Florencia", "Julieta", "Micaela"
    };
    private static final String[] LAST_NAMES = {
            "García", "Rodríguez", "González", "Fernández", "López",
            "Martínez", "Sánchez", "Pérez", "Gómez", "Díaz"
    };

    private DebugSeeder() {
    }

    public static final class SeedResult {
        public final int clientsCount;
        public final int appointmentsCount;
        public final long elapsedMs;

        public SeedResult(int clientsCount, int appointmentsCount, long elapsedMs) {
            this.clientsCount = clientsCount;
            this.appointmentsCount = appointmentsCount;
            this.elapsedMs = elapsedMs;
        }
    }

    @NonNull
    public static SeedResult seedDefaultVolume(@NonNull AppContainer container) {
        return seedVolume(container, DEFAULT_CLIENT_COUNT, DEFAULT_APPOINTMENT_COUNT);
    }

    @NonNull
    public static SeedResult seedVolume(
            @NonNull AppContainer container,
            int clientCount,
            int appointmentCount
    ) {
        long startMs = System.currentTimeMillis();
        Settings settings = container.settingsRepository().getSettings();
        List<SalonService> services = ensureSampleServices(container, settings);
        List<Professional> professionals = ensureSampleProfessionals(container, settings, services);
        if (services.isEmpty() || professionals.isEmpty() || clientCount <= 0) {
            return new SeedResult(0, 0, System.currentTimeMillis() - startMs);
        }

        Map<String, SalonService> servicesById = new LinkedHashMap<>();
        for (SalonService service : services) {
            servicesById.put(service.id(), service);
        }
        SalonService fallbackService = services.get(0);

        long now = container.clock().nowMillis();
        long today = container.clock().todayEpochDay();
        String ownerName = settings.currentOperator();
        AppointmentStatus[] statuses = {
                AppointmentStatus.PAID,
                AppointmentStatus.COMPLETED,
                AppointmentStatus.CONFIRMED,
                AppointmentStatus.PENDING
        };

        container.database().runInTransaction(() -> {
            SupportSQLiteDatabase db = container.database().getOpenHelper().getWritableDatabase();
            try (
                    SupportSQLiteStatement clientStmt = db.compileStatement(
                            "INSERT OR IGNORE INTO clients ("
                                    + "id, name, name_normalized, phone, birth_day, birth_month, "
                                    + "birth_year, notes, no_show_count, active, "
                                    + "created_at, updated_at) "
                                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                    SupportSQLiteStatement aptStmt = db.compileStatement(
                            "INSERT OR IGNORE INTO appointments ("
                                    + "id, client_id, professional_id, day, start_min, end_min, "
                                    + "status, adjustment_cents, notes, recurrence_rule_id, "
                                    + "cancel_reason, created_at, updated_at, created_by) "
                                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                    SupportSQLiteStatement itemStmt = db.compileStatement(
                            "INSERT OR IGNORE INTO appointment_items ("
                                    + "id, appointment_id, service_id, service_name_snapshot, "
                                    + "price_cents_snapshot, duration_min_snapshot, "
                                    + "commission_bp_snapshot, color_formula_id, position) "
                                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")
            ) {
                for (int c = 0; c < clientCount; c++) {
                    String clientId = formatId("seed-cli-", c);
                    String first = FIRST_NAMES[c % FIRST_NAMES.length];
                    String last = LAST_NAMES[(c / FIRST_NAMES.length) % LAST_NAMES.length];
                    String fullName = first + " " + last + " " + (c + 1);
                    String normalized = TextNormalizer.normalize(fullName);
                    String phone = String.format(Locale.US, "5491160%06d", c + 1);

                    clientStmt.bindString(1, clientId);
                    clientStmt.bindString(2, fullName);
                    clientStmt.bindString(3, normalized);
                    clientStmt.bindString(4, phone);
                    clientStmt.bindNull(5);
                    clientStmt.bindNull(6);
                    clientStmt.bindNull(7);
                    clientStmt.bindString(8, "");
                    clientStmt.bindLong(9, 0L);
                    clientStmt.bindLong(10, 1L);
                    clientStmt.bindLong(11, now);
                    clientStmt.bindLong(12, now);
                    clientStmt.executeInsert();
                }

                int proCount = professionals.size();
                for (int a = 0; a < appointmentCount; a++) {
                    String aptId = formatId("seed-apt-", a);
                    String clientId = formatId("seed-cli-", a % clientCount);
                    Professional pro = professionals.get(a % proCount);
                    SalonService service = pickServiceForProfessional(
                            pro, servicesById, fallbackService);

                    long day = (today - 180L) + (a % 365L);
                    int slotIndex = (a / proCount) % 10;
                    int startMin = 540 + (slotIndex * 60);
                    int endMin = startMin + service.durationMin();
                    AppointmentStatus status = statuses[a % statuses.length];

                    aptStmt.bindString(1, aptId);
                    aptStmt.bindString(2, clientId);
                    aptStmt.bindString(3, pro.id());
                    aptStmt.bindLong(4, day);
                    aptStmt.bindLong(5, startMin);
                    aptStmt.bindLong(6, endMin);
                    aptStmt.bindString(7, status.name());
                    aptStmt.bindLong(8, 0L);
                    aptStmt.bindString(9, "");
                    aptStmt.bindNull(10);
                    aptStmt.bindNull(11);
                    aptStmt.bindLong(12, now);
                    aptStmt.bindLong(13, now);
                    aptStmt.bindString(14, ownerName);
                    aptStmt.executeInsert();

                    itemStmt.bindString(1, formatId("seed-item-", a));
                    itemStmt.bindString(2, aptId);
                    itemStmt.bindString(3, service.id());
                    itemStmt.bindString(4, service.name());
                    itemStmt.bindLong(5, service.price().cents());
                    itemStmt.bindLong(6, service.durationMin());
                    if (status == AppointmentStatus.COMPLETED || status == AppointmentStatus.PAID) {
                        itemStmt.bindLong(7, pro.commissionBasisPoints());
                    } else {
                        itemStmt.bindNull(7);
                    }
                    itemStmt.bindNull(8);
                    itemStmt.bindLong(9, 0L);
                    itemStmt.executeInsert();
                }
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        });

        long elapsedMs = System.currentTimeMillis() - startMs;
        return new SeedResult(clientCount, appointmentCount, elapsedMs);
    }

    private static List<SalonService> ensureSampleServices(
            @NonNull AppContainer container,
            @NonNull Settings settings
    ) {
        List<SalonService> active = container.salonServiceRepository().findAll(false);
        if (!active.isEmpty()) {
            return active;
        }
        int slot = Math.max(5, settings.slotMinutes());
        container.saveSalonServiceUseCase().execute(new SaveSalonServiceUseCase.Request(
                null, "Corte clásico", "Corte", Money.ofCents(1_500_000L),
                slot * 2, false, true, 0));
        container.saveSalonServiceUseCase().execute(new SaveSalonServiceUseCase.Request(
                null, "Coloración completa", "Color", Money.ofCents(4_200_000L),
                slot * 4, true, true, 1));
        container.saveSalonServiceUseCase().execute(new SaveSalonServiceUseCase.Request(
                null, "Brushing", "Peinado", Money.ofCents(1_200_000L),
                slot * 2, false, true, 2));
        return container.salonServiceRepository().findAll(false);
    }

    private static List<Professional> ensureSampleProfessionals(
            @NonNull AppContainer container,
            @NonNull Settings settings,
            @NonNull List<SalonService> services
    ) {
        List<Professional> active = container.professionalRepository().findAll(false);
        if (!active.isEmpty() || services.isEmpty()) {
            return active;
        }
        Set<String> allServiceIds = new LinkedHashSet<>();
        for (SalonService service : services) {
            allServiceIds.add(service.id());
        }
        List<TimeRange> schedule = settings.generalSchedule();
        SaveProfessionalUseCase useCase = container.saveProfessionalUseCase();
        useCase.execute(new SaveProfessionalUseCase.Request(
                null, "Ana", "", "#2E5EAA", 4_000, true,
                schedule, allServiceIds, Collections.emptySet()));
        useCase.execute(new SaveProfessionalUseCase.Request(
                null, "Lucía", "", "#A65336", 4_500, true,
                schedule, allServiceIds, Collections.emptySet()));
        useCase.execute(new SaveProfessionalUseCase.Request(
                null, "Martín", "", "#4E7A5B", 3_500, true,
                schedule, allServiceIds, Collections.emptySet()));
        return container.professionalRepository().findAll(false);
    }

    @NonNull
    private static SalonService pickServiceForProfessional(
            @NonNull Professional pro,
            @NonNull Map<String, SalonService> servicesById,
            @NonNull SalonService fallbackService
    ) {
        for (String svcId : pro.serviceIds()) {
            SalonService found = servicesById.get(svcId);
            if (found != null) {
                return found;
            }
        }
        return fallbackService;
    }

    @NonNull
    private static String formatId(@NonNull String prefix, int index) {
        return prefix + String.format(Locale.US, "%06d", index);
    }
}
