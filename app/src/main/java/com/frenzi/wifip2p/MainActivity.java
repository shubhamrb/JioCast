package com.frenzi.wifip2p;


import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements SsdpDiscoveryTask.SsdpDiscoveryCallback, DeviceAdapter.OnDeviceSelectListener {
    private RecyclerView deviceRecyclerView;
    private Button btn_refresh;
    private DeviceAdapter deviceAdapter;
    private List<DeviceModel> deviceList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deviceList = new ArrayList<>();
        btn_refresh = findViewById(R.id.btn_refresh);
        deviceRecyclerView = findViewById(R.id.listView);

        deviceAdapter = new DeviceAdapter(this, deviceList, this);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        deviceRecyclerView.setLayoutManager(layoutManager);
        deviceRecyclerView.setAdapter(deviceAdapter);

        btn_refresh.setOnClickListener(v -> {
            if (deviceList != null) {
                deviceList.clear();
            }
            getConnectedDeviceList();
        });
        getConnectedDeviceList();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void getConnectedDeviceList() {
        SsdpDiscoveryTask ssdpDiscoveryTask = new SsdpDiscoveryTask(this);
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(ssdpDiscoveryTask);
    }

    @Override
    public void onDeviceNameReceived(DeviceModel deviceModel) {
        if (deviceModel != null) {
            runOnUiThread(() -> {
                deviceList.add(deviceModel);
                deviceAdapter.notifyDataSetChanged();
            });
        }
    }

    @Override
    public void onDeviceSelect(String location) {
        if (location != null) {
            try {
                String[] arr = location.split("/");
                if (arr.length <= 2) {
                    return;
                }
                URL url = new URL(arr[0] + arr[1] + arr[2] + "/apps/Netflix");

                MyNetworkThread myNetworkThread = new MyNetworkThread(url);
                myNetworkThread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.e("TAG", "Control URL for the YouTube app not found in the device description XML.");
        }
    }

    private class MyNetworkThread extends Thread {
        URL url;

        public MyNetworkThread(URL url) {
            this.url = url;
        }

        @Override
        public void run() {
            String response = "";
            try {
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
//                connection.setRequestProperty("Content-Length", "0");
                connection.connect();
                if (connection.getResponseCode() == HttpURLConnection.HTTP_CREATED) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response += line;
                    }
                    response = "Success: " + connection.getResponseCode() + " " + connection.getResponseMessage();
                    reader.close();
                } else {
                    response = "Error: " + connection.getResponseCode() + " " + connection.getResponseMessage();
                }

                connection.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
                response = "Error: " + e.getMessage();
            }

            // Update the UI with the result using runOnUiThread method
            final String finalResponse = response;
            runOnUiThread(() -> Log.e("finalResponse", finalResponse));
        }
    }
}