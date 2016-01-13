package com.iss.android.wearable.datalayer;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

public class ChooseRegisterServiceActivity extends Activity {

    public static Activity instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        FacebookSdk.sdkInitialize(getApplicationContext());
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean regComplete = prefs.getBoolean("registration", false);
        if (regComplete) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
        setContentView(R.layout.activity_choose_register_service);

        CallbackManager callbackManager = CallbackManager.Factory.create();

        LoginManager.getInstance().registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        // App code
                    }

                    @Override
                    public void onCancel() {
                        // App code
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        // App code
                    }
                });
    }

    public void onClicked(View view) {
        switch (view.getId()) {
            case R.id.EMail:
                startActivity(new Intent(this, RegisterUserActivity.class));
                break;
        }
    }

}
