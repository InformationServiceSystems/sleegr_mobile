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

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

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
public class MainActivity extends Activity {

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final int REQUEST_FINE_LOCATION = 1;
    private static final String[] PERMISSIONS_LOCATION = {
            Manifest.permission.ACCESS_FINE_LOCATION};
    private static final String TAG = "MainActivity";
    public static MainActivity itself;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    int current_time = 0;
    PendingIntent pendingInt = null;
    float heartbeat = 10;
    int steps = 1000;
    boolean allowHRM = false;
    private GoogleApiClient mGoogleApiClient;
    private Handler mHandler;
    private ArrayList<String> listItems = new ArrayList<String>();
    private ArrayAdapter<String> adapter;
    private Intent murderousIntent;
    private int warned = 0;
    BroadcastReceiver br = new BroadcastReceiver() {
        // Receives broadcasts sent from other points of the app, like the SensorsDataService
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(SensorsDataService.ACTION_BATTERY_STATUS)) {
                final TextView BatteryStatus = (TextView) findViewById(R.id.batteryLabel);
                int status = intent.getIntExtra(SensorsDataService.EXTRA_STATUS, 0);
                BatteryStatus.setText("HR: " + status + "%");
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
            } else if (intent.getAction().equals(SensorsDataService.ACTION_HR)) {
                // Prints out the heart rate
                final TextView HeartRate = (TextView) findViewById(R.id.heartRateLabel);
                int result = intent.getIntExtra(SensorsDataService.EXTRA_HR, 0);
                // Need to convert the Int to String or else the app crashes. GJ Google.
                HeartRate.setText(Integer.toString(result));
            } else if (intent.getAction().equals(SensorsDataService.UPDATE_GPS_PARAMS)) {
                // Prints out the heart rate
            } else if (intent.getAction().equals(SensorsDataService.ASK_USER_FOR_RPE)) {
                CheckToShowRPE();
            } else if (intent.getAction().equals(SensorsDataService.NEW_MESSAGE_AVAILABLE)) {
                // prints out the Outputevent messages
                final TextView MessageLabel = (TextView) findViewById(R.id.messageLabel);
                String message = intent.getStringExtra("message");
                MessageLabel.setText(message);
            } else if (intent.getAction().equals(SensorsDataService.UPDATE_TIMER_VALUE)) {
                String newtime = String.valueOf(intent.getIntExtra("minutes", 0))
                        + ":" + StringUtils.leftPad(Long.toString(intent.getIntExtra("seconds", 0)), 2, "0");
                TextView timetext = (TextView) findViewById(R.id.timer);
                timetext.setText(newtime);
            }

