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


import com.mbientlab.metawear.tutorial.multimw.database.SensorDatabase;
import com.mbientlab.metawear.tutorial.multimw.database.SensorDevice;

import java.util.List;

public class HumanActivity extends AppCompatActivity implements View.OnDragListener, View.OnLongClickListener{

    private final int MAX_NUM_SENSORS = 3;
    private boolean isLocked, isRecording;
    private SensorDatabase sensorDb;
    private SensorBox[] boxes;


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_human_drag);
        sensorDb = SensorDatabase.getInstance(getApplicationContext());

        //button controls
        isLocked = false;
        isRecording = false;

        TextView a = (TextView) findViewById(R.id.sensor_box_1);
        TextView b = (TextView) findViewById(R.id.sensor_box_2);
        TextView c = (TextView) findViewById(R.id.sensor_box_3);
        boxes = new SensorBox[MAX_NUM_SENSORS];
        boxes[0] = new SensorBox(a);
        boxes[1] = new SensorBox(b);
        boxes[2] = new SensorBox(c);

        for(int i = 0; i < boxes.length; i++) {
            setDraggable(boxes[i].getBox(), i);
        }

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

    @Override
    public boolean onLongClick(View v) {
        ClipData.Item item = new ClipData.Item((CharSequence) v.getTag());
        String[] mimeTypes = {ClipDescription.MIMETYPE_TEXT_PLAIN};
        ClipData data = new ClipData(v.getTag().toString(), mimeTypes, item);
        View.DragShadowBuilder dragshadow = new View.DragShadowBuilder(v);
        v.startDrag(data, dragshadow, null, 0);
        return true;
    }

    // This is the method that the system calls when it dispatches a drag event to the listener.
    @Override
    public boolean onDrag(View v, DragEvent event) {
            if(event.getAction() == DragEvent.ACTION_DRAG_ENDED) {
                    int new_x = (int) event.getX();
                    int new_y = (int) event.getY();
                    v.setX(new_x);
                    v.setY(new_y);
                    setNewLocation(new_x, new_y, (TextView) v);
            }
            return true;
        }


    private void retrieveSensors() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            final List<SensorDevice> sensors = sensorDb.sensorDao().getSensorList();
            runOnUiThread(() -> {
                for(int i = 0; i < sensors.size(); i++) {
                    SensorDevice s = sensors.get(i);
                    boxes[i].setAddress(s.uid);
                    boxes[i].setFriendlyName(s.friendlyName);
                    boxes[i].setLocation(s.x_location, s.y_location);
                    boxes[i].setIsVisible(true);
                }
            });
        });
    }


    private void setNewLocation(int x_coord, int y_coord, TextView v) {
        //end run
        AppExecutors.getInstance().diskIO().execute(() -> {
            int x = 0;
            boolean stillLooking = true;
            while(stillLooking) {
                if(boxes[x].isMyBox(v)) {
                    sensorDb.sensorDao().updateXYCoord(x_coord, y_coord, boxes[x].getAddress());
                    stillLooking = false;
                }
                else {
                    x++;
                    if(x > 2) {
                        System.out.println("Could not find the view you were looking for.");
                        stillLooking = false;
                    }
                } //end else
            } //end while
        });
    }

    private void setDraggable(TextView sensorbox, int i) {
        sensorbox.setTag("sensor" + i);
        sensorbox.setOnLongClickListener(this);
        sensorbox.setOnDragListener(this);
    }
}