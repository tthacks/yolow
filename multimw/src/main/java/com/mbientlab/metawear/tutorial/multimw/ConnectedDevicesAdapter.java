/*
 * Copyright 2015 MbientLab Inc. All rights reserved.
 *
 * IMPORTANT: Your use of this Software is limited to those specific rights
 * granted under the terms of a software license agreement between the user who
 * downloaded the software, his/her employer (which must be your employer) and
 * MbientLab Inc, (the "License").  You may not use this Software unless you
 * agree to abide by the terms of the License which can be found at
 * www.mbientlab.com/terms . The License limits your use, and you acknowledge,
 * that the  Software may not be modified, copied or distributed and can be used
 * solely and exclusively in conjunction with a MbientLab Inc, product.  Other
 * than for the foregoing purpose, you may not use, reproduce, copy, prepare
 * derivative works of, modify, distribute, perform, display or sell this
 * Software and/or its documentation for any purpose.
 *
 * YOU FURTHER ACKNOWLEDGE AND AGREE THAT THE SOFTWARE AND DOCUMENTATION ARE
 * PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED,
 * INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY, TITLE,
 * NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL
 * MBIENTLAB OR ITS LICENSORS BE LIABLE OR OBLIGATED UNDER CONTRACT, NEGLIGENCE,
 * STRICT LIABILITY, CONTRIBUTION, BREACH OF WARRANTY, OR OTHER LEGAL EQUITABLE
 * THEORY ANY DIRECT OR INDIRECT DAMAGES OR EXPENSES INCLUDING BUT NOT LIMITED
 * TO ANY INCIDENTAL, SPECIAL, INDIRECT, PUNITIVE OR CONSEQUENTIAL DAMAGES, LOST
 * PROFITS OR LOST DATA, COST OF PROCUREMENT OF SUBSTITUTE GOODS, TECHNOLOGY,
 * SERVICES, OR ANY CLAIMS BY THIRD PARTIES (INCLUDING BUT NOT LIMITED TO ANY
 * DEFENSE THEREOF), OR OTHER SIMILAR COSTS.
 *
 * Should you have any questions regarding your right to use this Software,
 * contact MbientLab Inc, at www.mbientlab.com.
 */

package com.mbientlab.metawear.tutorial.multimw;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Created by etsai on 5/22/2016.
 */
public class ConnectedDevicesAdapter extends ArrayAdapter<DeviceState> {

    SensorDatabase sensorDb;
    public ConnectedDevicesAdapter(Context context, int resource) {
        super(context, resource);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            convertView= LayoutInflater.from(getContext()).inflate(R.layout.sensor_status_settings, parent, false);

            viewHolder= new ViewHolder();
            viewHolder.deviceName= convertView.findViewById(R.id.status_device_name);
            viewHolder.total_dur = convertView.findViewById(R.id.text_total_duration);
            viewHolder.on_dur = convertView.findViewById(R.id.text_on_duration);
            viewHolder.off_dur = convertView.findViewById(R.id.text_off_duration);
            viewHolder.deviceAddress= convertView.findViewById(R.id.status_mac_address);
            viewHolder.connectingText= convertView.findViewById(R.id.text_connecting);
            viewHolder.connectingProgress= convertView.findViewById(R.id.connecting_progress);

            convertView.setTag(viewHolder);
        } else {
            viewHolder= (ViewHolder) convertView.getTag();
        }

        DeviceState state= getItem(position);
        // SensorDevice s = getSensorFromDb();
        //viewHolder.deviceName.setText(s.friendlyName);
        viewHolder.deviceName.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
            }

            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
//                SensorDevice s = getSensorFromId(state.btDevice.getAddress());
            }
        });



        viewHolder.deviceAddress.setText(state.btDevice.getAddress());
        viewHolder.total_dur.setText("");

        if (state.connecting) {
            viewHolder.connectingProgress.setVisibility(View.VISIBLE);
            viewHolder.connectingText.setVisibility(View.VISIBLE);
            viewHolder.total_dur.setVisibility(View.GONE);
            viewHolder.on_dur.setVisibility(View.GONE);
            viewHolder.off_dur.setVisibility(View.GONE);
        } else {
            viewHolder.connectingProgress.setVisibility(View.GONE);
            viewHolder.connectingText.setVisibility(View.GONE);
            viewHolder.total_dur.setVisibility(View.VISIBLE);
            viewHolder.on_dur.setVisibility(View.VISIBLE);
            viewHolder.off_dur.setVisibility(View.VISIBLE);
        }

        return convertView;
    }

//    private String getSensorFromDb(String uid ) {
//            AppExecutors.getInstance().diskIO().execute(new Runnable() {
//                @Override
//                public void run() {
//                    SensorDevice s = sensorDb.sensorDao().getSensorById(uid);
//                }
//            });
//    }

    private class ViewHolder {
        TextView deviceAddress, connectingText;
        EditText deviceName, total_dur, on_dur, off_dur;
        ProgressBar connectingProgress;
    }

    public void update(DeviceState newState) {
        int pos = getPosition(newState);
        if(pos == -1) {
            add(newState);
        }
        else {
        }
    }
}
