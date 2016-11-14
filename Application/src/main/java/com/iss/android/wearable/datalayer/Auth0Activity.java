package com.iss.android.wearable.datalayer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.auth0.android.Auth0;
import com.auth0.android.authentication.AuthenticationAPIClient;
import com.auth0.android.authentication.AuthenticationException;
import com.auth0.android.callback.BaseCallback;
import com.auth0.android.lock.AuthenticationCallback;
import com.auth0.android.lock.Lock;
import com.auth0.android.lock.LockCallback;
import com.auth0.android.lock.utils.LockException;
import com.auth0.android.result.Credentials;
import com.auth0.android.result.UserProfile;
import com.iss.android.wearable.datalayer.utils.CredentialsManager;

import java.util.HashMap;
import java.util.Map;


public class Auth0Activity extends Activity {

    private final LockCallback mCallback = new AuthenticationCallback() {
        @Override
        public void onAuthentication(Credentials credentials) {
            Toast.makeText(getApplicationContext(), "Log In - Success", Toast.LENGTH_SHORT).show();
            CredentialsManager.saveCredentials(getApplicationContext(), credentials);
            startActivity(new Intent(Auth0Activity.this, MainActivity.class));
            finish();
        }

        @Override
        public void onCanceled() {
            Toast.makeText(getApplicationContext(), "Log In - Cancelled", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onError(LockException error) {
            Toast.makeText(getApplicationContext(), "Log In - Error Occurred", Toast.LENGTH_SHORT).show();
        }
    };
    private Lock mLock;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Auth0 auth0 = new Auth0(getString(R.string.auth0_client_id), getString(R.string.auth0_domain));
        //Request a refresh token along with the id token.
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("scope", "openid offline_access");
        mLock = Lock.newBuilder(auth0, mCallback)
                .withAuthenticationParameters(parameters)
                //Add parameters to the build
                .build(this);

        if (CredentialsManager.getCredentials(this).getIdToken() == null) {
            startActivity(mLock.newIntent(this));
            return;
        }

        AuthenticationAPIClient aClient = new AuthenticationAPIClient(auth0);
        aClient.tokenInfo(CredentialsManager.getCredentials(this).getIdToken())
                .start(new BaseCallback<UserProfile, AuthenticationException>() {
                    @Override
                    public void onSuccess(final UserProfile payload) {
                        Auth0Activity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                // Toast.makeText(Auth0Activity.this, "Automatic Login Success", Toast.LENGTH_SHORT).show();
                                UserData.setProfile(payload);
                                UserData.setName(payload.getName());
                                UserData.setEmail(payload.getEmail());
                            }
                        });
                        startActivity(new Intent(getApplicationContext(), MainActivity.class));
                        finish();
                    }

                    @Override
                    public void onFailure(AuthenticationException error) {
                        Auth0Activity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(Auth0Activity.this, "Session Expired, please Log In", Toast.LENGTH_SHORT).show();
                            }
                        });
                        CredentialsManager.deleteCredentials(getApplicationContext());
                        startActivity(mLock.newIntent(Auth0Activity.this));
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Your own Activity code
        mLock.onDestroy(this);
        mLock = null;
    }

}