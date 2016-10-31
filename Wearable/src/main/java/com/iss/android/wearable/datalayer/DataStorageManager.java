package com.iss.android.wearable.datalayer;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Euler on 12/19/2015.
 */
public class DataStorageManager {

    static SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(MainActivity.getContext());

    // Collects all ISSRecordDatas in the database that haven't been sent to the smartphone yet
    public static ArrayList<ISSRecordData> GetAllFilesToUpload(long _ID) {
        ArrayList<ISSRecordData> result = new ArrayList<>();
        // A "projection" defines the columns that will be returned for each row
        String[] mProjection =
                {
                        ISSContentProvider._ID,
                        ISSContentProvider.USERID,
                        ISSContentProvider.MEASUREMENT,
                        ISSContentProvider.DATE,
                        ISSContentProvider.TIMESTAMP,
                        ISSContentProvider.EXTRA,
                        ISSContentProvider.VALUE1,
                        ISSContentProvider.VALUE2,
                        ISSContentProvider.VALUE3,
                        ISSContentProvider.SENSOR,
                        ISSContentProvider.MEASUREMENT_ID
                };

        // Defines a string to contain the selection clause
        String mSelectionClause = ISSContentProvider.MEASUREMENT_ID + " = " + _ID;

        // Initializes an array to contain selection arguments
        String[] mSelectionArgs = {};

        // Define a sorting order for the query results to appear in
        String mSortOrder = ISSContentProvider.TIMESTAMP + " DESC, " + ISSContentProvider.DATE + " DESC";

        Cursor mCursor = MainActivity.getContext().getContentResolver().query(
                ISSContentProvider.RECORDS_CONTENT_URI,    // The content URI of the words table
                mProjection,                       // The columns to return for each row
                mSelectionClause,                  // Either null, or the word the user entered
                mSelectionArgs,                    // Either empty, or the string the user entered
                mSortOrder);                       // The sort order for the returned rows

        if (null == mCursor) {
            // If the Cursor is empty, the provider found no matches
        } else if (mCursor.getCount() < 1) {
            // If the Cursor is empty, the provider found no matches
        } else {
            while (mCursor.moveToNext()) {
                ISSRecordData record = ISSDictionary.CursorToISSRecordDate(mCursor);
                Log.d("record caught", record.toString());
                result.add(record);
            }
        }

        return result;
    }

    public static void insertISSRecordData(ISSRecordData data) {
        if (data.measurementID != 0) {
            // Defines a new Uri object that receives the result of the insertion
            Uri mNewUri;

            // Defines an object to contain the new values to insert
            ContentValues mNewValues = new ContentValues();
            Log.d("inserted date", data.Date);

            /*
             * Sets the values of each column and inserts the word. The arguments to the "put"
             * method are "column name" and "value"
             */
            mNewValues.put(ISSContentProvider.DATE, data.Date);
            mNewValues.put(ISSContentProvider.TIMESTAMP, data.Timestamp);
            mNewValues.put(ISSContentProvider.MEASUREMENT, data.MeasurementType);
            mNewValues.put(ISSContentProvider.EXTRA, data.ExtraData);
            mNewValues.put(ISSContentProvider.VALUE1, data.Value1);
            mNewValues.put(ISSContentProvider.VALUE2, data.Value2);
            mNewValues.put(ISSContentProvider.VALUE3, data.Value3);
            mNewValues.put(ISSContentProvider.USERID, data.UserID);
            mNewValues.put(ISSContentProvider.SENSOR, data.Sensor);
            mNewValues.put(ISSContentProvider.MEASUREMENT_ID, data.measurementID);
            Log.d("values", mNewValues.toString());
            mNewUri = MainActivity.getContext().getContentResolver().insert(
                    ISSContentProvider.RECORDS_CONTENT_URI,   // the user dictionary content URI
                    mNewValues                          // the values to insert
            );
        }
    }

