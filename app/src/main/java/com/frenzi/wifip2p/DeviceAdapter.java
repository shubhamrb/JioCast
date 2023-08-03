package com.frenzi.wifip2p;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;


public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    private List<DeviceModel> deviceList;
    private Context context;
    private OnDeviceSelectListener listener;

    public DeviceAdapter(Context context, List<DeviceModel> deviceList, OnDeviceSelectListener listener) {
        this.context = context;
        this.deviceList = deviceList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.device_item, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        DeviceModel device = deviceList.get(position);
        holder.deviceName.setText(device.getName());
        holder.location.setText(device.getLocation());
        holder.itemView.setOnClickListener(v -> {
            listener.onDeviceSelect(device.getLocation());
        });
    }

    interface OnDeviceSelectListener {
        void onDeviceSelect(String location);
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    public static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView deviceName, location;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.deviceName);
            location = itemView.findViewById(R.id.location);
        }
    }
}

