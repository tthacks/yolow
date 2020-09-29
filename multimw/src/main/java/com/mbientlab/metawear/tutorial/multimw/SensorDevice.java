package com.mbientlab.metawear.tutorial.multimw;

public class SensorDevice {
    public String uid;
    public String friendlyName;
    public boolean connecting;
    public boolean usingCSV;
    public int totalCycles;
    public float onDuration;
    public float offDuration;
    public float x_loc;
    public float y_loc;
    public String csvFile;

    public SensorDevice(String uid, String friendlyName) {

        this.uid = uid;
        this.friendlyName = friendlyName;
        this.connecting = true;
        this.totalCycles = 2;
        this.onDuration = (float) 1.0;
        this.offDuration = (float) 1.0;
        this.x_loc = 0;
        this.y_loc = 0;
        this.usingCSV = false;
        this.csvFile = "";
    }

}
