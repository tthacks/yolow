package com.mbientlab.metawear.tutorial.multimw.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "csv")
public class HapticCSV {
    @PrimaryKey (autoGenerate = true)
    int _id;
    String filename;
    String onTime;
    String offTime;
    String intensity;

    public HapticCSV(@NonNull String filename, String onTime, String offTime, String intensity) {
        this.filename = filename;
        this.onTime = onTime;
        this.offTime = offTime;
        this.intensity = intensity;
    }

    public int getId() { return _id; }

    public String getFilename() {
        return filename;
    }

    public String getOnTime() {
        return onTime;
    }

    public String getOffTime() {
        return offTime;
    }

    public String getIntensity() { return intensity; }

}
