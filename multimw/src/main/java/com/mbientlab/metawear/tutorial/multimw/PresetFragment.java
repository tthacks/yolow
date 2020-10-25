package com.mbientlab.metawear.tutorial.multimw;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.mbientlab.metawear.tutorial.multimw.database.AppExecutors;
import com.mbientlab.metawear.tutorial.multimw.database.Preset;
import com.mbientlab.metawear.tutorial.multimw.database.PresetDatabase;

import java.util.List;

public class PresetFragment extends Fragment {

    private static final int PICKFILE_REQUEST_CODE = 2;
    private PresetAdapter adapter;
    private PresetDatabase pDatabase;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        adapter = new PresetAdapter(getActivity());
        setRetainInstance(true);
        return inflater.inflate(R.layout.fragment_preset, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        //TODO: Known bug: recycler does not scroll when >4 items are in it
        RecyclerView recyclerView = view.findViewById(R.id.sessions);
        Button upload_csv_button = view.findViewById(R.id.upload_csv_button);
        recyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));
        recyclerView.setAdapter(adapter);
        pDatabase = PresetDatabase.getInstance(getActivity().getApplicationContext());
        Button newPresetButton = view.findViewById(R.id.new_preset_button);
        newPresetButton.setOnClickListener(view1 -> {
            Preset new_p = new Preset("Preset " + adapter.getItemCount(), false, -1, adapter.getItemCount() == 0, "",2, 1.0f, 1.0f,  100f, 50, 50);
            AppExecutors.getInstance().diskIO().execute(() -> {
                pDatabase.pDao().insertPreset(new_p);
                retrievePresets();
            });
        });

        upload_csv_button.setOnClickListener(v -> {
            Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
            chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
            chooseFile.setType("text/csv");
            getActivity().startActivityForResult(Intent.createChooser(chooseFile, "Choose a file to upload"), PICKFILE_REQUEST_CODE);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        retrievePresets();
    }

    private void retrievePresets() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            final List<Preset> presetList = pDatabase.pDao().loadAllPresets();
            getActivity().runOnUiThread(() -> adapter.setPresets(presetList));
        });
    }
}