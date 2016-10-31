package com.iss.android.wearable.datalayer;

import android.content.ContentResolver;
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

    public static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd_HH:mm:ss");
    static final long serialVersionUID = 1L;
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
    static ContentResolver resolver = MainActivity.getContext().getContentResolver();
    public int UserID;
    public int MeasurementType;
    public String Date;
    public String Timestamp;
    public String ExtraData = null;
    public float Value1;
    public float Value2;
    public float Value3;
    public String Sensor;
    public int measurementID;

    public ISSRecordData(int UID, int MType, String date, String timestamp, String extraData, float v1, float v2, float v3, String sensor, int measurementNumber) {

        UserID = UID;
        MeasurementType = MType;
        Date = date;
        Timestamp = timestamp;
        ExtraData = extraData;
        Value1 = v1;
        Value2 = v2;
        Value3 = v3;
        Sensor = sensor;
        measurementID = measurementNumber;

    }

    // Converts the ISSRecordData to a String, the way it is stored in a *.csv
    public String toString() {

        String sep = ",";
        return UserID + sep + MeasurementType + sep + Date + "_" + Timestamp + sep + ExtraData + sep + Value1 + sep + Value2 + sep + Value3;

    }

    // A method that returns the date of the given ISSRecordData
    public Date getTimestamp() {

        Calendar time = Calendar.getInstance();

        try {
            Log.d("Timestamp pre", this.Date + "_" + this.Timestamp);
            time.setTime(sdf.parse(this.Date + "_" + this.Timestamp));
        } catch (ParseException e) {

        }

        Log.d("Timestamp post", time.getTime().toString());
        return time.getTime();
    }

    public String getSensorDeviceName() {
        return Sensor;
    }
}
