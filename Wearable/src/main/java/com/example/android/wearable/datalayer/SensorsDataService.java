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
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.wearable.view.GridViewPager;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.io.IOException;
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
    private int UserID = -1;
    private String UserHRM = "";

    private SensorManager mSensorManager;
    private Sensor androidSensor;/**/


    private GoogleApiClient mGoogleApiClient;
    private Handler mHandler;
    private GridViewPager mPager;

    // map below allows to reduce amount of collected data
    private Map<Integer, Integer> recordedSensorTypes = new HashMap<Integer, Integer>();
    public ArrayList<ISSRecordData> alldata = new ArrayList<ISSRecordData>();
    private BluetoothAdapter mBluetoothAdapter;

    private ArrayList<String> listItems=new ArrayList<String>();
    private ArrayAdapter<String> adapter;


    int [] sensorIDs = new int[]{Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GYROSCOPE};//,
    //int [] sensorIDs = new int[]{ Sensor.TYPE_SIGNIFICANT_MOTION};

    PowerManager.WakeLock wakeLock = null;
    // this wakes CPU for sensor measuring
    Alarm alarm = new Alarm();

    TimerTask timerTask = null;

    Timer timer = null;

    File mutexFile = new File(Environment.getExternalStorageDirectory(), "/mutex.bin");
    File sensorsData = new File(Environment.getExternalStorageDirectory(), "/triathlon.bin");

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


        mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyWakelockTag");

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();



        // create sensor data file

        if (!sensorsData.exists()){
            try {
                Serializer.SerializeToFile(alldata,sensorsData);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void ResetSensors(){

        if (!wakeLock.isHeld()){
            wakeLock.acquire();
        }

        for (int sensorID : sensorIDs)
        {
            androidSensor = mSensorManager.getDefaultSensor(sensorID);
            mSensorManager.unregisterListener(sensorEventListener, androidSensor);
            mSensorManager.registerListener(sensorEventListener, androidSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

            /*if (mBluetoothGatt != null ){
                mBluetoothGatt.connect();
            }*/

        recordedSensorTypes.clear();

        for ( int sensor: sensorIDs){
            recordedSensorTypes.put(sensor,1);
        }

        if (allowHRM){
            recordedSensorTypes.put(Sensor.TYPE_HEART_RATE, 1);
        }

    }

    SensorEventListener sensorEventListener = new SensorEventListener(){

        @Override
        public void onSensorChanged(SensorEvent event) {

            if (!recordedSensorTypes.containsKey( event.sensor.getType() )){
                return;
            }
            recordedSensorTypes.remove(event.sensor.getType());
            mSensorManager.unregisterListener(sensorEventListener, event.sensor);

            AddNewData(UserID, event.sensor.getType(), GetTimeNow() , null, event.values[0],event.values[1],event.values[1] );

            if (recordedSensorTypes.isEmpty()){
                if (wakeLock.isHeld()){
                    wakeLock.release();
                }
            }

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    public void AddNewData(int uid, int sensortype, String timenow, String extras, float v0, float v1, float v2){

        // data format: UserID, MeasurementType, Timestamp, ExtraData, MeasurementValue
        ISSRecordData data = new ISSRecordData(uid, sensortype, timenow, extras, v0, v1, v2);
        alldata.add(data);

        if (alldata.size() % 100 == 0){

            SaveNewDataToFile(alldata);
            alldata.clear();
            System.gc();

        }

    }


    private void SaveNewDataToFile(ArrayList<ISSRecordData> data) {

        try {

            if (!sensorsData.exists()) {
                Serializer.SerializeToFile(new ArrayList<ISSRecordData>(), sensorsData);
            }

            OutputEvent("Started saving the data ... ");

            long startTime = System.currentTimeMillis();

            ArrayList<ISSRecordData> savedData = (ArrayList<ISSRecordData>) Serializer.DeserializeFromFile(sensorsData);
            savedData.addAll(data);

            OutputEvent("Overall items so far: " + savedData.size());

            Serializer.SerializeToFile(savedData, sensorsData);

            long totalTime = System.currentTimeMillis() - startTime;

            OutputEvent("Total saving time: " + totalTime + " ms");


        } catch (Exception e) {

            e.printStackTrace();

        }

    }

    public String GetTimeNow(){

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd_HH:mm:ss");
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

                            String name = device.getAddress();

                            if (name.equals(UserHRM)){
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

            if (!recordedSensorTypes.containsKey( Sensor.TYPE_HEART_RATE )){
                return;
            }
            recordedSensorTypes.remove(Sensor.TYPE_HEART_RATE);

            int result = ReadHeartRateData(characteristic);

            AddNewData(UserID, Sensor.TYPE_HEART_RATE, GetTimeNow(), null, result, 0, 0);

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
            SwitchHRM_ON();
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

            timer.cancel();
        }

        allowHRM = !allowHRM;

        try {
            Serializer.SerializeToFile(allowHRM,mutexFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void SwitchHRM_ON(){
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
        mBluetoothAdapter.startLeScan(mLeScanCallback);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                OutputEvent("HRM serach stop.");
            }
        }, 10000);

        OutputEvent("Searching HRM ... ");
        timerTask = new TimerTask() {
            public void run() {
                ResetSensors();
            }
        };
        timer = new Timer();
        timer.schedule(timerTask, 0, SamplingRateMS);
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

                long startTime = System.currentTimeMillis();

                try {
                    data = Serializer.FileToBytes(sensorsData);
                    //data = Serializer.SerializeToBytes(alldata);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                long totalTime = System.currentTimeMillis() - startTime;

                if (nodes.size() > 0) {
                    for (int i = 0; i < nodes.size(); i++){
                        nodeId = nodes.get(i).getId();

                        Asset asset = Asset.createFromBytes(data);

                        PutDataMapRequest dataMap = PutDataMapRequest.create("/sensorData");
                        dataMap.getDataMap().putAsset("sensorData", asset);
                        PutDataRequest request = dataMap.asPutDataRequest();
                        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(mGoogleApiClient, request);


                        /*PutDataRequest request = PutDataRequest.create("/sensorData");
                        request.putAsset("sensorData", asset);
                        Wearable.DataApi.putDataItem(mGoogleApiClient, request);*/

                        //Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, "data", data);
                    }
                }

                OutputEvent("Sending data ...");

            }
        }).start();

    }

    public static String NEW_MESSAGE_AVAILABLE = "log the output";

    public void OutputEvent(String str){
        Intent intent = new Intent(this.NEW_MESSAGE_AVAILABLE);
        intent.putExtra("message", str);
        sendBroadcast(intent);
    }

    @Override
    public void onMessageReceived(MessageEvent event) {
        LOGD(TAG, "onMessageReceived: " + event);
        byte [] data = event.getData();

        if (data != null){
            if (data[0] == 1){
                // send available data
                SendCollectedData();
            }

            if (data[0] == 2){
                // send available data
                OutputEvent("Data saved on Smarpthone");
                alldata.clear();

                if (sensorsData.exists()){
                    sensorsData.delete();
                    try {
                        Serializer.SerializeToFile(new ArrayList<ISSRecordData>(), sensorsData);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        }
    }


    @Override
    public void onDestroy() {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        alarm.SetAlarm(this);

        // get unique id of the device
        String android_id = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        OutputEvent(android_id);

        Log.d("ISS", "Adroid ID: " + android_id);

        switch (android_id){
            case "760bd2c1de704a18":
                UserID = 256;
                UserHRM = "DA:2B:64:87:44:35";
                break;
            case "1ccb3fb5f594467b":
                UserID = 256;
                UserHRM = "CC:1F:BD:F5:24:FA";
                break;
            default:
                OutputEvent("Unknown android ID! Please report this error to admins.");
                break;
        }

        InitializeMutexRecovery();

        return START_STICKY;

    }

    // in case app crashes, its state is restored automatically
    public void InitializeMutexRecovery(){

        // create mutex file

        if (!mutexFile.exists() ){
            try {
                Serializer.SerializeToFile(allowHRM, mutexFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            allowHRM = (boolean) Serializer.DeserializeFromFile(mutexFile);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        OutputEvent("Mutex state: " + allowHRM);

        if (allowHRM){
            SwitchHRM_ON();
        }

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
