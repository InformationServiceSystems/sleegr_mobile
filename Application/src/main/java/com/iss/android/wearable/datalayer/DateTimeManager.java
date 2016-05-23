package com.iss.android.wearable.datalayer;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Euler on 1/29/2016.
 */
public class DateTimeManager
{

    // A method that determines the date as a string given a certain offset from today.
    public static String getDayFromToday(int dayoffset){

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cl = Calendar.getInstance();
        cl.add(Calendar.DAY_OF_YEAR, -dayoffset);
        String currentDateandTime = df.format(cl.getTime());

        return currentDateandTime;

    }

    // A method retrieving the exact second a day has started.
    private static Date getDateWithOutTime(Date targetDate) {
        Calendar newDate = Calendar.getInstance();
        newDate.setLenient(false);
        newDate.setTime(targetDate);
        newDate.set(Calendar.HOUR_OF_DAY, 0);
        newDate.set(Calendar.MINUTE, 0);
        newDate.set(Calendar.SECOND, 0);
        newDate.set(Calendar.MILLISECOND, 0);

        return newDate.getTime();

    }

    // A method that retrieves the date from today +- a certain offset.
    public static Date getDateFromToday(int dayoffset){

        Calendar cl = Calendar.getInstance();
        cl.add(Calendar.DAY_OF_YEAR, -dayoffset);

        return getDateWithOutTime(cl.getTime());

    }

    public static String format(Date timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd_HH:mm:ss");
        String dateString = sdf.format(timestamp);
        return dateString;
    }
}
