package com.metanoia.app.data.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteStatement;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.metanoia.app.data.db.dao.ReportsDao;
import com.metanoia.app.data.db.entity.AppointmentEntity;
import com.metanoia.app.data.db.entity.AppointmentItemEntity;
import com.metanoia.app.data.db.entity.CashMovementEntity;
import com.metanoia.app.data.db.entity.ClientEntity;
import com.metanoia.app.data.db.entity.CommissionSettlementEntity;
import com.metanoia.app.data.db.entity.ProfessionalEntity;
import com.metanoia.app.data.db.entity.SalonServiceEntity;
import com.metanoia.app.data.db.projection.FrequentClientProjection;
import com.metanoia.app.data.db.projection.IncomeByDayProjection;
import com.metanoia.app.data.db.projection.IncomeByMethodProjection;
import com.metanoia.app.data.db.projection.OccupiedSlotProjection;
import com.metanoia.app.data.db.projection.ProfessionalPerformanceProjection;
import com.metanoia.app.data.db.projection.ReceivableClientProjection;
import com.metanoia.app.data.db.projection.ReceivableTotalsProjection;
import com.metanoia.app.data.db.projection.TopServiceCategoryProjection;
import com.metanoia.app.data.db.projection.TopServiceProjection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class ReportsDaoTest {

    private AppDatabase db;
    private ReportsDao reportsDao;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        reportsDao = db.reportsDao();

        db.salonServiceDao().upsert(new SalonServiceEntity(
                "svc-cut", "Corte", "corte", "Peluquería", "peluqueria",
                1_000_00L, 30, false, true, 0, 1000L, 1000L));
        db.salonServiceDao().upsert(new SalonServiceEntity(
                "svc-color", "Color", "color", "Coloración", "coloracion",
                2_000_00L, 60, true, true, 1, 1000L, 1000L));

        db.professionalDao().upsertProfessional(new ProfessionalEntity(
                "pro-1", "Ana", "", "", 5000, true, 1000L, 1000L));
        db.professionalDao().upsertProfessional(new ProfessionalEntity(
                "pro-2", "Luis", "", "", 4000, true, 1000L, 1000L));

        db.clientDao().upsert(new ClientEntity(
                "cli-1", "Sofía", "sofia", "5491111111111",
                null, null, null, "", 0, true, 1000L, 1000L));
        db.clientDao().upsert(new ClientEntity(
                "cli-2", "Marco", "marco", "5491122222222",
                null, null, null, "", 0, true, 1000L, 1000L));
    }

    @After
    public void tearDown() {
        db.close();
    }

    @Test
    public void queries_aggregateAllSixReportsAccurately() {
        long d1 = LocalDate.of(2026, 2, 2).toEpochDay(); // Lunes
        long d2 = LocalDate.of(2026, 2, 3).toEpochDay(); // Martes
        long oldDay = LocalDate.of(2026, 1, 10).toEpochDay();

        // Turno 1: PAID, Ana, Sofía, d1, 09:00-10:00, Color (2000) con recargo +500 => total 2500, com 50% = 1250
        insertAppointment("apt-1", "cli-1", "pro-1", d1, 540, 600, "PAID", 500_00L,
                "svc-color", "Color", 2_000_00L, 5000);
        // Turno 2: COMPLETED con saldo parcial, Ana, Sofía, d2, 09:30-10:30, Corte (1000), pagó 400 => saldo 600
        insertAppointment("apt-2", "cli-1", "pro-1", d2, 570, 630, "COMPLETED", 0L,
                "svc-cut", "Corte", 1_000_00L, 5000);
        // Turno 3: NO_SHOW, Ana, Marco, d2
        insertAppointment("apt-3", "cli-2", "pro-1", d2, 660, 690, "NO_SHOW", 0L,
                "svc-cut", "Corte", 1_000_00L, 5000);
        // Turno 4: PAID fuera de rango (histórico) vs COMPLETED anterior por cobrar (Luis)
        insertAppointment("apt-old-rec", "cli-2", "pro-2", oldDay, 600, 630, "COMPLETED", 0L,
                "svc-cut", "Corte", 1_000_00L, 4000);

        // Movimientos de caja en [d1, d2]
        db.cashDao().insertMovement(new CashMovementEntity(
                "cm-1", d1, 100L, "APPOINTMENT_PAYMENT", "CASH",
                2_500_00L, "Pago apt-1", "apt-1", "cli-1", null, null, null, "Dueño", null));
        db.cashDao().insertMovement(new CashMovementEntity(
                "cm-2", d2, 200L, "APPOINTMENT_PAYMENT", "TRANSFER",
                400_00L, "Seña apt-2", "apt-2", "cli-1", null, null, null, "Dueño", null));
        db.cashDao().insertMovement(new CashMovementEntity(
                "cm-3", d2, 300L, "OTHER_INCOME", "CASH",
                500_00L, "Otro", null, null, null, null, null, "Dueño", null));
        db.cashDao().insertMovement(new CashMovementEntity(
                "cm-rev", d2, 350L, "REVERSAL", "CASH",
                500_00L, "Reverso Otro", null, null, null, null, "cm-3", "Dueño", null));
        db.cashDao().insertMovement(new CashMovementEntity(
                "cm-exp", d2, 400L, "EXPENSE", "CASH",
                200_00L, "Insumo", null, null, null, null, null, "Dueño", null));
        db.cashDao().insertMovement(new CashMovementEntity(
                "cm-comm", d2, 500L, "COMMISSION_PAYOUT", "CASH",
                1_250_00L, "Comisión Ana", null, null, null, null, null, "Dueño", null));
        db.commissionDao().insertSettlement(new CommissionSettlementEntity(
                "set-1", "pro-1", d1, d1, 2_500_00L, 1_250_00L, "cm-comm", 500L, "Dueño"));

        // 1. Ingresos global vs por profesional
        List<IncomeByDayProjection> byDayAll = reportsDao.queryIncomeByDaySync(d1, d2, null);
        assertEquals(2, byDayAll.size());
        assertEquals(2_500_00L, byDayAll.get(0).incomeCents);
        assertEquals(0L, byDayAll.get(0).expenseCents);
        assertEquals(400_00L, byDayAll.get(1).incomeCents);
        assertEquals(1_450_00L, byDayAll.get(1).expenseCents);

        List<IncomeByMethodProjection> byMethodPro1 = reportsDao.queryIncomeByMethodSync(d1, d2, "pro-1");
        assertEquals(2, byMethodPro1.size());
        assertEquals("CASH", byMethodPro1.get(0).method);
        assertEquals(2_500_00L, byMethodPro1.get(0).incomeCents);
        assertEquals(1_250_00L, byMethodPro1.get(0).expenseCents);

        // 2. Servicios top y categorías
        List<TopServiceProjection> topServices = reportsDao.queryTopServicesSync(d1, d2, null);
        assertEquals(2, topServices.size());
        List<TopServiceCategoryProjection> topCats = reportsDao.queryTopServiceCategoriesSync(d1, d2, null);
        assertEquals(2, topCats.size());
        assertEquals("Coloración", topCats.get(0).category);
        assertEquals(2_000_00L, topCats.get(0).revenueCents);

        // 3. Clientes frecuentes
        List<FrequentClientProjection> freq = reportsDao.queryFrequentClientsByVisitsSync(d1, d2, null, 10);
        assertEquals(1, freq.size());
        assertEquals("cli-1", freq.get(0).clientId);
        assertEquals(2, freq.get(0).visitCount);
        assertEquals(3_500_00L, freq.get(0).totalSpentCents);

        // 4. Ocupación (no cuenta NO_SHOW; apt-2 09:30-10:30 aporta 30m en hora 9 y 30m en hora 10)
        List<OccupiedSlotProjection> occ = reportsDao.queryOccupiedSlotMinutesSync(d1, d2, "pro-1");
        assertEquals(3, occ.size());
        assertEquals(1, occ.get(0).dayOfWeek);
        assertEquals(9, occ.get(0).slotHour);
        assertEquals(60L, occ.get(0).occupiedMinutes);
        assertEquals(2, occ.get(1).dayOfWeek);
        assertEquals(9, occ.get(1).slotHour);
        assertEquals(30L, occ.get(1).occupiedMinutes);
        assertEquals(2, occ.get(2).dayOfWeek);
        assertEquals(10, occ.get(2).slotHour);
        assertEquals(30L, occ.get(2).occupiedMinutes);

        // 5. Rendimiento por profesional
        List<ProfessionalPerformanceProjection> perf = reportsDao.queryProfessionalPerformanceSync(d1, d2, null);
        assertEquals(2, perf.size());
        ProfessionalPerformanceProjection anaPerf = perf.get(0);
        assertEquals("pro-1", anaPerf.professionalId);
        assertEquals(2, anaPerf.completedCount);
        assertEquals(3_500_00L, anaPerf.billedCents);
        assertEquals(1, anaPerf.noShowCount);
        assertEquals(1_250_00L, anaPerf.commissionCents);

        // 6. Por cobrar en rango vs histórico
        List<ReceivableClientProjection> rec = reportsDao.queryReceivablesByClientSync(d1, d2, null);
        assertEquals(1, rec.size());
        assertEquals(600_00L, rec.get(0).balanceCents);
        ReceivableTotalsProjection hist = reportsDao.queryHistoricalReceivablesTotalsSync(null);
        assertEquals(1_600_00L, hist.totalBalanceCents);
        assertEquals(2, hist.appointmentCount);
    }

    @Test
    public void queries_executeUnderOneSecondWithFiftyThousandAppointments() {
        long baseDay = LocalDate.of(2025, 1, 1).toEpochDay();
        db.runInTransaction(() -> {
            SupportSQLiteDatabase sql = db.getOpenHelper().getWritableDatabase();
            SupportSQLiteStatement aptStmt = sql.compileStatement(
                    "INSERT INTO appointments (id, client_id, professional_id, day, start_min, end_min, "
                            + "status, adjustment_cents, notes, recurrence_rule_id, cancel_reason, "
                            + "created_at, updated_at, created_by) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, 0, '', NULL, NULL, 1000, 1000, 'Dueño')");
            SupportSQLiteStatement itemStmt = sql.compileStatement(
                    "INSERT INTO appointment_items (id, appointment_id, service_id, "
                            + "service_name_snapshot, price_cents_snapshot, duration_min_snapshot, "
                            + "commission_bp_snapshot, color_formula_id, position) "
                            + "VALUES (?, ?, 'svc-cut', 'Corte', 100000, 30, 5000, NULL, 0)");
            for (int i = 0; i < 50_000; i++) {
                String id = "bulk-" + i;
                long day = baseDay + (i % 365);
                String proId = (i % 2 == 0) ? "pro-1" : "pro-2";
                String cliId = (i % 2 == 0) ? "cli-1" : "cli-2";
                String status = (i % 10 == 0) ? "COMPLETED" : "PAID";
                int start = 540 + (i % 10) * 30;
                aptStmt.bindString(1, id);
                aptStmt.bindString(2, cliId);
                aptStmt.bindString(3, proId);
                aptStmt.bindLong(4, day);
                aptStmt.bindLong(5, start);
                aptStmt.bindLong(6, start + 30);
                aptStmt.bindString(7, status);
                aptStmt.executeInsert();

                itemStmt.bindString(1, "item-" + i);
                itemStmt.bindString(2, id);
                itemStmt.executeInsert();
            }
        });

        long fromDay = baseDay + 300;
        long toDay = baseDay + 330;
        long startNs = System.nanoTime();
        List<TopServiceProjection> top = reportsDao.queryTopServicesSync(fromDay, toDay, null);
        List<FrequentClientProjection> clients = reportsDao.queryFrequentClientsByVisitsSync(
                fromDay, toDay, null, 20);
        List<OccupiedSlotProjection> occ = reportsDao.queryOccupiedSlotMinutesSync(
                fromDay, toDay, null);
        List<ProfessionalPerformanceProjection> perf = reportsDao.queryProfessionalPerformanceSync(
                fromDay, toDay, null);
        List<ReceivableClientProjection> rec = reportsDao.queryReceivablesByClientSync(
                fromDay, toDay, null);
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;

        assertEquals(1, top.size());
        assertEquals(2, clients.size());
        assertTrue(!occ.isEmpty());
        assertEquals(2, perf.size());
        assertEquals(2, rec.size());
        assertTrue("Agregaciones superaron 1000 ms: " + elapsedMs + " ms", elapsedMs < 1_000L);
    }

    private void insertAppointment(
            String id,
            String clientId,
            String proId,
            long day,
            int startMin,
            int endMin,
            String status,
            long adjustmentCents,
            String svcId,
            String svcName,
            long priceCents,
            int commBp
    ) {
        AppointmentEntity entity = new AppointmentEntity(
                id, clientId, proId, day, startMin, endMin, status, adjustmentCents,
                "", null, null, 1000L, 1000L, "Dueño");
        AppointmentItemEntity item = new AppointmentItemEntity(
                "item-" + id, id, svcId, svcName, priceCents, endMin - startMin, commBp, null, 0);
        db.appointmentDao().saveAppointmentWithItems(entity, Collections.singletonList(item));
    }
}