    public static int deleteISSRecords(Long _ID) {
        // Defines selection criteria for the rows you want to delete
        String mSelectionClause = "";
        String[] mSelectionArgs = null;

        // Defines a variable to contain the number of rows deleted
        int mRowsDeleted = 0;

        // Deletes the words that match the selection criteria
        mRowsDeleted = MainActivity.getContext().getContentResolver().delete(
                ISSContentProvider.RECORDS_CONTENT_URI,     // the user dictionary content URI
                ISSContentProvider.MEASUREMENT_ID + " = " + _ID,                   // the column to select on
                mSelectionArgs                      // the value to compare to
        );
        mRowsDeleted = MainActivity.getContext().getContentResolver().delete(
                ISSContentProvider.RPE_CONTENT_URI,     // the user dictionary content URI
                ISSContentProvider.MEASUREMENT_ID + " = " + _ID,                   // the column to select on
                mSelectionArgs                      // the value to compare to
        );
        mRowsDeleted = MainActivity.getContext().getContentResolver().delete(
                ISSContentProvider.MEASUREMENT_CONTENT_URI,     // the user dictionary content URI
                ISSContentProvider._ID + " = " + _ID,                   // the column to select on
                mSelectionArgs                      // the value to compare to
        );
        return mRowsDeleted;
    }

    public static void storeQuestionnaire(HashMap<String, Integer> answers) {
        int measurementID = GetLastMeasurementID();

        // Defines a new Uri object that receives the result of the insertion
        Uri mNewUri;

        // Defines an object to contain the new values to insert
        ContentValues mNewValues = new ContentValues();

        /*
         * Sets the values of each column and inserts the word. The arguments to the "put"
         * method are "column name" and "value"
         */
        mNewValues.put(ISSContentProvider.MEASUREMENT_ID, measurementID);
        mNewValues.put(ISSContentProvider.RPE_ANSWERS, ISSDictionary.MapToByteArray(answers));

        mNewUri = MainActivity.getContext().getContentResolver().insert(
                ISSContentProvider.RPE_CONTENT_URI,   // the user dictionary content URI
                mNewValues                          // the values to insert
        );
    }


    public static int GetLastMeasurementID() {
        return pref.getInt("LastMeasurement", 0);
    }

