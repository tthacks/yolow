package com.mbientlab.metawear.tutorial.multimw.database;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;


@Entity
public class SensorDevice {
    @PrimaryKey @NonNull
    public String uid;
    @ColumnInfo(name = "friendly_name")
    public String friendlyName;
    @ColumnInfo (name = "connecting")
    public boolean connecting;
    @ColumnInfo(name = "total_cycles")
    public int totalCycles;
    @ColumnInfo(name = "on_duration")
    public float onDuration;
    @ColumnInfo(name = "off_duration")
    public float offDuration;

    public SensorDevice(@NonNull String uid, String friendlyName, boolean connecting, int totalCycles, float onDuration, float offDuration) {

        this.uid = uid;
        this.friendlyName = friendlyName;
        this.connecting = connecting;
        this.totalCycles = totalCycles;
        this.onDuration = onDuration;
        this.offDuration = offDuration;
    }

}
