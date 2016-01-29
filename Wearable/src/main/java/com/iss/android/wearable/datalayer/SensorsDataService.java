package com.iss.android.wearable.datalayer;

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
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

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
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static com.iss.android.wearable.datalayer.DataLayerListenerService.LOGD;

public class SensorsDataService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener, MessageApi.MessageListener,
        NodeApi.NodeListener {

    public static final String
            ACTION_BATTERY_STATUS = SensorsDataService.class.getName() + "BatteryStatus",
            ACTION_HR = SensorsDataService.class.getName() + "HeartRate",
            EXTRA_STATUS = "extra_status",
            EXTRA_HR = "extra_hr",
            TAG = "MainActivity",
            ASK_USER_FOR_RPE = "show rpe dialog",
            CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb",
            UPDATE_TIMER_VALUE = "update the timer value",
            UPDATE_GPS_PARAMS = "update gps data",
            NEW_MESSAGE_AVAILABLE = "log the output";
    private static final UUID UUID_HRS =
            UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb"),
            UUID_HRD =
                    UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb"),
            Battery_Service_UUID =
                    UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb"),
            Battery_Level_UUID =
                    UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");

    String UserHRM = "";
    public static SensorsDataService itself;
    private final BluetoothGattCallback mGattCallback;
    public ArrayList<ISSRecordData> alldata = new ArrayList<ISSRecordData>();

    int[] sensorIDs = new int[]{Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GYROSCOPE};//,
    PowerManager.WakeLock wakeLock = null;
    // this wakes CPU for sensor measuring
    Alarm alarm = new Alarm();
    //int [] sensorIDs = new int[]{ Sensor.TYPE_SIGNIFICANT_MOTION};
    TimerTask timerTask = null;
    Timer timer = null;
    File mutexFile = new File(Environment.getExternalStorageDirectory(), "/triathlon_state.backup");


    File sensorsData = new File(Environment.getExternalStorageDirectory(), "/triathlon_iss_package.bin");
    BluetoothDevice hrmDevice = null;
    BluetoothGatt mBluetoothGatt = null;
    BluetoothGattService heartRateService = null;
    BluetoothGattCharacteristic heartRateCharacteristic = null;
    BluetoothGattService batteryLevelService = null;
    BluetoothGattCharacteristic batteryLevelCharacteristic = null;

    boolean SleepTrackingStopped = false;
    private int SamplingRate = 3; // in secs
    private long TrainingStart;
    private int UserID = 0;
    private SensorManager mSensorManager;
    private Sensor androidSensor;
    private GoogleApiClient mGoogleApiClient;
    private Handler mHandler;
    // map below allows to reduce amount of collected data
    private Map<Integer, Integer> recordedSensorTypes = new HashMap<Integer, Integer>();
    SensorEventListener sensorEventListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {

            if (!recordedSensorTypes.containsKey(event.sensor.getType())) {
                return;
            }
            recordedSensorTypes.remove(event.sensor.getType());
            mSensorManager.unregisterListener(sensorEventListener, event.sensor);

            AddNewData(UserID, event.sensor.getType(), GetTimeNow(), currentState, event.values[0], event.values[1], event.values[2]);

            if (recordedSensorTypes.isEmpty()) {
                if (wakeLock.isHeld()) {
                    wakeLock.release();
                }
            }

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };
    private BluetoothAdapter mBluetoothAdapter;
    private boolean isInitialising = true;
    private boolean hrmDisconnected = true;
    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {

                            // already found the hrm
                            if (hrmDevice != null) {
                                return;
                            }

                            String name = device.getAddress();

                            if (name.equals(UserHRM)) {
                                hrmDevice = device;
                                connectDevice(device);
                            }

                        }
                    }).run();
                }
            };
    public boolean needToShowRPE = false;
    private String USERID_FORDATASTORAGE = "smartwatch";
    private int USER_ASSUMED_SYNCTIME = 30*2;

    {
        mGattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                String intentAction;
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    mBluetoothGatt.discoverServices();
                    OutputEvent("HRM connected");
                    hrmDisconnected = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    OutputEvent("HRM disconnected");
                    hrmDisconnected = true;
                    //mBluetoothAdapter.startLeScan(mLeScanCallback);
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    heartRateService = gatt.getService(UUID_HRS);
                    batteryLevelService = gatt.getService(Battery_Service_UUID);

                    if (batteryLevelService != null) {
                        batteryLevelCharacteristic =
                                batteryLevelService.getCharacteristic(Battery_Level_UUID);
                    }


                    if (heartRateService != null) {

                        heartRateCharacteristic = heartRateService.getCharacteristic(UUID_HRD);
                        boolean res = gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER);
                        gatt.setCharacteristicNotification(heartRateCharacteristic, true);

                        try {
                            BluetoothGattDescriptor descriptor = heartRateCharacteristic.getDescriptor(
                                    UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));

                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

                            mBluetoothGatt.writeDescriptor(descriptor);
                            OutputEvent("Reading HRM");
                        } catch (Exception ex) {
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

                //OutputEvent("Characteristic read ");

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    int BatteryStatus = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    sendBatteryStatus(BatteryStatus);
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt,
                                                BluetoothGattCharacteristic characteristic) {

                if (recordedSensorTypes.containsKey(Sensor.TYPE_HEART_RATE)) {
                    recordedSensorTypes.remove(Sensor.TYPE_HEART_RATE);

                    int result = ReadHeartRateData(characteristic);

                    AddNewData(UserID, Sensor.TYPE_HEART_RATE, GetTimeNow(), currentState, result, 0, 0);

                    sendHR(result);

                    //SendHRtoSmartphone(result);

                    //mBluetoothGatt.disconnect();

                    mBluetoothGatt.readCharacteristic(batteryLevelCharacteristic);
                } else return;

            }
        };
    }

    @Override
    public void onCreate() {

        Log.d("SensorsDataService", "Created");

        itself = this;

        mHandler = new Handler();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();


        mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));

        Sensor accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        int fifoSize = accelerometer.getFifoReservedEventCount();
        if (fifoSize > 0) {
            Log.d("Accelerometer", "supports batching");
        } else {
            Log.d("Accelerometer", "does not support batching");
        }
        Sensor gyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        int gyrofifoSize = gyroscope.getFifoReservedEventCount();
        if (gyrofifoSize > 0) {
            Log.d("Gyroscope", "supports batching");
        } else {
            Log.d("Gyroscope", "does not support batching");
        }
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyWakelockTag");

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // create sensor data file

        if (!sensorsData.exists()) {
            try {
                Serializer.SerializeToFile(alldata, sensorsData);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        isInitialising = false;

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

    }

    int timerTime = 0;
    String currentState = "Idle";
    int timerTimeout = 60 * 60 * 24;
    int COOLING_MEASUREMENT_TIME = 60 * 60; // cooling is measured for 60 minutes
    int RESTING_MEASUREMENT_TIME = 60 * 5; // measure heart rate for 5 min
    int TRAINING_TIMEOUT = 60 * 60 * 24; // we assume that training times out eventually
    int COOLING_RPE_TIME = 60 * 15;

    public void TimerEvent() {

        timerTime = timerTime + 1;

        // this checks for missing hrm
        if ((timerTime % 300 == 0) && (hrmDisconnected)) {
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        }

        // fire this event only with some interval in seconds
        if (timerTime % SamplingRate == 0) {
            ResetSensors();
        }

        SendTimerTime(timerTime / 60, timerTime % 60);

        if (timerTime > timerTimeout) {

            boolean sleepMode = isNowASleepingHour() && currentState.equals("Resting");
            boolean startRecovery = currentState.equals("Cooldown");

            if (sleepMode){
                BringIntoState("Resting");
                startSleeping();
            } else if(startRecovery){
                BringIntoState("Recovery");
            }
            else{
                BringIntoState("Idle");
            }


        }



        OutputCurrentState();


        /*locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, gpsListener, null);
        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        gpsListener.onLocationChanged(lastKnownLocation);*/


        DataStorageManager.InitializeTriathlonFolder();

    }

    static boolean isNowASleepingHour(){

        Calendar clnd = Calendar.getInstance();
        return (clnd.get(Calendar.HOUR_OF_DAY) >= 12) || (clnd.get(Calendar.HOUR_OF_DAY) < 5);
    }


    private void AskUserForFeedback() {

        Intent myIntent = new Intent(this, DALDActivity.class);
        myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(myIntent);

        /*SensorsDataService.itself.needToShowRPE = true;

        Intent dialogIntent = new Intent(this, MainActivity.class);
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(dialogIntent);

        Intent intent = new Intent(this.ASK_USER_FOR_RPE);
        sendBroadcast(intent);*/
    }

    public void OutputCurrentState() {

        OutputEvent(currentState);

    }

    public void SendTimerTime(int minutes, int seconds) {

        Intent intent = new Intent(this.UPDATE_TIMER_VALUE);
        intent.putExtra("minutes", minutes);
        intent.putExtra("seconds", seconds);
        sendBroadcast(intent);

    }

    public void ResetSensors() {


        if (isInitialising) {
            return;
        }

        if (!wakeLock.isHeld()) {
            wakeLock.acquire();
        }

        for (int sensorID : sensorIDs) {
            androidSensor = mSensorManager.getDefaultSensor(sensorID);
            mSensorManager.unregisterListener(sensorEventListener, androidSensor);

            if (androidSensor.getFifoReservedEventCount() > 0) {
                //mSensorManager.registerListener(sensorEventListener, androidSensor, SensorManager.SENSOR_DELAY_NORMAL, 120);
                // there was some crashing going on, just in case I try the app without batching to see if bug persists
                mSensorManager.registerListener(sensorEventListener, androidSensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                mSensorManager.registerListener(sensorEventListener, androidSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }

            /*if (mBluetoothGatt != null ){
                mBluetoothGatt.connect();
            }*/

        recordedSensorTypes.clear();

        for (int sensor : sensorIDs) {
            recordedSensorTypes.put(sensor, 1);
        }


        recordedSensorTypes.put(Sensor.TYPE_HEART_RATE, 1);


        StopSleepTracking();

    }

    public void AddNewData(int uid, int sensortype, String timenow, String extras, float v0, float v1, float v2) {

        // data format: UserID, MeasurementType, Timestamp, ExtraData, MeasurementValue
        ISSRecordData data = new ISSRecordData(uid, sensortype, timenow, extras, v0, v1, v2);
        alldata.add(data);

        if (alldata.size() % 30 == 0) {

            ArrayList<ISSRecordData> copy = new ArrayList<>(alldata); // copy needed in order to avoid synchronization issues
            SaveNewDataToFile(copy);
            alldata.clear();
            System.gc();

        }

    }

    private void SaveNewDataToFile(ArrayList<ISSRecordData> data) {

        DataStorageManager.SaveNewDataToFile(data, USERID_FORDATASTORAGE);

    }

    public String GetTimeNow() {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd_HH:mm:ss");
        String currentDateandTime = sdf.format(new Date());
        return currentDateandTime;

    }

    public void connectDevice(BluetoothDevice device) {
        mBluetoothGatt = device.connectGatt(this, true, mGattCallback);
    }

    public void ReadCharact() {
        boolean result = mBluetoothGatt.readCharacteristic(heartRateCharacteristic);

        //int resultData = ReadHeartRateData(heartRateCharacteristic);
    }

    private void sendHR(int result) {
        // Send a broadcast with the current HR
        Intent hrintent = new Intent(ACTION_HR);
        hrintent.putExtra(EXTRA_HR, result);
        sendBroadcast(hrintent);
    }

    private void sendBatteryStatus(int Status) {
        // Send a broadcast with the battery status of the HRM
        Intent batteryintent = new Intent(ACTION_BATTERY_STATUS);
        batteryintent.putExtra(EXTRA_STATUS, Status);
        sendBroadcast(batteryintent);
    }

    public void SendHRtoSmartphone(float hr) {

        NodeApi.GetConnectedNodesResult result =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        List<Node> nodes = result.getNodes();
        String nodeId = null;

        byte[] data = null;

        /*long startTime = System.currentTimeMillis();

        try {
            data = Serializer.FileToBytes(sensorsData);
            //data = Serializer.SerializeToBytes(alldata);
        } catch (Exception e) {
            e.printStackTrace();
        }

        long totalTime = System.currentTimeMillis() - startTime;*/

        if (nodes.size() > 0) {
            for (int i = 0; i < nodes.size(); i++) {
                nodeId = nodes.get(i).getId();
                Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, Float.toString(hr), data);
            }
        }

    }

    public int ReadHeartRateData(BluetoothGattCharacteristic characteristic) {
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

    public void RecordActivitySwitch() {

        AddNewData(UserID, 0, GetTimeNow(), currentState, 0, 0, 0);

    }

    public boolean nonCoolable(String action){

        return action.equals("Resting") ||
                action.equals("Other") ||
                action.equals("Cooldown") ||
                action.equals("Recovery") ||
                action.contains("Cooling");

    }

    public void SwitchSportsAction(String action) {

        String newState = "";

        if (!currentState.equals("Idle")) {

            if (action.equals(currentState)){
                if (nonCoolable(currentState)) {
                    // stop recording cooling / resting prematurely
                    newState = "Idle";
                    RecordActivitySwitch();
                } else {
                    // start measuring cooling down
                    newState = currentState + ":Cooling";
                    OutputEvent("Cooling down ...");
                }
            }
            else
            {
                newState = action;
            }
        } else { // switch from idle state to measurement

            newState = action;
            RecordActivitySwitch();
        }

        BringIntoState(newState);
    }

    private void startSleeping(){
        Intent intent = new Intent(this, SensorsDataService.class);
        stopService(intent);

        // then launch sleep tracking
        Intent launchSleepIntent = getPackageManager().getLaunchIntentForPackage("com.urbandroid.sleep");
        startActivity(launchSleepIntent);

        // finally, kill the app in order to save the battery
        android.os.Process.killProcess(android.os.Process.myPid());
        return;
    }

    static File getRecordedActivitiesFile(){

        String daystr = DataStorageManager.getDayFromToday(0);
        File file = new File(DataStorageManager.userDataFolder, daystr + ".activities");
        return file;

    }

    public static void RecordActivityMeasured(String state){


        File file = getRecordedActivitiesFile();
        HashMap<String, Boolean> dictionary = new HashMap<>();

        try {
            if (!file.exists()){
                Serializer.SerializeToFile(dictionary, file);
            }

            dictionary = (HashMap<String, Boolean>) Serializer.DeserializeFromFile(file);

            if (dictionary.containsKey(state))
                return;

            if (state.equals("Resting")){
                state = state + ":" + itself.isNowASleepingHour();
            }

            dictionary.put(state, true);

            Serializer.SerializeToFile(dictionary, file);

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public static HashMap<String, Boolean> getRecordedActivities(){

        File file = getRecordedActivitiesFile();
        HashMap<String, Boolean> dictionary = new HashMap<>();

        try {

            if (file.exists()){
                dictionary = (HashMap<String, Boolean>) Serializer.DeserializeFromFile(file);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return dictionary;

    }

    private void BringIntoState(String state) {

        currentState = state;

        StopMeasuring();

        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        // Vibrate for 500 milliseconds
        v.vibrate(500);

        try {
            Serializer.SerializeToFile(state, mutexFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        RecordActivityMeasured(state);

        if (state.equals("Idle")) {
            return;
        }

        if (state.equals("Resting")) {
            // stop recording cooling / resting prematurely
            timerTimeout = RESTING_MEASUREMENT_TIME;

            if (isNowASleepingHour()){
                AskUserForFeedback();
            }

        }else if (state.contains("Cooldown")) {
            timerTimeout = RESTING_MEASUREMENT_TIME; // needed to recover the state of the app properly
            //StopGPS();
        }else if (state.contains("Recovery")) {
            timerTimeout = COOLING_MEASUREMENT_TIME; // needed to recover the state of the app properly

            if (!getRecordedActivities().containsKey("Cooldown")){
                Intent myIntent = new Intent(this, TrainingStartTimeActivity.class);
                myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(myIntent);
            }

            //StopGPS();
        } else if (state.contains("Cooling")) {
            timerTimeout = COOLING_MEASUREMENT_TIME; // needed to recover the state of the app properly
            OutputEvent("Cooling down ...");
            //StopGPS();
        } else {
            // start training
            timerTimeout = TRAINING_TIMEOUT;
            //StartGPS();
        }

        StartMeasuring();

    }

    void StartMeasuring() {

        GetHRMid();

        mBluetoothAdapter.stopLeScan(mLeScanCallback);
        mBluetoothAdapter.startLeScan(mLeScanCallback);

        /*mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                //OutputEvent("HRM search stop.");
            }
        }, 10000);*/

        OutputEvent("Searching HRM ... ");
        timerTask = new TimerTask() {
            public void run() {
                TimerEvent();
            }
        };

        timer = new Timer();
        //TrainingStart = System.currentTimeMillis() / 1000; // in seconds
        timerTime = 0;

        if (!wakeLock.isHeld()) {
            wakeLock.acquire();
        }

        timer.schedule(timerTask, 0, 1000);

    }

    void StopMeasuring() {

        if (wakeLock.isHeld()) {
            wakeLock.release();
        }

        mBluetoothAdapter.stopLeScan(mLeScanCallback);

        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
            hrmDevice = null;
        }

        OutputEvent("HRM off");

        if (timer != null) {
            timer.cancel();
        }
    }

    public void genData() {

        for (int i = 0; i < 1000; i++) {

            AddNewData(1, 21, GetTimeNow(), currentState, 1, 3.123f, 3);

        }

    }

    public void SendCollectedData() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                //mGoogleApiClient.blockingConnect(3000, TimeUnit.MILLISECONDS);
                NodeApi.GetConnectedNodesResult result =
                        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
                List<Node> nodes = result.getNodes();
                String nodeId = null;

                //genData();

                ArrayList<ISSRecordData> allData = new ArrayList<>();

                // I assume that user synced with sw in last 2 month
                ArrayList<File> files = DataStorageManager.GetAllFilesToUpload(USERID_FORDATASTORAGE, USER_ASSUMED_SYNCTIME);
                Collections.reverse(files); // make sure that files are from oldest to newest

                for (File file:files){

                    if (file.exists()){

                        try {
                            List<ISSRecordData> issRecordDatas = CSVManager.ReadCSVdata(file);
                            allData.addAll(issRecordDatas);
                        }catch (Exception ex){

                        }

                    }
                    //allData.add();

                }

                byte [] data = null;

                try {
                    data = Serializer.SerializeToBytes(allData);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (nodes.size() > 0) {
                    for (int i = 0; i < nodes.size(); i++) {
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

    public void OutputEvent(String str) {
        // Send a Broadcast with the message
        Intent intent = new Intent(this.NEW_MESSAGE_AVAILABLE);
        intent.putExtra("message", str);
        sendBroadcast(intent);
    }

    @Override
    public void onMessageReceived(MessageEvent event) {
        LOGD(TAG, "onMessageReceived: " + event);
        byte[] data = event.getData();

        if (data != null) {
            if (data[0] == 1) {
                // send available data
                SendCollectedData();
            }

            if (data[0] == 2) {
                // send available data
                OutputEvent("Data saved");
                alldata.clear();

                // clear the existing data on the smartwatch

                ArrayList<File> files = DataStorageManager.GetAllFilesToUpload(USERID_FORDATASTORAGE, USER_ASSUMED_SYNCTIME);
                Collections.reverse(files); // make sure that files are from oldest to newest

                for (File file:files){
                    if (file.exists()){
                        file.delete();
                    }
                }

            }

            if (data[0] == 3) {
                SleepTrackingStopped = true;
            }

        }
    }

    @Override
    public void onDestroy() {

    }

    // this method defined user heart rate monitor prior to the enabling of the training mode
    public void GetHRMid() {

        String android_id = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);

        switch (android_id) {
            case "cf533cb594eb941f":
                UserHRM = "E5:CF:3E:D5:22:1B";
                break;
            case "fb89ac5028563ab5":
                UserHRM = "C3:65:88:2F:C0:12";
                break;
            case "bd9050e5d954b9a3":
                UserHRM = "F7:71:B1:1D:EE:69";
                break;
            case "faa47b6b99e0a2b8":
                UserHRM = "F1:CC:A3:7E:66:BD";
                break;
            case "f77a4bb95172c007":
                UserHRM = "E0:28:1F:12:A1:20";
                break;
            case "760bd2c1de704a18":
                UserHRM = "DA:2B:64:87:44:35";
                break;
            case "1af13a491433cd6d":
                UserHRM = "CC:1F:BD:F5:24:FA";
                break;
            case "1f3ae220a852939f":
                UserHRM = "DA:2B:64:87:44:35";
                break;
            case "b0bfcacefe39d7d6":
                UserHRM = "C3:4D:73:79:04:0E";
                break;
            default:
                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
                UserHRM = pref.getString(getString(R.string.device_address), "");
                break;

        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        alarm.SetAlarm(this);

        // get unique id of the device
        String android_id = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        //OutputEvent(android_id);

        Log.d("ISS", "Android ID: " + android_id);

        // for known IMEI's (those of athletes phones) lets let for now the hrm to be hardcoded.
        // for other (potential) users, it would be read from the preferences
        // p.s. user id is not important on wearable app and is deprectaded, will be removed in future
        switch (android_id) {
            case "cf533cb594eb941f":
                UserID = 1;
                UserHRM = "E5:CF:3E:D5:22:1B";
                break;
            case "fb89ac5028563ab5":
                UserID = 2;
                UserHRM = "C3:65:88:2F:C0:12";
                break;
            case "bd9050e5d954b9a3":
                UserID = 3;
                UserHRM = "F7:71:B1:1D:EE:69";
                break;
            case "faa47b6b99e0a2b8":
                UserID = 4;
                UserHRM = "F1:CC:A3:7E:66:BD";
                break;
            case "f77a4bb95172c007":
                UserID = 5;
                UserHRM = "E0:28:1F:12:A1:20";
                break;
            case "760bd2c1de704a18":
                UserID = 256;
                UserHRM = "DA:2B:64:87:44:35";
                break;
            case "1af13a491433cd6d":
                UserID = 1024;
                UserHRM = "CC:1F:BD:F5:24:FA";
                break;
            case "1f3ae220a852939f":
                UserID = 127;
                UserHRM = "DA:2B:64:87:44:35";
                break;
            case "b0bfcacefe39d7d6":
                UserID = 257;
                UserHRM = "C3:4D:73:79:04:0E";
                break;
            default:
                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
                UserHRM = pref.getString(getString(R.string.device_address), "");
                break;
        }

        InitializeMutexRecovery();

        StopSleepTracking();

        return START_STICKY;

    }

    // in case app crashes, its state is restored automatically
    public void InitializeMutexRecovery() {

        // create mutex file

        if (!mutexFile.exists()) {
            try {
                Serializer.SerializeToFile(currentState, mutexFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            currentState = (String) Serializer.DeserializeFromFile(mutexFile);
            BringIntoState(currentState);
        } catch (Exception e) {
            e.printStackTrace();
            OutputEvent(e.toString());
        }

        //OutputEvent("Mutex state: " + allowHRM);


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

    public void StopSleep() {

        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    NodeApi.GetConnectedNodesResult result =
                            Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
                    List<Node> nodes = result.getNodes();
                    String nodeId = null;

                    byte[] data = null;

                    /*long startTime = System.currentTimeMillis();
                    try {
                        data = Serializer.FileToBytes(sensorsData);
                        //data = Serializer.SerializeToBytes(alldata);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    long totalTime = System.currentTimeMillis() - startTime;*/

                    if (nodes.size() > 0) {
                        for (int i = 0; i < nodes.size(); i++) {
                            nodeId = nodes.get(i).getId();
                            Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, "Stop sleep tracking", data);
                        }
                    }
                } catch (Exception ex) {
                    OutputEvent(ex.toString());
                }
            }
        }).start();

    }

    public void StopSleepTracking() {

        if (!SleepTrackingStopped) {
            StopSleep();
        }

    }

    public void AddTrainingScore(float position, String typeof) {
        AddNewData(0, 1024, GetTimeNow(), "Feedback:" + typeof, position, 0, 0);
        needToShowRPE = false;
    }

    // GPS processing

    LocationManager locationManager = null;
    LocationListener gpsListener = null;

    Location locPrev = null;
    double totalDistance = 0;

    public void TrainingEnd(float hour, float minute) {

        AddNewData(0, 13, GetTimeNow(), currentState, hour, minute, 0.0f);

    }
/*
    public void StartGPS() {

        gpsListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                double altitude = location.getAltitude();

                AddNewData(UserID, 512, GetTimeNow(), currentState, (float) latitude, (float) longitude, (float) altitude);

                float speed = location.getSpeed() * 3.6f;
                AddNewData(UserID, 513, GetTimeNow(), currentState, speed, 0, 0);

                if (locPrev != null) {
                    float dis = locPrev.distanceTo(location);
                    totalDistance += dis;
                }
                AddNewData(UserID, 514, GetTimeNow(), currentState, (float) totalDistance, 0, 0);

                SendGPSdata(speed, totalDistance / 1000);

                locPrev = location;

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        try {
            if (locationManager != null) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 0, gpsListener);
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 0, gpsListener);
            }
        } catch (Exception ex) {
            OutputEvent(ex.toString());
        }

    }

    // GPS processing

    public void StopGPS() {

        locationManager.removeUpdates(gpsListener);
        totalDistance = 0;

    }

    public void SendGPSdata(double speed, double totalDistance) {

        Intent intent = new Intent(this.UPDATE_GPS_PARAMS);
        intent.putExtra("speed", speed);
        intent.putExtra("totalDistance", totalDistance);
        sendBroadcast(intent);

    }
*/
}
