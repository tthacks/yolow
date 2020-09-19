package com.mbientlab.metawear.tutorial.multimw;

import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;

import com.mbientlab.metawear.tutorial.multimw.database.SensorDatabase;
import com.mbientlab.metawear.tutorial.multimw.database.SensorDevice;

import java.util.List;

public class HumanActivity extends AppCompatActivity {

    private boolean isLocked, isRecording;
    private RecyclerView recycler;
    private NameDevicesAdapter adapter;
    private SensorDatabase sensorDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_human);

        //TODO: create list of connected devices
        recycler = findViewById(R.id.device_list_human);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NameDevicesAdapter(this);
        recycler.setAdapter(adapter);
        sensorDb = SensorDatabase.getInstance(getApplicationContext());

        //button controls
        isLocked = false;
        isRecording = false;
        Button lock_button = findViewById(R.id.button_lock);
        Button record_button = findViewById(R.id.button_record);

        ImageView image_lock = findViewById(R.id.image_lock);
        ImageView image_unlock = findViewById(R.id.image_unlock);


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
                    image_unlock.setVisibility(View.GONE);
                    image_unlock.setVisibility(View.VISIBLE);
                }
                else {
                    lock_button.setText("LOCK");
                    image_unlock.setVisibility(View.VISIBLE);
                    image_unlock.setVisibility(View.GONE);
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
                    image_unlock.setVisibility(View.GONE);
                    image_unlock.setVisibility(View.VISIBLE);
                }
                else {
                    record_button.setText("START RECORDING");
                    lock_button.setEnabled(true);
                    image_unlock.setVisibility(View.VISIBLE);
                    image_unlock.setVisibility(View.GONE);
                }
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        retrieveSensors();
    }


    private void retrieveSensors() {
        AppExecutors.getInstance().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                final List<SensorDevice> sensors = sensorDb.sensorDao().getSensorList();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        adapter.setSensorList(sensors);
                    }
                });
            }
        });
    }

}