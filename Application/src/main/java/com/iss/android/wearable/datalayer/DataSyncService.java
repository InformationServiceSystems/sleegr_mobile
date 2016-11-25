package com.iss.android.wearable.datalayer;

import android.app.Activity;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

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
import com.iss.android.wearable.datalayer.utils.CredentialsManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class DataSyncService extends Service implements DataApi.DataListener,
        MessageApi.MessageListener, NodeApi.NodeListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "MainActivity";
    public static DataSyncService itself = null;
    public static String Message = "";
    public static String NEW_MESSAGE_AVAILABLE = "log the output";
    public boolean serverSync = false;
    public String UserID = "userID";
    String uploadUrl = "www.web01.iss.uni-saarland.de/post_json";
    SyncAlarm alarm = new SyncAlarm();
    private GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError = false;

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

    private static void LOGD(final String tag, String message) {
        if (Log.isLoggable(tag, Log.DEBUG)) {
            Log.d(tag, message);
        }
    }

    @Override
    public void onCreate() {

        super.onCreate();
        Handler mHandler = new Handler();

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

    // a method called after the service is started, that is responsible for the service not shutting down.
    // It also tries to get the UserID and welcomes the user.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        try {

            alarm.SetAlarm(this);

            //DataStorageManager.InitializeTriathlonFolder();

        } catch (Exception e) {

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

    // A method that is triggered if sensor data has changed and then saves the data from the sensor (I guess?)
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d("Finds out that", "data has changed");

        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED && event.getDataItem().getUri().getPath().equals("/sensorData")) {
                try {
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                    final Asset asset = dataMapItem.getDataMap().getAsset("sensorData");

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("Calls", "Save data from Asset");
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
            Log.d("Received", Arrays.toString(dataAsByteArray));
            byte[][] data = (byte[][]) Serializer.DeserializeFromBytes(dataAsByteArray);
            ArrayList<ISSRecordData> ISSRecords = (ArrayList<ISSRecordData>) Serializer.DeserializeFromBytes(data[0]);
            ArrayList<ISSMeasurement> Measurements = (ArrayList<ISSMeasurement>) Serializer.DeserializeFromBytes(data[1]);
            ArrayList<ISSRPEAnswers> RPEAnswers = (ArrayList<ISSRPEAnswers>) Serializer.DeserializeFromBytes(data[2]);

            // Okay this looks f'kin ugly, but there's no other way due to erasure of ArrayLists.
            // It's bs but there's no way around.
            for (ISSMeasurement row : Measurements) {
                Log.d("Trying to insert", "Measurement " + row._ID);
                DataStorageManager.insertISSMeasurement(row);
            }
            for (ISSRPEAnswers row : RPEAnswers) {
                Log.d("Trying to insert", "RPEAnswers");
                DataStorageManager.insertISSRPEAnswer(row);
            }
            for (ISSRecordData row : ISSRecords) {
                Log.d("Trying to insert", "Records");
                DataStorageManager.insertISSRecordData(row);
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

        OutputEvent("Received a measurement");
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

    // uploading json to server
    public String send_record_as_json(JSONObject jsonForServer, ArrayList<Integer> arrayOfMeasurementIDs) {
        String uri = getString(R.string.testserver_json);//"http://web01.iss.uni-saarland.de/post_json";
        Boolean server_transac_successful = false;

        String data = jsonForServer.toString();

        String result = null;
        try {

            URL object = new URL(uri);

            String header = "bearer ";
            String Token = CredentialsManager.getCredentials(MainActivity.getContext()).getIdToken();
            header += Token;

            HttpURLConnection con = (HttpURLConnection) object.openConnection();
            con.setDoOutput(true);
            con.setDoInput(true);
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Accept", "application/json");
            con.setRequestProperty("Authorization", header);
            con.setRequestMethod("POST");

            OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
            wr.write(data);
            wr.flush();

            StringBuilder sb = new StringBuilder();
            int HttpResult = con.getResponseCode();
            if (HttpResult == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(con.getInputStream(), "utf-8"));
                String line = null;
                while ((line = br.readLine()) != null) {
                    sb.append(line + "\n");
                }
                br.close();
                System.out.println("" + sb.toString());
                Log.d("message", sb.toString());
                server_transac_successful = sb.toString().contains("{\"status\": \"success\"}");
            } else {
                System.out.println(con.getResponseMessage());
                server_transac_successful = false;
            }

        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }

        if (server_transac_successful) {
            updateRecords(arrayOfMeasurementIDs);
            showToast(getApplicationContext(), "Sync successful", Toast.LENGTH_SHORT);
        } else {
            showToast(getApplicationContext(), "Sync failed. Please wait and try again later.", Toast.LENGTH_SHORT);
        }
        return result;
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

    public void ShareDataWithServer() {

        JSONObject jsonForServer = new JSONObject();
        JSONArray arrayOfFhirObservations = new JSONArray();
        ArrayList<Integer> arrayOfMeasurementIDs = new ArrayList<>();

        Uri CONTENT_URI = ISSContentProvider.MEASUREMENT_CONTENT_URI;

        String mSelectionClause = ISSContentProvider.SENT + " = 'false'";
        String[] mSelectionArgs = {};
        String[] mProjection =
                {
                        ISSContentProvider._ID,
                        ISSContentProvider.TYPE,
                        ISSContentProvider.TIMESTAMP
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
            mCursor.close();
        } else {
            while (mCursor.moveToNext()) {
                Log.d("ID", String.valueOf(mCursor.getInt(0)));
                arrayOfMeasurementIDs.add(mCursor.getInt(0));
                Log.d("Timestamp", mCursor.getString(2));
                ArrayList<ISSRecordData> records = queryForRecordsOfMeasurement(mCursor);
                // Here, I ask the server for the codes of the device that I user
                String devices;
                try {
                    devices = askForDeviceNames(records);
                } catch (ProtocolException e) {
                    e.printStackTrace();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                JSONObject fhirObservation = FhirFactory.constructFhirObservation(mCursor, records);
                arrayOfFhirObservations.put(fhirObservation);
            }
            mCursor.close();
        }

        if (arrayOfMeasurementIDs.size() > 0) {
            // This if clause guarantees, that if there are no new records, transmitting to the server will stop.

            try {
                jsonForServer.put("arrayOfFhirObservations", arrayOfFhirObservations);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            send_record_as_json(jsonForServer, arrayOfMeasurementIDs);
            try {
                Log.d("Final JSON", jsonForServer.toString(2));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        // TODO: Upload Sleep Data

        /*ArrayList<ISSRecordData> sleepData2 = CSVManager.ReadSleepDataISSREC();

        if(sleepData2 != null){
            for(ISSRecordData record: sleepData2){
                send_record_as_json(record);
            }
        }*/
    }

    private String askForDeviceNames(ArrayList<ISSRecordData> records) throws IOException {
        Set<String> devices = new HashSet<>();
        for (ISSRecordData record: records) {
            devices.add(record.Sensor);
        }

        JSONArray arrayOfDeviceNames = new JSONArray();
        for (String device: devices) {
            arrayOfDeviceNames.put(device);
        }

        String uri = getString(R.string.testserver_json_get_device);
        URL object = new URL(uri);

        String header = "bearer ";
        String Token = CredentialsManager.getCredentials(MainActivity.getContext()).getIdToken();
        header += Token;

        HttpURLConnection con = (HttpURLConnection) object.openConnection();
        con.setDoOutput(true);
        con.setDoInput(true);
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Accept", "application/json");
        con.setRequestProperty("Authorization", header);
        con.setRequestMethod("POST");
        OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
        wr.write(arrayOfDeviceNames.toString());
        wr.flush();

        StringBuilder sb = new StringBuilder();
        int HttpResult = con.getResponseCode();
        if (HttpResult == HttpURLConnection.HTTP_OK) {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), "utf-8"));
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line + "\n");
            }
            br.close();
            System.out.println("" + sb.toString());
            Log.d("message", sb.toString());
            Log.d("Server response", sb.toString());
        } else {
            System.out.println(con.getResponseMessage());
            Log.d("Server response", sb.toString());
        }
        return null;
    }

    private ArrayList<ISSRecordData> queryForRecordsOfMeasurement(Cursor mCursor) {
        ArrayList<ISSRecordData> records = new ArrayList<>();
        Uri CONTENT_URI = ISSContentProvider.RECORDS_CONTENT_URI;

        String mSelectionClause = ISSContentProvider.MEASUREMENT_ID + " = " + mCursor.getInt(0);
        String[] mSelectionArgs = {};
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
                        ISSContentProvider.SENSOR,
                        ISSContentProvider.MEASUREMENT_ID
                };
        String mSortOrder = ISSContentProvider.TIMESTAMP + " DESC";

        // Does a query against the table and returns a Cursor object
        Cursor innerCursor = MainActivity.getContext().getContentResolver().query(
                CONTENT_URI,                       // The content URI of the database table
                mProjection,                       // The columns to return for each row
                mSelectionClause,                  // Either null, or the word the user entered
                mSelectionArgs,                    // Either empty, or the string the user entered
                mSortOrder);                       // The sort order for the returned rows

        // Some providers return null if an error occurs, others throw an exception
        if (null == innerCursor) {
            // If the Cursor is empty, the provider found no matches
        } else if (innerCursor.getCount() < 1) {
            innerCursor.close();
            // If the Cursor is empty, the provider found no matches
        } else {
            while (innerCursor.moveToNext()) {
                records.add(ISSDictionary.CursorToISSRecordData(innerCursor));
            }
            innerCursor.close();
        }
        return records;
    }

    private void updateRecords(ArrayList<Integer> arrayOfMeasurementIDs) {
        ContentValues mUpdateValues = new ContentValues();
        //new DataSyncService().OutputEvent("Sent data files to server");

        int mRowsUpdated;
        StringBuilder stringBuilder = new StringBuilder();
        String or = "";
        for (Integer i : arrayOfMeasurementIDs) {
            stringBuilder.append(or).append(ISSContentProvider._ID).append(" = ").append(i);
            or = " OR ";
        }
        String mSelectionClause = stringBuilder.toString();

        mUpdateValues.put(ISSContentProvider.SENT, "TRUE");

        mRowsUpdated = MainActivity.getContext().getContentResolver().update(
                ISSContentProvider.MEASUREMENT_CONTENT_URI,   // the user dictionary content URI
                mUpdateValues,                       // the columns to update
                mSelectionClause,                    // the column to select on
                null                     // the value to compare to
        );
        Log.d("Updated", mRowsUpdated + " Values");

        // After we've updated the newest measurements that we have transferred, start anew to check if there are some left
        ShareDataWithServer();
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
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        if (mResolvingError) {
            // Already attempting to resolve an error.
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


}
