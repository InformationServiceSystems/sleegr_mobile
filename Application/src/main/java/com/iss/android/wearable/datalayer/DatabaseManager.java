package com.iss.android.wearable.datalayer;

import android.database.Cursor;
import android.net.Uri;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by micha on 25.04.2016.
 */
public class DatabaseManager {
    public static ArrayList<List<ISSRecordData>> GetData(String dateAsString, String measurement) {
        return null;
    }

    public static ArrayList<ISSRecordData> getData(Date time) {
        ArrayList<ISSRecordData> data = new ArrayList<ISSRecordData>();
        Uri CONTENT_URI = ISSContentProvider.CONTENT_URI;
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
                ISSRecordData record = ISSDictionary.CursorToISSRecordDate(mCursor);
                data.add(record);
            }
        }

        return data;
    }
}
