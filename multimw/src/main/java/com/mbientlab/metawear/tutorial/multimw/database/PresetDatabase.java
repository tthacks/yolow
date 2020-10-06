package com.mbientlab.metawear.tutorial.multimw.database;

import android.content.Context;
import android.util.Log;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Preset.class}, version = 1, exportSchema = false)
public abstract class PresetDatabase extends RoomDatabase {
    private static final String LOG_TAG = PresetDatabase.class.getSimpleName();
    private static final Object LOCK = new Object();
    private static final String DATABASE_NAME = "presets";
    private static PresetDatabase sInstance;

    public static PresetDatabase getInstance(Context context) {
        if (sInstance == null) {
            synchronized (LOCK) {
                Log.d(LOG_TAG, "Creating new database instance");
                sInstance = Room.databaseBuilder(context.getApplicationContext(),
                        PresetDatabase.class, PresetDatabase.DATABASE_NAME)
                        .build();
            }
        }
        Log.d(LOG_TAG, "Getting the database instance");
        return sInstance;
    }

    public abstract PresetDao pDao();
}
