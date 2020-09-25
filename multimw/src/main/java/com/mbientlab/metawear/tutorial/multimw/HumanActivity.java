package com.mbientlab.metawear.tutorial.multimw;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


import androidx.constraintlayout.widget.ConstraintLayout;

import com.mbientlab.metawear.tutorial.multimw.database.SensorDatabase;
import com.mbientlab.metawear.tutorial.multimw.database.SensorDevice;

import java.util.List;

public class HumanActivity extends AppCompatActivity implements View.OnTouchListener, View.OnDragListener {

    private boolean isLocked, isRecording;
    private SensorDatabase sensorDb;
    private View currentlyDragging = null;


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_human);
        sensorDb = SensorDatabase.getInstance(getApplicationContext());

        //button controls
        isLocked = false;
        isRecording = false;

        Button lock_button = findViewById(R.id.button_lock);
        Button record_button = findViewById(R.id.button_record);
        Button goto_settings_button = findViewById(R.id.button_goto_settings);
        goto_settings_button.setOnClickListener(view -> {
            Intent intent = new Intent(HumanActivity.this, MainActivity.class);
            startActivity(intent);
        });
        lock_button.setOnClickListener(view -> {
            isLocked = !isLocked;
            if(isLocked) {
                lock_button.setText("UNLOCK");
            }
            else {
                lock_button.setText("LOCK");
            }
        });
        record_button.setOnClickListener(view -> {
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
    protected void onResume() {
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
            runOnUiThread(() -> {
                ConstraintLayout constraintLayout = findViewById(R.id.sensorbox_area);
                for(int i = 0; i < sensors.size(); i++) {
                    SensorDevice s = sensors.get(i);
                    TextView sensorbox = new TextView(this);
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


}