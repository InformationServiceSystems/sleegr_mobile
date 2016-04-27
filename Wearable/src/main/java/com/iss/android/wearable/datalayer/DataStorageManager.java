package com.iss.android.wearable.datalayer;

import android.os.Environment;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by Euler on 12/19/2015.
 */
public class DataStorageManager {

    static String dataFolder = Environment.getExternalStorageDirectory().toString();
    static File sleepData = new File(dataFolder + "/sleep-data/sleep-export.csv");
    static File userDataFolder = new File(dataFolder, "triathlon");

    // Reformats the UserID (currently replaces only the "@" with "_at_"
    public static String getProperUserID(String UserID){

        return UserID.replace("@", "_at_");

    }

    // Collectsall files within a given timespan to upload to the phone
    public static ArrayList<ISSRecordData> GetAllFilesToUpload(String UserID){
        ArrayList<ISSRecordData> result = new ArrayList<>();
        // TODO: query the database to return all ISSRecords that haven't been sent to the phone yet
        return result;
    }

    public static File GetSleepData() {
        return sleepData;
    }
}
