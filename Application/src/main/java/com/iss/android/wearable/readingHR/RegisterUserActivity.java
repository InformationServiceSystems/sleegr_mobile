package com.iss.android.wearable.readingHR;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.EditText;

public class RegisterUserActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean regComplete = prefs.getBoolean("registration", false);
        if (regComplete) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
        setContentView(R.layout.activity_register_user);
    }

    // Handles the input information once the confirm button is hit
    public void onUserConfirm(View view){

        EditText editText = (EditText) findViewById(R.id.editText);
        String emailid = editText.getText().toString().replace(" ","");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("user_email", emailid);
        editor.putBoolean("registration", true);
        startActivity(new Intent(this, MainActivity.class));

        editor.apply();
        finish();

    }

}
