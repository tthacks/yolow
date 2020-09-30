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

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


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
        View view = LayoutInflater.from(context).inflate(R.layout.sensor_status_w_csv, viewGroup, false);
        return new SensorViewHolder(view);
}

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ConnectedDevicesAdapter.SensorViewHolder sensorViewHolder, @SuppressLint("RecyclerView") int i) {
        sensorViewHolder.deviceName.setText(sensorList.get(i).friendlyName);
        sensorViewHolder.deviceAddress.setText(sensorList.get(i).uid);
        sensorViewHolder.total_dur.setText("" + sensorList.get(i).totalCycles);
        sensorViewHolder.on_dur.setText("" + sensorList.get(i).onDuration);
        sensorViewHolder.off_dur.setText("" + sensorList.get(i).offDuration);
        sensorViewHolder.testHaptic.setOnClickListener(v -> testHapticClickListener.onTestHapticClick(sensorList.get(i)));

        sensorViewHolder.csvDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int x, long l) {
                sensorList.get(i).csvFile = sensorViewHolder.csvFileNames.get(x);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });


        if (sensorList.get(i).connecting) {
            sensorViewHolder.connectingProgress.setVisibility(View.VISIBLE);
            sensorViewHolder.connectingText.setVisibility(View.VISIBLE);
        } else {
            sensorViewHolder.connectingProgress.setVisibility(View.GONE);
            sensorViewHolder.connectingText.setVisibility(View.GONE);
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

    class SensorViewHolder extends RecyclerView.ViewHolder {
        TextView deviceAddress, connectingText, total_label, on_label, off_label;
        EditText deviceName, total_dur, on_dur, off_dur;
        RadioButton radioCSV, customCSV;
        ProgressBar connectingProgress;
        Button testHaptic;
        Spinner csvDropdown;
        List<String> csvFileNames;

        SensorViewHolder(@NonNull final View itemView) {
            super(itemView);
            csvFileNames = new ArrayList<>(MainActivityContainer.csvFiles.keySet());
            deviceName = itemView.findViewById(R.id.status_device_name);
            deviceAddress = itemView.findViewById(R.id.status_mac_address);
            total_label = itemView.findViewById(R.id.label_total_duration);
            on_label = itemView.findViewById(R.id.label_on_duration);
            off_label = itemView.findViewById(R.id.label_off_duration);
            total_dur = itemView.findViewById(R.id.text_total_duration);
            on_dur = itemView.findViewById(R.id.text_on_duration);
            off_dur = itemView.findViewById(R.id.text_off_duration);
            connectingText = itemView.findViewById(R.id.text_connecting);
            connectingProgress = itemView.findViewById(R.id.connecting_progress);
            radioCSV = itemView.findViewById(R.id.radio_csv);
            customCSV = itemView.findViewById(R.id.radio_custom);
            testHaptic = itemView.findViewById(R.id.test_haptic_button);
            csvDropdown = itemView.findViewById(R.id.csv_spinner);
            ArrayAdapter<String> dropdownAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, csvFileNames);
            dropdownAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            csvDropdown.setAdapter(dropdownAdapter);
            csvDropdown.setVisibility(View.INVISIBLE);

            radioCSV.setOnClickListener(view -> {
                radioCSV.setChecked(true);
                customCSV.setChecked(false);
                MainActivityContainer.getSensorById(sensorList.get(getAdapterPosition()).uid).usingCSV = true;
                    total_dur.setVisibility(View.INVISIBLE);
                    on_dur.setVisibility(View.INVISIBLE);
                    off_dur.setVisibility(View.INVISIBLE);
                    total_label.setVisibility(View.INVISIBLE);
                    on_label.setVisibility(View.INVISIBLE);
                    off_label.setVisibility(View.INVISIBLE);
                    csvDropdown.setVisibility(View.VISIBLE);
            });

            customCSV.setOnClickListener(view -> {
                radioCSV.setChecked(false);
                customCSV.setChecked(true);
                MainActivityContainer.getSensorById(sensorList.get(getAdapterPosition()).uid).usingCSV = false;
                    total_dur.setVisibility(View.VISIBLE);
                    on_dur.setVisibility(View.VISIBLE);
                    off_dur.setVisibility(View.VISIBLE);
                    total_label.setVisibility(View.VISIBLE);
                    on_label.setVisibility(View.VISIBLE);
                    off_label.setVisibility(View.VISIBLE);
                    csvDropdown.setVisibility(View.INVISIBLE);
            });

            //text changed listeners
            deviceName.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                String elementId = sensorList.get(getAdapterPosition()).uid;
                MainActivityContainer.getSensorById(elementId).friendlyName = s.toString();
            }
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {}
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {}
        });
            total_dur.addTextChangedListener(new TextWatcher() {

                public void afterTextChanged(Editable s) {
                    String elementId = sensorList.get(getAdapterPosition()).uid;
                    try {
                        MainActivityContainer.getSensorById(elementId).totalCycles = Integer.parseInt(s.toString());
                    } catch(NumberFormatException ignored) {}
                }

                public void beforeTextChanged(CharSequence s, int start,
                                              int count, int after) {}
                public void onTextChanged(CharSequence s, int start,
                                          int before, int count) {}
            });

            on_dur.addTextChangedListener(new TextWatcher() {

                public void afterTextChanged(Editable s) {
                    String elementId = sensorList.get(getAdapterPosition()).uid;
                    try {
                        MainActivityContainer.getSensorById(elementId).onDuration = Float.parseFloat(s.toString());
                    } catch (NumberFormatException ignored) {}
                }
                public void beforeTextChanged(CharSequence s, int start,
                                              int count, int after) {}
                public void onTextChanged(CharSequence s, int start,
                                          int before, int count) {}
            });

            off_dur.addTextChangedListener(new TextWatcher() {

                public void afterTextChanged(Editable s) {
                    String elementId = sensorList.get(getAdapterPosition()).uid;
                    try {
                        MainActivityContainer.getSensorById(elementId).offDuration = Float.parseFloat(s.toString());
                    }
                    catch(NumberFormatException ignored) {}
                }
                public void beforeTextChanged(CharSequence s, int start,
                                              int count, int after) {}
                public void onTextChanged(CharSequence s, int start,
                                          int before, int count) {}
            });
        }
    }
}