package com.iss.android.wearable.datalayer;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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


    String uploadUrl = "http://web01.iss.uni-saarland.de:81";

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

            byte[] dataAsByteArray = Serializer.InputStreamToByte(assetInputStream);
            byte[][] data = (byte[][]) Serializer.DeserializeFromBytes(dataAsByteArray);
            ArrayList<ISSRecordData> ISSRecords = (ArrayList<ISSRecordData>) Serializer.DeserializeFromBytes(data[0]);
            ArrayList<ISSMeasurement> Measurements = (ArrayList<ISSMeasurement>) Serializer.DeserializeFromBytes(data[1]);
            ArrayList<ISSRPEAnswers> RPEAnswers = (ArrayList<ISSRPEAnswers>) Serializer.DeserializeFromBytes(data[2]);

            // Okay this looks f'kin ugly, but there's no other way due to erasure of ArrayLists.
            // It's bs but there's no way around.
            ContentResolver resolver = MainActivity.getContext().getContentResolver();
            for (ISSRPEAnswers row: RPEAnswers) {

                ContentValues values = new ContentValues();
                values.put(ISSContentProvider.MEASUREMENT_ID,
                        row.Measurement_ID);
                values.put(ISSContentProvider.RPE_ANSWERS,
                        ISSDictionary.MapToByteArray(row.Answers));
                resolver.insert(ISSContentProvider.RPE_CONTENT_URI, values);
            }
            for (ISSRecordData row: ISSRecords) {
                Log.d("measurement id", String.valueOf(row.measurementID));
                ContentValues values = new ContentValues();
                values.put(ISSContentProvider.SENT, false);
                values.put(ISSContentProvider.USERID,
                        getUserID());
                values.put(ISSContentProvider.MEASUREMENT,
                        row.MeasurementType);
                values.put(ISSContentProvider.MEASUREMENT_ID,
                        row.measurementID);
                values.put(ISSContentProvider.DATE, row.Date);
                values.put(ISSContentProvider.TIMESTAMP, row.Timestamp);
                values.put(ISSContentProvider.EXTRA, row.ExtraData);
                values.put(ISSContentProvider.VALUE1, row.Value1);
                values.put(ISSContentProvider.VALUE2, row.Value2);
                values.put(ISSContentProvider.VALUE3, row.Value3);
                values.put(ISSContentProvider.SENT, "false");
                Log.d("values", values.toString());
                resolver.insert(ISSContentProvider.RECORDS_CONTENT_URI, values);
            }
            for (ISSMeasurement row: Measurements) {
                Log.d("tries to insert", String.valueOf(row._ID));
                ContentValues values = new ContentValues();
                values.put(ISSContentProvider._ID, row._ID);
                values.put(ISSContentProvider.TIMESTAMP,
                        row.timestamp);
                values.put(ISSContentProvider.TYPE,
                        row.type);
                resolver.insert(ISSContentProvider.MEASUREMENT_CONTENT_URI, values);
            }
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
        Log.d("Now clearing", "Data on the watch");

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
    // I query only for the values of the last 30 days, but that's easily adjustable.
    public void ShareDataWithServer_CSV() {
        ArrayList<String> dateList = createDateList();

        for (String date : dateList) {

            // get all data

            ArrayList<ISSRecordData> alldata = new ArrayList<>();
            Uri CONTENT_URI = ISSContentProvider.RECORDS_CONTENT_URI;

            String mSelectionClause = ISSContentProvider.DATE + " = ? "+"AND " + ISSContentProvider.SENT + " = 'false'";
            String[] mSelectionArgs = {date};
            String[] mProjection =
                    {
                            ISSContentProvider._ID,
                            ISSContentProvider.USERID,
                            ISSContentProvider.MEASUREMENT,
                            ISSContentProvider.DATE,
                            ISSContentProvider.TIMESTAMP,
                            ISSContentProvider.EXTRA,
                            ISSContentProvider.VALUE1,
                            ISSContentProvider.VALUE2,
                            ISSContentProvider.VALUE3,
                            ISSContentProvider.MEASUREMENT_ID
                    };
            String mSortOrder = ISSContentProvider.TIMESTAMP + " DESC";

            // Does a query against the table and returns a Cursor object
            Cursor mCursor = MainActivity.getContext().getContentResolver().query(
                    CONTENT_URI,                       // The content URI of the database table
                    mProjection,                       // The columns to return for each row
                    mSelectionClause,                  // Either null, or the word the user entered
                    mSelectionArgs,                    // Either empty, or the string the user entered
                    mSortOrder);                       // The sort order for the returned rows

            // Some providers return null if an error occurs, others throw an exception
            if (null == mCursor) {
                // If the Cursor is empty, the provider found no matches
            } else if (mCursor.getCount() < 1) {
                // If the Cursor is empty, the provider found no matches
            } else {
                while (mCursor.moveToNext()) {
                    ISSRecordData record = ISSDictionary.CursorToISSRecordData(mCursor);
                    Log.d("record", record.toString());
                    alldata.add(record);
                }
            }

            // sort data in chronological order?

            // separate channels, convert to string, upload to server

            if (alldata.size() == 0)
                continue;

            // determine all measurement types
            HashMap<Integer, ArrayList<ISSRecordData>> map = new HashMap<>();

            for (ISSRecordData record : alldata) {
                if (!map.containsKey(record.MeasurementType))
                    map.put(record.MeasurementType, new ArrayList<ISSRecordData>());

                map.get(record.MeasurementType).add(record);

            }

            // upload data separately for each measurement type

            for (Integer measurementType : map.keySet()) {
                String uploadingName = date + "-" + measurementType + ".csv";

                ArrayList<ISSRecordData> measurementData = map.get(measurementType);

                StringBuilder stringBuilder = CSVManager.RecordsToCSV(measurementData);
                String contents = stringBuilder.toString();

                UploadingManager.UploadUserFileToServer(contents.getBytes(), uploadingName, uploadUrl, UserID);
            }

        }

        updateAllRecords();

        // finally, upload sleep data
        UploadingManager.UploadUserFileToServer( Serializer.FileToBytes(DataStorageManager.sleepData), "sleep-export.csv", uploadUrl, UserID);
        OutputEvent("Sent data files to server");

    }

    // uploading json to server
    public static String send_record_as_json(ISSRecordData tosend) {
        String uri = "http://web01.iss.uni-saarland.de:443";
        HttpURLConnection urlConnection;
        String url;

        JSONObject json = new JSONObject();
        try {
            json.put("Id", tosend.UserID);
            json.put("type", tosend.MeasurementType);
            json.put("date", tosend.Timestamp);
            json.put("tag", tosend.ExtraData);
            json.put("val0", tosend.Value1);
            json.put("val1", tosend.Value2);
            json.put("val2", tosend.Value3);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String data = json.toString();

        String result = null;
        try {
            //Connect
            urlConnection = (HttpURLConnection) ((new URL(uri).openConnection()));
            //urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setRequestMethod("POST");
            urlConnection.connect();

            //Write
            OutputStream outputStream = urlConnection.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
            writer.write(data);
            writer.close();
            outputStream.close();

            //Read
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "UTF-8"));

            String line = null;
            StringBuilder sb = new StringBuilder();

            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }

            bufferedReader.close();
            result = sb.toString();

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }


    // A method that collects all files of the last week (see GetAllFilestoUpload),
    // converts them to an ISSRecordData list, seperates them by measurement type
    // and sends them to the server using the UploadingManager class.
    // I query only for the values of the last 30 days, but that's easily adjustable.
    public void ShareDataWithServer() {

        /*ISSRecordData d = new ISSRecordData(1,1, "h", "e", "llo", 1.0f, 1.0f, 1.0f, 1);
        send_record_as_json(d);*/

        ArrayList<String> dateList = createDateList();

        for (String date : dateList) {

            // get all data

            ArrayList<ISSRecordData> alldata = new ArrayList<>();
            Uri CONTENT_URI = ISSContentProvider.RECORDS_CONTENT_URI;

            String mSelectionClause = ISSContentProvider.DATE + " = ? "+"AND " + ISSContentProvider.SENT + " = 'false'";
            String[] mSelectionArgs = {date};
            String[] mProjection =
                    {
                            ISSContentProvider._ID,
                            ISSContentProvider.USERID,
                            ISSContentProvider.MEASUREMENT,
                            ISSContentProvider.DATE,
                            ISSContentProvider.TIMESTAMP,
                            ISSContentProvider.EXTRA,
                            ISSContentProvider.VALUE1,
                            ISSContentProvider.VALUE2,
                            ISSContentProvider.VALUE3,
                            ISSContentProvider.MEASUREMENT_ID
                    };
            String mSortOrder = ISSContentProvider.TIMESTAMP + " DESC";

            // Does a query against the table and returns a Cursor object
            Cursor mCursor = MainActivity.getContext().getContentResolver().query(
                    CONTENT_URI,                       // The content URI of the database table
                    mProjection,                       // The columns to return for each row
                    mSelectionClause,                  // Either null, or the word the user entered
                    mSelectionArgs,                    // Either empty, or the string the user entered
                    mSortOrder);                       // The sort order for the returned rows

            // Some providers return null if an error occurs, others throw an exception
            if (null == mCursor) {
                // If the Cursor is empty, the provider found no matches
            } else if (mCursor.getCount() < 1) {
                // If the Cursor is empty, the provider found no matches
            } else {
                while (mCursor.moveToNext()) {
                    ISSRecordData record = ISSDictionary.CursorToISSRecordData(mCursor);
                    Log.d("record", record.toString());
                    alldata.add(record);
                }
            }

            // sort data in chronological order?

            // separate channels, convert to string, upload to server

            if (alldata.size() == 0)
                continue;

            // determine all measurement types
            HashMap<Integer, ArrayList<ISSRecordData>> map = new HashMap<>();

            for (ISSRecordData record : alldata) {
                if (!map.containsKey(record.MeasurementType))
                    map.put(record.MeasurementType, new ArrayList<ISSRecordData>());

                map.get(record.MeasurementType).add(record);

            }

            // upload data separately for each measurement type

            for (Integer measurementType : map.keySet()) {
                String uploadingName = date + "-" + measurementType + ".csv";

                ArrayList<ISSRecordData> measurementData = map.get(measurementType);

                for(ISSRecordData record: measurementData){
                    send_record_as_json(record);
                }
            }

        }

        updateAllRecords();

        // finally, upload sleep data

        ArrayList<ISSRecordData> sleepData2 = CSVManager.ReadSleepDataISSREC();

        if(sleepData2 != null){
            for(ISSRecordData record: sleepData2){
                send_record_as_json(record);
            }
        }

        //UploadingManager.UploadUserFileToServer( Serializer.FileToBytes(DataStorageManager.sleepData), "sleep-export.csv", uploadUrl, UserID);
        OutputEvent("Sent data files to server");

    }

    private void updateAllRecords() {
        ContentValues mUpdateValues = new ContentValues();
        String mSelectionClause = ISSContentProvider.SENT +  "= 'false'";
        String[] mSelectionArgs = {};
        int mRowsUpdated = 0;
        mUpdateValues.put(ISSContentProvider.SENT, "'true'");

        mRowsUpdated = getContentResolver().update(
                ISSContentProvider.RECORDS_CONTENT_URI,   // the user dictionary content URI
                mUpdateValues,                       // the columns to update
                mSelectionClause,                    // the column to select on
                mSelectionArgs                      // the value to compare to
        );
    }

    private ArrayList<String> createDateList() {
        ArrayList<String> dateList = new ArrayList<String>();
        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < 30; i++){
            Date date = cal.getTime();
            cal.add(Calendar.DATE,-1);
            String dateAsString = ISSDictionary.dateToDayString(date);
            dateList.add(dateAsString);
        }
        return dateList;
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
