package com.iss.android.wearable.datalayer;

import android.database.Cursor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

/**
 * Created by micha on 25.04.2016.
 */
public class ISSDictionary {
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

    public static int getMeasurementNumber(String measurement) {
        switch (measurement) {
            case "resting":
                return MEASUREMENT_HR;
            case "cooldown":
                return MEASUREMENT_HR;
        }
        return 0;
    }

    public static ISSRecordData CursorToISSRecordDate(Cursor mCursor) {
        ISSRecordData record = new ISSRecordData(mCursor.getInt(1),
                mCursor.getInt(2),
                mCursor.getString(3),
                mCursor.getString(4),
                mCursor.getString(5),
                mCursor.getFloat(6),
                mCursor.getFloat(7),
                mCursor.getFloat(8),
                mCursor.getInt(9));
        return record;
    }

    // Convert Map to byte array
    public static byte[] MapToByteArray(HashMap<String, Integer> data) {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(byteOut);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (out != null) {
            try {
                out.writeObject(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return byteOut.toByteArray();
    }

    public static ISSMeasurement CursorToISSMeasurement(Cursor mCursor) {
        ISSMeasurement measurement = new ISSMeasurement(mCursor.getInt(0),
                mCursor.getString(1),
                mCursor.getString(2));
        return measurement;
    }

    public static ISSRPEAnswers CursorToISSRPEAnswers(Cursor mCursor) {
        ISSRPEAnswers measurement = new ISSRPEAnswers(mCursor.getString(1),
                BlobToMap(mCursor.getBlob(2)));
        return measurement;
    }

    private static HashMap<String, Integer> BlobToMap(byte[] blob) {
        HashMap<String, Integer> data2 = new HashMap<>();
        ByteArrayInputStream byteIn = new ByteArrayInputStream(blob);
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(byteIn);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            data2 = (HashMap<String, Integer>) in.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data2;
    }
}
