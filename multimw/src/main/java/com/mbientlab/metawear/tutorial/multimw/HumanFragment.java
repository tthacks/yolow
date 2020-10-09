package com.mbientlab.metawear.tutorial.multimw;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.mbientlab.metawear.AsyncDataProducer;
import com.mbientlab.metawear.Data;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.Subscriber;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.builder.RouteComponent;
import com.mbientlab.metawear.data.Acceleration;
import com.mbientlab.metawear.data.AngularVelocity;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.GyroBmi160;
import com.mbientlab.metawear.module.Haptic;
import com.mbientlab.metawear.tutorial.multimw.database.AppExecutors;
import com.mbientlab.metawear.tutorial.multimw.database.CSVDatabase;
import com.mbientlab.metawear.tutorial.multimw.database.HapticCSV;
import com.mbientlab.metawear.tutorial.multimw.database.Preset;
import com.mbientlab.metawear.tutorial.multimw.database.PresetDatabase;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import bolts.Capture;
import bolts.Continuation;
import bolts.Task;

public class HumanFragment extends Fragment implements ServiceConnection, View.OnTouchListener, View.OnDragListener, View.OnLongClickListener {

    private BtleService.LocalBinder binder;
    private PresetDatabase pDatabase;
    private CSVDatabase csvDb;
    private boolean isLocked, isRecording;
    private List<String> presets;
    private TextView lastSelected = null;
    private View sensorSettingsBar;
    private EditText sensorName;
    private Spinner presetSpinner;
    List<Accelerometer> accelModules = new ArrayList<>();
    // List<Capture<Accelerometer>> accelCaptures = new ArrayList<>();
    List<GyroBmi160> gyroModules = new ArrayList<>();

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

    @RequiresApi(api = Build.VERSION_CODES.O)
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
                //RECORDING CODE
//                final String filename = "Yolow"+ LocalDateTime.now() + ".csv";
//                final File path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename);
                for(int x = 0; x < accelModules.size(); x++) {
                    accelModules.get(x).start();
                    accelModules.get(x).acceleration().start();
                    gyroModules.get(x).angularVelocity().start();
                    gyroModules.get(x).start();
                }
            } else {
                record_button.setText(R.string.start_recording);
                lock_button.setEnabled(true);
                for(int x = 0; x < accelModules.size(); x++) {
                    accelModules.get(x).stop();
                    accelModules.get(x).acceleration().stop();
                    gyroModules.get(x).angularVelocity().stop();
                    gyroModules.get(x).stop();
                }
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
                    lastSelected.setText(truncate(editable.toString()));
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
        List<SensorDevice> sensors = new ArrayList<>(MainActivityContainer.getDeviceStates().values());
        for (int i = 0; i < sensors.size(); i++) {
            addSensorBox(sensors.get(i));
        }
    }


    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        binder= (BtleService.LocalBinder) service;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {}

    public void addNewDevice(BluetoothDevice btDevice) {
        final SensorDevice newDeviceState = new SensorDevice(btDevice.getAddress(), btDevice.getName(), getActivity().getApplicationContext());
        newDeviceState.setPresetName(MainActivityContainer.getDefaultPresetName());
        newDeviceState.setPreset_id(MainActivityContainer.getDefaultPresetId());
        final MetaWearBoard newBoard = binder.getMetaWearBoard(btDevice);

        MainActivityContainer.addDeviceToStates(newDeviceState);
        MainActivityContainer.addStateToBoards(btDevice.getAddress(), newBoard);

        newBoard.onUnexpectedDisconnect(status -> getActivity().runOnUiThread(() -> {
            MainActivityContainer.getDeviceStates().remove(newDeviceState.getUid());
            System.out.println("Lost connection with sensor " + newDeviceState.getFriendlyName() + " (Id: " + newDeviceState.getUid() + ")");
            newDeviceState.getView().setBackgroundResource(R.color.sensorBoxDisconnected);
        })
        );

        newBoard.connectAsync().onSuccessTask(task -> {
            Log.i("ADDNEWDEVICE", "Connected to " + newDeviceState.getUid());
            Accelerometer a = newBoard.getModule(Accelerometer.class);
            a.configure()
                    .odr(25f)
                    .commit();
            accelModules.add(a);
            return a.acceleration().addRouteAsync(source -> source.stream((Subscriber) (data, env) -> {
                Log.i("accelerometer", data.value(Acceleration.class).toString());
            }));
        }).onSuccessTask(task -> {
                GyroBmi160 g = newBoard.getModule(GyroBmi160.class);
                g.configure()
                        .odr(GyroBmi160.OutputDataRate.ODR_25_HZ)
                        .range(GyroBmi160.Range.FSR_2000)
                        .commit();
                gyroModules.add(g);
                return g.angularVelocity().addRouteAsync(source -> source.stream((Subscriber) (data, env) -> {
                    Log.i("gyroscope", data.value(AngularVelocity.class).toString());
                }));
        }).continueWith((Continuation<Route, Void>) task -> {
            if(task.isFaulted()) {
                Log.w("yolow", "Failed to configure app", task.getError());
            }
            else {
                newDeviceState.getView().setBackgroundResource(R.color.sensorBoxConnected);
            }
            return null;
        });
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

    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouch(View v, MotionEvent event) {
        SensorDevice s = MainActivityContainer.getDeviceStates().get(v.getTag().toString());
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if(isLocked) {
                //send haptic
                MetaWearBoard board = MainActivityContainer.getStateToBoards().get(s.getUid());
                if(board != null) {
                    sendHapticFromPreset(s, board, (TextView) v);
                }
            }
            else {
                lastSelected = (TextView) v;
                sensorSettingsBar.setVisibility(View.VISIBLE);
                sensorName.setText(truncate(s.getFriendlyName()));
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

    private String truncate(String s) {
        int MAX_LABEL_LENGTH = 10;
        if(s.length() > MAX_LABEL_LENGTH) {
            return s.substring(0, MAX_LABEL_LENGTH);
        }
        else {
            return s;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void addSensorBox(SensorDevice s) {
        ConstraintLayout constraintLayout = getView().findViewById(R.id.sensor_area);
        TextView t = s.getView();
        if(t.getParent() != null) {
            ((ConstraintLayout) t.getParent()).removeView(t);
        }
        t.setText(truncate(s.getFriendlyName()));
        if(s.isConnecting()) {
            t.setBackgroundResource(R.color.sensorBoxConnecting);
        }
        else {
            t.setBackgroundResource(R.color.sensorBoxConnected);
        }
        t.setOnTouchListener(this);
        t.setOnLongClickListener(this);
        t.setOnDragListener(this);
        t.setLayoutParams(new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT ));
        constraintLayout.addView(t);
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

    private void sendHapticFromPreset(SensorDevice s, MetaWearBoard board, TextView view) {
        int id = s.getPreset_id();
        if(id > -1) {
            AppExecutors.getInstance().diskIO().execute(() -> {
                view.setBackgroundResource(R.color.sensorBoxVibrating);
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
                view.setBackgroundResource(R.color.sensorBoxConnected);
            }); //end async
        }//end if
        else { //no sensor detected
            Toast.makeText(getActivity().getApplicationContext(), "No preset assigned to this sensor.", Toast.LENGTH_SHORT).show();
        }
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

}
