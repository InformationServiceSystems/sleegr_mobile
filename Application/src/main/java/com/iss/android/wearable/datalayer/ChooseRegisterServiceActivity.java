package com.iss.android.wearable.datalayer;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;

public class ChooseRegisterServiceActivity extends Activity {

    public static Activity instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean regComplete = prefs.getBoolean("registration", false);
        if (regComplete) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
        setContentView(R.layout.activity_choose_register_service);
    }

    public void onClicked(View view) {
        switch (view.getId()) {
            case R.id.EMail:
                startActivity(new Intent(this, RegisterUserActivity.class));
                break;
        }
    }

}