    public static byte[] BuildItem() throws IOException {
        ArrayList<ISSMeasurement> Measurements = GetAllMeasurements();
        ArrayList<ISSRecordData> ISSRecords = GetAllFilesToUpload(Measurements.get(0)._ID);
        ArrayList<ISSRPEAnswers> RPEAnswers = GetAllRPEAnswers(Measurements.get(0)._ID);
        byte[][] data = new byte[3][];

        try {
            data[0] = Serializer.SerializeToBytes(ISSRecords);
            data[1] = Serializer.SerializeToBytes(Measurements);
            data[2] = Serializer.SerializeToBytes(RPEAnswers);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Serializer.SerializeToBytes(data);
    }

    private static ArrayList<ISSRPEAnswers> GetAllRPEAnswers(long _ID) {
        ArrayList<ISSRPEAnswers> result = new ArrayList<>();
        // A "projection" defines the columns that will be returned for each row
        String[] mProjection =
                {
                        ISSContentProvider._ID,
                        ISSContentProvider.MEASUREMENT_ID,
                        ISSContentProvider.RPE_ANSWERS
                };

        // Defines a string to contain the selection clause
        String mSelectionClause = ISSContentProvider.MEASUREMENT_ID + " = " + _ID;

        // Initializes an array to contain selection arguments
        String[] mSelectionArgs = {};

        // Define a sorting order for the query results to appear in
        String mSortOrder = ISSContentProvider._ID + " ASC";

        Cursor mCursor = MainActivity.getContext().getContentResolver().query(
                ISSContentProvider.RPE_CONTENT_URI,    // The content URI of the words table
                mProjection,                       // The columns to return for each row
                mSelectionClause,                  // Either null, or the word the user entered
                mSelectionArgs,                    // Either empty, or the string the user entered
                mSortOrder);                       // The sort order for the returned rows

        if (null == mCursor) {
            // If the Cursor is empty, the provider found no matches
        } else if (mCursor.getCount() < 1) {
            // If the Cursor is empty, the provider found no matches
        } else {
            while (mCursor.moveToNext()) {
                ISSRPEAnswers issrpeAnswers = ISSDictionary.CursorToISSRPEAnswers(mCursor);
                result.add(issrpeAnswers);
            }
        }
        return result;
    }

    private static ArrayList<ISSMeasurement> GetAllMeasurements() {
        ArrayList<ISSMeasurement> result = new ArrayList<>();
        // A "projection" defines the columns that will be returned for each row
        String[] mProjection =
                {
                        ISSContentProvider._ID,
                        ISSContentProvider.TYPE,
                        ISSContentProvider.TIMESTAMP
                };

        // Defines a string to contain the selection clause
        String mSelectionClause = null;

        // Initializes an array to contain selection arguments
        String[] mSelectionArgs = {};

        // Define a sorting order for the query results to appear in
        String mSortOrder = ISSContentProvider._ID + " ASC " + " LIMIT 1";

        Cursor mCursor = MainActivity.getContext().getContentResolver().query(
                ISSContentProvider.MEASUREMENT_CONTENT_URI,    // The content URI of the words table
                mProjection,                       // The columns to return for each row
                mSelectionClause,                  // Either null, or the word the user entered
                mSelectionArgs,                    // Either empty, or the string the user entered
                mSortOrder);                       // The sort order for the returned rows

        if (null == mCursor) {
            // If the Cursor is empty, the provider found no matches
        } else if (mCursor.getCount() < 1) {
            // If the Cursor is empty, the provider found no matches
        } else {
            while (mCursor.moveToNext()) {
                ISSMeasurement measurement = ISSDictionary.CursorToISSMeasurement(mCursor);
                result.add(measurement);
            }
        }
        return result;
    }

    private static void SetLastTransferredMeasurementID(int j) {
        SharedPreferences.Editor editor = pref.edit();
        Log.d("Set transferred id", "to" + String.valueOf(j));
        editor.putInt("LastTransferredMeasurement", j);
        editor.apply();
    }

    private static int GetLastTransferredMeasurementID() {
        return pref.getInt("LastTransferredMeasurement", 0);
    }

    public static void SetLastMeasurementID(int measurementNumber) {
        SharedPreferences.Editor editor = pref.edit();
        Log.d("Set measurement id", "to" + String.valueOf(measurementNumber));
        editor.putInt("LastMeasurement", measurementNumber);
        editor.apply();
    }

    public static void clearLastMeasurement() {
        ArrayList<ISSMeasurement> Measurements = GetAllMeasurements();
        deleteISSRecords(Measurements.get(0)._ID);

    }

    public static boolean dataAvailable() {ArrayList<ISSMeasurement> result = new ArrayList<>();
        // A "projection" defines the columns that will be returned for each row
        String[] mProjection =
                {
                        ISSContentProvider._ID,
                        ISSContentProvider.TYPE,
                        ISSContentProvider.TIMESTAMP
                };

        // Defines a string to contain the selection clause
        String mSelectionClause = null;

        // Initializes an array to contain selection arguments
        String[] mSelectionArgs = {};

        // Define a sorting order for the query results to appear in
        String mSortOrder = null;

        Cursor mCursor = MainActivity.getContext().getContentResolver().query(
                ISSContentProvider.MEASUREMENT_CONTENT_URI,    // The content URI of the words table
                mProjection,                       // The columns to return for each row
                mSelectionClause,                  // Either null, or the word the user entered
                mSelectionArgs,                    // Either empty, or the string the user entered
                mSortOrder);                       // The sort order for the returned rows

        if (null == mCursor) {
            return false;
        } else if (mCursor.getCount() < 1) {
            return false;
        } else {
            return true;
        }
    }
}
