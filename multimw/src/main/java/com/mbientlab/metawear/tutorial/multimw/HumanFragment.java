package com.mbientlab.metawear.tutorial.multimw;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintSet;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import androidx.constraintlayout.widget.ConstraintLayout;

import com.mbientlab.metawear.AsyncDataProducer;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.Subscriber;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.AccelerometerBosch;
import com.mbientlab.metawear.module.AccelerometerMma8452q;
import com.mbientlab.metawear.module.Haptic;
import com.mbientlab.metawear.module.Switch;
import com.mbientlab.metawear.tutorial.multimw.database.AppExecutors;
import com.mbientlab.metawear.tutorial.multimw.database.HapticCSV;
import com.mbientlab.metawear.tutorial.multimw.database.Preset;
import com.mbientlab.metawear.tutorial.multimw.database.PresetDatabase;

import org.w3c.dom.Text;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import bolts.Capture;
import bolts.Continuation;

public class HumanFragment extends Fragment implements ServiceConnection, View.OnTouchListener, View.OnDragListener, View.OnLongClickListener {

    private BtleService.LocalBinder binder;
    private boolean isLocked, isRecording;
    private View lastSelected = null;
    private EditText sensorName;
    private Spinner presetSpinner;
    private PresetDatabase pDatabase;
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
        //button controls
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
                    //TODO: where you left off
                    AppExecutors.getInstance().diskIO().execute(() -> {
                        final int preset_id = pDatabase.pDao().getIdFromPresetName((String) adapterView.getItemAtPosition(i));
                        getActivity().runOnUiThread(() -> {s.setPreset_id(preset_id);});
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
                lock_button.setText("UNLOCK");
            } else {
                lock_button.setText("LOCK");
            }
        });
        record_button.setOnClickListener(v -> {
            isRecording = !isRecording;
            if (isRecording) {
                record_button.setText("STOP RECORDING");
                lock_button.setEnabled(false);
            } else {
                record_button.setText("START RECORDING");
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
        final MetaWearBoard newBoard = binder.getMetaWearBoard(btDevice);

        int idx = MainActivityContainer.addDeviceToStates(newDeviceState);
        MainActivityContainer.addStateToBoards(btDevice.getAddress(), newBoard);
//        TextView t = addSensorBox(newDeviceState, idx);
        retrieveSensors();

        newBoard.onUnexpectedDisconnect(status -> getActivity().runOnUiThread(() -> MainActivityContainer.getDeviceStates().remove(newDeviceState.getUid())));
        newBoard.connectAsync().onSuccessTask(task -> {
            getActivity().runOnUiThread(() -> {
                //TODO: change color after connecting
            });
            return null;
        });
    }

    private TextView addSensorBox(SensorDevice s, int idx) {
        ConstraintLayout constraintLayout = getView().findViewById(R.id.sensorbox_area);
        TextView newSensor = new TextView(getActivity().getApplicationContext());
        newSensor.setText(s.getFriendlyName());
        newSensor.setTag(s.getUid());
        newSensor.setX(idx * 300);
        newSensor.setY(300);
        newSensor.setBackgroundResource(R.color.colorAccentShy);
        newSensor.setPadding(24, 16, 24, 16);
        newSensor.setTextSize(24);
        newSensor.setOnLongClickListener(this);
        newSensor.setOnDragListener(this);
        newSensor.setOnTouchListener(this);
        newSensor.setLayoutParams(new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT ));
        constraintLayout.addView(newSensor);
        return newSensor;
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
                lastSelected = v;
                sensorName.setText(s.getFriendlyName());
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

    private void sendHapticFromCSV(String filename, MetaWearBoard board) {
        HapticCSV file = MainActivityContainer.csvFiles.get(filename);
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
                    Toast.makeText(Objects.requireNonNull(getActivity()).getApplicationContext(), "There was something wrong with the file.", Toast.LENGTH_SHORT).show();
                    break;
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void retrieveSensors() {
        List<SensorDevice> sensors = new ArrayList<>(MainActivityContainer.getDeviceStates().values());
        ConstraintLayout constraintLayout = Objects.requireNonNull(getView()).findViewById(R.id.sensorbox_area);
        for (int i = 0; i < sensors.size(); i++) {
            addSensorBox(sensors.get(i), i);
        }
    }

    private void retrievePresets() {
        AppExecutors.getInstance().diskIO().execute(() -> {
                final List<String> p_list = pDatabase.pDao().loadAllPresetNames();
                getActivity().runOnUiThread(() -> {
                    System.out.println("Presets: " + p_list);
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
