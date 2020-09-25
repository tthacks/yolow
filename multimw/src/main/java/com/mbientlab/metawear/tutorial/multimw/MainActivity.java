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

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import com.mbientlab.metawear.tutorial.multimw.database.SensorDatabase;

public class MainActivity extends AppCompatActivity {
    public static final int REQUEST_START_BLE_SCAN= 1;
    private SensorDatabase sensorDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //set view
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sensorDb = SensorDatabase.getInstance(getApplicationContext());
        //init button listeners
        Button scan_devices_button = findViewById(R.id.scan_devices_button);
        scan_devices_button.setOnClickListener(view -> startActivityForResult(new Intent(MainActivity.this, ScannerActivity.class), REQUEST_START_BLE_SCAN));
        Button goto_human_button = findViewById(R.id.button_goto_human);
        Button clear_data = findViewById(R.id.clear_data_button);
        goto_human_button.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, HumanActivity.class);
            startActivity(intent);
        });
        clear_data.setOnClickListener(view -> {
        clearAllTables();
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_START_BLE_SCAN) {
                BluetoothDevice selectedDevice= data.getParcelableExtra(ScannerActivity.EXTRA_DEVICE);
                if (selectedDevice != null) {
                    ((MainActivityFragment) getSupportFragmentManager().findFragmentById(R.id.main_activity_content)).addNewDevice(selectedDevice);
                }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void clearAllTables() {
        AppExecutors.getInstance().diskIO().execute(() -> sensorDb.clearAllTables());
    }

}
