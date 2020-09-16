package com.mbientlab.metawear.tutorial.multimw;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mbientlab.metawear.tutorial.multimw.database.SensorDatabase;
import com.mbientlab.metawear.tutorial.multimw.database.SensorDevice;

import java.util.List;

public class NameDevicesAdapter extends RecyclerView.Adapter<NameDevicesAdapter.NameHolder> {

    private Context context;
    private List<SensorDevice> sensorList;

    public NameDevicesAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public NameDevicesAdapter.NameHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(context).inflate(R.layout.simplerow, viewGroup, false);
        return new NameDevicesAdapter.NameHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NameDevicesAdapter.NameHolder sensorViewHolder, int i) {
        sensorViewHolder.name.setText(sensorList.get(i).friendlyName);
    }

    @Override
    public int getItemCount() {
        if(sensorList == null) {
            return 0;
        }
        return sensorList.size();
    }

    public void setSensorList(List<SensorDevice> s_list) {
        sensorList = s_list;
        notifyDataSetChanged();
    }

    public List<SensorDevice> getSensorList() {
        return sensorList;
    }

    class NameHolder extends RecyclerView.ViewHolder {
        TextView name;

        NameHolder(@NonNull final View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.rowTextView);
        }

}
}
