package com.mbientlab.metawear.tutorial.multimw;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mbientlab.metawear.tutorial.multimw.database.Session;

import java.util.Collections;
import java.util.List;


public class ExportAdapter extends RecyclerView.Adapter<ExportAdapter.ExportViewHolder> {

    private Context context;
    private List<Session> sessionList;

    public ExportAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public ExportViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(context).inflate(R.layout.export_item, viewGroup, false);
        return new ExportViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ExportAdapter.ExportViewHolder viewHolder, @SuppressLint("RecyclerView") int i) {
        viewHolder.exportName.setText(sessionList.get(i).getName());
        viewHolder.sensorPresetCount.setText(sessionList.get(i).getNumSensors() + " sensors, " + sessionList.get(i).getNumPresets() + " presets");
    }

    @Override
    public int getItemCount() {
        if (sessionList == null) {
            return 0;
        }
        return sessionList.size();
    }

    public void setSessions(List<Session> s_list) {
        sessionList = s_list;
        Collections.reverse(sessionList);
        notifyDataSetChanged();
    }

    class ExportViewHolder extends RecyclerView.ViewHolder {
        TextView exportName, sensorPresetCount;

        ExportViewHolder(@NonNull final View itemView) {
            super(itemView);
            exportName = itemView.findViewById(R.id.export_file_name);
            sensorPresetCount = itemView.findViewById(R.id.label_sensor_count);
        }
    }
}