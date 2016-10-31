package com.iss.android.wearable.datalayer;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static com.iss.android.wearable.datalayer.DataSyncService.getUserID;
import static com.iss.android.wearable.datalayer.ISSRecordData.resolver;

/**
 * Created by Euler on 12/19/2015.
 */
public class DataStorageManager {

    static SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(MainActivity.getContext());

    // I overwrote it so that it now takes Internal Storage, which is more fitting for in-app-data.
    // All other methods wouldn't let me write when using the scheduled RPE values.
    // static String dataFolder = MainActivity.getContext().getFilesDir().toString();
    static String dataFolder = Environment.getExternalStorageDirectory().getAbsolutePath();
    static File sleepData = new File(dataFolder + "/sleep-data/sleep-export.csv");


    // I use here external storage directory, as the previous versions of the
    // app use the external directory. In case ext. storage is not available, use
    // Environment.getDataDirectory().toString()
    static File userDataFolder = new File(dataFolder + "/triathlon");

    // a method responsible for creating the folders to write into
    public static void InitializeTriathlonFolder() {

        if (!userDataFolder.exists()) {
            userDataFolder.mkdir();
        }

    }

    // A method reading the schedule.csv file and parsing it to TimeSeries
    public static TimeSeries readUserSchedule() {

        TimeSeries result = new TimeSeries("RPE required");

        File csvSchedule = new File(userDataFolder, "schedule.csv");

        if (!csvSchedule.exists())
            return result;

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd");

        try (BufferedReader br = new BufferedReader(new FileReader(csvSchedule))) {
            String line;
            while ((line = br.readLine()) != null) {

                String dateStr = line.substring(0, 10);
                String contents = line.substring(11);

                Date date = dateFormat.parse(dateStr);
                Double value = Double.parseDouble(contents);

                result.AddValue(date, value);

            }
        } catch (Exception e) {

        }

        return result;

    }

    // A method that transcribes the UserID. Currently replaces "@" with "_at_"
    public static String getProperUserID(String UserID) {

        return UserID.replace("@", "_at_");

    }

    // sub-method of getKey
    public static String getStateKey(String mark) {

        int idx = mark.indexOf(":");

        if (idx < 0) {
            return mark;
        }

        return mark.substring(0, idx);

    }

    // A method that, together with getStateKey, returns the state, e.g. "resting", "idle" or "cooldown"
    public static String getKey(ISSRecordData record) {

        String mark = record.ExtraData;

        if (mark == null) {
            return null;
        }

        return getStateKey(mark);

    }

    // A method responsible for creating a file in which the scheduled RPE values will be saved (in CSVManager.WriteNewCSVdata)
    public static void storeScheduleLine(String scheduleString) {
        File file = new File(userDataFolder, "schedule.csv");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                Log.d("Creating file", "failed");
                e.printStackTrace();
            }
        }
        CSVManager.WriteNewCSVdata(file, scheduleString);
    }

    public static void insertISSRecordData(ISSRecordData data) {
        Log.d("measurement id", String.valueOf(data.measurementID));
        ContentValues values = new ContentValues();
        values.put(ISSContentProvider.SENT, false);
        values.put(ISSContentProvider.USERID,
                getUserID());
        values.put(ISSContentProvider.MEASUREMENT,
                data.MeasurementType);
        values.put(ISSContentProvider.MEASUREMENT_ID,
                data.measurementID);
        values.put(ISSContentProvider.DATE, data.Date);
        values.put(ISSContentProvider.TIMESTAMP, data.Timestamp);
        values.put(ISSContentProvider.EXTRA, data.ExtraData);
        values.put(ISSContentProvider.VALUE1, data.Value1);
        values.put(ISSContentProvider.VALUE2, data.Value2);
        values.put(ISSContentProvider.VALUE3, data.Value3);
        values.put(ISSContentProvider.SENSOR, data.Sensor);
        values.put(ISSContentProvider.SENT, "false");
        Log.d("values", values.toString());
        resolver.insert(ISSContentProvider.RECORDS_CONTENT_URI, values);

    }

    public static ArrayList<ISSRecordData> getData(Date time) {
        ArrayList<ISSRecordData> data = new ArrayList<ISSRecordData>();
        Uri CONTENT_URI = ISSContentProvider.RECORDS_CONTENT_URI;
        String date = time.toString();

        String mSelectionClause = ISSContentProvider.DATE + " = ?";
        String[] mSelectionArgs = {date};
        String[] mProjection = {ISSContentProvider._ID,
                ISSContentProvider.DATE,
                ISSContentProvider.TIMESTAMP,
                ISSContentProvider.EXTRA,
                ISSContentProvider.VALUE1,
                ISSContentProvider.VALUE2,
                ISSContentProvider.VALUE3,
                ISSContentProvider.SENSOR,
                ISSContentProvider.MEASUREMENT,
                ISSContentProvider.USERID};
        String mSortOrder = ISSContentProvider.TIMESTAMP + " DESC";

        // Does a query against the table and returns a Cursor object
        Cursor mCursor = MainActivity.getContext().getContentResolver().query(
                CONTENT_URI,                       // The content URI of the database table
                mProjection,                       // The columns to return for each row
                mSelectionClause,                  // Either null, or the word the user entered
                mSelectionArgs,                    // Either empty, or the string the user entered
                mSortOrder);                       // The sort order for the returned rows

        // Some providers return null if an error occurs, others throw an exception
        if (null == mCursor) {
            // If the Cursor is empty, the provider found no matches
        } else if (mCursor.getCount() < 1) {
            // If the Cursor is empty, the provider found no matches
        } else {
            while (mCursor.moveToNext()) {
                ISSRecordData record = ISSDictionary.CursorToISSRecordData(mCursor);
                data.add(record);
            }
        }

        return data;
    }

    public static void insertISSMeasurement(ISSMeasurement row) {
        Log.d("tries to insert", String.valueOf(row._ID));
        ContentValues values = new ContentValues();
        values.put(ISSContentProvider._ID, row._ID);
        values.put(ISSContentProvider.TIMESTAMP,
                row.timestamp);
        values.put(ISSContentProvider.TYPE,
                row.type);
        values.put(ISSContentProvider.SENT,
                "false");
        resolver.insert(ISSContentProvider.MEASUREMENT_CONTENT_URI, values);
    }

    public static int getLastMeasurementID() {
        return pref.getInt("LastMeasurement", 0);

    }

    public static void SetLastMeasurementID(int measurementNumber) {
        SharedPreferences.Editor editor = pref.edit();
        Log.d("Set measurement id", "to" + String.valueOf(measurementNumber));
        editor.putInt("LastMeasurement", measurementNumber);
        editor.apply();

    }
}
