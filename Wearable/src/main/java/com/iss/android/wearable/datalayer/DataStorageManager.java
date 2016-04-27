package com.iss.android.wearable.datalayer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
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

    // Collects all ISSRecordDatas in the database that haven't been sent to the smartphone yet
    public static ArrayList<ISSRecordData> GetAllFilesToUpload(String UserID){
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
                        ISSContentProvider.VALUE3
                };

        // Defines a string to contain the selection clause
        String mSelectionClause = null;

        // Initializes an array to contain selection arguments
        String[] mSelectionArgs = {""};

        // Define a sorting order for the query results to appear in
        String mSortOrder = ISSContentProvider.TIMESTAMP + " DESC, " + ISSContentProvider.DATE + " DESC";

        Cursor mCursor = MainActivity.getContext().getContentResolver().query(
                ISSContentProvider.CONTENT_URI,    // The content URI of the words table
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

    // Returns the file that contains the sleep data
    public static File GetSleepData() {
        return sleepData;
    }

    public static void insertISSRecordData(ISSRecordData data) {
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

        mNewUri = MainActivity.getContext().getContentResolver().insert(
                ISSContentProvider.CONTENT_URI,   // the user dictionary content URI
                mNewValues                          // the values to insert
        );
    }

    public static int deleteISSRecords() {
        // Defines selection criteria for the rows you want to delete
        String mSelectionClause = "";
        String[] mSelectionArgs = null;

        // Defines a variable to contain the number of rows deleted
        int mRowsDeleted = 0;

        // Deletes the words that match the selection criteria
        mRowsDeleted = MainActivity.getContext().getContentResolver().delete(
                ISSContentProvider.CONTENT_URI,     // the user dictionary content URI
                mSelectionClause,                   // the column to select on
                mSelectionArgs                      // the value to compare to
        );
        return mRowsDeleted;
    }
}
