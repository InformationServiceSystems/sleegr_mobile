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

package com.iss.android.wearable.datalayer;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Receives its own events using a listener API designed for foreground activities. Updates a data
 * item every second while it is open. Also allows user to take a photo and send that as an asset
 * to the paired wearable.
 */
public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    private GoogleApiClient mGoogleApiClient;

    private Button watchSyncButton;
    private ListView mDataItemList;
    private DataItemAdapter mDataItemListAdapter;
    private Handler mHandler;


    static final int REQUEST_IMAGE_CAPTURE = 1;


    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        mHandler = new Handler();
        setContentView(R.layout.main_activity);
        setupViews();
        //Necessary, terminating the choose register service activity
        //ChooseRegisterServiceActivity.instance.finish();

        // Stores DataItems received by the local broadcaster or from the paired watch.
        mDataItemListAdapter = new DataItemAdapter(this, android.R.layout.simple_list_item_1);
        mDataItemList.setAdapter(mDataItemListAdapter);

        Calendar date = new GregorianCalendar();
        SportsSession.retrieveCsvs(date);

        if (DataSyncService.itself == null) {
            Intent intent = new Intent(this, DataSyncService.class);
            startService(intent);
        }

/*
        pendingInt = PendingIntent.getActivity(this, 0, new Intent(getIntent()), getIntent().getFlags());
        // start handler which starts pending-intent after Application-Crash
        // That stuff may be cool for end users, but for developers it's nasty
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread paramThread, Throwable paramThrowable) {

                AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 2000, pendingInt);
                System.exit(2);

            }
        });*/

    }


    PendingIntent pendingInt = null;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    private DataUpdateReceiver dataUpdateReceiver;

    // this is used to communicate with Service
    private class DataUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(DataSyncService.NEW_MESSAGE_AVAILABLE)) {
                OutputEvent(intent.getExtras().getString("message"));
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (dataUpdateReceiver == null) dataUpdateReceiver = new DataUpdateReceiver();
        IntentFilter intentFilter = new IntentFilter(DataSyncService.NEW_MESSAGE_AVAILABLE);
        registerReceiver(dataUpdateReceiver, intentFilter);

    }

    @Override
    public void onPause() {
        super.onPause();
        if (dataUpdateReceiver != null) unregisterReceiver(dataUpdateReceiver);

    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    public void OutputEvent(final String content) {

        final String cont = content;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mDataItemListAdapter.add(new Event("Event", content));
            }
        });

    }

    /**
     * A View Adapter for presenting the Event objects in a list
     */
    private static class DataItemAdapter extends ArrayAdapter<Event> {

        private final Context mContext;

        public DataItemAdapter(Context context, int unusedResource) {
            super(context, unusedResource);
            mContext = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(android.R.layout.two_line_list_item, null);
                convertView.setTag(holder);
                holder.text1 = (TextView) convertView.findViewById(android.R.id.text1);
                holder.text2 = (TextView) convertView.findViewById(android.R.id.text2);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            Event event = getItem(position);
            holder.text1.setText(event.title);
            holder.text2.setText(event.text);
            return convertView;
        }

        private class ViewHolder {

            TextView text1;
            TextView text2;
        }
    }

    private class Event {

        String title;
        String text;

        public Event(String title, String text) {
            this.title = GetTimeNow();
            this.text = text;
        }

        public String GetTimeNow() {

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HH:mm:ss");
            String currentDateandTime = sdf.format(new Date());
            return currentDateandTime;

        }
    }

    private Collection<String> getNodes() {
        HashSet<String> results = new HashSet<>();
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();

        for (Node node : nodes.getNodes()) {
            results.add(node.getId());
        }

        return results;
    }

    public void onRegisterUser(View view) {

        final Intent bluetoothSelector = new Intent(this, RegisterUserActivity.class);
        startActivity(bluetoothSelector);

    }

    /**
     * Sets up UI components and their callback handlers.
     */
    private void setupViews() {
        mDataItemList = (ListView) findViewById(R.id.data_item_list);
        //mStartActivityBtn = findViewById(R.id.start_wearable_activity);
    }

    /**
     * As simple wrapper around Log.d
     */
    private static void LOGD(final String tag, String message) {
        if (Log.isLoggable(tag, Log.DEBUG)) {
            Log.d(tag, message);
        }
    }

    public void onServerSync(View view) {

        if (DataSyncService.itself != null) {
            DataSyncService.itself.ShareDataWithServer();
        }

    }

    public void onWatchSync(View view) {

        if (DataSyncService.itself != null) {
            DataSyncService.itself.RequestDataFromWatch();
        }

    }


    public TimeSeries randomRPEReq(int past, int future){

        TimeSeries requirements = new TimeSeries("RPE schedule");

        for (int i=0; i < past; i++){
            long round = Math.round(Math.random() * 10);
            Date date = DataProcessingManager.getDateFromToday(i);
            requirements.AddFirstValue(date, round);
        }

        for (int i=0; i < future; i++){
            long round = Math.round(Math.random() * 10);
            Date date = DataProcessingManager.getDateFromToday(-i-1);
            requirements.AddValue(date, round);
        }

        return requirements;

    }

    public double predictRecovery(ArrayList<Double> pastX, ArrayList<Double> pastY, Double futureX){


        double result = -1;

        // collect values of past records into bins

        double [] bins = new double[11];
        double [] counts = new double[11];

        for (int i = 0; i < 11; i++){
            bins[i] = 0;
            counts[i] = 0;
        }

        for (int i = 0; i < pastY.size(); i++){

            if (pastX.get(i) <0){
                continue;
            }

            bins[((int) Math.round(pastX.get(i)))] += pastY.get(i);
            counts[((int) Math.round(pastX.get(i)))] += 1;
        }

        for (int i = 0; i < 11; i++){
            if(counts[i] == 0){
                continue;
            }
            bins[i] = bins[i] / counts[i];
        }

        smoothenBins(bins, counts);

        result = bins[((int) Math.round(futureX))];

        return result;

    }

    public void smoothenBins(double [] bins, double [] counts){

        double cv = 0;

        for (int i= 0;i < bins.length; i++){
            if (counts[i] > 0){
                cv = bins[i];
            }
            bins[i] = cv;
        }

        for (int i=  bins.length-1;i >= 0; i--){
            if (counts[i] > 0){
                cv = bins[i];
            }
            bins[i] = cv;
        }

    }

    public Date offsetDate(Date input, int offset){

        Calendar clnd = Calendar.getInstance();
        clnd.setTime(input);
        clnd.add(Calendar.DAY_OF_MONTH, -offset);
        return clnd.getTime();

    }

    public TimeSeries predictTimeSeries(TimeSeries xReq, TimeSeries xActual, TimeSeries yActual, int offset){

        TimeSeries result = new TimeSeries(yActual.name+", pred.");

        for (int i = 0; i < xReq.Values.size()-offset; i++){

            Date date  = xReq.Values.get(i).x;
            Double val  = xReq.Values.get(i).y;

            TimeSeries xActBefore = xActual.beforeDate(date);
            TimeSeries yActBefore = yActual.beforeDate(date);

            if (xActBefore.Values.size() == 0){
                continue;
            }

            HashMap<String, Double> predValues = yActBefore.toDictionary();

            ArrayList<Double> xvalues = new ArrayList<>();
            ArrayList<Double> yvalues = new ArrayList<>();

            // generate training set
            for (int j = 0; j < xActBefore.Values.size(); j++){

                Date locdate = offsetDate( xActBefore.Values.get(j).x, offset);
                double x = xActBefore.Values.get(j).y;

                if (!predValues.containsKey( TimeSeries.formatData(locdate) )){
                    continue;
                }

                double y = predValues.get(TimeSeries.formatData(locdate));

                xvalues.add(x);
                yvalues.add(y);

            }

            double prediction = predictRecovery(xvalues, yvalues, val);
            result.AddValue(offsetDate(date, offset), prediction);
        }

        return result;

    }

    public void onGraphPlot(View view){

        new Thread(new Runnable() {
            @Override
            public void run() {
                try{

                    int days = 21;
                    //String UserID = DataStorageManager.getProperUserID(DataSyncService.itself.UserID);
                    String UserID = DataStorageManager.getProperUserID(DataSyncService.itself.UserID);

                    ArrayList<TimeSeries> allData = null;

                    File tempdata = new File(Environment.getExternalStorageDirectory().toString() , "data.tmp2" );
                    if (!tempdata.exists()) {
                        allData = DataProcessingManager.GetDailyRecoveryParameters(days, UserID);
                        Serializer.SerializeToFile(allData, tempdata);
                    }else {
                        allData = (ArrayList<TimeSeries>) Serializer.DeserializeFromFile(tempdata);
                    }

                    int pred_days = 7;
                    TimeSeries rpeReq = randomRPEReq(days, pred_days);

                    // this class will be used for plotting the data
                    Visualizations visualizations = new Visualizations();

                    // add requirement graph
                    Visualizations.Subplot subplot = visualizations.AddGraph("RPE requirements");
                    subplot.Add(allData.get(0), Color.RED);
                    subplot.Add(rpeReq, Color.GREEN);

                    TimeSeries avgDeviationsRPE = ComputeCompliences(rpeReq, allData.get(0), 3);
                    subplot.Add(avgDeviationsRPE, Color.GRAY);

                    // add prediction graphs
                    for (int i = 1; i < allData.size(); i++){

                        int offset = 0;

                        TimeSeries prediction = predictTimeSeries(rpeReq, allData.get(0), allData.get(i), offset);

                        subplot = visualizations.AddGraph(prediction.name);
                        subplot.Add(allData.get(i), Color.RED);
                        subplot.Add(prediction, Color.BLUE);

                    }

                    Intent i = new Intent(MainActivity.this, IntensityStatisticsActivity.class);
                    i.putExtra("visualizations", Serializer.SerializeToBytes(visualizations));
                    startActivity(i);

                }catch (Exception ex){
                    OutputEvent(ex.toString());
                }
            }
        }).start();



    }

    private TimeSeries ComputeCompliences(TimeSeries requirements, TimeSeries values, int timewindow) {

        TimeSeries result = new TimeSeries(values.name + ", avg. divergence");

        for (int i = 0; i < values.Values.size(); i++){

            Date x = requirements.Values.get(i).x;

            TimeSeries req = requirements.beforeDate(x);
            TimeSeries vals = values.beforeDate(x);

            ArrayList<Double> seq1 = new ArrayList<>();
            ArrayList<Double> seq2 = new ArrayList<>();

            // construct data
            HashMap<String, Double> reqVals = req.toDictionary();

            for (int j = 0; j < vals.Values.size(); j++){

                int last = vals.Values.size() - j - 1;

                Date d = vals.Values.get(last).x;

                if (!reqVals.containsKey( TimeSeries.formatData(d) ))
                    continue;

                Double yp = reqVals.get(TimeSeries.formatData(d));
                Double y = vals.Values.get(last).y;

                seq1.add(0,yp);
                seq2.add(0, y);

                if (seq1.size() >= timewindow){
                    break;
                }

            }

            if (seq1.size() == 0){
                result.AddValue(x, -1);
                continue;
            }

            result.AddValue(x, ComplienceMeasure(seq1, seq2));

        }


        return result;

    }

    private double ComplienceMeasure(ArrayList<Double> seq1, ArrayList<Double> seq2) {

        double result = 0;

        for (int i = 0; i < seq1.size(); i++){
            result += Math.abs( seq1.get(i) - seq2.get(i) ) / seq1.size();
        }

        return result;

    }

    public void onExploreData(View view){

        Intent i = new Intent(MainActivity.this, SelectAvailableData.class);
        startActivity(i);

    }

}
