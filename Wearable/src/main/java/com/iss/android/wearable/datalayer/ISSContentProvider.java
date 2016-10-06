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
    static final String RECORDS_URL = "content://" + PROVIDER_NAME + "/records";
    static final Uri RECORDS_CONTENT_URI = Uri.parse(RECORDS_URL);
    static final String MEASUREMENT_URL = "content://" + PROVIDER_NAME + "/measurement";
    static final Uri MEASUREMENT_CONTENT_URI = Uri.parse(MEASUREMENT_URL);
    static final String RPE_URL = "content://" + PROVIDER_NAME + "/rpeanswers";
    static final Uri RPE_CONTENT_URI = Uri.parse(RPE_URL);

    static final String _ID = "_id";
    static final String USERID = "user_id";
    static final String DATE = "date";
    static final String TIMESTAMP = "timestamp";
    static final String EXTRA = "extra";
    static final String VALUE1 = "value1";
    static final String VALUE2 = "value2";
    static final String VALUE3 = "value3";
    static final String MEASUREMENT = "measurement";
    static final String MEASUREMENT_ID = "measurement_id";
    static final String RPE_ANSWERS = "rpe_answers";
    static final String TYPE = "type";
    static final int RECORDSTYPE = 1;
    static final int RECORD_IDTYPE = 2;
    static final int MEASUREMENTSTYPE = 3;
    static final int MEASUREMENT_IDTYPE = 4;
    static final int RPESTYPE = 5;
    static final int RPE_IDTYPE = 6;
    static final UriMatcher uriMatcher;
    static final String DATABASE_NAME = "ISSRecordData";
    static final String RECORDS_TABLE_NAME = "records";
    static final String MEASUREMENTS_TABLE_NAME = "measurements";
    static final String RPE_TABLE_NAME = "RPESets";
    static final int DATABASE_VERSION = 38;
    static final String CREATE_RECORDS_DB_TABLE =
            " CREATE TABLE " + RECORDS_TABLE_NAME + " (" +
                    _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    USERID + " INTEGER NOT NULL, " +
                    MEASUREMENT + " INTEGER NOT NULL, " +
                    DATE + " TEXT NOT NULL, " +
                    TIMESTAMP + " TEXT NOT NULL, " +
                    EXTRA + " TEXT NOT NULL, " +
                    MEASUREMENT_ID + " INTEGER NOT NULL, " +
                    VALUE1 + " TEXT NOT NULL, " +
                    VALUE2 + " TEXT NOT NULL, " +
                    VALUE3 + " TEXT NOT NULL);";
    static final String CREATE_MEASUREMENT_DB_TABLE =
            " CREATE TABLE " + MEASUREMENTS_TABLE_NAME + " (" +
                    _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    TYPE + " TEXT NOT NULL, " +
                    TIMESTAMP + " TEXT NOT NULL);";
    static final String CREATE_RPE_DB_TABLE =
            " CREATE TABLE " + RPE_TABLE_NAME + " (" +
                    _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    MEASUREMENT_ID + " INTEGER NOT NULL, " +
                    RPE_ANSWERS + " BLOB);";
    private static HashMap<String, String> RECORDS_PROJECTION_MAP;
    /**
     * Database specific constant declarations
     */
    private static SQLiteDatabase db;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, "records", RECORDSTYPE);
        uriMatcher.addURI(PROVIDER_NAME, "records/#", RECORD_IDTYPE);
        uriMatcher.addURI(PROVIDER_NAME, "measurement", MEASUREMENTSTYPE);
        uriMatcher.addURI(PROVIDER_NAME, "measurement/#", MEASUREMENT_IDTYPE);
        uriMatcher.addURI(PROVIDER_NAME, "rpeanswers", RPESTYPE);
        uriMatcher.addURI(PROVIDER_NAME, "rpeanswers/#", RPE_IDTYPE);
    }

    public static void clear() {
        db.execSQL("DELETE FROM " + ISSContentProvider.RECORDS_TABLE_NAME + "; " +
                "DELETE FROM " + ISSContentProvider.MEASUREMENTS_TABLE_NAME + "; " +
                "DELETE FROM " + ISSContentProvider.RPE_TABLE_NAME + ";");
        String count = "SELECT count(*) FROM " + ISSContentProvider.RECORDS_TABLE_NAME;
        Cursor mcursor = db.rawQuery(count, null);
        mcursor.moveToFirst();
        int icount = mcursor.getInt(0);
        if (icount > 0) {
            Log.d("Deleted", "not everything");
        } else {
            Log.d("Deleted", "everything");
            // test
        }
    }

    public static SQLiteDatabase getDB() {
        return db;
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

        switch (uriMatcher.match(uri)) {
            case RECORDSTYPE:
                qb.setTables(RECORDS_TABLE_NAME);
                qb.setProjectionMap(RECORDS_PROJECTION_MAP);
                break;

            case RECORD_IDTYPE:
                qb.setTables(RECORDS_TABLE_NAME);
                qb.appendWhere(_ID + "=" + uri.getPathSegments().get(1));
                break;

            case MEASUREMENTSTYPE:
                qb.setTables(MEASUREMENTS_TABLE_NAME);
                qb.setProjectionMap(RECORDS_PROJECTION_MAP);
                break;

            case MEASUREMENT_IDTYPE:
                qb.setTables(MEASUREMENTS_TABLE_NAME);
                qb.appendWhere(_ID + "=" + uri.getPathSegments().get(1));
                break;

            case RPESTYPE:
                qb.setTables(RPE_TABLE_NAME);
                qb.setProjectionMap(RECORDS_PROJECTION_MAP);
                break;

            case RPE_IDTYPE:
                qb.setTables(RPE_TABLE_NAME);
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
            case RECORDSTYPE:
                return "vnd.android.cursor.dir/vnd.example.records";

            /**
             * Get a particular record
             */
            case RECORD_IDTYPE:
                return "vnd.android.cursor.item/vnd.example.records";

            // And so forth
            case MEASUREMENTSTYPE:
                return "vnd.android.cursor.dir/vnd.example.measurements";

            case MEASUREMENT_IDTYPE:
                return "vnd.android.cursor.item/vnd.example.measurements";

            case RPESTYPE:
                return "vnd.android.cursor.dir/vnd.example.rpes";

            case RPE_IDTYPE:
                return "vnd.android.cursor.item/vnd.example.rpes";

            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long rowID;
        switch (uriMatcher.match(uri)) {
            case RECORDSTYPE:
                /**
                 * Add a new ISS record
                 */
                rowID = db.insert(RECORDS_TABLE_NAME, "", values);

                /**
                 * If record is added successfully
                 */

                if (rowID > 0) {
                    Uri _uri = ContentUris.withAppendedId(RECORDS_CONTENT_URI, rowID);
                    getContext().getContentResolver().notifyChange(_uri, null);
                    return _uri;
                }
                throw new SQLException("Failed to add a record into " + uri);
            case MEASUREMENTSTYPE:
                rowID = db.insert(MEASUREMENTS_TABLE_NAME, "", values);

                if (rowID > 0) {
                    Uri _uri = ContentUris.withAppendedId(MEASUREMENT_CONTENT_URI, rowID);
                    getContext().getContentResolver().notifyChange(_uri, null);
                    return _uri;
                }
                throw new SQLException("Failed to add a record into " + uri);
            case RPESTYPE:
                rowID = db.insert(RPE_TABLE_NAME, "", values);

                if (rowID > 0) {
                    Uri _uri = ContentUris.withAppendedId(RPE_CONTENT_URI, rowID);
                    getContext().getContentResolver().notifyChange(_uri, null);
                    return _uri;
                }
                throw new SQLException("Failed to add a record into " + uri);
        }
        throw new SQLException("Failed to add a record into " + uri);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = 0;
        String id = "";

        switch (uriMatcher.match(uri)) {
            case RECORDSTYPE:
                count = db.delete(RECORDS_TABLE_NAME, selection, selectionArgs);
                break;

            case RECORD_IDTYPE:
                id = uri.getPathSegments().get(1);
                count = db.delete(RECORDS_TABLE_NAME, _ID + " = " + id +
                        (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
                break;
            case MEASUREMENTSTYPE:
                count = db.delete(MEASUREMENTS_TABLE_NAME, selection, selectionArgs);
                break;

            case MEASUREMENT_IDTYPE:
                id = uri.getPathSegments().get(1);
                count = db.delete(MEASUREMENTS_TABLE_NAME, _ID + " = " + id +
                        (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
                break;
            case RPESTYPE:
                count = db.delete(RPE_TABLE_NAME, selection, selectionArgs);
                break;

            case RPE_IDTYPE:
                id = uri.getPathSegments().get(1);
                count = db.delete(RPE_TABLE_NAME, _ID + " = " + id +
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
            case RECORDSTYPE:
                count = db.update(RECORDS_TABLE_NAME, values, selection, selectionArgs);
                break;

            case RECORD_IDTYPE:
                count = db.update(RECORDS_TABLE_NAME, values, _ID + " = " + uri.getPathSegments().get(1) +
                        (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

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
            db.execSQL(CREATE_RECORDS_DB_TABLE);
            db.execSQL(CREATE_MEASUREMENT_DB_TABLE);
            db.execSQL(CREATE_RPE_DB_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.d("updated", "now");
            db.execSQL("DROP TABLE IF EXISTS " + RECORDS_TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + MEASUREMENTS_TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + RPE_TABLE_NAME);
            onCreate(db);
        }
    }
}
