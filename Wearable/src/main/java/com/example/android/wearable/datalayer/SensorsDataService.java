package com.example.android.wearable.datalayer;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.wearable.view.GridViewPager;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static com.example.android.wearable.datalayer.DataLayerListenerService.LOGD;

public class SensorsDataService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener, MessageApi.MessageListener,
        NodeApi.NodeListener {

    public static SensorsDataService itself;
    public static String Message = "";

    private static final String TAG = "MainActivity";
    private int SamplingRateMS  = 10000;
    private int UserID = 1;

    private SensorManager mSensorManager;
    private Sensor mHeartRateSensor;/**/


    private GoogleApiClient mGoogleApiClient;
    private Handler mHandler;
    private GridViewPager mPager;

    // map below allows to reduce amount of collected data
    private Map<Integer, Integer> recordedSensorTypes = new HashMap<Integer, Integer>();

    TimerTask timerTask = new TimerTask() {
        public void run() {

            mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));

            for (int sensorID : sensorIDs)
            {
                mHeartRateSensor = mSensorManager.getDefaultSensor(sensorID);
                mSensorManager.unregisterListener(sensorEventListener, mHeartRateSensor);
                mSensorManager.registerListener(sensorEventListener, mHeartRateSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }

            /*if (mBluetoothGatt != null ){
                mBluetoothGatt.connect();
            }*/

            recordedSensorTypes.clear();

        }
    };

    private ArrayList<ISSRecordData> alldata = new ArrayList<ISSRecordData>();
    private BluetoothAdapter mBluetoothAdapter;

    private ArrayList<String> listItems=new ArrayList<String>();
    private ArrayAdapter<String> adapter;


    int [] sensorIDs = new int[]{Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GYROSCOPE};//,
    //int [] sensorIDs = new int[]{ Sensor.TYPE_SIGNIFICANT_MOTION};

    @Override
    public void onCreate() {

        itself = this;

        mHandler = new Handler();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();



        new Timer().schedule(timerTask, 0, SamplingRateMS);

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
    }

    SensorEventListener sensorEventListener = new SensorEventListener(){

        @Override
        public void onSensorChanged(SensorEvent event) {

            if (recordedSensorTypes.containsKey( event.sensor.getType() )){
                return;
            }
            recordedSensorTypes.put(event.sensor.getType(), 1);
            mSensorManager.unregisterListener(sensorEventListener, event.sensor);

            // data format: UserID, MeasurementType, Timestamp, ExtraData, MeasurementValue
            ISSRecordData data = new ISSRecordData(UserID, event.sensor.getType(), GetTimeNow() , null, event.values[0] );
            alldata.add(data);

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    public String GetTimeNow(){

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String currentDateandTime = sdf.format(new Date());
        return currentDateandTime;

    }

    BluetoothDevice hrmDevice = null;

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {

                            // already found the hrm
                            if (hrmDevice != null){
                                return;
                            }

                            String name = device.getName();

                            if (name == null){
                                return;
                            }


                            if (name.contains("RHYTHM")){
                                hrmDevice = device;
                                connectDevice(device);
                            }
                        }
                    }).run();
                }
            };

    BluetoothGatt mBluetoothGatt = null;

    public void connectDevice(BluetoothDevice device) {
        mBluetoothGatt = device.connectGatt(this, true, mGattCallback);
    }

    public void ReadCharact(){
        boolean result = mBluetoothGatt.readCharacteristic(heartRateCharacteristic);

        //int resultData = ReadHeartRateData(heartRateCharacteristic);
    }

    private static final UUID UUID_HRS =
            UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_HRD =
            UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");

    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    BluetoothGattService heartRateService = null;
    BluetoothGattCharacteristic heartRateCharacteristic = null;

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mBluetoothGatt.discoverServices();
                OutputEvent("HRM connected");
                //mBluetoothAdapter.stopLeScan(mLeScanCallback);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                OutputEvent("HRM disconnected");
                //mBluetoothAdapter.startLeScan(mLeScanCallback);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                heartRateService = gatt.getService(UUID_HRS);



                if (heartRateService != null){

                    heartRateCharacteristic =
                            heartRateService.getCharacteristic(UUID_HRD);
                    boolean res = gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER);
                    gatt.setCharacteristicNotification(heartRateCharacteristic,true);

                    try {
                        BluetoothGattDescriptor descriptor = heartRateCharacteristic.getDescriptor(
                                UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));

                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

                        mBluetoothGatt.writeDescriptor(descriptor);
                        OutputEvent("Reading HRM");
                    }catch (Exception ex){
                        Log.e(TAG, "wuuuuut?");

                    }



                }

            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }



        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {

            OutputEvent("Characteristic read ");

            if (status == BluetoothGatt.GATT_SUCCESS) {

            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {

            if (recordedSensorTypes.containsKey( Sensor.TYPE_HEART_RATE )){
                return;
            }
            recordedSensorTypes.put(Sensor.TYPE_HEART_RATE, 1);

            int result = ReadHeartRateData(characteristic);

            long unixTime = System.currentTimeMillis() / 1000L;
            ISSRecordData data = new ISSRecordData(UserID, Sensor.TYPE_HEART_RATE, GetTimeNow(), null, result);

            alldata.add(data);

            OutputEvent("HR: " + result);

            //mBluetoothGatt.disconnect();

        }
    };

    public int ReadHeartRateData(BluetoothGattCharacteristic characteristic){

        int flag = characteristic.getProperties();
        int format = -1;
        if ((flag & 0x01) != 0) {
            format = BluetoothGattCharacteristic.FORMAT_UINT16;
            Log.d(TAG, "Heart rate format UINT16.");
        } else {
            format = BluetoothGattCharacteristic.FORMAT_UINT8;
            Log.d(TAG, "Heart rate format UINT8.");
        }
        final int heartRate = characteristic.getIntValue(format, 1);
        return heartRate;

    }

    public boolean allowHRM = false;

    public void SwitchHRM(){

        if(mBluetoothAdapter == null){
            OutputEvent("Where is BT adapter?");
            return;
        }

        if (!allowHRM)
        {
            mBluetoothAdapter.startLeScan(mLeScanCallback);

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    OutputEvent("HRM serach stop.");
                }
            }, 10000);

            OutputEvent("Searching HRM ... ");
        }
        else
        {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);

            if (mBluetoothGatt!= null){
                mBluetoothGatt.close();
                mBluetoothGatt = null;
                hrmDevice = null;
            }

            OutputEvent("HRM off");
        }

        allowHRM = !allowHRM;
    }

    public void SendCollectedData(){

        new Thread(new Runnable() {
            @Override
            public void run() {
                //mGoogleApiClient.blockingConnect(3000, TimeUnit.MILLISECONDS);
                NodeApi.GetConnectedNodesResult result =
                        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
                List<Node> nodes = result.getNodes();
                String nodeId = null;

                byte[] data = null;

                try {
                    data = convertToBytes(alldata);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (nodes.size() > 0) {
                    for (int i = 0; i < nodes.size(); i++){
                        nodeId = nodes.get(i).getId();
                        Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, "data", data);
                    }
                }

            }
        }).start();

    }

    private byte[] convertToBytes(Object object) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutput out = new ObjectOutputStream(bos)) {
            out.writeObject(object);
            return bos.toByteArray();
        }
    }

    private Object convertFromBytes(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInput in = new ObjectInputStream(bis)) {
            return in.readObject();
        }
    }

    public static String NEW_MESSAGE_AVAILABLE = "log the output";

    public void OutputEvent(String str){
        this.Message = str;
        sendBroadcast(new Intent(SensorsDataService.NEW_MESSAGE_AVAILABLE));
    }

    @Override
    public void onMessageReceived(MessageEvent event) {
        LOGD(TAG, "onMessageReceived: " + event);
        byte [] data = event.getData();

        if (data != null){
            if (data[0] == 1){
                // send available data
                OutputEvent("Sending data ...");
                SendCollectedData();
            }

            if (data[0] == 2){
                // send available data
                OutputEvent("Data saved on Smarpthone");
                alldata.clear();
            }
        }
    }


    @Override
    public void onDestroy() {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    @Override
    public void onConnected(Bundle bundle) {
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
        Wearable.NodeApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {

    }

    @Override
    public void onPeerConnected(Node node) {

    }

    @Override
    public void onPeerDisconnected(Node node) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}
