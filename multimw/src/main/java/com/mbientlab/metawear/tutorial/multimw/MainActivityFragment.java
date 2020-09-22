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

import java.util.HashMap;
import java.util.List;

import bolts.Capture;
import bolts.Continuation;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment implements ServiceConnection, OnTestHapticClickListener {
    private final HashMap<String, MetaWearBoard> stateToBoards;
    private BtleService.LocalBinder binder;
    private SensorDatabase sensorDb;
    private RecyclerView recyclerView;
    private ConnectedDevicesAdapter adapter;
    private int numDevicesConnected;

    public MainActivityFragment() {
        stateToBoards = new HashMap<String, MetaWearBoard>();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Activity owner= getActivity();
        owner.getApplicationContext().bindService(new Intent(owner, BtleService.class), this, Context.BIND_AUTO_CREATE);
        numDevicesConnected = 0;

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().getApplicationContext().unbindService(this);
    }


    public void addNewDevice(BluetoothDevice btDevice) {

        //TODO: fix sensor collision issues on app failure
        final SensorDevice newDeviceState = new SensorDevice(btDevice.getAddress(), btDevice.getName(), true, false, 4, 1, 1, numDevicesConnected * 160, 0);
        final MetaWearBoard newBoard= binder.getMetaWearBoard(btDevice);
        addToDb(newDeviceState);
        numDevicesConnected++;
        retrieveSensors();
        stateToBoards.put(btDevice.getAddress(), newBoard);

        final Capture<AsyncDataProducer> orientCapture = new Capture<>();
        final Capture<Accelerometer> accelCapture = new Capture<>();

        newBoard.onUnexpectedDisconnect(status -> getActivity().runOnUiThread(() -> {removeFromDb(newDeviceState); numDevicesConnected--; retrieveSensors();}
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
                   //  newDeviceState.deviceOrientation = data.value(SensorOrientation.class).toString();
//                    connectedDevices.notifyDataSetChanged();
                });
            }));
        }).onSuccessTask(task -> newBoard.getModule(Switch.class).state().addRouteAsync(source -> source.stream((Subscriber) (data, env) -> getActivity().runOnUiThread(() -> {
            // newDeviceState.pressed = data.value(Boolean.class);
//                connectedDevices.notifyDataSetChanged();
        })))).continueWith((Continuation<Route, Void>) task -> {
            if (task.isFaulted()) {
                if (!newBoard.isConnected()) {
                    getActivity().runOnUiThread(() -> removeFromDb(newDeviceState));
                    numDevicesConnected--;
                    retrieveSensors();
                } else {
                    Snackbar.make(getActivity().findViewById(R.id.activity_main_layout), task.getError().getLocalizedMessage(), Snackbar.LENGTH_SHORT).show();
                    newBoard.tearDown();
                    newBoard.disconnectAsync().continueWith((Continuation<Void, Void>) task1 -> {
                        removeFromDb(newDeviceState);
                        numDevicesConnected--;
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
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        recyclerView = view.findViewById(R.id.connected_devices);
        recyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));
        recyclerView.setAdapter(adapter);
        sensorDb = SensorDatabase.getInstance(this.getContext());
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

    @Override
    public void onTestHapticClick(SensorDevice s) {
        MetaWearBoard board = stateToBoards.get(s.uid);
        for (int i = 0; i < s.totalDuration; i = i + 1000 * (s.offDuration + s.offDuration)) {
            board.getModule(Haptic.class).startBuzzer((short) (s.onDuration * 1000));
            try {
                Thread.sleep(s.offDuration * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}

