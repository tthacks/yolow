package com.mbientlab.metawear.tutorial.multimw.database;

import android.content.Context;
import android.util.Log;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {HapticCSV.class}, version = 1, exportSchema = false)
public abstract class CSVDatabase extends RoomDatabase {
    private static final String LOG_TAG = CSVDatabase.class.getSimpleName();
    private static final Object LOCK = new Object();
    private static final String DATABASE_NAME = "csvlist";
    private static CSVDatabase sInstance;

    public static CSVDatabase getInstance(Context context) {
        if (sInstance == null) {
            synchronized (LOCK) {
                Log.d(LOG_TAG, "Creating new database instance");
                sInstance = Room.databaseBuilder(context.getApplicationContext(),
                        CSVDatabase.class, CSVDatabase.DATABASE_NAME)
                        .build();
            }
        }
        Log.d(LOG_TAG, "Getting the database instance");
        return sInstance;
    }

    public abstract CSVDao hapticsDao();
}
