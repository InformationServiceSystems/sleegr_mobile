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
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

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

    @Override
    public void onCreate(Bundle b) {

        super.onCreate(b);

        pendingInt = PendingIntent.getActivity(this, 0, new Intent(getIntent()), getIntent().getFlags());
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

        if (SensorsDataService.itself == null){
            Intent intent = new Intent(this, SensorsDataService.class);
            startService(intent);
        }

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listItems);
        ListView listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(adapter);

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
        //mGoogleApiClient.connect();
        if (dataUpdateReceiver == null) dataUpdateReceiver = new DataUpdateReceiver();
        IntentFilter intentFilter = new IntentFilter(SensorsDataService.NEW_MESSAGE_AVAILABLE);
        registerReceiver(dataUpdateReceiver, intentFilter);

        /*final String StopSleepString = "com.urbandroid.sleep.alarmclock.STOP_SLEEP_TRACK";
        Intent StopSleepIntent = new Intent(StopSleepString);
        startActivity(StopSleepIntent);*/
        UpdateButtonText();

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

                Intent launchSleepIntent = getPackageManager().getLaunchIntentForPackage("com.urbandroid.sleep");
                startActivity(launchSleepIntent);

                murderousIntent = new Intent(this, SensorsDataService.class);
                //We need to kill again!
                stopService(murderousIntent);
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(2);

                /*Intent intent = new Intent(this, SensorsDataService.class);
                stopService(intent);
                android.os.Process.killProcess(android.os.Process.myPid());

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


