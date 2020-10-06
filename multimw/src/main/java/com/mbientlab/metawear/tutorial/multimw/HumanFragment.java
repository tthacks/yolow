package com.mbientlab.metawear.tutorial.multimw;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.module.Haptic;
import com.mbientlab.metawear.tutorial.multimw.database.AppExecutors;
import com.mbientlab.metawear.tutorial.multimw.database.CSVDatabase;
import com.mbientlab.metawear.tutorial.multimw.database.HapticCSV;
import com.mbientlab.metawear.tutorial.multimw.database.Preset;
import com.mbientlab.metawear.tutorial.multimw.database.PresetDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HumanFragment extends Fragment implements ServiceConnection, View.OnTouchListener, View.OnDragListener, View.OnLongClickListener {

    private BtleService.LocalBinder binder;
    private boolean isLocked, isRecording;
    private TextView lastSelected = null;
    private View sensorSettingsBar;
    private EditText sensorName;
    private Spinner presetSpinner;
    private PresetDatabase pDatabase;
    private CSVDatabase csvDb;
    private List<String> presets;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Activity owner = getActivity();
        owner.getApplicationContext().bindService(new Intent(owner, BtleService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().getApplicationContext().unbindService(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);
        return inflater.inflate(R.layout.fragment_human, container, false);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pDatabase = PresetDatabase.getInstance(getActivity().getApplicationContext());
        csvDb = CSVDatabase.getInstance(getActivity().getApplicationContext());
        //button controls
        sensorSettingsBar = view.findViewById(R.id.sensor_data_layout);
        sensorSettingsBar.setVisibility(View.INVISIBLE);
        isLocked = false;
        isRecording = false;
        sensorName = view.findViewById(R.id.sensor_name);
        presetSpinner = view.findViewById(R.id.sensor_preset_select);
        retrievePresets();

        presetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(lastSelected != null) {
                    SensorDevice s = MainActivityContainer.getDeviceStates().get(lastSelected.getTag().toString());
                    AppExecutors.getInstance().diskIO().execute(() -> {
                        final int preset_id = pDatabase.pDao().getIdFromPresetName((String) adapterView.getItemAtPosition(i));
                        getActivity().runOnUiThread(() -> {
                            s.setPreset_id(preset_id);
                            s.setPresetName((String) adapterView.getItemAtPosition(i));
                        });
                    });
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
        Button lock_button = view.findViewById(R.id.button_lock);
        Button record_button = view.findViewById(R.id.button_record);
        lock_button.setOnClickListener(v -> {
            isLocked = !isLocked;
            if (isLocked) {
                lock_button.setText(R.string.unlock);
            } else {
                lock_button.setText(R.string.lock);
            }
        });
        record_button.setOnClickListener(v -> {
            isRecording = !isRecording;
            if (isRecording) {
                record_button.setText(R.string.stop_recording);
                lock_button.setEnabled(false);
            } else {
                record_button.setText(R.string.start_recording);
                lock_button.setEnabled(true);
            }
        });

        sensorName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                if(lastSelected != null) {
                    lastSelected.setText(editable.toString());
                    SensorDevice s = MainActivityContainer.getDeviceStates().get(lastSelected.getTag().toString());
                    s.setFriendlyName(editable.toString());
                }
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        retrievePresets();
        retrieveSensors();
    }


    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        binder= (BtleService.LocalBinder) service;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {}

    public void addNewDevice(BluetoothDevice btDevice) {
        final SensorDevice newDeviceState = new SensorDevice(btDevice.getAddress(), btDevice.getName());
        newDeviceState.setPresetName(MainActivityContainer.getDefaultPresetName());
        newDeviceState.setPreset_id(MainActivityContainer.getDefaultPresetId());
        final MetaWearBoard newBoard = binder.getMetaWearBoard(btDevice);

         MainActivityContainer.addDeviceToStates(newDeviceState);
        MainActivityContainer.addStateToBoards(btDevice.getAddress(), newBoard);

        newBoard.onUnexpectedDisconnect(status -> getActivity().runOnUiThread(() -> MainActivityContainer.getDeviceStates().remove(newDeviceState.getUid())));
        newBoard.connectAsync().onSuccessTask(task -> {
            getActivity().runOnUiThread(() -> {
                newDeviceState.setConnecting(false);
                retrieveSensors();
            });
            return null;
        });
    }

    private void addSensorBox(SensorDevice s, int idx) {
        ConstraintLayout constraintLayout = getView().findViewById(R.id.sensor_area);
        TextView newSensor = new TextView(getActivity().getApplicationContext());
        newSensor.setText(s.getFriendlyName());
        newSensor.setTag(s.getUid());
        if(s.getX_loc() == 0) {
            newSensor.setX(0);
            newSensor.setY(idx * 100);
        }
        else {
            newSensor.setX(s.getX_loc());
            newSensor.setY(s.getY_loc());
        }
        if(s.isConnecting()) {
            newSensor.setBackgroundResource(R.color.colorAccentShy);
        }
        else {
            newSensor.setBackgroundResource(R.color.sensorboxDefault);
        }
        newSensor.setPadding(24, 16, 24, 16);
        newSensor.setTextSize(24);
        newSensor.setOnLongClickListener(this);
        newSensor.setOnDragListener(this);
        newSensor.setOnTouchListener(this);
        newSensor.setLayoutParams(new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT ));
        constraintLayout.addView(newSensor);
    }

    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouch(View v, MotionEvent event) {
        SensorDevice s = MainActivityContainer.getDeviceStates().get(v.getTag().toString());
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if(isLocked) {
                //send haptic
                MetaWearBoard board = MainActivityContainer.getStateToBoards().get(s.getUid());
                if(board != null) {
                    sendHapticFromPreset(s, board);
                }
            }
            else {
                lastSelected = (TextView) v;
                sensorSettingsBar.setVisibility(View.VISIBLE);
                sensorName.setText(s.getFriendlyName());
                if(presets.indexOf(s.getPresetName()) > -1) { //preset selected
                    presetSpinner.setSelection(presets.indexOf(s.getPresetName()));
                }
                else{
                    presetSpinner.setSelection(presets.indexOf(MainActivityContainer.getDefaultPresetName()));
                }
            }
            return true;
        }
        return false;
    }

    public boolean onDrag(View v, DragEvent event) {
        if (!isLocked) {
//            System.out.println("draggin");
//            int action = event.getAction();
//            switch (action) {
//                case DragEvent.ACTION_DRAG_STARTED:
//                    System.out.println("started");
//                    if(event.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
//                        v.setBackgroundColor(Color.BLUE);
//                        v.invalidate();
//                        return true;
//                    }
//                    return false;
//                case DragEvent.ACTION_DRAG_ENTERED:
//                     System.out.println("entered");
//                     return true;
//                 case DragEvent.ACTION_DRAG_LOCATION:
//                     return true;
//                case DragEvent.ACTION_DRAG_ENDED:
//                    System.out.println("ended");
//                    if(event.getResult()) {
//                        System.out.println("the drop was handled");
//                    }
//                    else {
//                        System.out.println("The drop didn't work.");
//                    }
//                    return true;
//                case DragEvent.ACTION_DRAG_EXITED:
//                     System.out.println("exited");
//                     return true;
//                case DragEvent.ACTION_DROP:
//                     System.out.println("dropped");
//                     v.setBackgroundColor(Color.RED);
//                default:
//                    return false;
//            }
        }
        System.out.println("Drag and drop not yet implemented.");
        return false;
    }

    private void sendHapticFromCSV(int fileId, MetaWearBoard board) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            final HapticCSV file = csvDb.hapticsDao().loadCSVFileById(fileId);
            getActivity().runOnUiThread(() -> {
                if(file != null) {
                    String[] onTime = file.getOnTime().split(",");
                    String[] offTime = file.getOffTime().split(",");
                    for (int i = 0; i < onTime.length; i++) {
                        try {
                            float on = Float.parseFloat(onTime[i]) * 1000;
                            float off = Float.parseFloat(offTime[i]) * 1000;
                            board.getModule(Haptic.class).startMotor((short) on);
                            try {
                                Thread.sleep((long) (on + off));
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                            Toast.makeText(Objects.requireNonNull(getActivity()).getApplicationContext(), "The file could not be parsed.", Toast.LENGTH_SHORT).show();
                            break;
                        }
                    }
                }
                else {
                    System.out.println("The file could not be found.");
                }
            });
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void retrieveSensors() {
        List<SensorDevice> sensors = new ArrayList<>(MainActivityContainer.getDeviceStates().values());
        for (int i = 0; i < sensors.size(); i++) {
            addSensorBox(sensors.get(i), i);
        }
    }

    private void retrievePresets() {
        AppExecutors.getInstance().diskIO().execute(() -> {
                final List<String> p_list = pDatabase.pDao().loadAllPresetNames();
                getActivity().runOnUiThread(() -> {
                    presets = p_list;
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity().getApplicationContext(), android.R.layout.simple_spinner_item, presets);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    presetSpinner.setAdapter(adapter);
                });
        });
    }

    private void sendHapticFromPreset(SensorDevice s, MetaWearBoard board) {
        int id = s.getPreset_id();
        if(id > -1) {
            AppExecutors.getInstance().diskIO().execute(() -> {
                Preset p = pDatabase.pDao().loadPresetFromId(id);
                if (p != null) {
                    System.out.println("Playing preset " + p.getName());
                    if(p.isFromCSV()) {
                        sendHapticFromCSV(p.getCsvFile(), board);
                    }
                    else {
                        for (int i = 0; i < p.getNumCycles(); i++) {
                            board.getModule(Haptic.class).startMotor((short) (p.getOn_time() * 1000));
                            try {
                                Thread.sleep((long) (p.getOn_time() * 1000) + (long) (p.getOff_time() * 1000));
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        } //end for
                    }
                }
                else {
                    System.out.println("No preset found. Id: " + id);
                 }
        }); //end async
        }//end if
        else { //no sensor detected
            Toast.makeText(getActivity().getApplicationContext(), "No preset assigned to this sensor.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if(!isLocked) {
//            ClipData.Item item = new ClipData.Item((CharSequence) v.getTag());
//            ClipData data = new ClipData(v.getTag().toString(), new String[] {ClipDescription.MIMETYPE_TEXT_PLAIN}, item);
//            View.DragShadowBuilder shadow = new View.DragShadowBuilder(v);
//            v.startDrag(data, shadow, null, 0);
        }
        return false;
    }
}
