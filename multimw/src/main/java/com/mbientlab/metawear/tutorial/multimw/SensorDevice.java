package com.mbientlab.metawear.tutorial.multimw;

public class SensorDevice {
    private String uid;
    private String friendlyName;
    private String presetName;
    private boolean connecting;
    private int preset_id;
    private float x_loc;
    private float y_loc;

    public SensorDevice(String uid, String friendlyName) {

        this.uid = uid;
        this.friendlyName = friendlyName;
        this.connecting = true;
        this.preset_id = MainActivityContainer.getDefaultIndex();
        this.presetName = "";
        this.x_loc = 0;
        this.y_loc = 0;
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

    public float getX_loc() {
        return x_loc;
    }

    public void setX_loc(float x_loc) {
        this.x_loc = x_loc;
    }

    public float getY_loc() {
        return y_loc;
    }

    public void setY_loc(float y_loc) {
        this.y_loc = y_loc;
    }

}
