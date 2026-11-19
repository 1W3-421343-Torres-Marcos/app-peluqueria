package com.metanoia.app.data.db;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteStatement;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.metanoia.app.data.db.entity.ClientEntity;
import com.metanoia.app.data.db.entity.ProfessionalEntity;
import com.metanoia.app.data.db.entity.SalonServiceEntity;
import com.metanoia.app.data.db.pojo.AppointmentWithItems;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;
import java.util.List;

/**
 * Verifica que las consultas de día y semana de la agenda respondan por debajo de 500 ms incluso
 * sobre una base poblada con 50.000 turnos.
 */
@RunWith(AndroidJUnit4.class)
public class AgendaStartupPerformanceTest {

    private AppDatabase db;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        db.salonServiceDao().upsert(new SalonServiceEntity(
                "svc-cut", "Corte", "corte", "Peluquería", "peluqueria",
                1_000_00L, 30, false, true, 0, 1000L, 1000L));
        db.professionalDao().upsertProfessional(new ProfessionalEntity(
                "pro-1", "Ana", "", "", 5000, true, 1000L, 1000L));
        db.clientDao().upsert(new ClientEntity(
                "cli-1", "Sofía", "sofia", "5491111111111",
                null, null, null, "", 0, true, 1000L, 1000L));
    }

    @After
    public void tearDown() {
        db.close();
    }

    @Test
    public void dayAndWeekQueries_withFiftyThousandAppointments_executeUnderFiveHundredMillis() {
        long baseDay = LocalDate.of(2025, 1, 1).toEpochDay();
        seedFiftyThousandAppointments(baseDay);

        long targetDay = baseDay + 180L;
        long startNs = System.nanoTime();
        List<AppointmentWithItems> dayResult = db.appointmentDao()
                .findByDayRangeSync(targetDay, targetDay);
        List<AppointmentWithItems> weekResult = db.appointmentDao()
                .findByDayRangeSync(targetDay, targetDay + 6L);
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;

        assertFalse(dayResult.isEmpty());
        assertFalse(weekResult.isEmpty());
        assertTrue("Consultas de agenda superaron 500 ms: " + elapsedMs + " ms", elapsedMs < 500L);
    }

    private void seedFiftyThousandAppointments(long baseDay) {
        db.runInTransaction(() -> {
            SupportSQLiteDatabase sql = db.getOpenHelper().getWritableDatabase();
            SupportSQLiteStatement aptStmt = sql.compileStatement(
                    "INSERT INTO appointments (id, client_id, professional_id, day, start_min, end_min, "
                            + "status, adjustment_cents, notes, recurrence_rule_id, cancel_reason, "
                            + "created_at, updated_at, created_by) "
                            + "VALUES (?, 'cli-1', 'pro-1', ?, ?, ?, 'CONFIRMED', 0, '', NULL, NULL, "
                            + "1000, 1000, 'Dueño')");
            SupportSQLiteStatement itemStmt = sql.compileStatement(
                    "INSERT INTO appointment_items (id, appointment_id, service_id, "
                            + "service_name_snapshot, price_cents_snapshot, duration_min_snapshot, "
                            + "commission_bp_snapshot, color_formula_id, position) "
                            + "VALUES (?, ?, 'svc-cut', 'Corte', 100000, 30, 5000, NULL, 0)");
            for (int i = 0; i < 50_000; i++) {
                String id = "bulk-agenda-" + i;
                long day = baseDay + (i % 365);
                int start = 540 + (i % 10) * 30;
                aptStmt.bindString(1, id);
                aptStmt.bindLong(2, day);
                aptStmt.bindLong(3, start);
                aptStmt.bindLong(4, start + 30);
                aptStmt.executeInsert();

                itemStmt.bindString(1, "item-agenda-" + i);
                itemStmt.bindString(2, id);
                itemStmt.executeInsert();
            }
        });
    }
}
