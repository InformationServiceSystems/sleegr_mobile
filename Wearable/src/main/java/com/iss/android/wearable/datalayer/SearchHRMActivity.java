package com.iss.android.wearable.datalayer;

import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Bundle;

/**
 * Created by Michael on 30.11.2015.
 */
public class SearchHRMActivity extends Activity {

    @Override
    public void onCreate(Bundle b) {

        super.onCreate(b);

        PackageManager myPackageManager = getPackageManager();

        // let's disable the Bluetooth Search Activity
        myPackageManager.setComponentEnabledSetting(new ComponentName(this, SearchHRMActivity.class),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

        // let's enable the MainActivity
        myPackageManager.setComponentEnabledSetting(new ComponentName(this, MainActivity.class),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

    }
}
