package com.iss.android.wearable.datalayer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
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
import com.auth0.android.result.Delegation;
import com.auth0.android.result.UserProfile;
import com.iss.android.wearable.datalayer.utils.CredentialsManager;

import java.util.HashMap;
import java.util.Map;

import static com.iss.android.wearable.datalayer.MainActivity.getContext;


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
        parameters.put("device", Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID));
        UserData.putDeviceID(Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID));
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
                    /**
                     * If the login per id token is successful, nothing else is needed to be done.
                     * @param payload
                     */
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

                    /**
                     * In case the automatic login did not work, try to get a new idToken with your refreshToken.
                     * @param error
                     */

                    @Override
                    public void onFailure(AuthenticationException error) {
                        Auth0 auth0 = new Auth0(getString(R.string.auth0_client_id), getString(R.string.auth0_domain));
                        AuthenticationAPIClient aClient = new AuthenticationAPIClient(auth0);
                        String refreshToken = UserData.getRefreshToken();
                        Log.d("Automatic Login failed", "Trying to get a new idToken");

                        aClient.delegationWithRefreshToken(refreshToken)
                                .start(new BaseCallback<Delegation, AuthenticationException>() {

                                    @Override
                                    public void onSuccess(Delegation payload) {
                                        Auth0 auth0 = new Auth0(getString(R.string.auth0_client_id), getString(R.string.auth0_domain));
                                        AuthenticationAPIClient aClient = new AuthenticationAPIClient(auth0);
                                        Log.d("Success", "Got a new idToken with the refreshToken");
                                        String idToken = payload.getIdToken(); // New ID Token
                                        UserData.setIdToken(idToken);
                                        long expiresIn = payload.getExpiresIn(); // New ID Token Expire Date
                                        Credentials credentials = CredentialsManager.getCredentials(getApplicationContext());
                                        Credentials newCredentials = new Credentials(idToken, credentials.getAccessToken(), credentials.getType(), credentials.getRefreshToken());
                                        CredentialsManager.saveCredentials(getApplicationContext(), newCredentials);

                                        aClient.tokenInfo(newCredentials.getIdToken())
                                                .start(new BaseCallback<UserProfile, AuthenticationException>() {
                                                    @Override
                                                    public void onSuccess(final UserProfile payload) {
                                                        Log.d("Success", "Login with the new idToken worked");
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
                                                        Log.d("Failure", "The new idToken did not work");
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
                                    public void onFailure(AuthenticationException error) {
                                        Log.d("Failure", "Could not get a new idToken with the help of the refreshToken");
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