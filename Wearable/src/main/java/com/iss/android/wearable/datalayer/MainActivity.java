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
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;

import java.util.ArrayList;

/**
 * The main activity with a view pager, containing three pages:<p/>
 * <ul>
 * <li>
 * Page 1: shows a list of DataItems received from the phone application
 * </li>
 * <li>
 * Page 2: shows the photo that is sent from the phone application
 * </li>
 * <li>
 * Page 3: includes two buttons to show the connected phone and watch devices
 * </li>
 * </ul>
 */
public class MainActivity extends Activity  {


    private static final String TAG = "MainActivity";
    private GoogleApiClient mGoogleApiClient;
    private Handler mHandler;


    private ArrayList<String> listItems=new ArrayList<String>();
    private ArrayAdapter<String> adapter;
    private Intent murderousIntent;
    private int warned = 0;

    @Override
    public void onCreate(Bundle b) {

        super.onCreate(b);

        pendingInt = PendingIntent.getActivity(this, 0, new Intent(getIntent()), getIntent().getFlags());
        // Intent that kills the app after a certain amount of time after the app has crashed
        murderousIntent = new Intent(this, SensorsDataService.class);
        // start handler which starts pending-intent after Application Crash
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread paramThread, Throwable paramThrowable) {
                // something went wrong ... this cannot continue like this anymore ...
                // we need to start everything from scratch ...
                AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 10000, pendingInt);
                // under the circumstances I have no choice but to murder the service and the app itself ...
                // put it out of its misery ...
                stopService(murderousIntent);
                android.os.Process.killProcess(android.os.Process.myPid());
                // It will hunt me until the end of my days in my nightmares...
                // My darkest secret, slowly eating on my soul and driving me mad ...
                System.exit(2);

            }
        });

        mHandler = new Handler();
        setContentView(R.layout.main_activity);

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listItems);
        ListView listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(adapter);

        initializeSWBatteryChecker();

        IntentFilter filter = new IntentFilter();
        filter.addAction(SensorsDataService.ACTION_BATTERY_STATUS);
        filter.addAction(SensorsDataService.ACTION_HR);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(br, filter);

    }

    private void initializeSWBatteryChecker() {
        final TextView SWBatteryStatus = (TextView) findViewById(R.id.SWbatteryLabel);
        final Handler h = new Handler();
        final int delay = 20000; //milliseconds

        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        int batteryPct = (int) (level / (float) scale * 100);
        SWBatteryStatus.setText("SW Battery: " + batteryPct + "%");

        // would've used timer, since it runs once initially, but it crashes the app.

        h.postDelayed(new Runnable() {
            public void run() {
                IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                Intent batteryStatus = registerReceiver(null, ifilter);

                int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

                int batteryPct = (int) (level / (float) scale * 100);
                SWBatteryStatus.setText("SW Battery: " + batteryPct + "%");
                h.postDelayed(this, delay);
            }
        }, delay);
    }

    BroadcastReceiver br = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(SensorsDataService.ACTION_BATTERY_STATUS)) {
                final TextView BatteryStatus = (TextView) findViewById(R.id.batteryLabel);
                int status = intent.getIntExtra(SensorsDataService.EXTRA_STATUS, 0);
                BatteryStatus.setText("HR Battery: " + status + "%");
                // Checks if the Battery status is 15% or below and if the User already has been alarmed.
                // If the battery got charged up again, reset the Warning.
                if (status <= 15 && status > 10 && warned != 1) {
                    warned = 1;
                    displayBatteryWarning(warned);
                } else if (status <= 10 && status > 5 && warned != 2) {
                    warned = 2;
                    displayBatteryWarning(warned);
                } else if (status <= 5 && warned != 3) {
                    warned = 3;
                    displayBatteryWarning(warned);
                } else if (status > 15 && warned != 0) {
                    warned = 0;
                }
            }
            if (intent.getAction().equals(SensorsDataService.ACTION_HR)) {
                Log.d("got here", "finally");
                final TextView HeartRate = (TextView) findViewById(R.id.heartRateLabel);
                int result = intent.getIntExtra(SensorsDataService.EXTRA_HR, 0);
                Log.d("printHR: ", String.valueOf(result));
                HeartRate.setText("HR: " + result);
            }
        }
    };

    private void displayBatteryWarning(int warned) {
        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // 2. Chain together various setter methods to set the dialog characteristics
        String warning = "";
        switch (warned) {
            case 1:
                warning = "HRM Battery Level at 15%";
                break;
            case 2:
                warning = "HRM Battery Level at 10%";
                break;
            case 3:
                warning = "HRM Battery Level at 5%";
                break;
        }
        builder.setMessage(warning)
                .setTitle(R.string.battery_warning_title)
                .setCancelable(true)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        // 3. Get the AlertDialog from create()
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    PendingIntent pendingInt = null;

    // this is used to communicate with Service
    private class DataUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(SensorsDataService.NEW_MESSAGE_AVAILABLE)) {
                UpdateButtonText();
                OutputEvent(intent.getExtras().getString("message"));
            }
        }
    }

    private DataUpdateReceiver dataUpdateReceiver;

    @Override
    protected void onResume() {
        super.onResume();

        // restart of the dataservice retrieving data from the sensor

        if (SensorsDataService.itself == null){
            Intent intent = new Intent(this, SensorsDataService.class);
            startService(intent);
        }

        //mGoogleApiClient.connect();
        if (dataUpdateReceiver == null) dataUpdateReceiver = new DataUpdateReceiver();
        IntentFilter intentFilter = new IntentFilter(SensorsDataService.NEW_MESSAGE_AVAILABLE);
        registerReceiver(dataUpdateReceiver, intentFilter);

        UpdateButtonText();

        /*if (SensorsDataService.itself != null){
            SensorsDataService.itself.StopSleepTracking();
        }*/

    }

    @Override
    protected void onPause() {
        super.onPause();
        /*Wearable.DataApi.removeListener(mGoogleApiClient, this);
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        Wearable.NodeApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();*/
        //mSensorManager.unregisterListener(this);
        if (dataUpdateReceiver != null) unregisterReceiver(dataUpdateReceiver);

    }


    float heartbeat = 10;
    int steps = 1000;

    boolean allowHRM = false;

    public void UpdateButtonText(){

        if (SensorsDataService.itself != null) {
            Button btn = (Button) findViewById(R.id.switchTrainingButton);
            String outputString = SensorsDataService.itself.allowHRM ? "Stop training" : "Start training";
            btn.setText(outputString);

            if (SensorsDataService.itself.allowHRM){
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }else{
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }

        }

    }

    public void onClicked(View view) {
        switch (view.getId()) {
            case R.id.switchTrainingButton:


               if (SensorsDataService.itself != null){
                    SensorsDataService.itself.SwitchHRM();
                    UpdateButtonText();
                }

                break;
            case R.id.exitAppButton:

                // first stop the service so that it does not stop the sleep tracking
                Intent intent = new Intent(this, SensorsDataService.class);
                stopService(intent);

                // then launch sleep tracking
                Intent launchSleepIntent = getPackageManager().getLaunchIntentForPackage("com.urbandroid.sleep");
                startActivity(launchSleepIntent);

                // finally, kill the app in order to save the battery
                android.os.Process.killProcess(android.os.Process.myPid());

                /*

                for (int i = 0; i < 10000; i++){
                    ISSRecordData data = new ISSRecordData(1,1, "yyyy.MM.dd_HH:mm:ss", null, 3.1415926535f,0,0);
                    SensorsDataService.itself.alldata.add(data);
                }

                OutputEvent("Created fake data");*/

                break;
            default:

                Log.e(TAG, "Unknown click event registered");
        }

    }

    public void OutputEvent(String content){

        final String cont = content;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                listItems.add(0,cont);
                while (listItems.size() > 64){
                    listItems.remove(listItems.size()-1);
                }
                adapter.notifyDataSetChanged();
            }
        });

    }

}


