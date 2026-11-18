package com.metanoia.app.data.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.metanoia.app.data.db.dao.AuditLogDao;
import com.metanoia.app.data.db.entity.AuditLogEntity;

/**
 * Base de datos local Room de Metanoia con migraciones versionadas (sin borrado destructivo).
 */
@Database(
        entities = {AuditLogEntity.class},
        version = 1,
        exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {

    public static final String DATABASE_NAME = "metanoia.db";

    public abstract AuditLogDao auditLogDao();

    public static AppDatabase create(Context context) {
        return Room.databaseBuilder(
                context.getApplicationContext(),
                AppDatabase.class,
                DATABASE_NAME
        ).build();
    }
}
