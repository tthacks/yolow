package com.mbientlab.metawear.tutorial.multimw;

import android.view.View;
import android.widget.TextView;

public class SensorBox {

    private TextView box;
    private String address;

    public SensorBox(TextView t) {
        this.box = t;
        this.address = "";
        this.box.setVisibility(View.GONE);
    }

    public TextView getBox() {
        return box;
    }

    public boolean isMyBox(TextView t) {
        return t.equals(box);
    }

    public void setFriendlyName(String s) {
        box.setText(s);
    }

    public void setAddress(String s) {
        address = s;
    }

    public String getAddress() {
        return address;
    }

    public void setLocation(int x, int y) {
        box.setX(x);
        box.setY(y);
    }

    public void setIsVisible(boolean b) {
        if(b) {
            box.setVisibility(View.VISIBLE);
        }
        else {
            box.setVisibility(View.GONE);
        }
    }


}
