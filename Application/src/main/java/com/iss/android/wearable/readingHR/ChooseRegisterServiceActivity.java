package com.iss.android.wearable.readingHR;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.view.View;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ChooseRegisterServiceActivity extends Activity {

    public static Activity instance;
    CallbackManager callbackManager;
    AccessTokenTracker accessTokenTracker;
    AccessToken accessToken;

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

        accessTokenTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(
                    AccessToken oldAccessToken,
                    AccessToken currentAccessToken) {
                // Set the access token using
                // currentAccessToken when it's loaded or set.
            }
        };
        // If the access token is available already assign it.
        accessToken = AccessToken.getCurrentAccessToken();

        LoginButton authButton = (LoginButton) this.findViewById(R.id.Facebook);
        authButton.setReadPermissions("public_profile", "email");

        try {
            PackageInfo info = getPackageManager().getPackageInfo(
                    "com.iss.android.wearable.datalayer",
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.d("KeyHash:", "Packagemanager name not found");
        } catch (NoSuchAlgorithmException e) {
            Log.d("KeyHash:", "No such algorithm");
        }

        callbackManager = CallbackManager.Factory.create();
        authButton.registerCallback(callbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        Log.d("login: ", "granted, permissions: " + String.valueOf(AccessToken.getCurrentAccessToken().getPermissions()));
                    }

                    @Override
                    public void onCancel() {
                        Log.d("login: ", "cancelled");
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        Log.d("login: ", "failed");
                    }
                });
    }

    // If the user opts to use his E-Mail for registration purposes, this happens
    public void onClicked(View view) {
        switch (view.getId()) {
            case R.id.EMail:
                startActivity(new Intent(this, RegisterUserActivity.class));
                break;
        }
    }

    // If the user opts to use his facebook account, this method retrieves the name, gender and
    // email from his FB account and stores them.
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i("onactivityResult", "CALLED");
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
        GraphRequest request = GraphRequest.newMeRequest(AccessToken.getCurrentAccessToken(), new GraphRequest.GraphJSONObjectCallback() {

            @Override
            public void onCompleted(JSONObject object, GraphResponse response) {
                Log.i("Login Activity", response.toString());
                try {
                    String name = object.getString("name");
                    String email = object.getString("email");
                    String gender = object.getString("gender");
                    Log.i("Name" + name, "Gender" + gender + email);
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(instance);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("user_email", email);
                    editor.putBoolean("registration", true);
                    startActivity(new Intent(instance, MainActivity.class));

                    editor.apply();
                    finish();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        Bundle parameters = new Bundle();
        parameters.putString("fields", "name,gender,email");
        request.setParameters(parameters);
        request.executeAsync();
    }

    // A method that, if the Facebook registration activity fails, stops tracking the accessToken,
    // so we can generate a new one and track that.
    @Override
    public void onDestroy() {
        super.onDestroy();
        accessTokenTracker.stopTracking();
    }

}
