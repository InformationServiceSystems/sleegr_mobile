package com.iss.android.wearable.datalayer;

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

    public void AddValue(Date x, double y){
        Values.add(new Tuple<>(x,y));
    }

    public TimeSeries(String name){
        this.name = name;
    }

    public TimeSeries(TimeSeries series){
        this.name = series.name;
        this.color = series.color;
    }

    public void AddFirstValue(Date x, double y) {
        Values.add(0,new Tuple<>(x,y));
    }

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


    public TimeSeries inTimeRange(Date startDate, Date endDate){

        TimeSeries result = new TimeSeries(this);

        for (int i = 0; i < this.Values.size(); i ++){
            Date time = this.Values.get(i).x;
            if (startDate.before(time) && endDate.after(time)){
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
