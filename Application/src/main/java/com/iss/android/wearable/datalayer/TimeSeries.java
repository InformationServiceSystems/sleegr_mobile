package com.iss.android.wearable.datalayer;

import android.util.Log;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by Euler on 1/11/2016.
 */
public class TimeSeries implements Serializable {

    static final long serialVersionUID = 1L;
    public enum SeriesLineTypes{Line, Bar}

    public SeriesLineTypes LineType = SeriesLineTypes.Line;

    public class Tuple<X, Y> implements Serializable {
        static final long serialVersionUID = 1L;
        public X x;
        public Y y;
        public Tuple(X x, Y y) {
            this.x = x;
            this.y = y;
        }
    }

    public ArrayList<Tuple<Date, Double>> Values = new ArrayList<>();

    public String name;

    public int color = 0;

    // Gets the first and the last timestamp of a TimeSeries
    public Date [] min_max(){

        if (Values.size() == 0)
            return null;

        Date [] result = new Date [2];
        result[0] = Values.get(0).x;
        result[1] = Values.get(0).x;

        for (Tuple<Date, Double> tuple: Values){

            if (tuple.x.getTime() < result[0].getTime()){
                result[0] = tuple.x;
            }

            if (tuple.x.getTime() > result[1].getTime()){
                result[1] = tuple.x;
            }

        }

        return result;

    }

    // Adds a value to a TimeSeries
    public void AddValue(Date x, double y){
        Values.add(new Tuple<>(x,y));
    }

    public TimeSeries(String name){
        this.name = name;
    }

    public TimeSeries(TimeSeries series){
        this.name = series.name;
        this.color = series.color;
        this.LineType = series.LineType;
    }

    // Adds a value as the first value of a TimeSeries
    public void AddFirstValue(Date x, double y) {
        Values.add(0,new Tuple<>(x,y));
    }

    // Returns a Timeseries made of the values before a given date in the current TimeSeries
    public TimeSeries beforeDate(Date date){

        TimeSeries result = new TimeSeries(this);

        Calendar cl = Calendar.getInstance();
        cl.setTime(date);
        cl.set(Calendar.HOUR_OF_DAY, 23);
        cl.set(Calendar.MINUTE, 59);
        Date cmpdate = cl.getTime();

        for (int i = 0; i < this.Values.size(); i ++){
            if (cmpdate.after( this.Values.get(i).x )){
                result.Values.add(this.Values.get(i));
            }
        }

        return result;

    }

    // Returns a timeSeries made of the values inbetween the given dates
    public TimeSeries inTimeRange(Date startDate, Date endDate){

        TimeSeries result = new TimeSeries(this);

        for (int i = 0; i < this.Values.size(); i ++){
            Date time = this.Values.get(i).x;
            Log.d("Start", startDate.toString());
            Log.d("Zeit", time.toString());
            if ( (startDate.getTime() <= time.getTime()) && (endDate.getTime() >= time.getTime()) ){
                result.Values.add(this.Values.get(i));
            }
        }

        return result;

    }

    public static final SimpleDateFormat dictionary_format = new SimpleDateFormat("yyyy.MM.dd");

    public static String formatData(Date d){

        return dictionary_format.format(d);

    }

    public HashMap<String, Double> toDictionary(){

        HashMap<String, Double> result = new HashMap<>();

        for (int i =0 ; i < this.Values.size(); i++){


            result.put(formatData(Values.get(i).x), this.Values.get(i).y);

        }

        return result;

    }
}
