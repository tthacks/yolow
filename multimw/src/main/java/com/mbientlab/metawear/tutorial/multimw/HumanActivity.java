package com.mbientlab.metawear.tutorial.multimw;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;

public class HumanActivity extends AppCompatActivity {

    private boolean isLocked, isRecording;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_human);
        isLocked = false;
        isRecording = false;
        Button lock_button = findViewById(R.id.button_lock);
        Button record_button = findViewById(R.id.button_record);

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
                }
                else {
                    lock_button.setText("LOCK");
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
                }
                else {
                    record_button.setText("START RECORDING");
                    lock_button.setEnabled(true);
                }
            }
        });

    }
}