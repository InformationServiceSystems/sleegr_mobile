package com.iss.android.wearable.datalayer;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.util.Log;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Euler on 10/8/2015.
 */
public class ISSRecordData implements Serializable {

    static final long serialVersionUID = 1L;
    static ContentResolver resolver = MainActivity.getContext().getContentResolver();

    static final int MEASUREMENT_HR = 21,
            MEASUREMENT_ACCELEROMETER = 1,
            MEASUREMENT_GPS = 512,
            MEASUREMENT_ALPHA_NOISY = 31,
            MEASUREMENT_ALPHA = 32,
            MEASUREMENT_HR_MORNING = 33,
            MEASUREMENT_HR_EVENING = 34,
            MEASUREMENT_RPE = 35,
            MEASUREMENT_DALDA = 36,
            MEASUREMENT_DEEP_SLEEP = 37,
            MEASUREMENT_SLEEP_LENGTH = 38,
            MEASUREMENT_TRAINING_END = 13,
            MEASUREMENT_STEPS = 39;

    public int UserID;
    public int MeasurementType;
    public String Timestamp;
    public String ExtraData = null;
    public float Value1;
    public float Value2;
    public float Value3;

    public static ISSRecordData fromString(String str){

        String[] split = str.split(",");

        if (split == null){
            return null;
        }

        int UID = Integer.parseInt(split[0]);
        int MType = Integer.parseInt(split[1]);
        String timestamp = split[2];
        String extraData = split[3];
        float v1 = Float.parseFloat(split[4]);
        float v2 = Float.parseFloat(split[5]);
        float v3 = Float.parseFloat(split[6]);

        return new ISSRecordData(UID, MType, timestamp, extraData, v1, v2, v3);

    }

    // Converts the ISSRecordData to a String, the way it is stored in a *.csv
    public String toString(){

        String sep = ",";
        return UserID + sep + MeasurementType + sep + Timestamp + sep + ExtraData + sep + Value1 + sep + Value2 + sep + Value3;

    }

    public ISSRecordData(int UID, int MType, String timestamp, String extraData, float v1, float v2, float v3){

        UserID = UID;
        MeasurementType = MType;
        Timestamp = timestamp;
        ExtraData = extraData;
        Value1 = v1;
        Value2 = v2;
        Value3 = v3;

    }

    public static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd_HH:mm:ss");

    // A method that returns the date of the given ISSRecordData
    public Date getTimestamp(){

        Calendar time = Calendar.getInstance();

        try {
            time.setTime(sdf.parse(this.Timestamp));
        }
        catch (ParseException e)
        {

        }

        return time.getTime();

    }


    public void saveToContentProvider() {
        /*ISSContentProvider provider = new ISSContentProvider();
        ContentValues values = new ContentValues();
        values.put(ISSContentProvider.USERID, UserID);
        values.put(ISSContentProvider.MEASUREMENT, MeasurementType);
        values.put(ISSContentProvider.TIMESTAMP, Timestamp);
        values.put(ISSContentProvider.EXTRA, ExtraData);
        values.put(ISSContentProvider.VALUE1, Value1);
        values.put(ISSContentProvider.VALUE2, Value2);
        values.put(ISSContentProvider.VALUE3, Value3);

        resolver.insert(ISSContentProvider.CONTENT_URI, values);

        Log.d("Inserting", "a value");*/
    }

    public static void saveToContentProvider(String data) {
        ContentValues values = new ContentValues();
        String[] valuesAsString = data.split(",");

        values.put(ISSContentProvider.USERID,
                (Integer.valueOf(valuesAsString[0])));
        values.put(ISSContentProvider.MEASUREMENT,
                (Integer.valueOf(valuesAsString[1])));
        values.put(ISSContentProvider.TIMESTAMP, valuesAsString[2]);
        values.put(ISSContentProvider.EXTRA, valuesAsString[3]);
        values.put(ISSContentProvider.VALUE1, valuesAsString[4]);
        values.put(ISSContentProvider.VALUE2, valuesAsString[5]);
        values.put(ISSContentProvider.VALUE3, valuesAsString[6]);

        Uri uri = resolver.insert(
                ISSContentProvider.CONTENT_URI, values);
    }
}
