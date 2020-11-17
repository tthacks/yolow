package com.mbientlab.metawear.tutorial.multimw;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedWriter;

public class SensorDevice {
    private String uid;
    private String uidFileFriendly;
    private String friendlyName;
    private String presetName;
    private BufferedWriter accel_writer;
    private BufferedWriter gyro_writer;
    private boolean connecting;
    private boolean hapticLocked;
    private int preset_id;
    private float x_loc;
    private float y_loc;
    private TextView view;

    public SensorDevice(String uid, String friendlyName, Context context) {

        this.uid = uid;
        this.uidFileFriendly = uid.replace(":", "-");
        this.hapticLocked = false;
        this.friendlyName = friendlyName;
        this.connecting = true;
        this.preset_id = -1;
        this.presetName = "";
        this.x_loc = 0;
        this.y_loc = 0;
        this.accel_writer = null;
        this.gyro_writer = null;
        this.view = new TextView(context);
        this.view.setTag(uid);
        this.view.setX(this.x_loc);
        this.view.setY(this.y_loc);
        this.view.setPadding(24, 16, 24, 16);
        this.view.setTextSize(24);
        this.view.setVisibility(View.VISIBLE);
    }

    public int getPreset_id() {
        return preset_id;
    }

    public void setPreset_id(int preset_id) {
        this.preset_id = preset_id;
    }

    public String getUid() {
        return uid;
    }

    public String getUidFileFriendly() {
        return uidFileFriendly;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public String getPresetName() {
        return presetName;
    }

    public void setPresetName(String presetName) {
        this.presetName = presetName;
    }

    public boolean isConnecting() {
        return connecting;
    }

    public void setConnecting(boolean connecting) {
        this.connecting = connecting;
    }

    public TextView getView() {
        return view;
    }

    public void lockHaptic(boolean lockState) {
        hapticLocked = lockState;
    }

    public boolean isHapticLocked() {
        return hapticLocked;
    }


    public BufferedWriter getAccel_writer() {
        return accel_writer;
    }

    public void setAccel_writer(BufferedWriter accel_writer) {
        this.accel_writer = accel_writer;
    }

    public BufferedWriter getGyro_writer() {
        return gyro_writer;
    }

    public void setGyro_writer(BufferedWriter gyro_writer) {
        this.gyro_writer = gyro_writer;
    }

}
