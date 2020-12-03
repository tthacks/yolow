package com.mbientlab.metawear.tutorial.multimw;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.tutorial.multimw.database.AppDatabase;
import com.mbientlab.metawear.tutorial.multimw.database.AppExecutors;
import com.mbientlab.metawear.tutorial.multimw.database.HapticCSV;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Objects;

/**
 * The main activity of the app that contains all of the fragments and views displayed.
 */
public class MainActivityContainer extends AppCompatActivity {
    public static final int REQUEST_START_BLE_SCAN= 1;
    public static final int PICKFILE_REQUEST_CODE = 2;
    private AppDatabase database;
    private static HashMap<String, MetaWearBoard> stateToBoards;
    private static HashMap<String, SensorDevice> deviceStates;
    private FragmentManager fm;

    public MainActivityContainer() {
        stateToBoards = new HashMap<>();
        deviceStates = new HashMap<>();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //set view
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_container);
        database = AppDatabase.getInstance(this);

        //init button listeners
        Button goto_human_button = findViewById(R.id.button_goto_human);
        Button goto_settings_button = findViewById(R.id.button_goto_settings);
        Button goto_exports_button = findViewById(R.id.button_goto_exports);

        fm = getSupportFragmentManager();
        goto_human_button.setOnClickListener(view -> {
                goto_human_button.setBackgroundResource(R.color.colorAccent);
                goto_settings_button.setBackgroundResource(R.color.colorPrimary);
                goto_exports_button.setBackgroundResource(R.color.colorPrimary);

                Fragment fragment = new HumanFragment();
                FragmentTransaction transaction = fm.beginTransaction();
                transaction.replace(R.id.fragment_container, fragment);
                transaction.addToBackStack(null);
                transaction.commit();
        });
        goto_settings_button.setOnClickListener(view -> {
                View initView = findViewById(R.id.main_activity_content);
                initView.setVisibility(View.GONE);
                goto_human_button.setBackgroundResource(R.color.colorPrimary);
                goto_settings_button.setBackgroundResource(R.color.colorAccent);
                goto_exports_button.setBackgroundResource(R.color.colorPrimary);
                Fragment fragment = new PresetFragment();
                FragmentTransaction transaction = fm.beginTransaction();
                transaction.replace(R.id.fragment_container, fragment);
                transaction.addToBackStack(null);
                transaction.commit();
        });

        goto_exports_button.setOnClickListener(view -> {
                View initView = findViewById(R.id.main_activity_content);
                initView.setVisibility(View.GONE);
                goto_human_button.setBackgroundResource(R.color.colorPrimary);
                goto_settings_button.setBackgroundResource(R.color.colorPrimary);
                goto_exports_button.setBackgroundResource(R.color.colorAccent);
                Fragment fragment = new ExportFragment();
                FragmentTransaction transaction = fm.beginTransaction();
                transaction.replace(R.id.fragment_container, fragment);
                transaction.addToBackStack(null);
                transaction.commit();
        });

    }

    /**
     * Logic after returning from either a bluetooth scan or a CSV file selection
     * @param requestCode identifies if the act returned was a scan or a file search
     * @param resultCode success or failure
     * @param data the data returned from the other activity
     */
    @SuppressLint("NewApi")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_START_BLE_SCAN) {
            if(data != null) {
                BluetoothDevice selectedDevice = data.getParcelableExtra(ScannerActivity.EXTRA_DEVICE);
                if (selectedDevice != null) {
                    ((HumanFragment) getSupportFragmentManager().findFragmentById(R.id.main_activity_content)).addNewDevice(selectedDevice);
                }
            }
        }
        if(requestCode == PICKFILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri uri;
            if (data != null) {
                uri = data.getData();
                try {
                    HapticCSV newFile = readTextFromUri(uri);
                    if(newFile != null) {
                        insertIntoCSVDb(newFile);
                        Toast.makeText(getApplicationContext(), "File " + newFile.getFilename() + " successfully uploaded", Toast.LENGTH_SHORT).show();
                    }
                }
                catch(IOException e) {
                    Toast.makeText(getApplicationContext(), "File not found.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /**
     * read a CSV file and parse it
     * @param uri the path of the file
     * @return a HapticCSV object containing the data from the CSV file
     * @throws IOException if the file does not exist
     */
    private HapticCSV readTextFromUri(Uri uri) throws IOException {
        StringBuilder onTime = new StringBuilder();
        StringBuilder offTime = new StringBuilder();
        StringBuilder intensity = new StringBuilder();
        boolean badFormatting = false;
        try (
                InputStream inputStream =
                     getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(Objects.requireNonNull(inputStream)))) {
            String line = reader.readLine(); //skip headers
            while ((line = reader.readLine()) != null && !badFormatting) {
                String[] tokens = line.split(",");
                if(tokens.length == 3) {
                    onTime.append(tokens[0]).append(",");
                    offTime.append(tokens[1]).append(",");
                    intensity.append(tokens[2]).append(",");
                }
                else if(tokens.length == 2) {
                    onTime.append(tokens[0]).append(",");
                    offTime.append(tokens[1]).append(",");
                    intensity.append("100,");
                }
                else {
                    badFormatting = true;
                }
            }
        }
        if(badFormatting) {
            Toast.makeText(getApplicationContext(), "The CSV file was poorly formed and could not be uploaded. Make sure there are only three columns.", Toast.LENGTH_LONG);
            return null;
        }
        return new HapticCSV(uri.getLastPathSegment(), onTime.toString(), offTime.toString(), intensity.toString());
    }

    /**
     * fetch the hardware objects of the sensors
     * @return
     */
    public static HashMap<String, MetaWearBoard> getStateToBoards() {
        return stateToBoards;
    }

    /**
     * add a new hardware object to the list of connected sensors
     * @param address the UID of the sensor
     * @param board the hardware object added to the list
     */
    public static void addStateToBoards(String address, MetaWearBoard board) {
        stateToBoards.put(address, board);
    }

    /**
     * fetch the list of collected sensors
     * @return
     */
    public static HashMap<String, SensorDevice> getDeviceStates() {
        return deviceStates;
    }

    /**
     * add a sensor to the collection
     * @param s the sensor to be added
     */
    public static void addDeviceToStates(SensorDevice s) {
        deviceStates.put(s.getUid(), s);
    }

    /**
     * add a CSV file to the database after uploading it
     * @param h the hapticCSV object to add
     */
    private void insertIntoCSVDb(HapticCSV h){
        AppExecutors.getInstance().diskIO().execute(() -> database.hDao().insertCSVFile(h));
    }
}

