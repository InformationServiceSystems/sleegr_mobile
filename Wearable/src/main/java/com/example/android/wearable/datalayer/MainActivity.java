/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.wearable.datalayer;

import static com.example.android.wearable.datalayer.DataLayerListenerService.LOGD;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.wearable.view.GridViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * The main activity with a view pager, containing three pages:<p/>
 * <ul>
 * <li>
 * Page 1: shows a list of DataItems received from the phone application
 * </li>
 * <li>
 * Page 2: shows the photo that is sent from the phone application
 * </li>
 * <li>
 * Page 3: includes two buttons to show the connected phone and watch devices
 * </li>
 * </ul>
 */
public class MainActivity extends Activity implements ConnectionCallbacks,
        OnConnectionFailedListener, DataApi.DataListener, MessageApi.MessageListener,
        NodeApi.NodeListener, SensorEventListener {


    private static final String TAG = "MainActivity";
    private int SamplingRateMS  = 1000;
    private int UserID = 1;

    private  SensorManager mSensorManager;
    private  Sensor mHeartRateSensor;/**/

    private GoogleApiClient mGoogleApiClient;
    private Handler mHandler;
    private GridViewPager mPager;


    private Map<Integer, Integer> allrecords = new HashMap<Integer, Integer>();
    private ArrayList<ISSRecordData> alldata = new ArrayList<ISSRecordData>();
    private BluetoothAdapter mBluetoothAdapter;

    private ArrayList<String> listItems=new ArrayList<String>();
    private ArrayAdapter<String> adapter;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        mHandler = new Handler();
        setContentView(R.layout.main_activity);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        if (SensorsDataService.itself == null){
            Intent intent = new Intent(this, SensorsDataService.class);
            startService(intent);
        }

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listItems);
        ListView listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(adapter);

    }

    // this is used to communicate with Service
    private class DataUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(SensorsDataService.NEW_MESSAGE_AVAILABLE)) {

                OutputEvent(SensorsDataService.Message);

            }
        }
    }

    private DataUpdateReceiver dataUpdateReceiver;

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
        if (dataUpdateReceiver == null) dataUpdateReceiver = new DataUpdateReceiver();
        IntentFilter intentFilter = new IntentFilter(SensorsDataService.NEW_MESSAGE_AVAILABLE);
        registerReceiver(dataUpdateReceiver, intentFilter);

    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        Wearable.NodeApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
        //mSensorManager.unregisterListener(this);
        if (dataUpdateReceiver != null) unregisterReceiver(dataUpdateReceiver);

    }



    float acceleration = 0;
    @Override
    public void onSensorChanged(SensorEvent event) {



    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onConnected(Bundle connectionHint) {
        LOGD(TAG, "onConnected(): Successfully connected to Google API client");
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
        Wearable.NodeApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        LOGD(TAG, "onConnectionSuspended(): Connection to Google API client was suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.e(TAG, "onConnectionFailed(): Failed to connect, with result: " + result);
    }

    private void generateEvent(final String title, final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

            }
        });
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        LOGD(TAG, "onDataChanged(): " + dataEvents);

        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                String path = event.getDataItem().getUri().getPath();
                if (DataLayerListenerService.IMAGE_PATH.equals(path)) {
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                    Asset photo = dataMapItem.getDataMap()
                            .getAsset(DataLayerListenerService.IMAGE_KEY);
                    final Bitmap bitmap = loadBitmapFromAsset(mGoogleApiClient, photo);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "Setting background image on second page..");
                            moveToPage(1);

                        }
                    });

                } else if (DataLayerListenerService.COUNT_PATH.equals(path)) {
                    LOGD(TAG, "Data Changed for COUNT_PATH");
                    generateEvent("DataItem Changed", event.getDataItem().toString());
                } else {
                    LOGD(TAG, "Unrecognized path: " + path);
                }

            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                generateEvent("DataItem Deleted", event.getDataItem().toString());
            } else {
                generateEvent("Unknown data event type", "Type = " + event.getType());
            }
        }
    }

    float heartbeat = 10;
    int steps = 1000;

    boolean allowHRM = false;

    public void onClicked(View view) {
        switch (view.getId()) {
            case R.id.switchTraining:

                Button btn = (Button ) findViewById(R.id.switchTraining);

                if (SensorsDataService.itself != null){
                    SensorsDataService.itself.SwitchHRM();
                    allowHRM = !allowHRM;

                    String outputString = allowHRM ? "Stop training" : "Start training";
                    btn.setText(outputString);
                }

                break;
            default:
                Log.e(TAG, "Unknown click event registered");
        }
    }

    /**
     * Find the connected nodes that provide at least one of the given capabilities
     */
    private void showNodes(final String... capabilityNames) {
        Wearable.CapabilityApi.getAllCapabilities(mGoogleApiClient,
                CapabilityApi.FILTER_REACHABLE).setResultCallback(

                new ResultCallback<CapabilityApi.GetAllCapabilitiesResult>() {
                    @Override
                    public void onResult(
                            CapabilityApi.GetAllCapabilitiesResult getAllCapabilitiesResult) {
                        if (!getAllCapabilitiesResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Failed to get capabilities");
                            return;
                        }
                        Map<String, CapabilityInfo>
                                capabilitiesMap = getAllCapabilitiesResult.getAllCapabilities();
                        Set<Node> nodes = new HashSet<>();
                        if (capabilitiesMap.isEmpty()) {
                            showDiscoveredNodes(nodes);
                            return;
                        }
                        for (String capabilityName : capabilityNames) {
                            CapabilityInfo capabilityInfo = capabilitiesMap.get(capabilityName);
                            if (capabilityInfo != null) {
                                nodes.addAll(capabilityInfo.getNodes());
                            }
                        }
                        showDiscoveredNodes(nodes);
                    }

                    private void showDiscoveredNodes(Set<Node> nodes) {
                        List<String> nodesList = new ArrayList<>();
                        for (Node node : nodes) {
                            nodesList.add(node.getDisplayName());
                        }
                        Log.d(TAG, "Connected Nodes: " + (nodesList.isEmpty()
                                ? "No connected device was found for the given capabilities"
                                : TextUtils.join(",", nodesList)));
                        String msg;
                        if (!nodesList.isEmpty()) {
                            msg = getString(R.string.connected_nodes,
                                    TextUtils.join(", ", nodesList));
                        } else {
                            msg = getString(R.string.no_device);
                        }
                        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Extracts {@link android.graphics.Bitmap} data from the
     * {@link com.google.android.gms.wearable.Asset}
     */
    private Bitmap loadBitmapFromAsset(GoogleApiClient apiClient, Asset asset) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must be non-null");
        }

        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                apiClient, asset).await().getInputStream();

        if (assetInputStream == null) {
            Log.w(TAG, "Requested an unknown Asset.");
            return null;
        }
        return BitmapFactory.decodeStream(assetInputStream);
    }

    @Override
    public void onMessageReceived(MessageEvent event) {
        LOGD(TAG, "onMessageReceived: " + event);

    }

    @Override
    public void onPeerConnected(Node node) {
        generateEvent("Node Connected", node.getId());
    }

    @Override
    public void onPeerDisconnected(Node node) {
        generateEvent("Node Disconnected", node.getId());
    }



    /**
     * Switches to the page {@code index}. The first page has index 0.
     */
    private void moveToPage(int index) {
        mPager.setCurrentItem(0, index, true);
    }

    public void OutputEvent(String content){

        final String cont = content;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                listItems.add(0,cont);
                while (listItems.size() > 64){
                    listItems.remove(listItems.size()-1);
                }
                adapter.notifyDataSetChanged();
            }
        });

    }

}


