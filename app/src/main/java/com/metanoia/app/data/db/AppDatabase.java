package com.metanoia.app.data.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.metanoia.app.data.db.dao.AppointmentDao;
import com.metanoia.app.data.db.dao.AuditLogDao;
import com.metanoia.app.data.db.dao.ClientDao;
import com.metanoia.app.data.db.dao.ProfessionalDao;
import com.metanoia.app.data.db.dao.RecurrenceRuleDao;
import com.metanoia.app.data.db.dao.SalonServiceDao;
import com.metanoia.app.data.db.dao.ScheduleBlockDao;
import com.metanoia.app.data.db.dao.SettingsDao;
import com.metanoia.app.data.db.entity.AppointmentEntity;
import com.metanoia.app.data.db.entity.AppointmentItemEntity;
import com.metanoia.app.data.db.entity.AuditLogEntity;
import com.metanoia.app.data.db.entity.ClientEntity;
import com.metanoia.app.data.db.entity.ProfessionalDayOffEntity;
import com.metanoia.app.data.db.entity.ProfessionalEntity;
import com.metanoia.app.data.db.entity.ProfessionalServiceCrossRef;
import com.metanoia.app.data.db.entity.RecurrenceRuleEntity;
import com.metanoia.app.data.db.entity.SalonHoursEntity;
import com.metanoia.app.data.db.entity.SalonServiceEntity;
import com.metanoia.app.data.db.entity.ScheduleBlockEntity;
import com.metanoia.app.data.db.entity.SettingsEntity;
import com.metanoia.app.data.db.entity.WorkScheduleEntity;
import com.metanoia.app.data.db.migration.Migration_1_2;
import com.metanoia.app.data.db.migration.Migration_2_3;
import com.metanoia.app.data.db.migration.Migration_3_4;
import com.metanoia.app.data.db.migration.Migration_4_5;
import com.metanoia.app.data.db.migration.Migration_5_6;

/**
 * Base de datos local Room de Metanoia con migraciones versionadas (sin borrado destructivo).
 */
@Database(
        entities = {
                AuditLogEntity.class,
                SettingsEntity.class,
                SalonHoursEntity.class,
                SalonServiceEntity.class,
                ProfessionalEntity.class,
                WorkScheduleEntity.class,
                ProfessionalDayOffEntity.class,
                ProfessionalServiceCrossRef.class,
                ClientEntity.class,
                RecurrenceRuleEntity.class,
                AppointmentEntity.class,
                AppointmentItemEntity.class,
                ScheduleBlockEntity.class
        },
        version = 6,
        exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {

    public static final String DATABASE_NAME = "metanoia.db";

    public abstract AuditLogDao auditLogDao();

    public abstract SettingsDao settingsDao();

    public abstract SalonServiceDao salonServiceDao();

    public abstract ProfessionalDao professionalDao();

    public abstract ClientDao clientDao();

    public abstract AppointmentDao appointmentDao();

    public abstract ScheduleBlockDao scheduleBlockDao();

    public abstract RecurrenceRuleDao recurrenceRuleDao();

    public static Migration[] allMigrations() {
        return new Migration[]{
                Migration_1_2.INSTANCE,
                Migration_2_3.INSTANCE,
                Migration_3_4.INSTANCE,
                Migration_4_5.INSTANCE,
                Migration_5_6.INSTANCE
        };
    }

    public static AppDatabase create(Context context) {
        return Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        DATABASE_NAME
                )
                .addMigrations(allMigrations())
                .addCallback(new Callback() {
                    @Override
                    public void onCreate(@NonNull SupportSQLiteDatabase db) {
                        super.onCreate(db);
                        Migration_1_2.seedDefaults(db);
                    }
                })
                .build();
    }
}
