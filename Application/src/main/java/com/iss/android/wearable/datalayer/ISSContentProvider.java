package com.iss.android.wearable.datalayer;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.util.HashMap;

/**
 * Created by Michael on 14.03.2016.
 */
public class ISSContentProvider extends ContentProvider {

    static final String PROVIDER_NAME = "com.iss.android.wearable.datalayer.provider";
    static final String URL = "content://" + PROVIDER_NAME + "/records";
    static final Uri CONTENT_URI = Uri.parse(URL);

    static final String _ID = "_id";
    static final String USERID = "UserID";
    static final String TIMESTAMP = "timestamp";
    static final String EXTRA = "extra";
    static final String VALUE1 = "value1";
    static final String VALUE2 = "value2";
    static final String VALUE3 = "value3";
    static final String MEASUREMENT = "Measurement";

    private static HashMap<String, String> RECORDS_PROJECTION_MAP;

    static final int RECORDS = 1;
    static final int RECORD_ID = 2;

    static final UriMatcher uriMatcher;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, "records", RECORDS);
        uriMatcher.addURI(PROVIDER_NAME, "records/#", RECORD_ID);
    }

    /**
     * Database specific constant declarations
     */
    private SQLiteDatabase db;
    static final String DATABASE_NAME = "ISSRecordData";
    static final String RECORDS_TABLE_NAME = "records";
    static final int DATABASE_VERSION = 2;
    static final String CREATE_DB_TABLE =
            " CREATE TABLE " + RECORDS_TABLE_NAME + " (" +
                    _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    USERID + " INTEGER NOT NULL, " +
                    MEASUREMENT + " INTEGER NOT NULL, " +
                    TIMESTAMP + " TEXT NOT NULL, " +
                    EXTRA + " TEXT NOT NULL, " +
                    VALUE1 + " TEXT NOT NULL, " +
                    VALUE2 + " TEXT NOT NULL, " +
                    VALUE3 + " TEXT NOT NULL);";

    /**
     * Helper class that actually creates and manages
     * the provider's underlying data repository.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_DB_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + RECORDS_TABLE_NAME);
            onCreate(db);
        }
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();
        DatabaseHelper dbHelper = new DatabaseHelper(context);

        /**
         * Create a write able database which will trigger its
         * creation if it doesn't already exist.
         */
        db = dbHelper.getWritableDatabase();
        return (db != null);
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(RECORDS_TABLE_NAME);

        switch (uriMatcher.match(uri)) {
            case RECORDS:
                qb.setProjectionMap(RECORDS_PROJECTION_MAP);
                break;

            case RECORD_ID:
                qb.appendWhere(_ID + "=" + uri.getPathSegments().get(1));
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        if (sortOrder == null || sortOrder.equals("")) {
            /**
             * By default sort on record IDs
             */
            sortOrder = _ID;
        }
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);

        /**
         * register to watch a content URI for changes
         */
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
            /**
             * Get all ISS records
             */
            case RECORDS:
                return "vnd.android.cursor.dir/vnd.example.records";

            /**
             * Get a particular record
             */
            case RECORD_ID:
                return "vnd.android.cursor.item/vnd.example.records";

            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        /**
         * Add a new ISS record
         */
        Log.d("Inserted ", "something");
        long rowID = db.insert(RECORDS_TABLE_NAME, "", values);

        /**
         * If record is added successfully
         */

        if (rowID > 0) {
            Uri _uri = ContentUris.withAppendedId(CONTENT_URI, rowID);
            getContext().getContentResolver().notifyChange(_uri, null);
            return _uri;
        }
        throw new SQLException("Failed to add a record into " + uri);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = 0;

        switch (uriMatcher.match(uri)) {
            case RECORDS:
                count = db.delete(RECORDS_TABLE_NAME, selection, selectionArgs);
                break;

            case RECORD_ID:
                String id = uri.getPathSegments().get(1);
                count = db.delete(RECORDS_TABLE_NAME, _ID + " = " + id +
                        (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int count = 0;

        switch (uriMatcher.match(uri)) {
            case RECORDS:
                count = db.update(RECORDS_TABLE_NAME, values, selection, selectionArgs);
                break;

            case RECORD_ID:
                count = db.update(RECORDS_TABLE_NAME, values, _ID + " = " + uri.getPathSegments().get(1) +
                        (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
}
