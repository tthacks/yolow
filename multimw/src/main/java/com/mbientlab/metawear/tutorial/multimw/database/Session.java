package com.mbientlab.metawear.tutorial.multimw.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "sessions")
public class Session {
    @PrimaryKey (autoGenerate = true)
    int _id;
    String name;
    int numSensors;
    int numPresets;

    public int getNumSensors() {
        return numSensors;
    }

    public int getNumPresets() {
        return numPresets;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public Session(String name, int numSensors, int numPresets) {
        this.name = name;
        this.numSensors = numSensors;
        this.numPresets = numPresets;

    }

}
