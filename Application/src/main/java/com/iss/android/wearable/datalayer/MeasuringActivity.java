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
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MeasuringActivity extends Activity {

    private static final int REQUEST_LOCATION = 1;
    private static final String[] PERMISSIONS_LOCATION = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION};
    private BluetoothAdapter mBluetoothAdapter;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BroadcastReceiver mReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measuring);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        checkPermissions();

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

        IntentFilter filter = new IntentFilter();
        // This is a method that adds an intent receiver that listens to every event that is triggered by bluetooth
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                    Log.d("MeasuringActivityLog", "Discovery started");
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    Log.d("MeasuringActivityLog", "Discovery finished");
                } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Get the BluetoothDevice object from the Intent
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    mLeDeviceListAdapter.addDevice(device);
                }
            }
        };
        registerReceiver(mReceiver, filter);
        mBluetoothAdapter.startDiscovery();
    }

    private void checkPermissions() {
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

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.switchBluetoothDevice:
                onOpenSwitchBluetoothDeviceDialog();
                break;
        }
    }

    /*Following are methods dealing with the Bluetooth dialog*/

    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);
        mBluetoothAdapter.cancelDiscovery();
        super.onDestroy();
    }

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
                        editor.putString(getString(R.string.device_address), device.getAddress());
                        Log.d("Device Address", device.getAddress());

                        final String deviceName = device.getName();
                        if (deviceName != null && deviceName.length() > 0)
                            editor.putString(getString(R.string.device_name), device.getName());
                        else
                            editor.putString(getString(R.string.device_name), getString(R.string.unknown_device));
                        editor.apply();
                        Toast.makeText(getApplicationContext(), "Set " + pref.getString(getString(R.string.device_name), "nothing") + " as your sensor!", Toast.LENGTH_SHORT).show();
                        Log.d("Device Address", "has been stored");
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = builderSingle.create();
        dialog.show();
    }

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
}
