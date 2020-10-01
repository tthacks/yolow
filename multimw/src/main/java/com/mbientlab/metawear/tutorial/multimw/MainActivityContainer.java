package com.mbientlab.metawear.tutorial.multimw;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.tutorial.multimw.database.HapticCSV;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Objects;

public class MainActivityContainer extends AppCompatActivity {
    public static final int REQUEST_START_BLE_SCAN= 1;
    public static final int PICKFILE_REQUEST_CODE = 2;
    private static int DEFAULT_INDEX = 0;
    private static HashMap<String, MetaWearBoard> stateToBoards;
    private static HashMap<String, SensorDevice> deviceStates;
    public static HashMap<String, HapticCSV> csvFiles;
    private boolean viewingSettings = true;
    private FragmentManager fm;

    public MainActivityContainer() {
        stateToBoards = new HashMap<>();
        deviceStates = new HashMap<>();
        csvFiles = new HashMap<>();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //set view
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_container);
        //init button listeners
        Button goto_human_button = findViewById(R.id.button_goto_human);
        Button goto_settings_button = findViewById(R.id.button_goto_settings);
        Button scan_devices_button = findViewById(R.id.scan_devices_button);
        Button upload_csv_button = findViewById(R.id.upload_csv_button);
        scan_devices_button.setOnClickListener(view -> startActivityForResult(new Intent(MainActivityContainer.this, ScannerActivity.class), REQUEST_START_BLE_SCAN));
        upload_csv_button.setOnClickListener(view -> {
            Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
            chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
            chooseFile.setType("text/csv");
            startActivityForResult(Intent.createChooser(chooseFile, "Choose a file to upload"), PICKFILE_REQUEST_CODE);
        });


        fm = getSupportFragmentManager();
        goto_human_button.setOnClickListener(view -> {
            if(viewingSettings) {
                viewingSettings = false;
                View initSettings = findViewById(R.id.main_activity_content);
                initSettings.setVisibility(View.GONE);
                goto_human_button.setBackgroundResource(R.color.colorPrimary);
                goto_settings_button.setBackgroundResource(R.color.colorAccent);
                scan_devices_button.setVisibility(View.GONE);
                upload_csv_button.setVisibility(View.GONE);
                Fragment fragment = new HumanFragment();
                FragmentTransaction transaction = fm.beginTransaction();
                transaction.replace(R.id.fragment_container, fragment);
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });
        goto_settings_button.setOnClickListener(view -> {
            if(!viewingSettings) {
                viewingSettings = true;
                goto_human_button.setBackgroundResource(R.color.colorAccent);
                goto_settings_button.setBackgroundResource(R.color.colorPrimary);
                scan_devices_button.setVisibility(View.VISIBLE);
                upload_csv_button.setVisibility(View.VISIBLE);
                Fragment fragment = new PresetFragment();
                FragmentTransaction transaction = fm.beginTransaction();
                transaction.replace(R.id.fragment_container, fragment);
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_START_BLE_SCAN) {
            if(data != null) {
                BluetoothDevice selectedDevice = data.getParcelableExtra(ScannerActivity.EXTRA_DEVICE);
                if (selectedDevice != null) {
                    ((SettingsFragment) getSupportFragmentManager().findFragmentById(R.id.main_activity_content)).addNewDevice(selectedDevice);
                }
            }
        }
        else if(requestCode == PICKFILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri uri;
            if (data != null) {
                uri = data.getData();
                try {
                    HapticCSV newFile = readTextFromUri(uri);
                    csvFiles.put(newFile.getFilename(), newFile);
                    Toast.makeText(getApplicationContext(), "File " + newFile.getFilename() +  " successfully uploaded", Toast.LENGTH_SHORT).show();
                }
                catch(IOException e) {
                    Toast.makeText(getApplicationContext(), "File not found.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private HapticCSV readTextFromUri(Uri uri) throws IOException {
        StringBuilder onTime = new StringBuilder();
        StringBuilder offTime = new StringBuilder();
        try (InputStream inputStream =
                     getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(Objects.requireNonNull(inputStream)))) {
            String line = reader.readLine(); //skip headers
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");
                onTime.append(tokens[0]).append(",");
                offTime.append(tokens[1]).append(",");
            }
        }
        System.out.println("On Time:" + onTime.toString());
        return new HapticCSV(uri.getLastPathSegment(), onTime.toString(), offTime.toString());
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

    public static HashMap<String, MetaWearBoard> getStateToBoards() {
        return stateToBoards;
    }

    public static void addStateToBoards(String address, MetaWearBoard board) {
        stateToBoards.put(address, board);
    }

    public static HashMap<String, SensorDevice> getDeviceStates() {
        return deviceStates;
    }

    public static SensorDevice getSensorById(String id) {
        return deviceStates.get(id);
    }

    public static void setDefaultIndex(int x) {
        DEFAULT_INDEX = x;
    }

    public static int getDefaultIndex() {
        return DEFAULT_INDEX;
    }
}

