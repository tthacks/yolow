package com.mbientlab.metawear.tutorial.multimw;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipDescription;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;


import androidx.constraintlayout.widget.ConstraintLayout;

import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.module.Haptic;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HumanFragment extends Fragment implements View.OnTouchListener, View.OnDragListener {

    private boolean isLocked, isRecording;
    private View currentlyDragging = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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

        //button controls
        isLocked = false;
        isRecording = false;

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

    }

    @Override
    public void onResume() {
        super.onResume();
        retrieveSensors();
    }

    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (!isLocked) {
                ClipData.Item item = new ClipData.Item((CharSequence) v.getTag());
                String[] mimeTypes = {ClipDescription.MIMETYPE_TEXT_PLAIN};
                ClipData data = new ClipData(v.getTag().toString(), mimeTypes, item);
                View.DragShadowBuilder dragshadow = new View.DragShadowBuilder(v);
                v.startDrag(data, dragshadow, null, 0);
                currentlyDragging = v;
                return true;
            } else {
                //send haptic
                SensorDevice currSensor = MainActivityContainer.getDeviceStates().get(v.getTag().toString());
                MetaWearBoard board = MainActivityContainer.getStateToBoards().get(v.getTag().toString());
                if(currSensor != null && board != null) {
                    for (int i = 0; i < currSensor.totalCycles; i++) {
                        board.getModule(Haptic.class).startMotor((short) (currSensor.onDuration * 1000));
                        try {
                            Thread.sleep((long) (currSensor.onDuration * 1000) + (long) (currSensor.offDuration * 1000));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                else {
                    v.setBackgroundResource(R.color.sensorboxError);
                }
            }
            return true;
        }
        return false;
    }

    public boolean onDrag(View v, DragEvent event) {
        if (!isLocked) {
            int action = event.getAction();
            switch (action) {
                case DragEvent.ACTION_DRAG_ENDED:
                    if (currentlyDragging != null) {
                        currentlyDragging.setX(event.getX());
                        currentlyDragging.setY(event.getY());
                        currentlyDragging = null;
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
            sensorbox.setX(i * 300);
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
}