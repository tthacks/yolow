package com.mbientlab.metawear.tutorial.multimw;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
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

import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.builder.filter.Comparison;
import com.mbientlab.metawear.builder.function.Function1;
import com.mbientlab.metawear.data.Acceleration;
import com.mbientlab.metawear.data.AngularVelocity;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.GyroBmi160;
import com.mbientlab.metawear.module.Haptic;
import com.mbientlab.metawear.module.Settings;
import com.mbientlab.metawear.tutorial.multimw.database.AppDatabase;
import com.mbientlab.metawear.tutorial.multimw.database.AppExecutors;
import com.mbientlab.metawear.tutorial.multimw.database.HapticCSV;
import com.mbientlab.metawear.tutorial.multimw.database.Preset;
import com.mbientlab.metawear.tutorial.multimw.database.Session;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import bolts.Continuation;


public class HumanFragment extends Fragment implements ServiceConnection, View.OnTouchListener, View.OnDragListener {

    private static final int REQUEST_START_BLE_SCAN = 1;
    private static final int MOVING_AVERAGE = 50;
    private static final boolean VERBOSE = true;
    private static boolean streamStarted = false;
    private BtleService.LocalBinder binder;
    private AppDatabase database;
    private boolean isLocked, isRecording;
    private List<String> presets;
    private TextView lastSelected = null;
    private View sensorSettingsBar;
    private EditText sensorName;
    private Spinner presetSpinner;
    private Preset defaultPreset;
    private String defaultPresetName = "";
    List<Accelerometer> accelModules = new ArrayList<>();
    List<GyroBmi160> gyroModules = new ArrayList<>();
    List<FileWriter> fileWriters = new ArrayList<>();
    HashMap<String, BufferedWriter> gyro_files = new HashMap<>();
    HashMap<String, BufferedWriter> accel_files = new HashMap<>();
    FileWriter hapticWriter;

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
        database = AppDatabase.getInstance(getActivity().getApplicationContext());
        fetchDefaultPreset();
        //button controls
        sensorSettingsBar = view.findViewById(R.id.sensor_data_layout);
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
                    if (s != null) {
                        AppExecutors.getInstance().diskIO().execute(() -> {
                            final int preset_id = database.pDao().getIdFromPresetName((String) adapterView.getItemAtPosition(i));
                            getActivity().runOnUiThread(() -> {
                                s.setPreset_id(preset_id);
                                s.setPresetName((String) adapterView.getItemAtPosition(i));
                            });
                        });
                    }
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        Button lock_button = view.findViewById(R.id.button_lock);
        Button record_button = view.findViewById(R.id.button_record);
        Button scan_devices_button = view.findViewById(R.id.scan_devices_button);
        scan_devices_button.setOnClickListener(v -> getActivity().startActivityForResult(new Intent(getActivity(), ScannerActivity.class), REQUEST_START_BLE_SCAN));

