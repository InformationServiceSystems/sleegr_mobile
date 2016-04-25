package com.iss.android.wearable.datalayer;

import android.database.Cursor;
import android.net.Uri;
import android.view.View;
import android.widget.SimpleCursorAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by micha on 25.04.2016.
 */
public class DatabaseManager {
    public static ArrayList<List<ISSRecordData>> GetData(String dateAsString, String measurement) {
        int measurementNumber = ISSDictionary.getMeasurementNumber(measurement);
        Uri CONTENT_URI = ISSContentProvider.CONTENT_URI;

        String mSelectionClause = ISSContentProvider.MEASUREMENT + " = ? " + ISSContentProvider.EXTRA + " = ? "
                + ISSContentProvider.TIMESTAMP + " = ?";
        String[] mSelectionArgs = {String.valueOf(measurementNumber), measurement, dateAsString};
        String[] mProjection = {ISSContentProvider._ID, ISSContentProvider.TIMESTAMP, ISSContentProvider.VALUE1};
        String mSortOrder = ISSContentProvider.TIMESTAMP + " DESC";

        // Does a query against the table and returns a Cursor object
        Cursor mCursor = getContentResolver().query(
                CONTENT_URI,                       // The content URI of the database table
                mProjection,                       // The columns to return for each row
                mSelectionClause,                  // Either null, or the word the user entered
                mSelectionArgs,                    // Either empty, or the string the user entered
                mSortOrder);                       // The sort order for the returned rows

        // Some providers return null if an error occurs, others throw an exception
        if (null == mCursor) {
            emptyBox.setVisibility(View.VISIBLE);
            // If the Cursor is empty, the provider found no matches
        } else if (mCursor.getCount() < 1) {
            emptyBox.setVisibility(View.VISIBLE);
        } else {
            // Insert code here to do something with the results
            emptyBox.setVisibility(View.INVISIBLE);
        }

        return null;
    }
}
