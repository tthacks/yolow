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
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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

import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.builder.filter.Comparison;
import com.mbientlab.metawear.builder.filter.ThresholdOutput;
import com.mbientlab.metawear.builder.function.Function1;
import com.mbientlab.metawear.data.Acceleration;
import com.mbientlab.metawear.data.AngularVelocity;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.GyroBmi160;
import com.mbientlab.metawear.module.Haptic;
import com.mbientlab.metawear.module.Logging;
import com.mbientlab.metawear.tutorial.multimw.database.AppExecutors;
import com.mbientlab.metawear.tutorial.multimw.database.CSVDatabase;
import com.mbientlab.metawear.tutorial.multimw.database.HapticCSV;
import com.mbientlab.metawear.tutorial.multimw.database.Preset;
import com.mbientlab.metawear.tutorial.multimw.database.PresetDatabase;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import bolts.Continuation;
import bolts.Task;

public class HumanFragment extends Fragment implements ServiceConnection, View.OnTouchListener, View.OnDragListener {

    private BtleService.LocalBinder binder;
    private PresetDatabase pDatabase;
    private CSVDatabase csvDb;
    private boolean isLocked, isRecording, dragging;
    private List<String> presets;
    private TextView lastSelected = null;
    private View sensorSettingsBar;
    private EditText sensorName;
    private Spinner presetSpinner;
    List<Accelerometer> accelModules = new ArrayList<>();
    List<GyroBmi160> gyroModules = new ArrayList<>();
    List<Logging> logModules = new ArrayList<>();
    List<String> filenames = new ArrayList<>();

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
                for(int x = 0; x < accelModules.size(); x++) {
//                    logModules.get(x).start(false);
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
//                    logModules.get(x).stop();
//                    downloadLogs(logModules.get(x), filenames.get(x));
                }
           //  tearDownBoards();
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

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void downloadLogs(Logging logging, String filename) {
//        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
//        String filePath = dir + File.separator + filename;
//        File f = new File(filePath);
//        PrintStream fileStream = null;
//        try {
//            fileStream = new PrintStream(filename);
//        System.setOut(fileStream);

        logging.downloadAsync()
                .continueWithTask((Continuation<Void, Task<Void>>) task -> {
                    Log.i("MainActivity", "Download completed");
                    return null;
                });
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }

    }

    private void tearDownBoards() {
        List<MetaWearBoard> boards = new ArrayList<>(MainActivityContainer.getStateToBoards().values());
        for(int x = 0; x < boards.size(); x++) {
            boards.get(x).tearDown();
        }
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

    @RequiresApi(api = Build.VERSION_CODES.O)
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
            Accelerometer a = newBoard.getModule(Accelerometer.class);
            Logging l = newBoard.getModule(Logging.class);
            logModules.add(l);
            String writeToFile = newDeviceState.getFriendlyName() + "_" + LocalDateTime.now() + ".csv";
            filenames.add(writeToFile);
            a.configure()
                    .odr(25f)
                    .commit();
            accelModules.add(a);
            return a.acceleration().addRouteAsync(source ->
                    source.map(Function1.RSS).lowpass((byte) 4).filter(ThresholdOutput.BINARY, 0.5f)
                            .multicast()
                            .to().stream((data, env) -> System.out.println(newDeviceState.getFriendlyName()+ "," + data.formattedTimestamp() + ",accel," + data.value(Acceleration.class).x() + "," + data.value(Acceleration.class).y() + "," + data.value(Acceleration.class).z()))
                            .to().filter(Comparison.EQ, 1).stream((data, env) -> {System.out.println(newDeviceState.getFriendlyName() + " sending haptic"); sendHapticFromPreset(newDeviceState, newBoard);})
                            .end());
        }).onSuccessTask(task -> {
                GyroBmi160 g = newBoard.getModule(GyroBmi160.class);
                g.configure()
                        .odr(GyroBmi160.OutputDataRate.ODR_25_HZ)
                        .range(GyroBmi160.Range.FSR_2000)
                        .commit();
                gyroModules.add(g);
            return g.angularVelocity().addRouteAsync(source ->
                    source.stream((data, env) -> System.out.println(newDeviceState.getFriendlyName()+ "," + data.formattedTimestamp() + ",gyro," + data.value(AngularVelocity.class).x() + "," + data.value(AngularVelocity.class).y() + data.value(AngularVelocity.class).z())));
        }).continueWith((Continuation<Route, Void>) task -> {
            if(task.isFaulted()) {
                Log.w("yolow", "Failed to configure app", task.getError());
                MainActivityContainer.getDeviceStates().remove(newDeviceState);
                MainActivityContainer.getStateToBoards().remove(newBoard);
                newBoard.tearDown();
                newDeviceState.getView().setVisibility(View.GONE);
            }
            else {
                newDeviceState.setConnecting(false);
                newDeviceState.getView().setBackgroundResource(R.color.sensorBoxConnected);
                }
            return null;
        });
    }

//    @Override
//    public boolean onLongClick(View v) {
//        System.out.println("on long click detected");
//        if(!isLocked) {
//            lastSelected = (TextView) v;
//
//        }
//        return false;
//    }

    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouch(View v, MotionEvent event) {
        SensorDevice s = MainActivityContainer.getDeviceStates().get(v.getTag().toString());
        float x = event.getX();
        float y = event.getY();
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
        else if(event.getAction() == MotionEvent.ACTION_MOVE) {
            if (!isLocked) {
                ClipData data = ClipData.newPlainText("", "");
                View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
                v.startDrag(data, shadowBuilder, v, 0);
                return true;
            }
        }
        return false;
    }

    public boolean onDrag(View v, DragEvent event) {
        System.out.println("Starting position: " + lastSelected.getX() + " " + lastSelected.getY());
        if (!isLocked) {
            System.out.println("Drag type: " + event.getAction());
            if(event.getAction() == DragEvent.ACTION_DRAG_STARTED) {
                return true;
            }
            else if(event.getAction() == DragEvent.ACTION_DRAG_ENDED) {
                float x = event.getX();
                float y = event.getY() - 300;
                lastSelected.setX(x - lastSelected.getWidth() / 2);
                lastSelected.setY(y);
                System.out.println("Ending position: " + lastSelected.getX() + " " + lastSelected.getY());
                return true;
            }
        }
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
//        t.setOnLongClickListener(this);
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

    private void sendHapticFromPreset(SensorDevice s, MetaWearBoard board) {
        int id = s.getPreset_id();
        if(id > -1) {
            AppExecutors.getInstance().diskIO().execute(() -> {
                s.getView().setBackgroundResource(R.color.sensorBoxVibrating);
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
                s.getView().setBackgroundResource(R.color.sensorBoxConnected);
            }); //end async
        }//end if
        else { //no sensor detected
//            Toast.makeText(getActivity().getApplicationContext(), "No preset assigned to this sensor.", Toast.LENGTH_SHORT).show();
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