        lock_button.setOnClickListener(v -> {
            isLocked = !isLocked;
            if (isLocked) {
                lock_button.setText(R.string.unlock);
                presetSpinner.setEnabled(false);
                sensorName.setEnabled(false);

            } else {
                lock_button.setText(R.string.lock);
                presetSpinner.setEnabled(true);
                sensorName.setEnabled(true);
            }
        });
        record_button.setOnClickListener(v -> {
            if (!isRecording) {
                LocalDateTime now = LocalDateTime.now();
                String timestamp = now.toString().replaceAll(":", ".");
                boolean fileSuccessful = createSessionFiles(timestamp);
                if(fileSuccessful) {
                    for (int x = 0; x < accelModules.size(); x++) {
                        accelModules.get(x).packedAcceleration().start();
                        accelModules.get(x).start();
                        gyroModules.get(x).packedAngularVelocity().start();
                        gyroModules.get(x).start();
                    }
                    isRecording = true;
                    isLocked = true;
                    record_button.setText(R.string.stop_recording);
                    record_button.setBackgroundResource(R.color.sensorBoxVibrating);
                    lock_button.setEnabled(false);
                    scan_devices_button.setEnabled(false);
                }
                else {
                    if(VERBOSE) {
                        Log.i("CreateSessionFiles", "could not start the session.");
                    }
                }
            } else {
                isRecording = false;
                isLocked = false;
                record_button.setText(R.string.start_recording);
                record_button.setBackgroundResource(R.color.colorAccent2);
                lock_button.setEnabled(true);
                scan_devices_button.setEnabled(true);
                for(int x = 0; x < accelModules.size(); x++) {
                    accelModules.get(x).packedAcceleration().stop();
                    accelModules.get(x).stop();
                    gyroModules.get(x).packedAngularVelocity().stop();
                    gyroModules.get(x).stop();
                }
                closeSessionFiles();
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

    private void createPresetFiles(File dir) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            final List<Preset> presetList = database.pDao().loadAllPresets();
                try {
                    for (int i = 0; i < presetList.size(); i++) {
                        Preset p = presetList.get(i);
                        File p_file = new File(dir, p.getName() + ".csv");
                        if (!p_file.exists()) {
                            p_file.createNewFile();
                        }
                        FileWriter p_fileWriter = new FileWriter(p_file);
                        p_fileWriter.write("on,off,intensity\n");
                        if (p.isFromCSV()) {
                            writePresetFromCSV(p.getCsvFile(), p_fileWriter);
                        } else {
                            for (int x = 0; x < p.getNumCycles(); x++) {
                                p_fileWriter.write(p.getOn_time() + "," + p.getOff_time() + "," + p.getIntensity() + "\n");
                            }
                            p_fileWriter.close();
                        }
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private boolean createSessionFiles(String timestamp) {

        if(presets.size() == 0) {
            if(VERBOSE) {
                Log.i("CreateSessionFiles", "There are no presets. You will need to create some to record data.");
            }
            Snackbar.make(getActivity().findViewById(R.id.activity_main_layout), "There are no presets. You will need to create some to record data.", Snackbar.LENGTH_SHORT).show();
            return false;
        }
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            //If it isn't mounted - we can't write into it.
            return false;
        }
        //Create a new file that points to the root directory, with the given name:
            File dir = new File(getActivity().getApplicationContext().getExternalFilesDir(null), "yolow_" + timestamp);
        if(!dir.mkdirs()) {
            dir.mkdirs();
        }

        try {
            File haptics_file = new File(dir, timestamp + "_haptic.csv");
            if (!haptics_file.exists()) {
                haptics_file.createNewFile();
            }
            hapticWriter = new FileWriter(haptics_file);
            hapticWriter.write("Timestamp,Sensor Name,Preset\n");
            createPresetFiles(dir);

            List<SensorDevice> sensorDevices = new ArrayList<>(MainActivityContainer.getDeviceStates().values());
            Session new_session = new Session(timestamp, sensorDevices.size(), presets.size());
            AppExecutors.getInstance().diskIO().execute(() -> {
                database.sDao().insert(new_session);
            });
            for (int x = 0; x < sensorDevices.size(); x++) {
                SensorDevice s = sensorDevices.get(x);
                if (s == null) {
                    if(VERBOSE) {
                        Log.i("Create Session Files", "Sorry, something's gone wrong. Could not find the sensors.");
                    }
                    Snackbar.make(getActivity().findViewById(R.id.activity_main_layout), "Sorry, something's gone wrong. Could not find the sensors.", Snackbar.LENGTH_SHORT).show();
                    return false;
                }
                String sensorFileName = s.getFriendlyName() + "_" + timestamp + "_" + s.getUidFileFriendly();
                File newAccelFile = new File(dir, sensorFileName + "_accelerometer.csv");
                if (!newAccelFile.exists()) {
                    newAccelFile.createNewFile();
                }
                File newGyroFile = new File(dir, sensorFileName + "_gyroscope.csv");
                if (!newGyroFile.exists()) {
                    newGyroFile.createNewFile();
                }
                FileWriter fw = new FileWriter(newAccelFile);
                FileWriter fw2 = new FileWriter(newGyroFile);
                BufferedWriter accelWriter = new BufferedWriter(fw);
                BufferedWriter gyroWriter = new BufferedWriter(fw2);
                //headers
                accelWriter.write("epoc (ms),timestamp (-0700),elapsed (s),x-axis (g),y-axis (g),z-axis (g)\n");
                gyroWriter.write("epoc (ms),timestamp (-0700),elapsed (s),x-axis (deg/s),y-axis (deg/s),z-axis (deg/s)\n");
                accel_files.put(s.getUid(), accelWriter);
                gyro_files.put(s.getUid(), gyroWriter);
                s.setAccel_writer(accelWriter);
                s.setGyro_writer(gyroWriter);
                MainActivityContainer.getDeviceStates().replace(s.getUid(), s);
                fileWriters.add(fw);
                fileWriters.add(fw2);
            }
        }
        catch(IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void closeSessionFiles() {
        List<BufferedWriter> accelFiles = new ArrayList<>(accel_files.values());
        List<BufferedWriter> gyroFiles = new ArrayList<>(gyro_files.values());
        try {
            hapticWriter.flush();
            hapticWriter.close();
            for (int x = 0; x < accelFiles.size(); x++) {
                accelFiles.get(x).close();
                gyroFiles.get(x).close();
            }
            for(FileWriter f : fileWriters) {
                f.close();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        streamStarted = false;
        Snackbar.make(getActivity().findViewById(R.id.activity_main_layout), "Stream successfully saved.", Snackbar.LENGTH_SHORT).show();
    }

    private void tearDownBoards() {
        List<MetaWearBoard> boards = new ArrayList<>(MainActivityContainer.getStateToBoards().values());
        for(MetaWearBoard board : boards) {
            board.tearDown();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        retrievePresets();
        List<SensorDevice> sensors = new ArrayList<>(MainActivityContainer.getDeviceStates().values());
        for(SensorDevice sensor : sensors) {
            addSensorBox(sensor);
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
        if(defaultPreset == null) {
            //no presets have been created yet
            newDeviceState.setPreset_id(-1);
        }
        else {
            newDeviceState.setPreset_id(defaultPreset.getId());
        }
        newDeviceState.setPresetName(defaultPresetName);
        final MetaWearBoard newBoard = binder.getMetaWearBoard(btDevice);

        MainActivityContainer.addDeviceToStates(newDeviceState);
        MainActivityContainer.addStateToBoards(btDevice.getAddress(), newBoard);

        newBoard.onUnexpectedDisconnect(status -> getActivity().runOnUiThread(() -> {
            MainActivityContainer.getDeviceStates().remove(newDeviceState.getUid());
            if(VERBOSE) {
                Log.w("ADDNEWDEVICE", "Lost connection with sensor " + newDeviceState.getFriendlyName() + " (Id: " + newDeviceState.getUid() + ")");
            }
            newDeviceState.getView().setBackgroundResource(R.color.sensorBoxDisconnected);
            newDeviceState.getView().setTextColor(getResources().getColor(R.color.white));
                    Snackbar.make(getActivity().findViewById(R.id.activity_main_layout), "Lost connection with sensor " + newDeviceState.getFriendlyName() + "(Id: " + newDeviceState.getUid() + ")", Snackbar.LENGTH_LONG).show();
        })
        );

        newBoard.connectAsync().onSuccessTask(task -> {
            Accelerometer a = newBoard.getModule(Accelerometer.class);
            a.configure()
                    .odr(100)
                    .range(4f)
                    .commit();
            accelModules.add(a);
            return a.packedAcceleration().addRouteAsync(source ->
                    source.multicast()
                    .to().map(Function1.RMS).lowpass((byte) MOVING_AVERAGE).filter(Comparison.GTE, 1).stream((data, env) -> sendHapticFromPreset(newDeviceState, newBoard, false))
                    .to().stream((data, env) -> {
                        try {
                            newDeviceState.getAccel_writer().write(data.timestamp().getTimeInMillis() + "," + data.formattedTimestamp() + ",," + data.value(Acceleration.class).x() + "," + data.value(Acceleration.class).y() + "," + data.value(Acceleration.class).z() + "\n");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    })
                    .end());
        }).onSuccessTask(task -> {
                GyroBmi160 g = newBoard.getModule(GyroBmi160.class);
                g.configure()
                        .odr(GyroBmi160.OutputDataRate.ODR_100_HZ)
                        .commit();
                gyroModules.add(g);
            return g.packedAngularVelocity().addRouteAsync(source ->
                    source.stream((data, ev) -> {
                        try {
                            newDeviceState.getGyro_writer().write(data.timestamp().getTimeInMillis() + "," + data.formattedTimestamp() + ",," + data.value(AngularVelocity.class).x() + "," + data.value(AngularVelocity.class).y() + "," + data.value(AngularVelocity.class).z() + "\n");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }));
        }).continueWith((Continuation<Route, Void>) task -> {
            if (task.isFaulted()) {
                if(VERBOSE) {
                    Log.w("yolow", "Failed to configure app", task.getError());
                }
                if (!newBoard.isConnected()) {
                    getActivity().runOnUiThread(() -> {
                        Snackbar.make(getActivity().findViewById(R.id.activity_main_layout), task.getError().getLocalizedMessage(), Snackbar.LENGTH_SHORT).show();
                        MainActivityContainer.getDeviceStates().remove(newDeviceState.getUid());
                        MainActivityContainer.getStateToBoards().remove(newDeviceState.getUid());
                        newDeviceState.getView().setVisibility(View.GONE);
                    });
                } else {
                    Snackbar.make(getActivity().findViewById(R.id.activity_main_layout), task.getError().getLocalizedMessage(), Snackbar.LENGTH_SHORT).show();
                    newBoard.tearDown();
                    newBoard.disconnectAsync().continueWith((Continuation<Void, Void>) task1 -> {
                        MainActivityContainer.getDeviceStates().remove(newDeviceState.getUid());
                        MainActivityContainer.getStateToBoards().remove(newDeviceState.getUid());
                        newDeviceState.getView().setVisibility(View.GONE);
                        return null;
                    });
                }
            }
            else {
                newDeviceState.setConnecting(false);
                newDeviceState.getView().setBackgroundResource(R.color.sensorBoxConnected);
                newBoard.getModule(Settings.class).editBleConnParams()
                        .maxConnectionInterval(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? 11.25f : 7.5f)
                        .commit();
            }
            return null;
        });
    }

//    @RequiresApi(api = Build.VERSION_CODES.O)
//    private void computeMovingAvg(String uid, Data data, Queue<Double> queue) {
//        double runningAvg = runningAvgs.get(uid);
//        double newVal = Math.sqrt(Math.pow(data.value(Acceleration.class).x(), 2) + Math.pow(data.value(Acceleration.class).y(), 2) + Math.pow(data.value(Acceleration.class).z(), 2));
//        queue.add(newVal);
//        runningAvg += newVal;
//        if (queue.size() == MOVING_AVERAGE) {
//            double moving_avg = runningAvg / MOVING_AVERAGE;
//            if(VERBOSE) {
//                Log.i("Moving average", "" + moving_avg);
//            }
//            if(moving_avg > 2) {
//            sendHapticFromPreset(MainActivityContainer.getDeviceStates().get(uid), MainActivityContainer.getStateToBoards().get(uid), false);
//            }
//            double val = queue.remove();
//            runningAvg = runningAvg - val;
//        }
//        runningAvgs.replace(uid, runningAvg);
//    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouch(View v, MotionEvent event) {
        SensorDevice s = MainActivityContainer.getDeviceStates().get(v.getTag().toString());
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if(isLocked) {
                //send haptic
                if(s == null) {
                    System.out.println("Device has been disconnected.");
                    return false;
                }
                MetaWearBoard board = MainActivityContainer.getStateToBoards().get(s.getUid());
                if(board != null) {
                   sendHapticFromPreset(s, board, true);
                }
            }
            else {
                if(s == null) {
                    System.out.println("Device has been disconnected.");
                    return false;
                }
                lastSelected = (TextView) v;
                sensorSettingsBar.setVisibility(View.VISIBLE);
                sensorName.setText(truncate(s.getFriendlyName()));
                if(presets.indexOf(s.getPresetName()) > -1) { //preset selected
                    presetSpinner.setSelection(presets.indexOf(s.getPresetName()));
                }
                else{
                    presetSpinner.setSelection(presets.indexOf(defaultPresetName));
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
        if (!isLocked) {
            if(event.getAction() == DragEvent.ACTION_DRAG_STARTED) {
                return true;
            }
            else if(event.getAction() == DragEvent.ACTION_DRAG_ENDED) {
                if(event.getX() == 0.0 && event.getY() == 0) {
                    return true;
                }
                float x = event.getX() + 20 - lastSelected.getWidth() / 2;
                float y = event.getY() - 280;
                if(y < 0) {
                    y = 0;
                }
                if(x < 0) {
                    x = 0;
                }
                lastSelected.setX(x - lastSelected.getWidth() / 2);
                lastSelected.setY(y);
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
        t.setOnDragListener(this);
        t.setLayoutParams(new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT ));
        constraintLayout.addView(t);
    }

    private void retrievePresets() {
        AppExecutors.getInstance().diskIO().execute(() -> {
                final List<String> p_list = database.pDao().loadAllPresetNames();
                fetchDefaultPreset();
                getActivity().runOnUiThread(() -> {
                    presets = p_list;
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity().getApplicationContext(), android.R.layout.simple_spinner_item, presets);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    presetSpinner.setAdapter(adapter);
                });
        });
    }

    private void fetchDefaultPreset() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            final Preset def = database.pDao().getDefaultPreset();
               defaultPreset = def;
               if(def != null) {
                   defaultPresetName = def.getName();
               }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void sendHapticFromPreset(SensorDevice s, MetaWearBoard board, boolean fromTouch) {
        if(s.isHapticLocked()) { //already vibrating - dismiss the secondary haptic quietly
            return;
        }
        int id = s.getPreset_id();
        int boxColor = fromTouch ? R.color.sensorBoxVibrating : R.color.sensorBoxCriteriaMet;
        if(id > -1) {
            AppExecutors.getInstance().diskIO().execute(() -> {
                Preset p_id = database.pDao().loadPresetFromId(id);
                Preset p = p_id == null ? defaultPreset : p_id;
                if (p != null) {
                    HapticCSV file = database.hDao().loadCSVFileById(p.getCsvFile());
                    System.out.println("Playing preset " + p.getName());
                    if(isRecording) {
                        try {
                            hapticWriter.append( Calendar.getInstance().getTimeInMillis() + "," + s.getFriendlyName() + "," + p.getName() + "\n");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    Runnable runnable;
                    if(p.isFromCSV()) {
                        runnable = () -> {
                            s.getView().setBackgroundResource(boxColor);
                            sendHapticFromCSV(file, board);
                            s.getView().setBackgroundResource(R.color.sensorBoxConnected);
                            s.lockHaptic(false);
                        };
                    }
                    else {
                        runnable = () -> {
                            s.lockHaptic(true);
                            s.getView().setBackgroundResource(boxColor);
                            for (int i = 0; i < p.getNumCycles(); i++) {
                                board.getModule(Haptic.class).startMotor(p.getIntensity(), (short) (p.getOn_time() * 1000));
                                try {
                                    Thread.sleep((long) (p.getOn_time() * 1000) + (long) (p.getOff_time() * 1000));
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            } //end for
                            s.lockHaptic(true);
                            s.getView().setBackgroundResource(R.color.sensorBoxConnected);
                            s.lockHaptic(false);
                        };//end runnable
                    }
                    Thread thread = new Thread(runnable);
                    thread.start();
                }
                else {
                    if(VERBOSE) {
                        Log.i("sendHapticFromPreset", "Invalid preset. Was it deleted?");
                    }
                }
            }); //end async
        }//end if
        else { //no sensor detected
            if(VERBOSE) {
                Log.i("sendHapticFromPReset", "No sensor found.");
            }
        }
    }

    private void writePresetFromCSV(int fileId, FileWriter fileWriter) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            final HapticCSV file = database.hDao().loadCSVFileById(fileId);
                if (file != null) {
                    String[] onTime = file.getOnTime().split(",");
                    String[] offTime = file.getOffTime().split(",");
                    String[] intensity = file.getIntensity().split(",");
                    for (int i = 0; i < onTime.length; i++) {
                        try {
                            fileWriter.write(onTime[i] + "," + offTime[i] + "," + intensity[i] + "\n");
                        } catch (IOException e) {
                            e.printStackTrace();
                            if(VERBOSE) {
                                Log.i("WritePresetFromCSV", "The file could not be parsed. File: " + file.getFilename());
                            }
                            break;
                        }
                    }
                }
                try {
                    fileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
    }

    private void sendHapticFromCSV(HapticCSV file, MetaWearBoard board) {
                if(file != null) {
                    String[] onTime = file.getOnTime().split(",");
                    String[] offTime = file.getOffTime().split(",");
                    String[] intensity = file.getIntensity().split(",");
                    for (int i = 0; i < onTime.length; i++) {
                        try {
                            float on = Float.parseFloat(onTime[i]) * 1000;
                            float off = Float.parseFloat(offTime[i]) * 1000;
                            float intens = Float.parseFloat(intensity[i]);
                            board.getModule(Haptic.class).startMotor(intens, (short) on);
                            try {
                                Thread.sleep((long) (on + off));
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                            System.out.println("The file could not be parsed. File: " + file.getFilename());
                            break;
                        }
                    }
                }
                else {
                    System.out.println("The file could not be found.");
                }
    }

    synchronized void startStream() {
        if(!streamStarted && isRecording) {
            Snackbar.make(getActivity().findViewById(R.id.activity_main_layout), "Streaming successfully started.", Snackbar.LENGTH_SHORT).show();
            streamStarted = true;
        }
    }

}
