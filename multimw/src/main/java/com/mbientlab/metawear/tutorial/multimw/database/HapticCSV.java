package com.mbientlab.metawear.tutorial.multimw.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "csv")
public class HapticCSV {
    @PrimaryKey @NonNull
    String filename;
    String onTime;
    String offTime;

    public HapticCSV(@NonNull String filename, String onTime, String offTime) {
        this.filename = filename;
        this.onTime = onTime;
        this.offTime = offTime;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getOnTime() {
        return onTime;
    }

    public void setOnTime(String onTime) {
        this.onTime = onTime;
    }

    public String getOffTime() {
        return offTime;
    }

    public void setOffTime(String offTime) {
        this.offTime = offTime;
    }

}
