package com.iss.android.wearable.datalayer;

import com.auth0.android.result.UserProfile;

/**
 * Created by micha on 17.10.2016.
 */
public class UserData {
    private static String idToken = "default";
    private static String email = "default";
    private static String name = "default";
    private static UserProfile profile;
    private static String refreshToken;
    private static String deviceID;

    public static String getIdToken() {
        return UserData.idToken;
    }

    public static String getRefreshToken() {
        return UserData.refreshToken;
    }

    public static void setIdToken(String idToken) {
        UserData.idToken = idToken;
    }

    public static void setRefreshToken(String refreshToken) {
        UserData.refreshToken = refreshToken;
    }

    public static String getEmail() {
        return UserData.email;
    }

    public static void setEmail(String email) {
        UserData.email = email;
    }

    public static String getName() {
        return UserData.name;
    }

    public static void setName(String name) {
        UserData.name = name;
    }

    public static void setProfile(UserProfile profile) {
        UserData.profile = profile;
    }

    public static Object getUserProfile() {
        return UserData.profile;
    }

    static String getUserID() { return UserData.profile.getId(); }

    public static String getDeviceID() {
        return UserData.deviceID;
    }

    public static void putDeviceID(String string) {
        UserData.deviceID = string;
    }
}
