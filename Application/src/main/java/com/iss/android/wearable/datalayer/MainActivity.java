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

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

import static com.iss.android.wearable.datalayer.DateTimeManager.getDateFromToday;

/**
 * Receives its own events using a listener API designed for foreground activities. Updates a data
 * item every second while it is open. Also allows user to take a photo and send that as an asset
 * to the paired wearable.
 */
public class MainActivity extends Activity {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int NUM_ITEMS = 30;
    private static final String TAG = "MainActivity";
    public static android.content.Context itself;
    ViewPager mPager;
    int mCurrentTabPosition = 30;
    PendingIntent pendingInt = null;
    private GoogleApiClient mGoogleApiClient;
    private Button watchSyncButton;
    private ListView mDataItemList;
    private DataItemAdapter mDataItemListAdapter;
    private Handler mHandler;
    private Calendar date = new GregorianCalendar();
    private DataUpdateReceiver dataUpdateReceiver;

    // A method which returns a double value as a formatted String in the form d.dd
    static String formatDouble(Double value) {
        if (value == null)
            return "not measured";

        NumberFormat formatter = new DecimalFormat("#0.00");
        return formatter.format(value);
    }

    /**
     * As simple wrapper around Log.d
     */
    private static void LOGD(final String tag, String message) {
        if (Log.isLoggable(tag, Log.DEBUG)) {
            Log.d(tag, message);
        }
    }

    public static Context getContext() {
        return itself;
    }

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        mHandler = new Handler();
        itself = this;
        setContentView(R.layout.main_activity);

        if (DataSyncService.itself == null) {
            Intent intent = new Intent(this, DataSyncService.class);
            startService(intent);
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        1);
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    1);
        }

        // This resets the app every 3 hours. This fixes the date issue.
        // I don't know how to dynamically do this, but this shouldn't be too bad.
        TimerTask action = new TimerTask() {
            public void run() {
                Intent intent = getIntent();
                finish();
                startActivity(intent);
            }
        };

        Timer caretaker = new Timer();
        caretaker.schedule(action, 3*60*60000);

        pendingInt = PendingIntent.getActivity(this, 0, new Intent(getIntent()), getIntent().getFlags());

        // This is a method to fill in the database to test the IntelXDK app.

        /*AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                Databasetestclass.fillWithData();
            }
        });*/


        // start handler which starts pending-intent after Application-Crash
        // That stuff may be cool for end users, but for developers it's nasty
        // Iaroslav: sorry, I uncomment sometimes this (and forget to comment it back) to check what exception crashed the app.
       /*  Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
           @Override
           public void uncaughtException(Thread paramThread, Throwable paramThrowable) {

               AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
               mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 2000, pendingInt);
               System.exit(2);

           }
       }); */

    }

    // A method which determines the current position of the ViewPager.
    // Depending on the position the button to move left or right is made invisible.
    private void checkPositon(int currentItem) {
        if (currentItem >= 29) {
            ImageButton button = (ImageButton) findViewById(R.id.right);
            button.setVisibility(View.INVISIBLE);
        } else if (currentItem == 0) {
            ImageButton button = (ImageButton) findViewById(R.id.left);
            button.setVisibility(View.INVISIBLE);
        } else {
            ImageButton lbutton = (ImageButton) findViewById(R.id.left);
            ImageButton rbutton = (ImageButton) findViewById(R.id.right);
            lbutton.setVisibility(View.VISIBLE);
            rbutton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.button2:
                onServerSync();
                return true;
            case R.id.button3:
                onWatchSync();
                return true;
            case R.id.registerUserMenu:
                onRegisterUser();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // A method handling the click on the Server Sync button.
    public void onServerSync(View view) {
        RecomputeSynchronize();
    }

    // A method handling the click on the server sync button.
    public void onServerSync() {
        RecomputeSynchronize();
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

    // puts a String to the system message textview
    public void OutputEvent(final String content) {
        final String cont = content;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                TextView tv = (TextView) findViewById(R.id.systemMessage);
                tv.setText(content);
            }
        });

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

    // A method starting the RegisterUserActivity if no view is supplied.
    public void onRegisterUser() {

        final Intent registerUser = new Intent(this, RegisterUserActivity.class);
        startActivity(registerUser);

    }

    /**
     * Sets up UI components and their callback handlers.
     */
    private void setupViews() {
        mDataItemList = (ListView) findViewById(R.id.data_item_list);
        //mStartActivityBtn = findViewById(R.id.start_wearable_activity);
    }

    // A method that starts the quest to retrieve data from the watch if the button is clicked
    public void onWatchSync() {

        if (DataSyncService.itself != null) {
            DataSyncService.itself.RequestDataFromWatch();
        }

    }

    // A method that predicts the recovery based on history data
    public double predictRecovery(ArrayList<Double> pastX, ArrayList<Double> pastY, Double futureX) {


        double result = -1;

        // collect values of past records into bins

        double[] bins = new double[11];
        double[] counts = new double[11];

        for (int i = 0; i < 11; i++) {
            bins[i] = 0;
            counts[i] = 0;
        }

        for (int i = 0; i < pastY.size(); i++) {

            if (pastX.get(i) < 0) {
                continue;
            }

            bins[((int) Math.round(pastX.get(i)))] += pastY.get(i);
            counts[((int) Math.round(pastX.get(i)))] += 1;
        }

        for (int i = 0; i < 11; i++) {
            if (counts[i] == 0) {
                continue;
            }
            bins[i] = bins[i] / counts[i];
        }

        smoothenBins(bins, counts);

        result = bins[((int) Math.round(futureX))];

        return result;

    }

    // A method that smoothens a double array to remove noise
    public void smoothenBins(double[] bins, double[] counts) {

        double cv = 0;

        for (int i = 0; i < bins.length; i++) {
            if (counts[i] > 0) {
                cv = bins[i];
            }
            bins[i] = cv;
        }

        for (int i = bins.length - 1; i >= 0; i--) {
            if (counts[i] > 0) {
                cv = bins[i];
            }
            bins[i] = cv;
        }

    }

    // A method that computes a date given a certain date and an offset in days
    public Date offsetDate(Date input, int offset) {

        Calendar clnd = Calendar.getInstance();
        clnd.setTime(input);
        clnd.add(Calendar.DAY_OF_MONTH, -offset);
        return clnd.getTime();

    }

    public void onSyncClick(View view) {
        onWatchSync();
    }

    // Sends data of the last 30 days to the server
    private void RecomputeSynchronize() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    // recompute the params for last 60 days

                    DataSyncService.itself.ShareDataWithServer();

                } catch (Exception ex) {
                    OutputEvent(ex.toString());
                }
            }
        }).start();

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

    // this is used to communicate with Service
    private class DataUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(DataSyncService.NEW_MESSAGE_AVAILABLE)) {
                OutputEvent(intent.getExtras().getString("message"));
            }
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

}