            showCurrentAppState();

        }
    };
    private DataUpdateReceiver dataUpdateReceiver;

    public static Context getContext() {
        return itself;
    }

    @Override
    public void onCreate(Bundle b) {

        super.onCreate(b);
        itself = this;

        // needs to be the first thing to happen
        initializeSelfRestarting();

        mHandler = new Handler();
        setContentView(R.layout.main_activity);

        initializeSWBatteryChecker();

        RegisterBroadcastsReceiver();

        CheckToShowRPE();

        CheckPermissions();

    }

    private void CheckPermissions() {
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }

        permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_LOCATION,
                    REQUEST_FINE_LOCATION
            );
        }
    }

    void CheckToShowRPE() {

        if (SensorsDataService.itself == null) {
            return;
        }

        if (!SensorsDataService.itself.needToShowRPE) {
            return;
        }


        SensorsDataService.itself.needToShowRPE = false;

    }

    // Updates the app state shown through the interface
    private void showCurrentAppState() {

        if (SensorsDataService.itself == null) {
            return;
        }

        // get measured states
        HashMap<String, Boolean> recordedActivities = SensorsDataService.getRecordedActivities();

        ImageButton morningHR = (ImageButton) findViewById(R.id.morningHR);
        ImageButton trainingHR = (ImageButton) findViewById(R.id.trainingHR);
        ImageButton startCooldown = (ImageButton) findViewById(R.id.startCooldown);
        ImageButton continueCooldown = (ImageButton) findViewById(R.id.continueCooldown);
        ImageButton eveningHR = (ImageButton) findViewById(R.id.eveningHR);

        morningHR.setBackgroundColor(recordedActivities.containsKey("MorningHR") ? Color.GREEN : Color.GRAY);
        trainingHR.setBackgroundColor(recordedActivities.containsKey("TrainingHR") ? Color.GREEN : Color.GRAY);
        startCooldown.setBackgroundColor(recordedActivities.containsKey("Cooldown") ? Color.GREEN : Color.GRAY);
        continueCooldown.setBackgroundColor(recordedActivities.containsKey("Recovery") ? Color.GREEN : Color.GRAY);
        eveningHR.setBackgroundColor(recordedActivities.containsKey("EveningHR") ? Color.GREEN : Color.GRAY);

        int inProgressColor = Color.argb(255, 255, 165, 0);

        if (SensorsDataService.itself.currentState.equals("Cooldown")) {
            startCooldown.setBackgroundColor(inProgressColor);
        }
        if (SensorsDataService.itself.currentState.equals("TrainingHR")) {
            trainingHR.setBackgroundColor(inProgressColor);
        }
        if (SensorsDataService.itself.currentState.equals("Recovery")) {
            continueCooldown.setBackgroundColor(inProgressColor);
        }
        if (SensorsDataService.itself.currentState.equals("MorningHR")) {
            morningHR.setBackgroundColor(inProgressColor);
        }
        if (SensorsDataService.itself.currentState.equals("EveningHR")) {
            eveningHR.setBackgroundColor(inProgressColor);
        }
    }

    // If the app crashes, restart it. Currently disabled so we can bugfix
    private void initializeSelfRestarting() {

        pendingInt = PendingIntent.getActivity(this, 0, new Intent(getIntent()), getIntent().getFlags());
        // Intent that kills the app after a certain amount of time after the app has crashed
        murderousIntent = new Intent(this, SensorsDataService.class);
        // start handler which starts pending-intent after Application Crash
        /*Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread paramThread, Throwable paramThrowable) {
                Log.d("Killer", "kills");
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
        });/**/

    }

    // Handles received broadcasted intents
    private void RegisterBroadcastsReceiver() {


        IntentFilter filter = new IntentFilter();
        filter.addAction(SensorsDataService.ACTION_BATTERY_STATUS);
        filter.addAction(SensorsDataService.ACTION_HR);
        filter.addAction(SensorsDataService.NEW_MESSAGE_AVAILABLE);
        filter.addAction(SensorsDataService.ASK_USER_FOR_RPE);
        filter.addAction(SensorsDataService.UPDATE_TIMER_VALUE);
        filter.addAction(SensorsDataService.UPDATE_GPS_PARAMS);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(br, filter);

    }

    // initialises a battery checker for the smartwatch
    private void initializeSWBatteryChecker() {
        final TextView SWBatteryStatus = (TextView) findViewById(R.id.SWbatteryLabel);
        final Handler h = new Handler();
        final int delay = 20000; //milliseconds
        final boolean[] warned_evening = {false};

        // Timer's fine for Java, but kills android apps.

        h.postDelayed(new Runnable() {
            public void run() {
                IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                Intent batteryStatus = registerReceiver(null, ifilter);

                int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

                int batteryPct = (int) (level / (float) scale * 100);
                SWBatteryStatus.setText("SW: " + batteryPct + "%");
                Calendar clnd = Calendar.getInstance();
                if (clnd.get(Calendar.HOUR_OF_DAY) >= 20) {
                    if (batteryPct < 75 && !warned_evening[0]) {
                        warned_evening[0] = true;
                        displaySWBatteryWarning();
                    } else if (batteryPct >= 75 && warned_evening[0]) {
                        warned_evening[0] = false;
                    }
                }
                h.postDelayed(this, delay);
            }
        }, 0);
    }

    // Displays a battery warning if it is to be assumed the battery won't last until dawn
    private void displaySWBatteryWarning() {
        // Display a cancelable warning that the HRM battery is running low.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        String warning = "The Smartwatch battery charge is below 75%. Please charge it to ensure it lasts the night.";
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(100);
        builder.setMessage(warning)
                .setTitle(R.string.battery_warning_title)
                .setCancelable(true)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // displays a battery warning if the battery is low
    private void displayBatteryWarning(int warned) {
        // Display a cancelable warning that the HRM battery is running low.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

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
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(100);
        builder.setMessage(warning)
                .setTitle(R.string.battery_warning_title)
                .setCancelable(true)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.setCancelable(true);
        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // restart of the dataservice retrieving data from the sensor

        if (SensorsDataService.itself == null) {
            Intent intent = new Intent(this, SensorsDataService.class);
            startService(intent);
        }

        //mGoogleApiClient.connect();
        if (dataUpdateReceiver == null) dataUpdateReceiver = new DataUpdateReceiver();
        IntentFilter intentFilter = new IntentFilter(SensorsDataService.NEW_MESSAGE_AVAILABLE);
        registerReceiver(dataUpdateReceiver, intentFilter);

        /*if (SensorsDataService.itself != null){
            SensorsDataService.itself.StopSleepTracking();
        }*/

        RegisterBroadcastsReceiver();

        showCurrentAppState();

    }

    @Override
    protected void onPause() {
        super.onPause();
        /*Wearable.DataApi.removeListener(mGoogleApiClient, this);
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        Wearable.NodeApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();*/
        //mSensorManager.unregisterListener(this);
        if (br != null) unregisterReceiver(br);

    }

    public void onClicked(View view) {
        switch (view.getId()) {
            case R.id.morningHR:

                if (SensorsDataService.itself != null) {

                    if (!SensorsDataService.isNowASleepingHour())
                        SensorsDataService.itself.SwitchSportsAction("MorningHR");
                }

                break;
            case R.id.trainingHR:

                if (SensorsDataService.itself != null) {
                    SensorsDataService.itself.SwitchSportsAction("TrainingHR");
                }

                break;
            case R.id.startCooldown:

                if (SensorsDataService.itself != null) {
                    SensorsDataService.itself.SwitchSportsAction("Cooldown");
                }

                break;
            case R.id.continueCooldown:

                if (SensorsDataService.itself != null) {
                    SensorsDataService.itself.SwitchSportsAction("Recovery");
                }


                break;
            case R.id.eveningHR:

                if (SensorsDataService.itself != null) {
                    if (SensorsDataService.isNowASleepingHour())
                        SensorsDataService.itself.SwitchSportsAction("EveningHR");
                }


                break;
            case R.id.searchForHRM:
                try {
                    final Intent bluetoothSelector = new Intent(this, DeviceScanActivity.class);
                    startActivity(bluetoothSelector);
                } catch (Exception ex) {
                    Log.e(TAG, ex.toString());
                }
                break;
            default:

                Log.e(TAG, "Unknown click event registered");
        }

        showCurrentAppState();

    }

    // this is used to communicate with Service
    private class DataUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(SensorsDataService.NEW_MESSAGE_AVAILABLE)) {
                //UpdateButtonText was called from here, but  that method didn't do anything.
            }


        }
    }
}


