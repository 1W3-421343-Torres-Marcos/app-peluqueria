package com.metanoia.app.data.db;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.metanoia.app.data.db.entity.AppointmentEntity;
import com.metanoia.app.data.db.entity.AppointmentItemEntity;
import com.metanoia.app.data.db.entity.ClientEntity;
import com.metanoia.app.data.db.entity.ProfessionalEntity;
import com.metanoia.app.data.db.entity.SalonServiceEntity;
import com.metanoia.app.data.db.migration.Migration_1_2;
import com.metanoia.app.data.repository.AppointmentRepositoryImpl;
import com.metanoia.app.data.repository.AuditLogRepositoryImpl;
import com.metanoia.app.data.repository.CashRepositoryImpl;
import com.metanoia.app.data.repository.CommissionRepositoryImpl;
import com.metanoia.app.data.repository.ProfessionalRepositoryImpl;
import com.metanoia.app.data.repository.SettingsRepositoryImpl;
import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.model.Appointment;
import com.metanoia.domain.model.CommissionSettlement;
import com.metanoia.domain.model.PaymentMethod;
import com.metanoia.domain.repository.CommissionRepository;
import com.metanoia.domain.usecase.SettleCommissionUseCase;
import com.metanoia.domain.usecase.SettleCommissionUseCase.Request;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class CommissionSettlementAtomicityTest {

    private AppDatabase db;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();

        Migration_1_2.seedDefaults(db.getOpenHelper().getWritableDatabase());
        db.salonServiceDao().upsert(new SalonServiceEntity(
                "svc-1", "Corte", "corte", "Corte", "corte",
                1_000_000L, 30, false, true, 0, 1_000L, 1_000L));
        db.professionalDao().upsertProfessional(new ProfessionalEntity(
                "pro-1", "Ana", "", "", 4_000, true, 1_000L, 1_000L));
        db.clientDao().upsert(new ClientEntity(
                "cli-1", "Sofía", "sofia", "5491122334455",
                null, null, null, "", 0, true, 1_000L, 1_000L));

        db.appointmentDao().saveAppointmentWithItems(
                new AppointmentEntity(
                        "apt-1", "cli-1", "pro-1", 20_500L, 540, 570,
                        "PAID", 0L, "", null, null, 1_000L, 1_000L, "Dueño"),
                Collections.singletonList(new AppointmentItemEntity(
                        "item-1", "apt-1", "svc-1", "Corte",
                        1_000_000L, 30, 4_000, null, 0)));
    }

    @After
    public void tearDown() {
        db.close();
    }

    @Test
    public void failedSettlementInsert_rollsBackCashOutflow() {
        CommissionRepository realComm = new CommissionRepositoryImpl(db);
        CommissionRepository failingComm = new CommissionRepository() {
            @Override
            public List<CommissionSettlement> findSettlementsByProfessional(String professionalId) {
                return realComm.findSettlementsByProfessional(professionalId);
            }

            @Override
            public boolean hasOverlappingSettlement(
                    String professionalId, long fromDay, long toDay) {
                return realComm.hasOverlappingSettlement(professionalId, fromDay, toDay);
            }

            @Override
            public List<Appointment> findPaidAppointmentsInPreviousSettledPeriods(
                    String professionalId, long beforeDayEpoch) {
                return realComm.findPaidAppointmentsInPreviousSettledPeriods(
                        professionalId, beforeDayEpoch);
            }

            @Override
            public void saveSettlement(CommissionSettlement settlement) {
                throw new IllegalStateException("Fallo simulado al persistir liquidación");
            }

            @Override
            public void runInTransaction(Runnable action) {
                realComm.runInTransaction(action);
            }
        };

        Clock clock = Clock.systemArgentina();
        SettleCommissionUseCase useCase = new SettleCommissionUseCase(
                failingComm,
                new AppointmentRepositoryImpl(db, db.appointmentDao(), db.recurrenceRuleDao()),
                new ProfessionalRepositoryImpl(db.professionalDao(), db.appointmentDao()),
                new CashRepositoryImpl(db),
                new SettingsRepositoryImpl(db.settingsDao(), clock),
                new AuditLogRepositoryImpl(db.auditLogDao()),
                clock,
                IdGenerator.uuid()
        );

        try {
            useCase.execute(new Request("pro-1", 20_500L, 20_506L, PaymentMethod.CASH));
        } catch (IllegalStateException expected) {
            // Rollback esperado en Room
        }

        assertEquals(0, db.cashDao().findByDaySync(clock.todayEpochDay()).size());
        assertEquals(0, db.commissionDao().findByProfessionalWithEntriesSync("pro-1").size());
    }
}
