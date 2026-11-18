package com.metanoia.app.data.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.metanoia.app.data.db.dao.AuditLogDao;
import com.metanoia.app.data.db.dao.SalonServiceDao;
import com.metanoia.app.data.db.dao.SettingsDao;
import com.metanoia.app.data.db.entity.AuditLogEntity;
import com.metanoia.app.data.db.entity.SalonHoursEntity;
import com.metanoia.app.data.db.entity.SalonServiceEntity;
import com.metanoia.app.data.db.entity.SettingsEntity;
import com.metanoia.app.data.db.migration.Migration_1_2;
import com.metanoia.app.data.db.migration.Migration_2_3;

/**
 * Base de datos local Room de Metanoia con migraciones versionadas (sin borrado destructivo).
 */
@Database(
        entities = {
                AuditLogEntity.class,
                SettingsEntity.class,
                SalonHoursEntity.class,
                SalonServiceEntity.class
        },
        version = 3,
        exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {

    public static final String DATABASE_NAME = "metanoia.db";

    public abstract AuditLogDao auditLogDao();

    public abstract SettingsDao settingsDao();

    public abstract SalonServiceDao salonServiceDao();

    public static AppDatabase create(Context context) {
        return Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        DATABASE_NAME
                )
                .addMigrations(
                        Migration_1_2.INSTANCE,
                        Migration_2_3.INSTANCE
                )
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
