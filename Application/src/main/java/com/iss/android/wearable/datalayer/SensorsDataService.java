package com.iss.android.wearable.datalayer;

import android.app.Activity;
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
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class SensorsDataService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener,
        NodeApi.NodeListener {

    public static final String
            ACTION_BATTERY_STATUS = SensorsDataService.class.getName() + "BatteryStatus",
            MESSAGE = SensorsDataService.class.getName() + "Message",
            ACTION_HR = SensorsDataService.class.getName() + "HeartRate",
            EXTRA_STATUS = "extra_status",
            EXTRA_HR = "extra_hr",
            TAG = "MainActivity",
            CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb",
            UPDATE_TIMER_VALUE = "update the timer value";
    private static final UUID UUID_HRS =
            UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb"),
            UUID_HRD =
                    UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb"),
            Battery_Service_UUID =
                    UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb"),
            Battery_Level_UUID =
                    UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
    public static SensorsDataService itself;
    private static HashMap<String, Boolean> recordedActivities = new HashMap<String, Boolean>();
    private final BluetoothGattCallback mGattCallback;
    String UserHRM = "";
    int[] sensorIDs = new int[]{Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GYROSCOPE, Sensor.TYPE_STEP_COUNTER};//,
    PowerManager.WakeLock wakeLock = null;
    // this wakes CPU for sensor measuring
    Alarm alarm = new Alarm();
    //int [] sensorIDs = new int[]{ Sensor.TYPE_SIGNIFICANT_MOTION};
    TimerTask timerTask = null;
    Timer timer = null;
    BluetoothDevice hrmDevice = null;
    BluetoothGatt mBluetoothGatt = null;
    BluetoothGattService heartRateService = null;
    BluetoothGattCharacteristic heartRateCharacteristic = null;
    BluetoothGattService batteryLevelService = null;
    BluetoothGattCharacteristic batteryLevelCharacteristic = null;
    boolean SleepTrackingStopped = false;
    int timerTime = 0;
    String currentState = "Idle";
    int timerTimeout = 60 * 60 * 24;
    int RESTING_MEASUREMENT_TIME = 60 * 3; // measure heart rate for 3 min
    int TRAINING_MEASUREMENT_TIME = 60 * 60 * 2;
    private int SamplingRate = 3; // in secs
    private long TrainingStart;
    private int UserID = 0;
    private SensorManager mSensorManager;
    private Sensor androidSensor;
    private GoogleApiClient mGoogleApiClient;
    private Handler mHandler;
    // map below allows to reduce amount of collected data
    private Map<Integer, Integer> recordedSensorTypes = new HashMap<Integer, Integer>();
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
    private String USERID_FORDATASTORAGE = "smartwatch";
    private int measurementNumber = 0;
    SensorEventListener sensorEventListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            // Don't unregister the Step Counter Sensor or it will lose all information.
            if (event.sensor.getType() != Sensor.TYPE_STEP_COUNTER) {
                if (!recordedSensorTypes.containsKey(event.sensor.getType())) {
                    return;
                }
                recordedSensorTypes.remove(event.sensor.getType());
                mSensorManager.unregisterListener(sensorEventListener, event.sensor);
            }

            if (event.values.length == 1) {
                ISSRecordData data = new ISSRecordData(UserID, event.sensor.getType(), GetDateNow(), GetTimeNow(), currentState, event.values[0], 0, 0, PreferenceManager.getDefaultSharedPreferences(MainActivity.getContext()).getString(getString(R.string.device_name), "dummy sensor"), measurementNumber);
                DataStorageManager.insertISSRecordData(data);
            } else {
                ISSRecordData data = new ISSRecordData(UserID, event.sensor.getType(), GetDateNow(), GetTimeNow(), currentState, event.values[0], event.values[1], event.values[2], android.os.Build.MODEL, measurementNumber);
                DataStorageManager.insertISSRecordData(data);
            }

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

    {
        mGattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                String intentAction;
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    mBluetoothGatt.discoverServices();
                    showToast(getApplicationContext(), "HRM connected", Toast.LENGTH_SHORT);
                    hrmDisconnected = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    showToast(getApplicationContext(), "HRM disconnected", Toast.LENGTH_SHORT);
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
                            showToast(getApplicationContext(), "Reading HRM", Toast.LENGTH_SHORT);
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

                    ISSRecordData data = new ISSRecordData(UserID, Sensor.TYPE_HEART_RATE, GetDateNow(), GetTimeNow(), currentState, result, 0, 0, PreferenceManager.getDefaultSharedPreferences(MainActivity.getContext()).getString(getString(R.string.device_name), "dummy sensor"), measurementNumber);
                    DataStorageManager.insertISSRecordData(data);

                    sendHR(result);

                    mBluetoothGatt.readCharacteristic(batteryLevelCharacteristic);
                } else return;

            }
        };
    }

    public static HashMap<String, Boolean> getRecordedActivities() {
        return recordedActivities;
    }

    static boolean isNowASleepingHour() {

        Calendar clnd = Calendar.getInstance();
        return (clnd.get(Calendar.HOUR_OF_DAY) >= 13);
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

        isInitialising = false;

    }

    // Checks if 3 minutes have passed since pressing the cooldown button.
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
            boolean sleepMode = currentState.equals("eveningHR");
            BringIntoState("Idle");

            if (sleepMode) {
                startSleeping();
            }
        }
        OutputCurrentState();
    }

    public void OutputCurrentState() {
        Intent i = new Intent("com.iss.android.wearable.datalayer." + currentState);
        i.putExtra("message", currentState);
        sendBroadcast(i);
    }

    public void OutputMessage(String message) {
        Intent i = new Intent(MESSAGE);
        i.putExtra("message", message);
        sendBroadcast(i);
    }

    // broadcasts the time elapsed via intent
    public void SendTimerTime(int minutes, int seconds) {
        Intent intent = new Intent(UPDATE_TIMER_VALUE);
        intent.putExtra("minutes", minutes);
        intent.putExtra("seconds", seconds);
        sendBroadcast(intent);
    }

    // Unregisters and registers sensors.
    public void ResetSensors() {
        if (isInitialising) {
            return;
        }

        if (!wakeLock.isHeld()) {
            wakeLock.acquire();
        }

        for (int sensorID : sensorIDs) {
            // If we unregister the Step Counter, it stops tracking steps.
            // This type of sensor counts all steps since the last reboot.
            // Thus, the difference between the latest value for a day
            // and the day before is the daily steps.
            if (sensorID != Sensor.TYPE_STEP_COUNTER) {
                androidSensor = mSensorManager.getDefaultSensor(sensorID);
                mSensorManager.unregisterListener(sensorEventListener, androidSensor);

                if (androidSensor.getFifoReservedEventCount() > 0) {
                    //mSensorManager.registerListener(sensorEventListener, androidSensor, SensorManager.SENSOR_DELAY_NORMAL, 120);
                    // there was some crashing going on, just in case I try the app without batching to see if bug persists
                    mSensorManager.registerListener(sensorEventListener, androidSensor, SensorManager.SENSOR_DELAY_NORMAL);
                } else {
                    mSensorManager.registerListener(sensorEventListener, androidSensor, SensorManager.SENSOR_DELAY_NORMAL);
                }

                recordedSensorTypes.clear();

                for (int sensor : sensorIDs) {
                    recordedSensorTypes.put(sensor, 1);
                }
            }

            recordedSensorTypes.put(Sensor.TYPE_STEP_COUNTER, 1);
            recordedSensorTypes.put(Sensor.TYPE_HEART_RATE, 1);


            StopSleepTracking();
        }

    }

    // returns the current time in string form
    public String GetTimeNow() {

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(new Date());

    }

    public String GetDateNow() {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd");
        return sdf.format(new Date());

    }

    public void connectDevice(BluetoothDevice device) {
        mBluetoothGatt = device.connectGatt(this, true, mGattCallback);
    }

    // broadcasts the heart rate via intent
    private void sendHR(int result) {
        // Send a broadcast with the current HR
        Intent hrintent = new Intent(ACTION_HR);
        hrintent.putExtra(EXTRA_HR, result);
        sendBroadcast(hrintent);
    }

    // broadcasts the battery status via intent
    private void sendBatteryStatus(int Status) {
        // Send a broadcast with the battery status of the HRM
        Intent batteryintent = new Intent(ACTION_BATTERY_STATUS);
        batteryintent.putExtra(EXTRA_STATUS, Status);
        sendBroadcast(batteryintent);
    }

    // reads the heart rate from the bluetooth sensor's value
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

    // Switches the sports action to the new activity type
    public void SwitchSportsAction(String action) {
        Log.d("Switched to", action);

        String newState = "";

        if (!currentState.equals("Idle")) {

            if (action.equals(currentState)) {
                // stop recording cooling / resting prematurely
                newState = "Idle";
                DeleteLatestMeasurement();
            } else {
                newState = action;
            }
        } else { // switch from idle state to measurement

            newState = action;
        }

        BringIntoState(newState);
    }

    private void DeleteLatestMeasurement() {

    }

    // starts sleep tracking
    private void startSleeping() {
        alarm.CancelAlarm(getApplicationContext());
        Intent intent = new Intent(this, SensorsDataService.class);
        stopService(intent);

        // then launch sleep tracking
        Intent launchSleepIntent = getPackageManager().getLaunchIntentForPackage("com.urbandroid.sleep");
        startActivity(launchSleepIntent);

        // finally, kill the app in order to save the battery
        System.exit(0);
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    // vibrates for 0.2 seconds, 0.2 seconds silence, 4 times.
    void outputVibration() {

        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        long length = 200;
        v.vibrate(new long[]{length, length, length, length, length, length, length, length}, -1);

    }

    // Changes the state of the app so as to realise what is being measured at the moment
    private void BringIntoState(String state) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = pref.edit();
        Calendar calendar = new GregorianCalendar();
        String date = calendar.get(Calendar.DAY_OF_MONTH) + calendar.get(Calendar.MONTH) + calendar.get(Calendar.YEAR) + "";
        recordedActivities.put(state, true);

        currentState = state;

        StopMeasuring();

        outputVibration();

        if (state.equals("Idle")) {
            alarm.CancelAlarm(getApplicationContext());
            showToast(getApplicationContext(), "Stopped Measuring", Toast.LENGTH_SHORT);
            OutputMessage("Invalidate Button Colors, Finished");
            return;
        }
        alarm.SetAlarm(getApplicationContext());

        if (state.equals("MorningHR")) {
            timerTimeout = RESTING_MEASUREMENT_TIME;
            editor.putBoolean(date + state, true);
            OutputMessage("Invalidate Morning Colors");
        } else if (state.equals("EveningHR")) {
            timerTimeout = RESTING_MEASUREMENT_TIME;
            editor.putBoolean(date + state, true);
            OutputMessage("Invalidate Evening Colors");
        } else if (state.equals("TrainingHR")) {
            timerTimeout = TRAINING_MEASUREMENT_TIME;
            editor.putBoolean(date + state, true);
            OutputMessage("Invalidate Training Colors");
        } else if (state.equals("Cooldown")) {
            timerTimeout = RESTING_MEASUREMENT_TIME;
            editor.putBoolean(date + state, true);
            OutputMessage("Invalidate Cooldown Colors");
        } else if (state.contains("Recovery")) {
            timerTimeout = RESTING_MEASUREMENT_TIME;
            editor.putBoolean(date + state, true);
            OutputMessage("Invalidate Recovery Colors");
        }
        editor.apply();
        StartMeasuring();
    }

    // Starts measuring the heart rate and, during that time, holds wakelock
    void StartMeasuring() {
        // Starts a new Measurement in the database.
        Date date = new Date();
        ISSMeasurement measurement = new ISSMeasurement(Log.d("Loaded measurement as", String.valueOf(measurementNumber)), currentState, date.toString());
        DataStorageManager.insertISSMeasurement(measurement);
        // as a secondary key for the rpe values and the record data

        getHrmAddress();

        mBluetoothAdapter.stopLeScan(mLeScanCallback);
        mBluetoothAdapter.startLeScan(mLeScanCallback);

        showToast(getApplicationContext(), "Starting measuring", Toast.LENGTH_SHORT);
        timerTask = new TimerTask() {
            public void run() {
                TimerEvent();
            }
        };

        timerTime = 0;
        timer = new Timer();

        if (!wakeLock.isHeld()) {
            wakeLock.acquire();
        }

        timer.schedule(timerTask, 0, 1000);

    }

    // Stops measuring the heart rate
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

        if (timer != null) {
            timer.cancel();
        }
    }

    @Override
    public void onDestroy() {
        // ensures that all objects will be finalized and garbage collected when the the application exits
        System.runFinalizersOnExit(true);
    }

    // this method defined user heart rate monitor prior to the enabling of the training mode
    public void getHrmAddress() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        UserHRM = pref.getString(getString(R.string.device_address), "");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        alarm.SetAlarm(this);
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        UserHRM = pref.getString(getString(R.string.device_address), "");

        StopSleepTracking();

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

    private void showToast(final Context context, final String message, final int length) {
        Activity mActivity = (Activity) MainActivity.itself;
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, length).show();
            }
        });
    }

    // stops sleep tracking
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

                    if (nodes.size() > 0) {
                        for (int i = 0; i < nodes.size(); i++) {
                            nodeId = nodes.get(i).getId();
                            Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, "Stop sleep tracking", data);
                        }
                    }
                } catch (Exception ex) {
                    showToast(getApplicationContext(), ex.getMessage(), Toast.LENGTH_SHORT);
                }
            }
        }).start();

    }

    public void StopSleepTracking() {
        if (!SleepTrackingStopped) {
            StopSleep();
        }
    }
}
