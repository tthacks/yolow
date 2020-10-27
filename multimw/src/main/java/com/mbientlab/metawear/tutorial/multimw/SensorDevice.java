package com.mbientlab.metawear.tutorial.multimw;

import android.content.Context;
import android.widget.TextView;

public class SensorDevice {
    private String uid;
    private String uidFileFriendly;
    private String friendlyName;
    private String presetName;
    private boolean connecting;
    private int preset_id;
    private float x_loc;
    private float y_loc;
    private TextView view;

    public SensorDevice(String uid, String friendlyName, Context context) {

        this.uid = uid;
        this.uidFileFriendly = uid.replace(":", "-");
        this.friendlyName = friendlyName;
        this.connecting = true;
        this.preset_id = -1;
        this.presetName = "";
        this.x_loc = 0;
        this.y_loc = 0;
        this.view = new TextView(context);
        this.view.setTag(uid);
        this.view.setX(this.x_loc);
        this.view.setY(this.y_loc);
        this.view.setPadding(24, 16, 24, 16);
        this.view.setTextSize(24);
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

}
