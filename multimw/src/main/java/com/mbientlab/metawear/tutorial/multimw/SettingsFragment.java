package com.mbientlab.metawear.tutorial.multimw;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

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
import com.mbientlab.metawear.tutorial.multimw.database.SensorDatabase;
import com.mbientlab.metawear.tutorial.multimw.database.SensorDevice;

import java.util.List;

import bolts.Capture;
import bolts.Continuation;

public class SettingsFragment extends Fragment implements ServiceConnection, OnTestHapticClickListener {
    public static final int REQUEST_START_BLE_SCAN= 1;
    private BtleService.LocalBinder binder;
    private SensorDatabase sensorDb;
    private RecyclerView recyclerView;
    private ConnectedDevicesAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Activity owner= getActivity();
        owner.getApplicationContext().bindService(new Intent(owner, BtleService.class), this, Context.BIND_AUTO_CREATE);

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().getApplicationContext().unbindService(this);
    }


    public void addNewDevice(BluetoothDevice btDevice) {

        //TODO: fix sensor collision issues on app failure
        final SensorDevice newDeviceState = new SensorDevice(btDevice.getAddress(), btDevice.getName(), true, 2, 1, 1);
        final MetaWearBoard newBoard= binder.getMetaWearBoard(btDevice);
        addToDb(newDeviceState);
        retrieveSensors();
        MainActivityContainer.addStateToBoards(btDevice.getAddress(), newBoard);

        final Capture<AsyncDataProducer> orientCapture = new Capture<>();
        final Capture<Accelerometer> accelCapture = new Capture<>();

        newBoard.onUnexpectedDisconnect(status -> getActivity().runOnUiThread(() -> {removeFromDb(newDeviceState); retrieveSensors();}
        ));
        newBoard.connectAsync().onSuccessTask(task -> {
            getActivity().runOnUiThread(() -> {
                newDeviceState.connecting= false;
                updateConnectionStatusInDb(newDeviceState);
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
            // newDeviceState.pressed = data.value(Boolean.class);
//                connectedDevices.notifyDataSetChanged();
        })))).continueWith((Continuation<Route, Void>) task -> {
            if (task.isFaulted()) {
                if (!newBoard.isConnected()) {
                    getActivity().runOnUiThread(() -> removeFromDb(newDeviceState));
                    retrieveSensors();
                } else {
                    Snackbar.make(getActivity().findViewById(R.id.activity_main_layout), task.getError().getLocalizedMessage(), Snackbar.LENGTH_SHORT).show();
                    newBoard.tearDown();
                    newBoard.disconnectAsync().continueWith((Continuation<Void, Void>) task1 -> {
                        removeFromDb(newDeviceState);
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
        adapter = new ConnectedDevicesAdapter(getActivity(), this);

        setRetainInstance(true);
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        recyclerView = view.findViewById(R.id.connected_devices);
        recyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));
        recyclerView.setAdapter(adapter);
        sensorDb = SensorDatabase.getInstance(this.getContext());
        Button clear_data_button = view.findViewById(R.id.clear_data_button);
        clear_data_button.setOnClickListener(v -> {
            clearAllTables();
        });
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        binder= (BtleService.LocalBinder) service;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

    @Override
    public void onResume() {
        super.onResume();
        retrieveSensors();
    }

    private void retrieveSensors() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            final List<SensorDevice> sensors = sensorDb.sensorDao().getSensorList();
            getActivity().runOnUiThread(() -> adapter.setSensorList(sensors));
        });
    }

    private void removeFromDb(SensorDevice s) {
        AppExecutors.getInstance().diskIO().execute(() -> sensorDb.sensorDao().deleteSensor(s));
    }

    private void addToDb(SensorDevice s) {
        AppExecutors.getInstance().diskIO().execute(() -> sensorDb.sensorDao().insertSensor(s));
    }

    private void updateConnectionStatusInDb(SensorDevice s) {
        AppExecutors.getInstance().diskIO().execute(() -> sensorDb.sensorDao().updateSensorConnectionStatus(s.connecting, s.uid));
    }

    private void clearAllTables() {
        AppExecutors.getInstance().diskIO().execute(() -> sensorDb.clearAllTables());
    }

    @Override
    public void onTestHapticClick(SensorDevice s) {
        MetaWearBoard board = MainActivityContainer.getStateToBoards().get(s.uid);
        System.out.println("Repeating " + s.totalCycles + " times");
        for (int i = 0; i < s.totalCycles; i++) {
            board.getModule(Haptic.class).startMotor((short) (s.onDuration * 1000));
            System.out.println("buzz " + i);
            try {
                Thread.sleep((long)(s.onDuration * 1000) + (long)(s.offDuration * 1000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}