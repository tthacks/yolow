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
import android.content.res.Configuration;
import android.graphics.Bitmap;
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
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.mbientlab.metawear.tutorial.multimw.database.AppDatabase;
import com.mbientlab.metawear.tutorial.multimw.database.AppExecutors;
import com.mbientlab.metawear.tutorial.multimw.database.HapticCSV;
import com.mbientlab.metawear.tutorial.multimw.database.Preset;

import java.util.ArrayList;
import java.util.List;


public class PresetAdapter extends RecyclerView.Adapter<PresetAdapter.SensorViewHolder> {

    private Context context;
    private List<Preset> pList;
    private AppDatabase database;
    private List<String> csvList;

    public PresetAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public SensorViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(context).inflate(R.layout.sensor_preset, viewGroup, false);
        database = AppDatabase.getInstance(context);
        csvList = new ArrayList<>();
        return new SensorViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull PresetAdapter.SensorViewHolder presetViewHolder, @SuppressLint("RecyclerView") int i) {
        presetViewHolder.presetName.setText(pList.get(i).getName());
        presetViewHolder.total_dur.setText("" + pList.get(i).getNumCycles());
        presetViewHolder.on_dur.setText("" + pList.get(i).getOn_time());
        presetViewHolder.off_dur.setText("" + pList.get(i).getOff_time());
//        presetViewHolder.gyro_sample.setText("" + pList.get(i).getGyro_sample());
//        presetViewHolder.accel_sample.setText("" + pList.get(i).getAccel_sample());
        presetViewHolder.intensity.setText("" + pList.get(i).getIntensity());
        presetViewHolder.set_default_switch.setChecked(pList.get(i).isDefault());
        presetViewHolder.customCSV.setChecked(!pList.get(i).isFromCSV());
        presetViewHolder.csv_text.setText(pList.get(i).getCsvFileName());
        presetViewHolder.radioCSV.setChecked(pList.get(i).isFromCSV());
        presetViewHolder.spinner.setSelection(0);
        if(pList.get(i).isFromCSV()) {
            presetViewHolder.total_dur.setVisibility(View.GONE);
            presetViewHolder.on_dur.setVisibility(View.GONE);
            presetViewHolder.off_dur.setVisibility(View.GONE);
            presetViewHolder.total_label.setVisibility(View.GONE);
            presetViewHolder.on_label.setVisibility(View.GONE);
            presetViewHolder.off_label.setVisibility(View.GONE);
            presetViewHolder.intensity.setVisibility(View.GONE);
            presetViewHolder.intens_label.setVisibility(View.GONE);
            presetViewHolder.spinner.setVisibility(View.VISIBLE);
        }
        else {
            presetViewHolder.radioCSV.setChecked(false);
            presetViewHolder.customCSV.setChecked(true);
            presetViewHolder.total_dur.setVisibility(View.VISIBLE);
            presetViewHolder.on_dur.setVisibility(View.VISIBLE);
            presetViewHolder.off_dur.setVisibility(View.VISIBLE);
            presetViewHolder.intensity.setVisibility(View.VISIBLE);
            presetViewHolder.total_label.setVisibility(View.VISIBLE);
            presetViewHolder.on_label.setVisibility(View.VISIBLE);
            presetViewHolder.off_label.setVisibility(View.VISIBLE);
            presetViewHolder.intens_label.setVisibility(View.VISIBLE);
            presetViewHolder.spinner.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        if(pList == null) {
            return 0;
        }
        return pList.size();
    }

    public void setPresets(List<Preset> p_list) {
        pList = p_list;
        notifyDataSetChanged();
    }

    class SensorViewHolder extends RecyclerView.ViewHolder {
        TextView total_label, on_label, off_label, csv_text, intens_label;
        Button delete_button;
        EditText presetName, total_dur, on_dur, off_dur, accel_sample, gyro_sample, intensity;
        RadioButton radioCSV, customCSV;
        Switch set_default_switch;
        Spinner spinner;

        SensorViewHolder(@NonNull final View itemView) {
            super(itemView);
            presetName = itemView.findViewById(R.id.preset_name);
            delete_button = itemView.findViewById(R.id.delete_preset_button);
            total_label = itemView.findViewById(R.id.label_total_duration);
            customCSV = itemView.findViewById(R.id.radio_custom);
            on_label = itemView.findViewById(R.id.label_on_duration);
            off_label = itemView.findViewById(R.id.label_off_duration);
            total_dur = itemView.findViewById(R.id.text_total_duration);
            on_dur = itemView.findViewById(R.id.text_on_duration);
            off_dur = itemView.findViewById(R.id.text_off_duration);
            intens_label = itemView.findViewById(R.id.label_intensity);
            intensity = itemView.findViewById(R.id.text_intensity);
            radioCSV = itemView.findViewById(R.id.radio_csv);
            csv_text = itemView.findViewById(R.id.current_file_text);
            spinner = itemView.findViewById(R.id.csv_spinner);
            retrieveCSVs(spinner);

//            accel_sample = itemView.findViewById(R.id.text_sample_accel);
//            gyro_sample = itemView.findViewById(R.id.text_sample_gyro);
            set_default_switch = itemView.findViewById(R.id.set_default_switch);

            set_default_switch.setOnClickListener(view -> {
                Preset p = pList.get(getAdapterPosition());
                updateDefault(p);
            });

            radioCSV.setOnClickListener(view -> {
                radioCSV.setChecked(true);
                customCSV.setChecked(false);
                total_dur.setVisibility(View.GONE);
                on_dur.setVisibility(View.GONE);
                off_dur.setVisibility(View.GONE);
                total_label.setVisibility(View.GONE);
                on_label.setVisibility(View.GONE);
                off_label.setVisibility(View.GONE);
                intensity.setVisibility(View.GONE);
                intens_label.setVisibility(View.GONE);
                spinner.setVisibility(View.VISIBLE);
                Preset p = pList.get(getAdapterPosition());
                p.setFromCSV(true);
                updatePreset(p);
            });

            customCSV.setOnClickListener(view -> {
                radioCSV.setChecked(false);
                customCSV.setChecked(true);
                total_dur.setVisibility(View.VISIBLE);
                on_dur.setVisibility(View.VISIBLE);
                off_dur.setVisibility(View.VISIBLE);
                intensity.setVisibility(View.VISIBLE);
                intens_label.setVisibility(View.VISIBLE);
                total_label.setVisibility(View.VISIBLE);
                on_label.setVisibility(View.VISIBLE);
                off_label.setVisibility(View.VISIBLE);
                spinner.setVisibility(View.INVISIBLE);
                Preset p = pList.get(getAdapterPosition());
                p.setFromCSV(false);
                updatePreset(p);
            });

            delete_button.setOnClickListener(view -> {
                boolean resetDefault = false;
                Preset toDelete = pList.get(getAdapterPosition());
                if(toDelete.isDefault()) {
                    resetDefault = true;
                }
                deletePreset(toDelete);
                pList.remove(toDelete);
                if(resetDefault && pList.size() > 0) {
                    Preset newDefault = pList.get(0);
                    newDefault.setDefault(true);
                    updatePreset(newDefault);
                }
                notifyDataSetChanged();
            });

            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int selected, long l) {
                    if(selected > 0) {
                        Preset p = pList.get(getAdapterPosition());
                        String filenameSelected = (String) adapterView.getItemAtPosition(selected);
                        csv_text.setText(filenameSelected);
                        updateCSVFileInPreset(p, filenameSelected);
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {}
            });

            //text changed listeners
            presetName.addTextChangedListener(new TextWatcher() {
                public void afterTextChanged(Editable s) {
                    Preset p = pList.get(getAdapterPosition());
                    p.setName(s.toString());
                    updatePreset(p);
                }
                public void beforeTextChanged(CharSequence s, int start,
                                              int count, int after) {}
                public void onTextChanged(CharSequence s, int start,
                                          int before, int count) {}
            });
            total_dur.addTextChangedListener(new TextWatcher() {

                public void afterTextChanged(Editable s) {
                    Preset p = pList.get(getAdapterPosition());
                    try {
                        p.setNumCycles(Integer.parseInt(s.toString()));
                        updatePreset(p);
                    } catch(NumberFormatException ignored) {}
                }

                public void beforeTextChanged(CharSequence s, int start,
                                              int count, int after) {}
                public void onTextChanged(CharSequence s, int start,
                                          int before, int count) {}
            });

            on_dur.addTextChangedListener(new TextWatcher() {

                public void afterTextChanged(Editable s) {
                    Preset p = pList.get(getAdapterPosition());
                    try {
                        p.setOn_time(Float.parseFloat(s.toString()));
                        updatePreset(p);
                    } catch(NumberFormatException ignored) {}
                }
                public void beforeTextChanged(CharSequence s, int start,
                                              int count, int after) {}
                public void onTextChanged(CharSequence s, int start,
                                          int before, int count) {}
            });

            off_dur.addTextChangedListener(new TextWatcher() {
                public void afterTextChanged(Editable s) {
                    Preset p = pList.get(getAdapterPosition());
                    try {
                        p.setOff_time(Float.parseFloat(s.toString()));
                        updatePreset(p);
                    } catch(NumberFormatException ignored) {}
                }
                public void beforeTextChanged(CharSequence s, int start,
                                              int count, int after) {}
                public void onTextChanged(CharSequence s, int start,
                                          int before, int count) {}
            });
            intensity.addTextChangedListener(new TextWatcher() {
                public void afterTextChanged(Editable s) {
                    Preset p = pList.get(getAdapterPosition());
                    try {
                        p.setIntensity(Float.parseFloat(s.toString()));
                        updatePreset(p);
                    } catch(NumberFormatException ignored) {}
                }
                public void beforeTextChanged(CharSequence s, int start,
                                              int count, int after) {}
                public void onTextChanged(CharSequence s, int start,
                                          int before, int count) {}
            });
//            accel_sample.addTextChangedListener(new TextWatcher() {
//                public void afterTextChanged(Editable s) {
//                    Preset p = pList.get(getAdapterPosition());
//                    try {
//                        p.setAccel_sample(Float.parseFloat(s.toString()));
//                        updatePreset(p);
//                    } catch(NumberFormatException ignored) {}
//                }
//                public void beforeTextChanged(CharSequence s, int start,
//                                              int count, int after) {}
//                public void onTextChanged(CharSequence s, int start,
//                                          int before, int count) {}
//            });
//            gyro_sample.addTextChangedListener(new TextWatcher() {
//                public void afterTextChanged(Editable s) {
//                    Preset p = pList.get(getAdapterPosition());
//                    try {
//                        p.setGyro_sample(Float.parseFloat(s.toString()));
//                        updatePreset(p);
//                    } catch(NumberFormatException ignored) {}
//                }
//                public void beforeTextChanged(CharSequence s, int start,
//                                              int count, int after) {}
//                public void onTextChanged(CharSequence s, int start,
//                                          int before, int count) {}
//            });
        }
    }

    public void updateCSVFileInPreset(Preset p, String filename) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            HapticCSV h = database.hDao().loadCSVFileByName(filename);
            ((MainActivityContainer)context).runOnUiThread(() -> {
                if(h != null) {
                    p.setCsvFileName(h.getFilename());
                    p.setCsvFile(h.getId());
                    updatePreset(p);
                }
        });
    });
    }

    public void updatePreset(Preset p) {
            AppExecutors.getInstance().diskIO().execute(() -> database.pDao().updatePreset(p));
}

public void updateDefault(Preset p) {
    AppExecutors.getInstance().diskIO().execute(() -> {
        Preset d = database.pDao().getDefaultPreset();
        d.setDefault(false);
        p.setDefault(true);
        database.pDao().updatePreset(p);
        database.pDao().updatePreset(d);
    });
}

    public void deletePreset(Preset p) {
        AppExecutors.getInstance().diskIO().execute(() -> database.pDao().deletePreset(p));
    }

    public void retrieveCSVs(Spinner spinner) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            List<String> csvs = database.hDao().loadAllCSVFileNames();
            ((MainActivityContainer)context).runOnUiThread(() -> {
                csvList = csvs;
                csvList.add(0, "");
                ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, csvList);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(adapter);
            });
        });
    }



}