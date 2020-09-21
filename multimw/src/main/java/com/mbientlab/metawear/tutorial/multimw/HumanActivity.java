package com.mbientlab.metawear.tutorial.multimw;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.DragEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;


import com.mbientlab.metawear.tutorial.multimw.database.SensorDatabase;
import com.mbientlab.metawear.tutorial.multimw.database.SensorDevice;

import java.util.List;

public class HumanActivity extends AppCompatActivity implements View.OnDragListener, View.OnLongClickListener{

    private boolean isLocked, isRecording;
//    private RecyclerView recycler;
//    private NameDevicesAdapter adapter;
    private SensorDatabase sensorDb;
    private RelativeLayout.LayoutParams layoutParams;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_human_drag);

        //TODO: create list of connected devices
//        recycler = findViewById(R.id.device_list_human);
//        recycler.setLayoutManager(new LinearLayoutManager(this));
//        adapter = new NameDevicesAdapter(this);
//        recycler.setAdapter(adapter);
        sensorDb = SensorDatabase.getInstance(getApplicationContext());

        //button controls
        isLocked = false;
        isRecording = false;
        Button lock_button = findViewById(R.id.button_lock);
        Button record_button = findViewById(R.id.button_record);
        TextView example_drag = findViewById(R.id.text_example_drag);
        example_drag.setTag("EXAMPLE TEXT");

        ImageView image_lock = findViewById(R.id.image_lock);
        ImageView image_unlock = findViewById(R.id.image_unlock);
        example_drag.setOnDragListener(this);
        example_drag.setOnLongClickListener(this);

        Button goto_settings_button = findViewById(R.id.button_goto_settings);
        goto_settings_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(HumanActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
        lock_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isLocked = !isLocked;
                if(isLocked) {
                    lock_button.setText("UNLOCK");
//                    image_unlock.setVisibility(View.GONE);
//                    image_lock.setVisibility(View.VISIBLE);
                }
                else {
                    lock_button.setText("LOCK");
//                    image_lock.setVisibility(View.VISIBLE);
//                    image_unlock.setVisibility(View.GONE);
                }
            }
        });
        record_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isRecording = !isRecording;
                if(isRecording) {
                    record_button.setText("STOP RECORDING");
                    lock_button.setEnabled(false);
//                    image_unlock.setVisibility(View.GONE);
//                    image_lock.setVisibility(View.VISIBLE);
                }
                else {
                    record_button.setText("START RECORDING");
                    lock_button.setEnabled(true);
//                    image_unlock.setVisibility(View.VISIBLE);
//                    image_lock.setVisibility(View.GONE);
                }
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
            switch(event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    layoutParams = (RelativeLayout.LayoutParams)v.getLayoutParams();
                    break;

                case DragEvent.ACTION_DRAG_ENTERED:
                    int x_cord = (int) event.getX();
                    int y_cord = (int) event.getY();
                    break;

                case DragEvent.ACTION_DRAG_ENDED :
                    x_cord = (int) event.getX();
                    y_cord = (int) event.getY();
                    layoutParams.leftMargin = x_cord;
                    layoutParams.topMargin = y_cord;
                    v.setLayoutParams(layoutParams);
                    break;

                default: break;
            }
            return true;
        }


    private void retrieveSensors() {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                final List<SensorDevice> sensors = sensorDb.sensorDao().getSensorList();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // adapter.setSensorList(sensors);
                    }
                });
            }
        });
    }

}