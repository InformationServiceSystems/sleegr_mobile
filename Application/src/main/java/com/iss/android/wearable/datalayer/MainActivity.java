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
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
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

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

/**
 * Receives its own events using a listener API designed for foreground activities. Updates a data
 * item every second while it is open. Also allows user to take a photo and send that as an asset
 * to the paired wearable.
 */
public class MainActivity extends Activity  {

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

        // Stores DataItems received by the local broadcaster or from the paired watch.
        mDataItemListAdapter = new DataItemAdapter(this, android.R.layout.simple_list_item_1);
        mDataItemList.setAdapter(mDataItemListAdapter);

        if (DataSyncService.itself == null){
            Intent intent = new Intent(this, DataSyncService.class);
            startService(intent);
        }


        pendingInt = PendingIntent.getActivity(this, 0, new Intent(getIntent()), getIntent().getFlags());
        // start handler which starts pending-intent after Application-Crash
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread paramThread, Throwable paramThrowable) {

                AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 2000, pendingInt);
                System.exit(2);

            }
        });


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



    public void OutputEvent(final String content){

        final String cont = content;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mDataItemListAdapter.add( new Event( "Event", content ));
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

        public String GetTimeNow(){

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

    public void onSyncClick(View view) {

        //SendToServer(watchData);
        //RequestDataFromWatch();

        if (DataSyncService.itself != null){
            DataSyncService.itself.RequestDataFromWatch();
        }

    }


    public void onSendToServerClick(View view) {

        //SendToServer(watchData);
        //RequestDataFromWatch();

        if (DataSyncService.itself != null){
            DataSyncService.itself.ShareDataWithServer();
        }

    }

    public void onRegisterUser(View view) {

        final Intent bluetoothSelector = new Intent(this, RegisterUserActivity.class);
        startActivity(bluetoothSelector);

    }

    /**
     * Sets up UI components and their callback handlers.
     */
    private void setupViews() {
        watchSyncButton = (Button) findViewById(R.id.syncWithWatchButton);
        //mThumbView = (ImageView) findViewById(R.id.imageView);
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

}
