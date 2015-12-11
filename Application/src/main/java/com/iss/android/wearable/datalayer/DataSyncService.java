package com.iss.android.wearable.datalayer;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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


    File sleepData = new File(Environment.getExternalStorageDirectory().toString() + "/sleep-data/sleep-export.csv");
    File userDataFolder = new File(Environment.getExternalStorageDirectory().toString() , "triathlon" );
    String uploadUrl = "http://46.101.214.58:5001/upload2/";

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

    public File getSensorsFile(){
        return getSensorsFile(0);
    }

    public File getSensorsFile(int dayoffset){

        DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
        Calendar cl = Calendar.getInstance();
        cl.add(Calendar.DAY_OF_YEAR, -dayoffset);
        String currentDateandTime = df.format(cl.getTime());

        String userIDstring = UserID.replace("@", "_at_");

        File sensorsData = new File(userDataFolder,  userIDstring +  "-" + currentDateandTime + ".bin");
        return sensorsData;

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        alarm.SetAlarm(this);

        String android_id = Settings.Secure.getString(this.getContentResolver(),Settings.Secure.ANDROID_ID);
        OutputEvent(android_id);

        Log.d("ISS", "Adroid ID: " + android_id);

        switch (android_id){
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

        boolean result = false;
        if(!userDataFolder.exists()){
            result = userDataFolder.mkdir();
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

    public void StopSleepTracking(){
        try{
            Intent intent = new Intent("com.urbandroid.sleep.alarmclock.STOP_SLEEP_TRACK");
            sendBroadcast(intent);
            ConfirmSleepTrackingStopped();
        }
        catch(Exception ex){
            System.out.println(ex.toString());
        }
    }

    @Override
    public void onMessageReceived(final MessageEvent messageEvent) {


        if(messageEvent.getPath().equals("Stop sleep tracking")){
            StopSleepTracking();
        }
        else{
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

    public void SendHRtoServer(String HR){

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
        }
        catch (Exception ex){

            OutputEvent(ex.toString());

        }

    }


    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {

        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED && event.getDataItem().getUri().getPath().equals("/sensorData")) {
                try {
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                    Asset asset = dataMapItem.getDataMap().getAsset("sensorData");
                    SaveDataFromAsset(asset);

                }catch(Exception ex){
                    ex.printStackTrace();
                }

            }
        }
    }

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
            OutputEvent("Read data from the watch of size " + receivedData.size());
            SaveNewDataToFile(receivedData);
            ClearWatchData();

        } catch (Exception e) {
            e.printStackTrace();
            OutputEvent(e.toString());
        }

    }

    public void OutputEvent(String str) {

        Intent intent = new Intent(this.NEW_MESSAGE_AVAILABLE);
        intent.putExtra("message", str);
        sendBroadcast(intent);

    }


    private void SaveNewDataToFile(ArrayList<ISSRecordData> data) {

        try {

            File sensorsData = getSensorsFile();

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

    // interation with the watch procecdures

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

    public byte[] FileToBytes(File file) {

        int size = (int) file.length();
        byte[] bytes = new byte[size];

        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(bytes, 0, bytes.length);
            buf.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return bytes;
    }

    // Returns String which is a server response
    public void UploadFileToServer(File fileToUpload, String uploadUrl) {

        if (!fileToUpload.exists()){
            return;
        }

        final File file = fileToUpload;
        final String serverurl = uploadUrl;

        /*StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);*/

        OutputEvent("Starting to sync with server ... ");

        new Thread(new Runnable() {
            @Override
            public void run() {

                try {

                    // Static stuff:

                    String attachmentName = "file";
                    String attachmentFileName = file.getName();
                    String crlf = "\r\n";
                    String twoHyphens = "--";
                    String boundary = "*****";

                    //Setup the request:

                    HttpURLConnection httpUrlConnection = null;
                    URL url = new URL(serverurl);
                    httpUrlConnection = (HttpURLConnection) url.openConnection();
                    httpUrlConnection.setUseCaches(false);
                    httpUrlConnection.setDoOutput(true);

                    httpUrlConnection.setRequestMethod("POST");
                    httpUrlConnection.setRequestProperty("Connection", "Keep-Alive");
                    httpUrlConnection.setRequestProperty("Cache-Control", "no-cache");
                    httpUrlConnection.setRequestProperty(
                            "Content-Type", "multipart/form-data;boundary=" + boundary);

                    // Start content wrapper:

                    DataOutputStream request = new DataOutputStream(
                            httpUrlConnection.getOutputStream());

                    request.writeBytes(twoHyphens + boundary + crlf);
                    request.writeBytes("Content-Disposition: form-data; name=\"" +
                            attachmentName + "\";filename=\"" +
                            attachmentFileName + "\"" + crlf);
                    request.writeBytes(crlf);

                    // read all file bytes

                    byte[] filecontents = FileToBytes(file);

                    // end content wrapper

                    request.write(filecontents);

                    request.writeBytes(crlf);
                    request.writeBytes(twoHyphens + boundary +
                            twoHyphens + crlf);

                    // flush the output buffer

                    request.flush();
                    request.close();

                    // Get server response:

                    InputStream responseStream = new
                            BufferedInputStream(httpUrlConnection.getInputStream());

                    BufferedReader responseStreamReader =
                            new BufferedReader(new InputStreamReader(responseStream));

                    String line = "";
                    StringBuilder stringBuilder = new StringBuilder();

                    while ((line = responseStreamReader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    responseStreamReader.close();

                    String response = stringBuilder.toString();

                    OutputEvent("Sent " + filecontents.length + " bytes. Server responded: " + response);

                    // release resources:

                    responseStream.close();
                    httpUrlConnection.disconnect();

                } catch (Exception ex) {
                    OutputEvent("Exception during sync: " + ex.toString());
                }


            }
        }).start();


    }

    // Magic that is supposed to keep the process running on the Android v 4.4 even
    // after the app is swiped away; seems to be a bug of this android version. GJ Google
    // http://stackoverflow.com/questions/20677781/in-android-4-4-swiping-app-out-of-recent-tasks-permanently-kills-application-wi
    public void onTaskRemoved(Intent rootIntent) {
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
    }

    // a simple wrapper around UploadFileToServer
    public void UploadUserFileToServer(File file, String uploadUrl){
        String completeUrl = uploadUrl + UserID;
        UploadFileToServer(file, completeUrl);
    }

    public void ShareDataWithServer() {

        UploadUserFileToServer(sleepData, uploadUrl);

        for (int i = 0; i < 7; i++){
            UploadUserFileToServer(getSensorsFile(i), uploadUrl);
        }

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
