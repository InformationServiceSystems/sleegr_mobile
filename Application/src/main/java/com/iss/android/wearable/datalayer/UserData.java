package com.iss.android.wearable.datalayer;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.iss.android.wearable.datalayer.utils.CredentialsManager;

import static com.iss.android.wearable.datalayer.MainActivity.getContext;

/**
 * Created by micha on 17.10.2016.
 */
public class UserData {
    private static String idToken = "default";
    private static String email = "default";
    private static String name = "default";

    public static String getIdToken() {
        return UserData.idToken;
    }

    public static String getEmail() {
        return UserData.email;
    }

    public static String getName() {
        return UserData.name;
    }

    public static void setName(String name) {
        UserData.name = name;
    }

    public static void setEmail(String email) {
        UserData.email = email;
    }

    public static void setIdToken(String idToken) {
        UserData.idToken = idToken;
    }
}