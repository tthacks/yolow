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
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ConnectedDevicesAdapter extends RecyclerView.Adapter<ConnectedDevicesAdapter.SensorViewHolder> {

    private Context context;
    private List<SensorDevice> sensorList;
    private OnTestHapticClickListener testHapticClickListener;
    private List<String> csvSelect;

    public ConnectedDevicesAdapter(Context context, OnTestHapticClickListener hapticClickListener) {
        this.context = context;
        this.testHapticClickListener = hapticClickListener;
    }

    @NonNull
    @Override
    public SensorViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        csvSelect = new ArrayList<>();
        Field[] fields = R.raw.class.getFields();
        for(int x = 0; x < fields.length; x++) {
            csvSelect.add(fields[x].getName());
        }

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
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this.context, android.R.layout.simple_spinner_item, csvSelect);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sensorViewHolder.csvDropdown.setAdapter(arrayAdapter);
        sensorViewHolder.csvDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                MainActivityContainer.getSensorById(sensorList.get(i).uid).csvFile = csvSelect.get(position);
            }
            @Override
            public void onNothingSelected(AdapterView <?> parent) {
                MainActivityContainer.getSensorById(sensorList.get(i).uid).csvFile = csvSelect.get(0);
            }
        });

        sensorViewHolder.testHaptic.setOnClickListener(v -> testHapticClickListener.onTestHapticClick(sensorList.get(i)));
        sensorViewHolder.uploadCSV.setOnClickListener(v -> {
            //TODO: upload csv files
            System.out.println("TODO: Not yet implemented.");
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
        Button testHaptic, uploadCSV;
        Spinner csvDropdown;


        SensorViewHolder(@NonNull final View itemView) {
            super(itemView);
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
            uploadCSV = itemView.findViewById(R.id.button_upload_csv);
            testHaptic = itemView.findViewById(R.id.test_haptic_button);
            csvDropdown = itemView.findViewById(R.id.csv_spinner);
            csvDropdown.setVisibility(View.INVISIBLE);
            uploadCSV.setVisibility(View.INVISIBLE);

            radioCSV.setOnClickListener(view -> {
                radioCSV.setChecked(true);
                customCSV.setChecked(false);
                    updateUsingCSV(sensorList.get(getAdapterPosition()).uid, true);
                    total_dur.setVisibility(View.INVISIBLE);
                    on_dur.setVisibility(View.INVISIBLE);
                    off_dur.setVisibility(View.INVISIBLE);
                    total_label.setVisibility(View.INVISIBLE);
                    on_label.setVisibility(View.INVISIBLE);
                    off_label.setVisibility(View.INVISIBLE);
                    csvDropdown.setVisibility(View.VISIBLE);
                    uploadCSV.setVisibility(View.VISIBLE);
            });

            customCSV.setOnClickListener(view -> {
                radioCSV.setChecked(false);
                customCSV.setChecked(true);
                    updateUsingCSV(sensorList.get(getAdapterPosition()).uid, false);
                    total_dur.setVisibility(View.VISIBLE);
                    on_dur.setVisibility(View.VISIBLE);
                    off_dur.setVisibility(View.VISIBLE);
                    total_label.setVisibility(View.VISIBLE);
                    on_label.setVisibility(View.VISIBLE);
                    off_label.setVisibility(View.VISIBLE);
                    csvDropdown.setVisibility(View.INVISIBLE);
                    uploadCSV.setVisibility(View.INVISIBLE);
            });

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
            total_dur.addTextChangedListener(new TextWatcher() {

                public void afterTextChanged(Editable s) {
                    String elementId = sensorList.get(getAdapterPosition()).uid;
                    try {
                        int val = Integer.parseInt(s.toString());
                        updateCycleDuration(elementId, val);
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
                        float val = Float.parseFloat(s.toString());
                        updateOnOffDuration(elementId, val, true);
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
                        float val = Float.parseFloat(s.toString());
                        updateOnOffDuration(elementId, val, false);
                    }
                    catch(NumberFormatException ignored) {}
                }

                public void beforeTextChanged(CharSequence s, int start,
                                              int count, int after) {}

                public void onTextChanged(CharSequence s, int start,
                                          int before, int count) {}
            });

        }

        private void updateUsingCSV(String id, boolean using) {
            MainActivityContainer.getSensorById(id).usingCSV = using;
        }

        private void updateFriendlyName(String id, String s) {
            MainActivityContainer.getSensorById(id).friendlyName = s;
        }

        private void updateCycleDuration(String id, int length) {
            MainActivityContainer.getSensorById(id).totalCycles = length;

        }

        private void updateOnOffDuration(String id, float length, boolean isOn) {
            if(isOn) {
                MainActivityContainer.getSensorById(id).onDuration = length;
            }
            else {
                MainActivityContainer.getSensorById(id).offDuration = length;
            }
        }
    }
}