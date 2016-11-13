package com.iss.android.wearable.datalayer;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class MeasuringActivity extends Activity {

    private static final int REQUEST_LOCATION = 1;
    private static final String[] PERMISSIONS_LOCATION = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION};
    private BluetoothAdapter mBluetoothAdapter;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private int warned = 0;
    private SensorsDataService sensorsDataService;
    private boolean broadcastReceiverIsRegistered = false;

    /*
    This broadcast receiver receives intents from the SensorsDataService class. As this is non-static,
    the only acceptable way to interact with its UI from the SensorsDataService class is through intents.
     */
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        // Receives broadcasts sent from other points of the app, like the SensorsDataService
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                //Search for bluetooth devices in surroundings started
                Log.d("MeasuringActivityLog", "Discovery started");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //Search for bluetooth devices in surroundings ended
                Log.d("MeasuringActivityLog", "Discovery finished");
            } else if (action.equals(SensorsDataService.MESSAGE)) {
                // A message from SensorsDataService has been received.
                // To get color-coding for the buttons, I check the message for the given date, if a measurement of this type already has been performed.
                //TODO: switch to database queries for smartwatch compatibility.
                Calendar calendar = new GregorianCalendar();
                String date = calendar.get(Calendar.DAY_OF_MONTH) + calendar.get(Calendar.MONTH) + calendar.get(Calendar.YEAR) + "";
                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
                if (intent.getStringExtra("message").equals("Invalidate Morning Colors")) {
                    ImageButton button = (ImageButton) findViewById(R.id.morningHR);
                    button.setBackgroundColor(getResources().getColor(R.color.com_facebook_button_like_background_color_selected));
                } else if (intent.getStringExtra("message").equals("Invalidate Evening Colors")) {
                    ImageButton button = (ImageButton) findViewById(R.id.eveningHR);
                    button.setBackgroundColor(getResources().getColor(R.color.com_facebook_button_like_background_color_selected));
                } else if (intent.getStringExtra("message").equals("Invalidate Training Colors")) {
                    ImageButton button = (ImageButton) findViewById(R.id.trainingHR);
                    button.setBackgroundColor(getResources().getColor(R.color.com_facebook_button_like_background_color_selected));
                } else if (intent.getStringExtra("message").equals("Invalidate Cooldown Colors")) {
                    ImageButton button = (ImageButton) findViewById(R.id.cooldownHR);
                    button.setBackgroundColor(getResources().getColor(R.color.com_facebook_button_like_background_color_selected));
                } else if (intent.getStringExtra("message").equals("Invalidate Recovery Colors")) {
                    ImageButton button = (ImageButton) findViewById(R.id.recoveryHR);
                    button.setBackgroundColor(getResources().getColor(R.color.com_facebook_button_like_background_color_selected));
                } else if (intent.getStringExtra("message").equals("Invalidate Button Colors, Finished")) {
                    // The measurement has finished, fade all buttons that have been used today.
                    if (pref.getBoolean(date + "Cooldown", false)) {
                        ImageButton button = (ImageButton) findViewById(R.id.cooldownHR);
                        button.setBackgroundColor(getResources().getColor(R.color.com_facebook_button_background_color_disabled));
                    } if (pref.getBoolean(date + "EveningHR", false)) {
                        ImageButton button = (ImageButton) findViewById(R.id.eveningHR);
                        button.setBackgroundColor(getResources().getColor(R.color.com_facebook_button_background_color_disabled));
                    } if (pref.getBoolean(date + "Recovery", false)) {
                        ImageButton button = (ImageButton) findViewById(R.id.recoveryHR);
                        button.setBackgroundColor(getResources().getColor(R.color.com_facebook_button_background_color_disabled));
                    } if (pref.getBoolean(date + "TrainingHR", false)) {
                        ImageButton button = (ImageButton) findViewById(R.id.trainingHR);
                        button.setBackgroundColor(getResources().getColor(R.color.com_facebook_button_background_color_disabled));
                    } if (pref.getBoolean(date + "MorningHR", false)) {
                        ImageButton button = (ImageButton) findViewById(R.id.morningHR);
                        button.setBackgroundColor(getResources().getColor(R.color.com_facebook_button_background_color_disabled));
                    }
                } else if (intent.getStringExtra("message").equals("")) {

                }
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // A bluetooth device has been found in the surroundings. add it to the listhandler for the popup.
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mLeDeviceListAdapter.addDevice(device);
            } else if (action.equals(SensorsDataService.ACTION_BATTERY_STATUS)) {
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
            } else if (action.equals(SensorsDataService.ACTION_HR)) {
                // Prints out the heart rate
                final TextView HeartRate = (TextView) findViewById(R.id.heartRateDisplay);
                int result = intent.getIntExtra(SensorsDataService.EXTRA_HR, 0);
                // Need to convert the Int to String or else the app crashes. GJ Google.
                HeartRate.setText(Integer.toString(result));
            } else if (action.equals(SensorsDataService.UPDATE_TIMER_VALUE)) {
                // Prints the time since the measurement has started to the smartphone display. Useful for the measurer.
                String newtime = String.valueOf(intent.getIntExtra("minutes", 0))
                        + ":" + StringUtils.leftPad(Long.toString(intent.getIntExtra("seconds", 0)), 2, "0");
                TextView timetext = (TextView) findViewById(R.id.timer);
                timetext.setText(newtime);
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measuring);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        RegisterBroadcastReceiver();
        checkPermissions();

        if (SensorsDataService.itself == null) {
            Log.d("Starting", "SensorsDataService");
            Intent intent = new Intent(this, SensorsDataService.class);
            startService(intent);
        }

        OutputMessage("Invalidate Button Colors, Finished");

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        Log.d("MeasuringActivityLog", "initialised a manager");
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            int REQUEST_ENABLE_BT = 1;
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        mBluetoothAdapter.startDiscovery();
    }

    private void RegisterBroadcastReceiver() {
        // Register all the different types of broadcast intents the receiver should listen to.
        IntentFilter filter = new IntentFilter();
        filter.addAction(SensorsDataService.ACTION_BATTERY_STATUS);
        filter.addAction(SensorsDataService.MESSAGE);
        filter.addAction(SensorsDataService.ACTION_HR);
        filter.addAction(SensorsDataService.UPDATE_TIMER_VALUE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(broadcastReceiver, filter);
        broadcastReceiverIsRegistered = true;
    }

    private void checkPermissions() {
        // The app needs to be able to have access to bluetooth devices, or else the sensors won't work.
        // This checks if that is given, and, if not, asks the user to grant this permission.
        int permission = android.support.v4.app.ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            android.support.v4.app.ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_LOCATION,
                    REQUEST_LOCATION
            );
        }
    }

    /*
    Function that handles how the user response towards the eprmission request was answered.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay!
                    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    mBluetoothAdapter.startDiscovery();

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public void onResume() {
        // if we resume this activity, the broadcastreceievr has been unregistered before, so now we re-register it.
        RegisterBroadcastReceiver();
        broadcastReceiverIsRegistered = true;
        super.onResume();
    }

    @Override
    public void onPause() {
        // important to unregister the broadcastreceiver, we dont want that be dangling in the
        // background and closing randomly, sometimes having multiple versions open.
        if (broadcastReceiverIsRegistered) {
            unregisterReceiver(broadcastReceiver);
            broadcastReceiverIsRegistered = false;
        }
        super.onPause();
    }

    /*
    Clickhandler for buttons in this activity.
     */
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.switchBluetoothDevice:
                // Button to popup the bluetooth device list, from which the user can choose what device to use
                onOpenSwitchBluetoothDeviceDialog();
                break;
            case R.id.morningHR:

                // Button to start morning measurement
                if (SensorsDataService.itself != null) {

                    if (!SensorsDataService.isNowASleepingHour()) {
                        SensorsDataService.itself.SwitchSportsAction("MorningHR");
                    } else {
                        Toast.makeText(this, R.string.too_late_for_morning, Toast.LENGTH_SHORT).show();
                    }
                }

                break;
            case R.id.trainingHR:

                // Button to start training measurement
                if (SensorsDataService.itself != null) {
                    SensorsDataService.itself.SwitchSportsAction("TrainingHR");
                }

                break;
            case R.id.cooldownHR:

                // Button to start cooldown measurement
                if (SensorsDataService.itself != null) {
                    SensorsDataService.itself.SwitchSportsAction("Cooldown");
                }

                break;
            case R.id.recoveryHR:

                // Button to start recovery measurement
                if (SensorsDataService.itself != null) {
                    SensorsDataService.itself.SwitchSportsAction("Recovery");
                }


                break;
            case R.id.eveningHR:

                // Button to start evening measurement
                if (SensorsDataService.itself != null) {
                    if (SensorsDataService.isNowASleepingHour()) {
                        SensorsDataService.itself.SwitchSportsAction("EveningHR");
                    } else {
                        Toast.makeText(this, R.string.too_early_for_evening, Toast.LENGTH_SHORT).show();
                    }
                }


                break;
            default:
                // If the user clicks somewhere where no listener has been added, log this error.
                Log.e("OnClick", "Unknown click event registered");
        }
    }

    @Override
    public void onDestroy() {
        // important to unregister the broadcastreceiver, we dont want that be dangling in the
        // background and closing randomly, sometimes having multiple versions open.
        if (broadcastReceiverIsRegistered) {
            unregisterReceiver(broadcastReceiver);
            broadcastReceiverIsRegistered = false;
        }
        mBluetoothAdapter.cancelDiscovery();
        super.onDestroy();
    }

    /*Following are methods dealing with the Bluetooth dialog*/

    private void onOpenSwitchBluetoothDeviceDialog() {
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(this, R.style.MyDialogTheme);
        builderSingle.setTitle("Select your heart rate sensor:");

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
        }

        // Build a cancel button for the dialog, if the user chooses not to use a bluetooth device.
        builderSingle.setNegativeButton(
                "cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builderSingle.setAdapter(mLeDeviceListAdapter,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mBluetoothAdapter.cancelDiscovery();
                        BluetoothDevice device = mLeDeviceListAdapter.getDevice(which);
                        if (device == null) return;
                        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        SharedPreferences.Editor editor = pref.edit();
                        // Store the device address in Shared preferences. It overwrites, so no need to delete old addresses.
                        editor.putString(getString(R.string.device_address), device.getAddress());
                        Log.d("Device Address", device.getAddress());

                        final String deviceName = device.getName();
                        if (deviceName != null && deviceName.length() > 0)
                            // Store the device name in the sharedpreferences, as long as it is not null (may happen with unknown devices)
                            editor.putString(getString(R.string.device_name), device.getName());
                        else
                            editor.putString(getString(R.string.device_name), getString(R.string.unknown_device));
                        editor.apply();
                        // Show the user that it has been saved.
                        Toast.makeText(getApplicationContext(), "Set " + pref.getString(getString(R.string.device_name), "nothing") + " as your sensor!", Toast.LENGTH_SHORT).show();
                        Log.d("Device Address", "has been stored");
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = builderSingle.create();
        dialog.show();
    }

    /*
    Viewholder which describes a list element. Needed so that the listadapter can put stuff in it.
     */
    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

    /*Following are methods affecting the open UI:*/

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<>();
            mInflator = MeasuringActivity.this.getLayoutInflater();
        }

        void addDevice(BluetoothDevice device) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    /* Following are methods directly affecting the activity's layout*/

    public void showState(String currentState) {
        TextView currentStateTextView = (TextView) findViewById(R.id.showCurrentState);
        currentStateTextView.setText(currentState);
    }

    // displays a battery warning if the battery is low
    private void displayBatteryWarning(int warned) {
        // Display a cancelable warning that the HRM battery is running low.
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);

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

        android.app.AlertDialog dialog = builder.create();
        dialog.setCancelable(true);
        dialog.show();
    }

    public void OutputMessage(String message) {
        Intent i = new Intent(SensorsDataService.MESSAGE);
        i.putExtra("message", message);
        sendBroadcast(i);
    }
}
