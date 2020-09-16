package com.mbientlab.metawear.tutorial.multimw;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.mbientlab.metawear.tutorial.multimw.SensorDevice;
import com.mbientlab.metawear.tutorial.multimw.SensorDeviceDao;

@Database(entities = SensorDevice.class, exportSchema = false, version = 1)
public abstract class SensorDatabase extends RoomDatabase {
    private static final String DB_NAME = "sensor_db";
    private static SensorDatabase instance;

    public static synchronized SensorDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(), SensorDatabase.class, DB_NAME)
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }

    public abstract SensorDeviceDao sensorDao();
}