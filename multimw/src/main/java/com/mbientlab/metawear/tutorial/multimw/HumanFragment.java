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
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.mbientlab.metawear.Data;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.builder.RouteComponent;
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

import org.jetbrains.annotations.Nullable;

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
    private static final boolean VERBOSE = true;
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
    //HashMap<String, Data> prevAccelData = new HashMap<>();

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


    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // fetch database and default settings
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
                    // if you want to ONLY run accelerometer or gyroscope for certain sensors,
                    // this is where you would do it
                    for (int x = 0; x < accelModules.size(); x++) {
                        accelModules.get(x).acceleration().start();
                        accelModules.get(x).start();
                        gyroModules.get(x).angularVelocity().start();
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
                    accelModules.get(x).acceleration().stop();
                    accelModules.get(x).stop();
                    gyroModules.get(x).angularVelocity().stop();
                    gyroModules.get(x).stop();
                }
                closeSessionFiles();
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

    /**
     * Writes the preset patterns to files on recording start
     * @param dir the directory in which the files are saved
     */
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

    /**
     * Create the files and FileWriters for the recording session
     * @param timestamp The time the files were created
     * @return true if the files were created successfully
     */
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

    /**
     * Flushes and closes the file writers and performs cleanup when the recording session has ended.
     */
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
        Snackbar.make(getActivity().findViewById(R.id.activity_main_layout), "Stream successfully saved.", Snackbar.LENGTH_SHORT).show();
    }

    /**
     * Connect to and configure a new sensor and set up accelerometer and gyroscope data routes
     * @param btDevice the bluetooth device connected
     */
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
        // if the bluetooth connection was lost unexpectedly, due to poor connection or battery loss
        newBoard.onUnexpectedDisconnect(status -> getActivity().runOnUiThread(() -> {
            MainActivityContainer.getDeviceStates().remove(newDeviceState.getUid());
            if(VERBOSE) {
                Log.w("ADDNEWDEVICE", "Lost connection with sensor " + newDeviceState.getFriendlyName() + " (Id: " + newDeviceState.getUid() + ")");
            }
            newDeviceState.getView().setBackgroundResource(R.color.sensorBoxDisconnected);
            newDeviceState.getView().setTextColor(getResources().getColor(R.color.white, getActivity().getTheme()));
                    Snackbar.make(getActivity().findViewById(R.id.activity_main_layout), "Lost connection with sensor " + newDeviceState.getFriendlyName() + "(Id: " + newDeviceState.getUid() + ")", Snackbar.LENGTH_LONG).show();
        })
        );
        // configuring the accelerometer data route
        newBoard.connectAsync().onSuccessTask(task -> {
            Accelerometer a = newBoard.getModule(Accelerometer.class);
            // accelerometer settings: odr = Output Data Rate (Hz)
            a.configure()
                    .odr(25f)
                    .range(4f)
                    .commit();
            accelModules.add(a);
            return a.acceleration().addRouteAsync(source ->
                    //processAccel(source, newDeviceState, newBoard);
                    source.multicast()
//                .to() //chain your function here and remove the comment (//)
                            .to().map(Function1.RMS).lowpass((byte) 100).filter(Comparison.GTE, 1).stream((data, env) -> sendHapticFromPreset(newDeviceState, newBoard, false))
                            .to().stream((data, env) -> { // DO NOT REMOVE THIS CODE unless you don't want data to be written to the file
                        try {
                            newDeviceState.getAccel_writer().write(data.timestamp().getTimeInMillis() + "," + data.formattedTimestamp() + ",," + data.value(Acceleration.class).x() + "," + data.value(Acceleration.class).y() + "," + data.value(Acceleration.class).z() + "\n");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }})
                            .end());
            // configuring the gyroscope data route
        }).onSuccessTask(task -> {
                GyroBmi160 g = newBoard.getModule(GyroBmi160.class);
                // gyroscope settings: odr = Ouptut Data Rate (Hz)
                g.configure()
                        .odr(GyroBmi160.OutputDataRate.ODR_25_HZ)
                        .commit();
                gyroModules.add(g);
            return g.angularVelocity().addRouteAsync(source ->
//                    processGyro(source, newDeviceState, newBoard));
                    source.multicast()
//                .to() //chain your function here and remove the comment (//)
                            .to().stream((data, env) -> { //DO NOT DELETE THIS - used to write the sensor data to the file
                        try {
                            newDeviceState.getGyro_writer().write(data.timestamp().getTimeInMillis() + "," + data.formattedTimestamp() + ",," + data.value(AngularVelocity.class).x() + "," + data.value(AngularVelocity.class).y() + "," + data.value(AngularVelocity.class).z() + "\n");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }})
                            .end());

        }).continueWith((Continuation<Route, Void>) task -> {
            // the connection was faulty, try again. If problem persists, reset bluetooth on the device
            if (task.isFaulted()) {
                if(VERBOSE) {
                    Log.w("yolow", "Failed to configure app", task.getError());
                }
                if (!newBoard.isConnected()) {
                    getActivity().runOnUiThread(() -> {
                        Snackbar.make(getActivity().findViewById(R.id.activity_main_layout), task.getError().getLocalizedMessage(), Snackbar.LENGTH_SHORT).show();
                        MainActivityContainer.getDeviceStates().remove(newDeviceState.getUid());
                        MainActivityContainer.getStateToBoards().remove(newDeviceState.getUid());
                        removeView(newDeviceState);
                    });
                } else {
                    Snackbar.make(getActivity().findViewById(R.id.activity_main_layout), task.getError().getLocalizedMessage(), Snackbar.LENGTH_SHORT).show();
                    newBoard.tearDown();
                    newBoard.disconnectAsync().continueWith((Continuation<Void, Void>) task1 -> {
                        MainActivityContainer.getDeviceStates().remove(newDeviceState.getUid());
                        MainActivityContainer.getStateToBoards().remove(newDeviceState.getUid());
                        removeView(newDeviceState);
                        return null;
                    });
                }
            }
            // connection was completely successful and ready to record
            else {
                newDeviceState.setConnecting(false);
                newDeviceState.getView().setBackgroundResource(R.color.sensorBoxConnected);
                newBoard.getModule(Settings.class).editBleConnParams()
                        .maxConnectionInterval(11.25f)
                        .commit();
            }
            return null;
        });
    }

    /**
     * Touch handler for the sensor boxes that appear on the screen.
     * @param v the sensor box touched
     * @param event the type of touch that occurred
     * @return true once the touch has been completed
     */
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

    /**
     * Drag handler for the boxes that represent the sensors on the screen
     * @param v The box selected
     * @param event The type of drag event that happened
     * @return true once the drag has been completed
     */
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

    /**
     * shortens the displayed name
     * @param s the sensor name
     * @return the shortened name, if necessary, to display
     */
    private String truncate(String s) {
        int MAX_LABEL_LENGTH = 10;
        if(s.length() > MAX_LABEL_LENGTH) {
            return s.substring(0, MAX_LABEL_LENGTH);
        }
        else {
            return s;
        }
    }

    /**
     * Creates a new sensor box when a new device is connected
     * @param s The device that has just been connected
     */
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

    private void removeView(SensorDevice s) {
        TextView t = s.getView();
        if(t.getParent() != null) {
            ((ConstraintLayout) t.getParent()).removeView(t);
        }
    }

    /**
     * retrieves the saved presets from the preset page
     */
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

    /**
     * retrieves the preset marked as default
     */
    private void fetchDefaultPreset() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            final Preset def = database.pDao().getDefaultPreset();
               defaultPreset = def;
               if(def != null) {
                   defaultPresetName = def.getName();
               }
        });
    }

    /**
     * Triggers the haptic pattern
     * @param s the device to vibrate
     * @param board the hardware component of the device to vibrate
     * @param fromTouch true if the user triggered this haptic pattern, false if the haptic pattern
     *                  was triggered by the device itself
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void sendHapticFromPreset(SensorDevice s, MetaWearBoard board, boolean fromTouch) {
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

    /**
     * Writes the presets generated from CSV files to files in the recording session's folder
     * @param fileId the ID of the CSV file that will be written
     * @param fileWriter the fileWriter that will write to the file
     */
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

    /**
     * Triggers the haptic motor in a sensor if the sensor is programmed using a CSV file
     * @param file File pointer to the file recording the haptic records
     * @param board the hardware of a sensor, carrying the haptic motor
     */
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

    /**
     * Processing logic for accelerometer data
     * @param source the data route
     * @param device the sensor producing the data
     * @param board the object that manages the hardware of the sensor, such as haptics
     */
    private void processAccel(RouteComponent source, SensorDevice device, MetaWearBoard board) {
        source.multicast()
//                .to() //chain your function here and remove the comment (//)
                .to().map(Function1.RMS).lowpass((byte) 100).filter(Comparison.GTE, 1).stream((data, env) -> sendHapticFromPreset(device, board, false))
                .to().stream((data, env) -> {
            try {
                device.getAccel_writer().write(data.timestamp().getTimeInMillis() + "," + data.formattedTimestamp() + ",," + data.value(Acceleration.class).x() + "," + data.value(Acceleration.class).y() + "," + data.value(Acceleration.class).z() + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }})
                .end();
    }

    /**
     * Processing logic for gyroscope data
     * @param source the data route
     * @param device the sensor producing the data
     * @param board the object that manages the hardware of the sensor, such as haptics
     */
    private void processGyro(RouteComponent source, SensorDevice device, MetaWearBoard board) {
        source.multicast()
//                .to() //chain your function here and remove the comment (//)
                .to().stream((data, env) -> { //DO NOT DELETE THIS - used to write the sensor data to the file
            try {
                device.getGyro_writer().write(data.timestamp().getTimeInMillis() + "," + data.formattedTimestamp() + ",," + data.value(AngularVelocity.class).x() + "," + data.value(AngularVelocity.class).y() + "," + data.value(AngularVelocity.class).z() + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }})
                .end();
    }

    /**Example functions **/
    // copy and paste these functions between these or write your own!
    // for more information about data routes, read the documentation from mbientlab:
    // https://mbientlab.com/androiddocs/latest/data_route.html?highlight=route#handling-data

    // for more examples of functions you can program, see the documentation at
    // https://mbientlab.com/documents/metawear/android/3/com/mbientlab/metawear/builder/RouteComponent.html

    /**
     * Calculate the moving average over MOVING_AVERAGE samples
     * .map(Function1.RMS) turns the data points into values using the root mean square.
     * .lowpass((byte) 100) calculates the moving average of the previous 100 samples
     * .filter(Comparison.GTE,1) will compare the average: if it is greater or equal to 1 (GTE) then
     * it will trigger the code in stream(), which will make the sensor vibrate.
     */
//    .map(Function1.RMS).lowpass((byte) MOVING_AVERAGE)
//            .filter(Comparison.GTE, 1)
//    .stream((data, env) ->
//            sendHapticFromPreset(newDeviceState, newBoard, false))


    /**
     * comparing a data point to the immediate previous datapoint received by the sensor
     * on clientside instead of sensorside.
     * Note: this function requires a hashmap called prevAccelData, which you will have to add at
     * the top of the file. (It is currently commented out, as it is not being used.)
     */
//    private void compareAccelToPrevious(String uid, Data data) {
//        Data prev = prevAccelData.get(uid);
//        if(prev == null) {
//            //this is the first data point
//            prevAccelData.put(uid, data);
//            return;
//        }
//        double x  =Math.pow(prev.value(Acceleration.class).x() - data.value(Acceleration.class).x(), 2);
//        double y = Math.pow(prev.value(Acceleration.class).y() - data.value(Acceleration.class).y(), 2);
//        double z = Math.pow(prev.value(Acceleration.class).z() - data.value(Acceleration.class).z(), 2);
//        double resultant = Math.sqrt(x + y + z);
//        if(VERBOSE) {
//            Log.i("Accel resultant", "" + prev.value(Acceleration.class) + ", " + data.value(Acceleration.class) + ": " + resultant);
//        }
//        if(resultant > 2) {
//            sendHapticFromPreset(MainActivityContainer.getDeviceStates().get(uid), MainActivityContainer.getStateToBoards().get(uid), false);
//        }
//        prevAccelData.replace(uid, data);
//    }

}
