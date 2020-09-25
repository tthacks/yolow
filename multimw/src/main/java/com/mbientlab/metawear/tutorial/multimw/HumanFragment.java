package com.mbientlab.metawear.tutorial.multimw;

import android.annotation.SuppressLint;
import android.app.Activity;
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
import com.mbientlab.metawear.tutorial.multimw.database.SensorDatabase;
import com.mbientlab.metawear.tutorial.multimw.database.SensorDevice;

import java.util.List;

public class HumanFragment extends Fragment implements View.OnTouchListener, View.OnDragListener, OnTestHapticClickListener {

    private boolean isLocked, isRecording;
    private SensorDatabase sensorDb;
    private View currentlyDragging = null;
    //private OnTestHapticClickListener hapticClickListener;

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
        sensorDb = SensorDatabase.getInstance(getActivity().getApplicationContext());

        //button controls
        isLocked = false;
        isRecording = false;

        Button lock_button = view.findViewById(R.id.button_lock);
        Button record_button = view.findViewById(R.id.button_record);
        lock_button.setOnClickListener(v -> {
            isLocked = !isLocked;
            if(isLocked) {
                lock_button.setText("UNLOCK");
            }
            else {
                lock_button.setText("LOCK");
            }
        });
        record_button.setOnClickListener(v -> {
            isRecording = !isRecording;
            if(isRecording) {
                record_button.setText("STOP RECORDING");
                lock_button.setEnabled(false);
            }
            else {
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

    public boolean onTouch(View v, MotionEvent event) {
        if (!isLocked && event.getAction() == MotionEvent.ACTION_DOWN) {
            ClipData.Item item = new ClipData.Item((CharSequence) v.getTag());
            String[] mimeTypes = {ClipDescription.MIMETYPE_TEXT_PLAIN};
            ClipData data = new ClipData(v.getTag().toString(), mimeTypes, item);
            View.DragShadowBuilder dragshadow = new View.DragShadowBuilder(v);
            v.startDrag(data, dragshadow, null, 0);
            currentlyDragging = v;
            return true;
        }
        return false;
    }

    public boolean onDrag(View v, DragEvent event) {
        if (!isLocked) {
            int action = event.getAction();
            switch (action) {
                case DragEvent.ACTION_DRAG_ENDED:
                    if(currentlyDragging != null) {
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


    private void retrieveSensors() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            final List<SensorDevice> sensors = sensorDb.sensorDao().getSensorList();
            getActivity().runOnUiThread(() -> {
                ConstraintLayout constraintLayout = getView().findViewById(R.id.sensorbox_area);
                for(int i = 0; i < sensors.size(); i++) {
                    SensorDevice s = sensors.get(i);
                    TextView sensorbox = new TextView(getActivity().getApplicationContext());
                    sensorbox.setText(s.friendlyName);
                    sensorbox.setBackgroundResource(R.color.sensorboxDefault);
                    sensorbox.setX(i * 300);
                    sensorbox.setTextSize(24);
                    sensorbox.setPadding(16, 16, 16, 16);
                    ConstraintLayout.LayoutParams clpSensorbox = new ConstraintLayout.LayoutParams(
                            ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
                    sensorbox.setLayoutParams(clpSensorbox);
                    constraintLayout.addView(sensorbox);
                    setDraggable(sensorbox, i);
                }
            });
        });
    }

    private void setDraggable(TextView sensorbox, int i) {
        sensorbox.setTag("sensor" + i);
        sensorbox.setOnTouchListener(this);
        sensorbox.setOnDragListener(this);
    }

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