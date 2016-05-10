package com.iss.android.wearable.datalayer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;

import org.apache.commons.lang3.ArrayUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Blob;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

/**
 * Created by Euler on 12/19/2015.
 */
public class DataStorageManager {

    static String dataFolder = Environment.getExternalStorageDirectory().toString();

    // Collects all ISSRecordDatas in the database that haven't been sent to the smartphone yet
    public static ArrayList<ISSRecordData> GetAllFilesToUpload(){
        ArrayList<ISSRecordData> result = new ArrayList<>();
        // A "projection" defines the columns that will be returned for each row
        String[] mProjection =
                {
                        ISSContentProvider._ID,
                        ISSContentProvider.USERID,
                        ISSContentProvider.DATE,
                        ISSContentProvider.TIMESTAMP,
                        ISSContentProvider.MEASUREMENT,
                        ISSContentProvider.EXTRA,
                        ISSContentProvider.VALUE1,
                        ISSContentProvider.VALUE2,
                        ISSContentProvider.VALUE3,
                        ISSContentProvider.MEASUREMENT_ID
                };

        // Defines a string to contain the selection clause
        String mSelectionClause = null;

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
            mNewValues.put(ISSContentProvider.MEASUREMENT_ID, data.measurementID);

            mNewUri = MainActivity.getContext().getContentResolver().insert(
                    ISSContentProvider.RECORDS_CONTENT_URI,   // the user dictionary content URI
                    mNewValues                          // the values to insert
            );
        }
    }

    public static int deleteISSRecords() {
        // Defines selection criteria for the rows you want to delete
        String mSelectionClause = "";
        String[] mSelectionArgs = null;

        // Defines a variable to contain the number of rows deleted
        int mRowsDeleted = 0;

        // Deletes the words that match the selection criteria
        mRowsDeleted = MainActivity.getContext().getContentResolver().delete(
                ISSContentProvider.RECORDS_CONTENT_URI,     // the user dictionary content URI
                mSelectionClause,                   // the column to select on
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
        int measurementNumber = 0;
        Uri CONTENT_URI = ISSContentProvider.MEASUREMENT_CONTENT_URI;

        String mSelectionClause = null;
        String[] mSelectionArgs = {};
        String[] mProjection = {ISSContentProvider._ID};
        String mSortOrder = ISSContentProvider._ID + " DESC";

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
            mCursor.moveToNext();
            measurementNumber = mCursor.getInt(0);
        }
        if (measurementNumber > 0){
            return measurementNumber;
        } else {
            return 0;
        }
    }

    public static byte[] BuildItem() throws IOException {
        ArrayList<ISSRecordData> ISSRecords = GetAllFilesToUpload();
        ArrayList<ISSMeasurement> Measurements = GetAllMeasurements();
        ArrayList<ISSRPEAnswers> RPEAnswers = GetAllRPEAnswers();
        byte [][] data = new byte[3][];

        try {
            data[0] = Serializer.SerializeToBytes(ISSRecords);
            data[1] = Serializer.SerializeToBytes(Measurements);
            data[2] = Serializer.SerializeToBytes(RPEAnswers);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Serializer.SerializeToBytes(data);
    }

    private static ArrayList<ISSRPEAnswers> GetAllRPEAnswers() {
        ArrayList<ISSRPEAnswers> result = new ArrayList<>();
        // A "projection" defines the columns that will be returned for each row
        String[] mProjection =
                {
                        ISSContentProvider._ID,
                        ISSContentProvider.MEASUREMENT_ID,
                        ISSContentProvider.RPE_ANSWERS
                };

        // Defines a string to contain the selection clause
        String mSelectionClause = null;

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
        String mSortOrder = ISSContentProvider._ID + " ASC";

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
}
