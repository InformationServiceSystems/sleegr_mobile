package com.iss.android.wearable.datalayer;

import android.database.Cursor;
import android.graphics.Color;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

/**
 * Created by micha on 25.04.2016.
 */
class ISSDictionary {
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

    static ISSRecordData CursorToISSRecordData(Cursor mCursor) {
        ISSRecordData record = new ISSRecordData(mCursor.getInt(1),
                mCursor.getInt(2),
                mCursor.getString(3),
                mCursor.getString(4),
                mCursor.getString(5),
                mCursor.getFloat(6),
                mCursor.getFloat(7),
                mCursor.getFloat(8),
                mCursor.getString(9),
                mCursor.getInt(10));
        return record;
    }

    static Date DateStringToDate(String string) {
        DateFormat format = new SimpleDateFormat("EE MMM dd HH:mm:ss z yyyy", Locale.US);
        Date date = new Date();
        try {
            date = format.parse(string);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    static byte[] MapToByteArray(HashMap<String, Integer> answers) {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(byteOut);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (out != null) {
            try {
                out.writeObject(answers);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return byteOut.toByteArray();
    }

    static String dateToDayString(Date date) {
        DateFormat df = new SimpleDateFormat("yyyy.MM.dd");
        return df.format(date);
    }

    public static String dateToTimeString(Date date) {
        DateFormat df = new SimpleDateFormat("hh:mm:ss");
        return df.format(date);
    }

    static String makeTimestampBeautiful(String string) {
        Log.d("received", string);
        DateFormat format = new SimpleDateFormat("EE MMM dd HH:mm:ss z yyyy", Locale.US);
        Date date = new Date();
        try {
            date = format.parse(string);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        format = new SimpleDateFormat("HH:mm");
        return format.format(date);
    }

    static String translate(String type) {
        switch (type) {
            case "TrainingHR":
                return "Training";
            case "MorningHR":
                return "Morning";
            case "EveningHR":
                return "Evening";
            default:
                return type;
        }
    }

    static String convertToFhirDate(String string) {
        DateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        DateFormat formatTime = new SimpleDateFormat("HH:mm:ss z", Locale.US);
        Date date = DateStringToDate(string);
        String dateString = formatDate.format(date) + "T" + formatTime.format(date);
        Log.d("Conceived date", dateString);
        return dateString;
    }

    static int getGraphSeriesColor(String s) {
        switch (s) {
            case "Cooldown":
                return Color.BLUE;
            case "Recovery":
                return Color.GREEN;
            case "TrainingHR":
                return Color.RED;
            default:
                return Color.GRAY;
        }
    }

    public static String convertToFhirDate(String dateString, String timestamp) {
        DateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        DateFormat formatTime = new SimpleDateFormat("HH:mm:ss z", Locale.US);
        Date date = SplitDateStringToDate(dateString, timestamp);
        String timeString = formatDate.format(date) + "T" + formatTime.format(date);
        Log.d("Conceived date", timeString);
        return timeString;
    }

    private static Date SplitDateStringToDate(String dateString, String timestamp) {
        String timeString = dateString + " " + timestamp;
        DateFormat format = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
        Date date = new Date();
        try {
            date = format.parse(timeString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }
}
