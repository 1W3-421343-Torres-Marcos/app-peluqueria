package com.metanoia.app.data.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.metanoia.app.data.db.dao.AppointmentDao;
import com.metanoia.app.data.db.entity.AppointmentEntity;
import com.metanoia.app.data.db.entity.AppointmentItemEntity;
import com.metanoia.app.data.db.entity.ClientEntity;
import com.metanoia.app.data.db.entity.ProfessionalEntity;
import com.metanoia.app.data.db.entity.SalonServiceEntity;
import com.metanoia.app.data.db.pojo.AppointmentWithItems;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class AppointmentDaoTest {

    private AppDatabase db;
    private AppointmentDao appointmentDao;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        appointmentDao = db.appointmentDao();

        db.salonServiceDao().upsert(new SalonServiceEntity(
                "svc-1", "Corte", "corte", "Corte", "corte",
                1_500_000L, 30, false, true, 0, 1000L, 1000L));
        db.professionalDao().upsertProfessional(new ProfessionalEntity(
                "pro-1", "Ana", "", "", 4000, true, 1000L, 1000L));
        db.clientDao().upsert(new ClientEntity(
                "cli-1", "Sofía", "sofia", "5491122334455",
                null, null, null, "", 0, true, 1000L, 1000L));
    }

    @After
    public void tearDown() {
        db.close();
    }

    @Test
    public void findByDayRangeSync_returnsOnlyAppointmentsInsideRange() {
        insertAppointment("apt-in-1", 20_500L, 540, 570);
        insertAppointment("apt-in-2", 20_502L, 600, 630);
        insertAppointment("apt-out", 20_510L, 600, 630);

        List<AppointmentWithItems> range = appointmentDao.findByDayRangeSync(20_500L, 20_506L);

        assertEquals(2, range.size());
        assertEquals("apt-in-1", range.get(0).appointment.id);
        assertEquals("apt-in-2", range.get(1).appointment.id);
        assertEquals(1, range.get(0).items.size());
        assertTrue(appointmentDao.existsFutureOccupyingForProfessionalSync("pro-1", 20_500L));
    }

    private void insertAppointment(String id, long day, int startMin, int endMin) {
        AppointmentEntity entity = new AppointmentEntity(
                id, "cli-1", "pro-1", day, startMin, endMin,
                "CONFIRMED", 0L, "", null, null, 1000L, 1000L, "Dueño");
        AppointmentItemEntity item = new AppointmentItemEntity(
                "item-" + id, id, "svc-1", "Corte",
                1_500_000L, 30, null, null, 0);
        appointmentDao.saveAppointmentWithItems(entity, Collections.singletonList(item));
    }
}
