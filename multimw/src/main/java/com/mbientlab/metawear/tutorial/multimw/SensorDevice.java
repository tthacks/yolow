package com.mbientlab.metawear.tutorial.multimw;

import android.bluetooth.BluetoothDevice;

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
    @ColumnInfo(name = "total_duration")
    public int totalDuration;
    @ColumnInfo(name = "x_location")
    public int x_location;
    @ColumnInfo(name = "y_location")
    public int y_location;

    public SensorDevice(String uid, String friendlyName, int totalDuration, int x_location, int y_location) {

        this.uid = uid;
        this.friendlyName = friendlyName;
        this.totalDuration = totalDuration;
        this.x_location = x_location;
        this.y_location = y_location;
    }

    public void setFriendlyName(String f) {
        friendlyName = f;
    }
}
