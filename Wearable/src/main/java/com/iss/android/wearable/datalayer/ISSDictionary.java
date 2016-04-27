package com.iss.android.wearable.datalayer;

import android.database.Cursor;

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
        switch (measurement){
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
                        mCursor.getFloat(8));
        return record;
    }
}
