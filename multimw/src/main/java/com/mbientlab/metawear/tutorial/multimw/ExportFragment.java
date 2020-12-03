package com.mbientlab.metawear.tutorial.multimw;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mbientlab.metawear.tutorial.multimw.database.AppDatabase;
import com.mbientlab.metawear.tutorial.multimw.database.AppExecutors;
import com.mbientlab.metawear.tutorial.multimw.database.Session;

import java.util.List;

public class ExportFragment extends Fragment {

    private ExportAdapter adapter;
    private AppDatabase database;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        adapter = new ExportAdapter(getActivity());
        setRetainInstance(true);
        return inflater.inflate(R.layout.fragment_export, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        database = AppDatabase.getInstance(getActivity().getApplicationContext());
        RecyclerView recyclerView = view.findViewById(R.id.sessions);
        recyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        retrieveSessions();
    }

    /**
     * fetch the session list from the database
     */
    private void retrieveSessions() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            final List<Session> sessionList = database.sDao().loadAllSessions();
            getActivity().runOnUiThread(() -> adapter.setSessions(sessionList));
        });
    }
}