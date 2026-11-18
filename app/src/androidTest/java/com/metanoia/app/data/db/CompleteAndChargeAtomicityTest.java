package com.metanoia.app.data.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
import com.metanoia.app.data.db.pojo.AppointmentWithItems;
import com.metanoia.app.data.repository.AppointmentRepositoryImpl;
import com.metanoia.app.data.repository.AuditLogRepositoryImpl;
import com.metanoia.app.data.repository.CashRepositoryImpl;
import com.metanoia.app.data.repository.ProfessionalRepositoryImpl;
import com.metanoia.app.data.repository.SettingsRepositoryImpl;
import com.metanoia.domain.common.Clock;
import com.metanoia.domain.common.IdGenerator;
import com.metanoia.domain.model.CashClosing;
import com.metanoia.domain.model.CashMovement;
import com.metanoia.domain.model.Money;
import com.metanoia.domain.model.PaymentMethod;
import com.metanoia.domain.repository.CashRepository;
import com.metanoia.domain.rules.CashPaymentSupport.PaymentSplit;
import com.metanoia.domain.usecase.CompleteAndChargeUseCase;
import com.metanoia.domain.usecase.CompleteAndChargeUseCase.Request;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RunWith(AndroidJUnit4.class)
public class CompleteAndChargeAtomicityTest {

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
                1_500_000L, 30, false, true, 0, 1_000L, 1_000L));
        db.professionalDao().upsertProfessional(new ProfessionalEntity(
                "pro-1", "Ana", "", "", 4_000, true, 1_000L, 1_000L));
        db.clientDao().upsert(new ClientEntity(
                "cli-1", "Sofía", "sofia", "5491122334455",
                null, null, null, "", 0, true, 1_000L, 1_000L));

        db.appointmentDao().saveAppointmentWithItems(
                new AppointmentEntity(
                        "apt-1", "cli-1", "pro-1", 20_500L, 540, 570,
                        "CONFIRMED", 0L, "", null, null, 1_000L, 1_000L, "Dueño"),
                Collections.singletonList(new AppointmentItemEntity(
                        "item-1", "apt-1", "svc-1", "Corte",
                        1_500_000L, 30, null, null, 0)));
    }

    @After
    public void tearDown() {
        db.close();
    }

    @Test
    public void failedChargeSave_rollsBackCompletionAndCommissionSnapshot() {
        CashRepository realCash = new CashRepositoryImpl(db);
        CashRepository failingOnSaveCash = new CashRepository() {
            @Override
            public Optional<CashMovement> findMovementById(String id) {
                return realCash.findMovementById(id);
            }

            @Override
            public List<CashMovement> findMovementsByDay(long dayEpoch) {
                return realCash.findMovementsByDay(dayEpoch);
            }

            @Override
            public List<CashMovement> findMovementsByAppointmentId(String appointmentId) {
                return realCash.findMovementsByAppointmentId(appointmentId);
            }

            @Override
            public List<CashMovement> findMovementsByClientId(String clientId) {
                return realCash.findMovementsByClientId(clientId);
            }

            @Override
            public boolean existsReversalForMovement(String movementId) {
                return realCash.existsReversalForMovement(movementId);
            }

            @Override
            public boolean isDayClosed(long dayEpoch) {
                return realCash.isDayClosed(dayEpoch);
            }

            @Override
            public Optional<CashClosing> findClosingByDay(long dayEpoch) {
                return realCash.findClosingByDay(dayEpoch);
            }

            @Override
            public Optional<Long> findLatestUnclosedDayWithMovementsBefore(long dayEpoch) {
                return realCash.findLatestUnclosedDayWithMovementsBefore(dayEpoch);
            }

            @Override
            public void saveMovement(CashMovement movement) {
                throw new IllegalStateException("Fallo simulado al guardar movimiento");
            }

            @Override
            public void saveMovements(List<CashMovement> movements) {
                throw new IllegalStateException("Fallo simulado al guardar movimientos");
            }

            @Override
            public void saveClosingAndLockDay(CashClosing closing) {
                realCash.saveClosingAndLockDay(closing);
            }

            @Override
            public void runInTransaction(Runnable action) {
                realCash.runInTransaction(action);
            }
        };

        Clock clock = Clock.systemArgentina();
        CompleteAndChargeUseCase useCase = new CompleteAndChargeUseCase(
                new AppointmentRepositoryImpl(db.appointmentDao(), db.recurrenceRuleDao()),
                new ProfessionalRepositoryImpl(db.professionalDao(), db.appointmentDao()),
                failingOnSaveCash,
                new SettingsRepositoryImpl(db.settingsDao(), clock),
                new AuditLogRepositoryImpl(db.auditLogDao()),
                clock,
                IdGenerator.uuid()
        );

        try {
            useCase.execute(new Request(
                    "apt-1",
                    null,
                    null,
                    Collections.singletonList(
                            new PaymentSplit(PaymentMethod.CASH, Money.ofCents(1_500_000L)))));
        } catch (IllegalStateException expected) {
            // Excepción esperada que dispara rollback en Room
        }

        AppointmentWithItems after = db.appointmentDao().findByIdWithItemsSync("apt-1");
        assertEquals("CONFIRMED", after.appointment.status);
        assertNull(after.items.get(0).commissionBasisPointsSnapshot);
        assertEquals(0, db.cashDao().findByAppointmentIdSync("apt-1").size());
    }
}
