package com.iss.android.wearable.datalayer;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Michael on 22.03.2016.
 */
public class Databasetestclass {
    static Calendar date = new GregorianCalendar();
    static int i = 0;
    static ContentResolver resolver = MainActivity.getContext().getContentResolver();

    public static void fillWithData() {

        final Timer timer = new Timer();
        TimerTask timerTask = new TimerTask () {
            @Override
            public void run() {
                i++;
                resolver.insert(ISSContentProvider.CONTENT_URI, generateValues());
                if (i>1000){
                    this.cancel();
                    timer.cancel();
                    timer.purge();
                }
            }
        };
        timer.schedule(timerTask, 0, 10);

    }

    private static ContentValues generateValues() {
        ContentValues values = new ContentValues();

        values.put(ISSContentProvider.USERID,
                (20));
        values.put(ISSContentProvider.MEASUREMENT,
                (33));
        values.put(ISSContentProvider.DATE, generateDate());
        values.put(ISSContentProvider.TIMESTAMP, generateTime());
        values.put(ISSContentProvider.EXTRA, "");
        int hrvalue = ((int) (Math.random() * 100) + 60);
        values.put(ISSContentProvider.VALUE1, hrvalue);
        values.put(ISSContentProvider.VALUE2, "0");
        values.put(ISSContentProvider.VALUE3, "0");
        Log.d("Added " + hrvalue + " at", "" + values.get(ISSContentProvider.TIMESTAMP));

        return values;
    }

    private static String generateDate() {
        date.add(Calendar.SECOND, 1);
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
        return sdf.format(date.getTime());
    }

    private static String generateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(date.getTime());
    }
}
