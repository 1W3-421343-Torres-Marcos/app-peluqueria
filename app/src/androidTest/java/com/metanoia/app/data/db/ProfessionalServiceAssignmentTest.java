package com.metanoia.app.data.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.metanoia.app.data.db.entity.ProfessionalEntity;
import com.metanoia.app.data.db.entity.ProfessionalServiceCrossRef;
import com.metanoia.app.data.db.entity.SalonServiceEntity;
import com.metanoia.app.data.db.entity.WorkScheduleEntity;
import com.metanoia.app.data.db.migration.Migration_1_2;
import com.metanoia.app.data.db.pojo.ProfessionalWithDetails;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;

@RunWith(AndroidJUnit4.class)
public class ProfessionalServiceAssignmentTest {

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
        db.professionalDao().saveComplete(
                new ProfessionalEntity(
                        "pro-1", "Ana", "", "", 4_000, true, 1_000L, 1_000L),
                Collections.singletonList(
                        new WorkScheduleEntity("pro-1", 1, 540, 1140)),
                Collections.emptyList(),
                Collections.emptyList());
    }

    @After
    public void tearDown() {
        db.close();
    }

    @Test
    public void addFirstService_preservesExistingProfessionalAndSchedule() {
        db.professionalDao().saveComplete(
                new ProfessionalEntity(
                        "pro-1", "Ana", "", "", 4_000, true, 1_000L, 2_000L),
                Collections.singletonList(
                        new WorkScheduleEntity("pro-1", 1, 540, 1140)),
                Collections.singletonList(
                        new ProfessionalServiceCrossRef("pro-1", "svc-1")),
                Collections.emptyList());

        ProfessionalWithDetails saved =
                db.professionalDao().findByIdWithDetailsSync("pro-1");

        assertEquals("Ana", saved.professional.name);
        assertEquals(1, saved.workSchedules.size());
        assertEquals(540, saved.workSchedules.get(0).startMin);
        assertEquals(1140, saved.workSchedules.get(0).endMin);
        assertEquals(1, saved.serviceRefs.size());
        assertTrue(saved.serviceRefs.stream()
                .anyMatch(ref -> "svc-1".equals(ref.serviceId)));
    }
}
