package com.mbientlab.metawear.tutorial.multimw;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.DragEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


import androidx.constraintlayout.widget.ConstraintLayout;

import com.mbientlab.metawear.tutorial.multimw.database.SensorDatabase;
import com.mbientlab.metawear.tutorial.multimw.database.SensorDevice;

import java.util.List;

public class HumanActivity extends AppCompatActivity {

    private boolean isLocked, isRecording;
    private SensorDatabase sensorDb;


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_human_drag);
        sensorDb = SensorDatabase.getInstance(getApplicationContext());

        //button controls
        isLocked = false;
        isRecording = false;



//        TextView a = findViewById(R.id.sensor_box_1);
//        TextView b = findViewById(R.id.sensor_box_2);
//        TextView c = findViewById(R.id.sensor_box_3);
//        setDraggable(a, 0);
//        setDraggable(b, 1);
//        setDraggable(c, 2);

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

    private class BoxLongClickListener implements View.OnLongClickListener {
        @Override
        public boolean onLongClick(View v) {
            if (!isLocked) {
                ClipData.Item item = new ClipData.Item((CharSequence) v.getTag());
                String[] mimeTypes = {ClipDescription.MIMETYPE_TEXT_PLAIN};
                ClipData data = new ClipData(v.getTag().toString(), mimeTypes, item);
                View.DragShadowBuilder dragshadow = new View.DragShadowBuilder(v);
                v.startDrag(data, dragshadow, null, 0);
                return true;
            }
            return false;
        }
    }

    private class BoxDragListener implements View.OnDragListener {
        @Override
        public boolean onDrag(View v, DragEvent event) {
            if (!isLocked) {
                if (event.getAction() == DragEvent.ACTION_DRAG_ENDED) {
                    v.setX(event.getX());
                    v.setY(event.getY());
                    System.out.println("Drag n drop happened");
                }
                return true;
            }
            return false;
        }
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
        sensorbox.setOnLongClickListener(new BoxLongClickListener());
        sensorbox.setOnDragListener(new BoxDragListener());
    }
}