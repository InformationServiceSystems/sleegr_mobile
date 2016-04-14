package com.iss.android.wearable.datalayer;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DataSyncService extends Service implements DataApi.DataListener,
        MessageApi.MessageListener, NodeApi.NodeListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    public static DataSyncService itself = null;
    public boolean serverSync = false;
    public static String Message = "";

    private static final String TAG = "MainActivity";

    private GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError = false;
    private Handler mHandler;

    public static String NEW_MESSAGE_AVAILABLE = "log the output";


    String uploadUrl = "http://46.101.214.58:5001/upload2/";

    // A method broadcasting a String.
    public static void OutputEventSq(String str) {

        if (DataSyncService.itself != null) {
            DataSyncService.itself.OutputEvent(str);
        }

    }

    // A method responsible for getting the UserID.
    public static String getUserID() {

        return DataStorageManager.getProperUserID(itself.UserID);
        //return "1024";
    }

    @Override
    public void onCreate() {

        super.onCreate();
        mHandler = new Handler();

        // create artificial data

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mGoogleApiClient.connect();

        itself = this;

        /*PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyWakelockTag");

        wakeLock.acquire();*/

        /*if (sensorsData.exists()) {
            sensorsData.delete();
        }*/

    }

    SyncAlarm alarm = new SyncAlarm();

    public String UserID = "userID";

    // a method called after the service is started, that is responsible for the service not shutting down.
    // It also tries to get the UserID and welcomes the user.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        try {

            alarm.SetAlarm(this);

            String android_id = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
            OutputEvent(android_id);

            Log.d("ISS", "Android ID: " + android_id);

            switch (android_id) {
                case "144682d5efc12dcb":
                    UserID = "1";
                    break;
                case "4b251c5e4f524b05":
                    UserID = "2";
                    break;
                case "3622852bee38de73":
                    UserID = "3";
                    break;
                case "7b1d96be4726dd22":
                    UserID = "4";
                    break;
                case "847222e512faa744":
                    UserID = "5";
                    break;
                case "867ee27023b1f8b7":
                    UserID = "256";
                    break;
                case "65e9172b7bb0638d":
                    UserID = "1024";
                    break;
                case "3032a1d80ae293bd ":
                    UserID = "257";
                    break;
                default:
                    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
                    UserID = pref.getString("user_email", "unknown");
                    OutputEvent("Welcome user " + UserID + "!");
                    break;
            }

            DataStorageManager.InitializeTriathlonFolder();

        }catch (Exception ex){

        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onConnected(Bundle bundle) {
        mResolvingError = false;
        //mStartActivityBtn.setEnabled(true);
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
        Wearable.NodeApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    // A method that stops sleep tracking (used when this app is woken up)
    public void StopSleepTracking() {
        try {
            Intent intent = new Intent("com.urbandroid.sleep.alarmclock.STOP_SLEEP_TRACK");
            sendBroadcast(intent);
            ConfirmSleepTrackingStopped();
        } catch (Exception ex) {
            System.out.println(ex.toString());
        }
    }

    // A method that receives messages and handles them.
    @Override
    public void onMessageReceived(final MessageEvent messageEvent) {


        if (messageEvent.getPath().equals("Stop sleep tracking")) {
            StopSleepTracking();
        } else {
            SendHRtoServer(messageEvent.getPath());
        }

        //OutputEvent(messageEvent.getPath());

        //

       /*if (messageEvent.getPath().equals("data")) {
            byte[] data = messageEvent.getData();
            try {
                ArrayList<ISSRecordData> receivedData = (ArrayList<ISSRecordData>) Serializer.DeserializeFromBytes(data);
                OutputEvent("Read data from the watch of size " + receivedData.size());
                SaveNewDataToFile(receivedData);
                ClearWatchData();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }*/

    }

    // A method that sends a string containing the heart rate to the server and checks if the server got the message.
    public void SendHRtoServer(String HR) {

        try {
            URL obj = new URL("http://46.101.214.58:5100/realtime?hrm=" + HR);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            int responseCode = con.getResponseCode();
            System.out.println("GET Response Code :: " + responseCode);
            if (responseCode == HttpURLConnection.HTTP_OK) { // success
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }

                in.close();

                // print result
                OutputEvent(response.toString());
            } else {
                OutputEvent("GET request not worked");
            }
        } catch (Exception ex) {

            OutputEvent(ex.toString());

        }

    }

    // A method that is triggered if sensor data has changed and then saves the data from the sensor (I guess?)
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {

        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED && event.getDataItem().getUri().getPath().equals("/sensorData")) {
                try {
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                    final Asset asset = dataMapItem.getDataMap().getAsset("sensorData");

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            SaveDataFromAsset(asset);
                        }
                    }).start();


                } catch (Exception ex) {
                    ex.printStackTrace();
                }

            }
        }
    }

    // A method that transforms the data from an Asset byte array to the ISSRecordData
    // it has been generated from and then stores the ISSRecordData, handing it DataStorageManager,
    // then clearing the data on the watch.
    public void SaveDataFromAsset(Asset asset) {

        if (asset == null) {
            throw new IllegalArgumentException("Asset must be non-null");
        }

        ConnectionResult result =
                mGoogleApiClient.blockingConnect(1000 * 100, TimeUnit.MILLISECONDS);

        if (!result.isSuccess()) {
            return;
        }
        // convert asset into a file descriptor and block until it's ready
        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                mGoogleApiClient, asset).await().getInputStream();

        if (assetInputStream == null) {
            Log.w(TAG, "Requested an unknown Asset.");
            return;
        }


        try {

            byte[] data = Serializer.InputStreamToByte(assetInputStream);

            ArrayList<ISSRecordData> receivedData = (ArrayList<ISSRecordData>) Serializer.DeserializeFromBytes(data);
            OutputEvent("Recieved " + receivedData.size() + " of data");
            DataStorageManager.SaveNewDataToFile(receivedData, UserID);
            ClearWatchData();

        } catch (Exception e) {
            e.printStackTrace();
            OutputEvent(e.toString());
        }

    }

    // A method broadcasting a String, generally updates about the state of the app.
    public void OutputEvent(String str) {

        Intent intent = new Intent(NEW_MESSAGE_AVAILABLE);
        intent.putExtra("message", str);
        sendBroadcast(intent);

    }


    // A method that connects to the Smartwatch and asks it to send data it has stored.
    public void RequestDataFromWatch() {

        OutputEvent("Requested data from the watch ... ");

        new Thread(new Runnable() {
            @Override
            public void run() {
                //mGoogleApiClient.blockingConnect(3000, TimeUnit.MILLISECONDS);
                NodeApi.GetConnectedNodesResult result =
                        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
                List<Node> nodes = result.getNodes();
                String nodeId = null;
                if (nodes.size() > 0) {
                    for (int i = 0; i < nodes.size(); i++) {
                        nodeId = nodes.get(i).getId();
                        Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, "Please send data", new byte[]{1});
                    }
                }
            }
        }).start();

    }

    // A method that handles deletion of data that has successfully been send to the phone.
    public void ClearWatchData() {

        OutputEvent("Data saved. Clearing data on the watch");

        new Thread(new Runnable() {
            @Override
            public void run() {
                //mGoogleApiClient.blockingConnect(3000, TimeUnit.MILLISECONDS);
                NodeApi.GetConnectedNodesResult result =
                        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
                List<Node> nodes = result.getNodes();
                String nodeId = null;
                if (nodes.size() > 0) {
                    for (int i = 0; i < nodes.size(); i++) {
                        nodeId = nodes.get(i).getId();
                        Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, "Clear the data", new byte[]{2});
                    }
                }

            }
        }).start();
    }

    // A method that is called after sleep tracking has been stopped. It sends this information to the Smartwatch
    public void ConfirmSleepTrackingStopped() {

        OutputEvent("Stopped the sleep tracking");

        new Thread(new Runnable() {
            @Override
            public void run() {
                //mGoogleApiClient.blockingConnect(3000, TimeUnit.MILLISECONDS);
                NodeApi.GetConnectedNodesResult result =
                        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
                List<Node> nodes = result.getNodes();
                String nodeId = null;
                if (nodes.size() > 0) {
                    for (int i = 0; i < nodes.size(); i++) {
                        nodeId = nodes.get(i).getId();
                        Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, "Sleep tracking stopped", new byte[]{3});
                    }
                }

            }
        }).start();
    }


    // Magic that is supposed to keep the process running on the Android v 4.4 even
    // after the app is swiped away; seems to be a bug of this android version. GJ Google
    // http://stackoverflow.com/questions/20677781/in-android-4-4-swiping-app-out-of-recent-tasks-permanently-kills-application-wi
    /*public void onTaskRemoved(Intent rootIntent) {
        Log.e("FLAGX : ", ServiceInfo.FLAG_STOP_WITH_TASK + "");
        Intent restartServiceIntent = new Intent(getApplicationContext(),
                this.getClass());
        restartServiceIntent.setPackage(getPackageName());

        PendingIntent restartServicePendingIntent = PendingIntent.getService(
                getApplicationContext(), 1, restartServiceIntent,
                PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmService = (AlarmManager) getApplicationContext()
                .getSystemService(Context.ALARM_SERVICE);
        alarmService.set(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 1000,
                restartServicePendingIntent);

        super.onTaskRemoved(rootIntent);
    }*/


    // A method that collects all files of the last week (see GetAllFilestoUpload),
    // converts them to an ISSRecordData list, seperates them by measurement type
    // and sends them to the server using the UploadingManager class.
    public void ShareDataWithServer() {

        ArrayList<ArrayList<File>> arrayLists = DataStorageManager.GetAllFilesToUpload(UserID, 7);

        for (ArrayList<File> day : arrayLists) {

            // get all data

            ArrayList<ISSRecordData> alldata = new ArrayList<>();

            for (File file : day) {
                List<ISSRecordData> records = CSVManager.ReadCSVdata(file);
                alldata.addAll(records);
            }

            // sort data in chronological order?

            // separate channels, convert to string, upload to server

            if (alldata.size() == 0)
                continue;

            // the day component of file name
            String dateName = TimeSeries.dictionary_format.format(alldata.get(0).getTimestamp());

            // determine all measurement types
            HashMap<Integer, ArrayList<ISSRecordData>> map = new HashMap<>();

            for (ISSRecordData record : alldata) {
                if (!map.containsKey(record.MeasurementType))
                    map.put(record.MeasurementType, new ArrayList<ISSRecordData>());

                map.get(record.MeasurementType).add(record);

            }

            // upload data separately for each measurement type

            for (Integer measurementType : map.keySet()) {
                String uploadingName = dateName + "-" + measurementType + ".csv";

                ArrayList<ISSRecordData> measurementData = map.get(measurementType);

                StringBuilder stringBuilder = CSVManager.RecordsToCSV(measurementData);
                String contents = stringBuilder.toString();

                UploadingManager.UploadUserFileToServer(contents.getBytes(), uploadingName, uploadUrl, UserID);
            }

        }

        // finally, upload sleep data
        UploadingManager.UploadUserFileToServer( Serializer.FileToBytes(DataStorageManager.sleepData), "sleep-export.csv", uploadUrl, UserID);
        OutputEvent("Sent data files to server");

    }

    @Override
    public void onPeerConnected(Node node) {
        OutputEvent("Peer connected: " + node.toString());
    }

    @Override
    public void onPeerDisconnected(Node node) {
        OutputEvent("Peer disconnected: " + node.toString());
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (result.hasResolution()) {
            mResolvingError = true;
            mGoogleApiClient.connect();
        } else {
            Log.e(TAG, "Connection to Google API client has failed");
            mResolvingError = false;
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            Wearable.MessageApi.removeListener(mGoogleApiClient, this);
            Wearable.NodeApi.removeListener(mGoogleApiClient, this);
        }
    }

    private static void LOGD(final String tag, String message) {
        if (Log.isLoggable(tag, Log.DEBUG)) {
            Log.d(tag, message);
        }
    }


}
