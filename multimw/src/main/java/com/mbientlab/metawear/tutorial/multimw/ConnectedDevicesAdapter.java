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
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.tutorial.multimw.database.SensorDatabase;
import com.mbientlab.metawear.tutorial.multimw.database.SensorDevice;

import java.util.List;

/**
 * Created by etsai on 5/22/2016.
 */
public class ConnectedDevicesAdapter extends RecyclerView.Adapter<ConnectedDevicesAdapter.SensorViewHolder> {

    private Context context;
    private List<SensorDevice> sensorList;
    private OnTestHapticClickListener testHapticClickListener;

    public ConnectedDevicesAdapter(Context context, OnTestHapticClickListener hapticClickListener) {
        this.context = context;
        this.testHapticClickListener = hapticClickListener;
    }

    @NonNull
    @Override
    public SensorViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(context).inflate(R.layout.sensor_status_settings, viewGroup, false);
        return new SensorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConnectedDevicesAdapter.SensorViewHolder sensorViewHolder, int i) {
        sensorViewHolder.deviceName.setText(sensorList.get(i).friendlyName);
        sensorViewHolder.deviceAddress.setText(sensorList.get(i).uid);
        sensorViewHolder.total_dur.setText("" + sensorList.get(i).totalDuration);
        sensorViewHolder.on_dur.setText("" + sensorList.get(i).onDuration);
        sensorViewHolder.off_dur.setText("" + sensorList.get(i).offDuration);

        sensorViewHolder.testHaptic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testHapticClickListener.onTestHapticClick(sensorList.get(i));
            }
        });


        if (sensorList.get(i).connecting) {
            sensorViewHolder.connectingProgress.setVisibility(View.VISIBLE);
            sensorViewHolder.connectingText.setVisibility(View.VISIBLE);
            sensorViewHolder.total_dur.setVisibility(View.GONE);
            sensorViewHolder.on_dur.setVisibility(View.GONE);
            sensorViewHolder.off_dur.setVisibility(View.GONE);
        } else {
            sensorViewHolder.connectingProgress.setVisibility(View.GONE);
            sensorViewHolder.connectingText.setVisibility(View.GONE);
            sensorViewHolder.total_dur.setVisibility(View.VISIBLE);
            sensorViewHolder.on_dur.setVisibility(View.VISIBLE);
            sensorViewHolder.off_dur.setVisibility(View.VISIBLE);
        }


    }

    @Override
    public int getItemCount() {
        if(sensorList == null) {
            return 0;
        }
        return sensorList.size();
    }

    public void setSensorList(List<SensorDevice> s_list) {
        sensorList = s_list;
        notifyDataSetChanged();
    }

    public List<SensorDevice> getSensorList() {
        return sensorList;
    }

    class SensorViewHolder extends RecyclerView.ViewHolder {
        TextView deviceAddress, connectingText;
        EditText deviceName, total_dur, on_dur, off_dur;
        ProgressBar connectingProgress;
        SensorDatabase sensorDb;
        Button testHaptic;

        SensorViewHolder(@NonNull final View itemView) {
            super(itemView);
            sensorDb = SensorDatabase.getInstance(context);
            deviceName = itemView.findViewById(R.id.status_device_name);
            deviceAddress = itemView.findViewById(R.id.status_mac_address);
            total_dur = itemView.findViewById(R.id.text_total_duration);
            on_dur = itemView.findViewById(R.id.text_on_duration);
            off_dur = itemView.findViewById(R.id.text_off_duration);
            connectingText = itemView.findViewById(R.id.text_connecting);
            connectingProgress = itemView.findViewById(R.id.connecting_progress);
            testHaptic = itemView.findViewById(R.id.test_haptic_button);

            //text changed listeners
            deviceName.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                String elementId = sensorList.get(getAdapterPosition()).uid;
                updateFriendlyName(elementId, s.toString());
            }

            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                }
        });

//            total_dur.addTextChangedListener(new TextWatcher() {
//
//                public void afterTextChanged(Editable s) {
//                    String elementId = sensorList.get(getAdapterPosition()).uid;
//                    updateHapticDuration(elementId, s.toString(), on_dur.getText().toString(), off_dur.getText().toString());
//                }
//
//                public void beforeTextChanged(CharSequence s, int start,
//                                              int count, int after) {
//                }
//
//                public void onTextChanged(CharSequence s, int start,
//                                          int before, int count) {
//                }
//            });
//            on_dur.addTextChangedListener(new TextWatcher() {
//
//                public void afterTextChanged(Editable s) {
//                    String elementId = sensorList.get(getAdapterPosition()).uid;
//                    updateHapticDuration(elementId, total_dur.toString(), s.toString(), off_dur.toString());
//                }
//
//                public void beforeTextChanged(CharSequence s, int start,
//                                              int count, int after) {
//                }
//
//                public void onTextChanged(CharSequence s, int start,
//                                          int before, int count) {
//                }
//            });
//            off_dur.addTextChangedListener(new TextWatcher() {
//
//                public void afterTextChanged(Editable s) {
//                    String elementId = sensorList.get(getAdapterPosition()).uid;
//                    updateHapticDuration(elementId, total_dur.toString(), on_dur.toString(), s.toString());
//                }
//
//                public void beforeTextChanged(CharSequence s, int start,
//                                              int count, int after) {
//                }
//
//                public void onTextChanged(CharSequence s, int start,
//                                          int before, int count) {
//                }
//            });



        }

        private void updateFriendlyName(String elementId, String s) {

            AppExecutors.getInstance().diskIO().execute(new Runnable() {
                @Override
                public void run() {
                    sensorDb.sensorDao().updateFriendlyName(s, elementId);
                }
            });
        }

        private void updateHapticDuration(String elementId, String on, String off, String total) {

            AppExecutors.getInstance().diskIO().execute(new Runnable() {
                @Override
                public void run() {
                    int on_num, off_num, tot_num;
                    try {
                        on_num = Integer.parseInt(on);
                    }
                    catch(NumberFormatException e) {
                        on_num = 0;
                    }
                    try {
                        off_num = Integer.parseInt(off);
                    }
                    catch(NumberFormatException e) {
                        off_num = 0;
                    }
                    try {
                        tot_num = Integer.parseInt(total);
                    }
                    catch(NumberFormatException e) {
                        tot_num = 0;
                    }

                    sensorDb.sensorDao().updateHaptic(on_num, off_num, tot_num, elementId);
                }
            });
        }
    }
}