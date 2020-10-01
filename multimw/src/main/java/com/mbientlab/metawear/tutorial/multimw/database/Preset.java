package com.mbientlab.metawear.tutorial.multimw.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "presets")
public class Preset {
    @PrimaryKey (autoGenerate = true)
    int id;
    String name;
    boolean fromCSV;
    String csvFile;
    int numCycles;
    float on_time;
    float off_time;
    float accel_sample;
    float gyro_sample;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isFromCSV() {
        return fromCSV;
    }

    public void setFromCSV(boolean fromCSV) {
        this.fromCSV = fromCSV;
    }

    public String getCsvFile() {
        return csvFile;
    }

    public void setCsvFile(String csvFile) {
        this.csvFile = csvFile;
    }

    public int getNumCycles() {
        return numCycles;
    }

    public void setNumCycles(int numCycles) {
        this.numCycles = numCycles;
    }

    public float getOn_time() {
        return on_time;
    }

    public void setOn_time(float on_time) {
        this.on_time = on_time;
    }

    public float getOff_time() {
        return off_time;
    }

    public void setOff_time(float off_time) {
        this.off_time = off_time;
    }

    public float getAccel_sample() {
        return accel_sample;
    }

    public void setAccel_sample(float accel_sample) {
        this.accel_sample = accel_sample;
    }

    public float getGyro_sample() {
        return gyro_sample;
    }

    public void setGyro_sample(float gyro_sample) {
        this.gyro_sample = gyro_sample;
    }

    public Preset(@NonNull String name, boolean fromCSV, String csvFile, int numCycles, float on_time, float off_time, float accel_sample, float gyro_sample) {
        this.name = name;
        this.fromCSV = fromCSV;
        this.csvFile = csvFile;
        this.numCycles = numCycles;
        this.on_time = on_time;
        this.off_time = off_time;
        this.accel_sample = accel_sample;
        this.gyro_sample = gyro_sample;
    }

}
