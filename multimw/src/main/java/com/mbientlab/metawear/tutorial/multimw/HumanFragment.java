package com.mbientlab.metawear.tutorial.multimw;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
import com.mbientlab.metawear.tutorial.multimw.database.PresetDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import bolts.Capture;
import bolts.Continuation;

public class HumanFragment extends Fragment implements ServiceConnection, View.OnTouchListener, View.OnDragListener {

    private BtleService.LocalBinder binder;
    private boolean isLocked, isRecording;
    private View lastSelected = null;
    private EditText sensorName;
    private Spinner sensorPreset;
    private PresetDatabase pDatabase;
    private List<String> presets;
    private ArrayAdapter<String> presetAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Activity owner = getActivity();
        owner.getApplicationContext().bindService(new Intent(owner, BtleService.class), this, Context.BIND_AUTO_CREATE);
        presets = new ArrayList<>();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().getApplicationContext().unbindService(this);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        binder= (BtleService.LocalBinder) service;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {}


    public void addNewDevice(BluetoothDevice btDevice) {

        final SensorDevice newDeviceState = new SensorDevice(btDevice.getAddress(), btDevice.getName());
        final MetaWearBoard newBoard= binder.getMetaWearBoard(btDevice);
        MainActivityContainer.getDeviceStates().put(newDeviceState.uid, newDeviceState);
        retrieveSensors();
        MainActivityContainer.addStateToBoards(btDevice.getAddress(), newBoard);

        final Capture<AsyncDataProducer> orientCapture = new Capture<>();
        final Capture<Accelerometer> accelCapture = new Capture<>();

        newBoard.onUnexpectedDisconnect(status -> getActivity().runOnUiThread(() -> {
            MainActivityContainer.getDeviceStates().remove(newDeviceState.uid);
            retrieveSensors();}
        ));
        newBoard.connectAsync().onSuccessTask(task -> {
            getActivity().runOnUiThread(() -> {
                newDeviceState.connecting= false;
                MainActivityContainer.getSensorById(newDeviceState.uid).connecting = false;
                retrieveSensors();
            });

            final Accelerometer accelerometer = newBoard.getModule(Accelerometer.class);
            accelCapture.set(accelerometer);

            final AsyncDataProducer orientation;
            if (accelerometer instanceof AccelerometerBosch) {
                orientation = ((AccelerometerBosch) accelerometer).orientation();
            } else {
                orientation = ((AccelerometerMma8452q) accelerometer).orientation();
            }
            orientCapture.set(orientation);

            return orientation.addRouteAsync(source -> source.stream((data, env) -> {
                getActivity().runOnUiThread(() -> {
                });
            }));
        }).onSuccessTask(task -> newBoard.getModule(Switch.class).state().addRouteAsync(source -> source.stream((Subscriber) (data, env) -> getActivity().runOnUiThread(() -> {
        })))).continueWith((Continuation<Route, Void>) task -> {
            if (task.isFaulted()) {
                if (!newBoard.isConnected()) {
                    getActivity().runOnUiThread(() -> MainActivityContainer.getDeviceStates().remove(newDeviceState.uid));
                    retrieveSensors();
                } else {
                    Snackbar.make(getActivity().findViewById(R.id.activity_main_layout), task.getError().getLocalizedMessage(), Snackbar.LENGTH_SHORT).show();
                    newBoard.tearDown();
                    newBoard.disconnectAsync().continueWith((Continuation<Void, Void>) task1 -> {
                        MainActivityContainer.getDeviceStates().remove(newDeviceState.uid);
                        retrieveSensors();
                        return null;
                    });
                }
            } else {
                orientCapture.get().start();
                accelCapture.get().start();
            }
            return null;
        });
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
        sensorPreset = view.findViewById(R.id.sensor_preset_select);
        presetAdapter = new ArrayAdapter<>(getActivity().getApplicationContext(), android.R.layout.simple_spinner_item, presets);
        retrievePresets();
        Button lock_button = view.findViewById(R.id.button_lock);
        Button record_button = view.findViewById(R.id.button_record);
        if(lastSelected == null) {
            sensorName.setClickable(false);
            sensorPreset.setClickable(false);
        }
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
                    s.friendlyName = editable.toString();
                }
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        retrieveSensors();
    }

    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouch(View v, MotionEvent event) {
        SensorDevice s = MainActivityContainer.getDeviceStates().get(v.getTag().toString());
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (!isLocked) {
                ClipData.Item item = new ClipData.Item((CharSequence) v.getTag());
                String[] mimeTypes = {ClipDescription.MIMETYPE_TEXT_PLAIN};
                ClipData data = new ClipData(v.getTag().toString(), mimeTypes, item);
                View.DragShadowBuilder dragshadow = new View.DragShadowBuilder(v);
                v.startDrag(data, dragshadow, null, 0);
                lastSelected = v;
                sensorName.setClickable(true);
                sensorName.setText(s.friendlyName);
                sensorPreset.setClickable(true);
                return true;
            } else {
                //send haptic
                MetaWearBoard board = MainActivityContainer.getStateToBoards().get(s.uid);
                if(board != null) {
                    if(s.usingCSV) {
                            sendHapticFromCSV(s.csvFile, board);
                        }
                        else {
                            for (int i = 0; i < s.totalCycles; i++) {
                                board.getModule(Haptic.class).startMotor((short) (s.onDuration * 1000));
                                try {
                                    Thread.sleep((long) (s.onDuration * 1000) + (long) (s.offDuration * 1000));
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                }
            }
            return true;
        }
        return false;
    }

    private void sendHapticFromCSV(String filename, MetaWearBoard board) {
        HapticCSV file = MainActivityContainer.csvFiles.get(filename);
        if(file != null) {
            System.out.println("on: " + file.getOnTime());
            System.out.println("off" + file.getOffTime());
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

    public boolean onDrag(View v, DragEvent event) {
        if (!isLocked) {
            int action = event.getAction();
            switch (action) {
                case DragEvent.ACTION_DRAG_ENDED:
                    if (lastSelected != null) {
                        lastSelected.setX(event.getX());
                        lastSelected.setY(event.getY());
                        SensorDevice s = MainActivityContainer.getSensorById(lastSelected.getTag().toString());
                        if(s != null) {
                            s.x_loc = event.getX();
                            s.y_loc = event.getY();
                        }
                    }
                    return true;
                case DragEvent.ACTION_DRAG_STARTED:
                    return true;
            }
            return false;
        }
        return false;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void retrieveSensors() {
        List<SensorDevice> sensors = new ArrayList<>(MainActivityContainer.getDeviceStates().values());
        ConstraintLayout constraintLayout = Objects.requireNonNull(getView()).findViewById(R.id.sensorbox_area);
        for (int i = 0; i < sensors.size(); i++) {
            SensorDevice s = sensors.get(i);
            TextView sensorbox = new TextView(Objects.requireNonNull(getActivity()).getApplicationContext());
            sensorbox.setText(s.friendlyName);
            System.out.println("UID: " + s.uid);
            sensorbox.setTag(s.uid);
            sensorbox.setBackgroundResource(R.color.sensorboxDefault);
            if(s.x_loc == 0) {
                sensorbox.setX(i * 300);
            }
            else {
                sensorbox.setX(s.x_loc);
                sensorbox.setY(s.y_loc);
            }
            sensorbox.setTextSize(24);
            sensorbox.setPadding(16, 16, 16, 16);
            ConstraintLayout.LayoutParams clpSensorbox = new ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
            sensorbox.setLayoutParams(clpSensorbox);
            constraintLayout.addView(sensorbox);
            sensorbox.setOnTouchListener(this);
            sensorbox.setOnDragListener(this);
        }
    }

    private void retrievePresets() {
        AppExecutors.getInstance().diskIO().execute(() -> {
                final List<String> p_list = pDatabase.pDao().loadAllPresetNames();
                getActivity().runOnUiThread(() -> {
                        presets = p_list;
                });
        });
    }
}
