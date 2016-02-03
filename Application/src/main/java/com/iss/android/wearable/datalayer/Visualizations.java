package com.iss.android.wearable.datalayer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Euler on 1/12/2016.
 */
public class Visualizations implements Serializable {

    static final long serialVersionUID = 1L;

    public class Subplot implements Serializable{
        static final long serialVersionUID = 1L;

        public String name;
        public ArrayList<TimeSeries> data = new ArrayList<>();
        private Date[] bounds;

        public void Add(TimeSeries series, int color){
            series.color = color;
            data.add(series);
        }

        public Date [] min_max_x(){

            if (data.size() == 0)
                return null;

            Date [] result = null;

            for (TimeSeries series: data){

                Date[] dates = series.min_max();

                if (dates == null)
                    continue;

                if (result == null)
                {
                    result = dates;
                    continue;
                }

                if (result[0].getTime() > dates[0].getTime())
                    result[0] = dates[0];

                if (result[1].getTime() < dates[1].getTime())
                    result[1] = dates[1];

            }

            return result;

        }

        public Date[] getBounds() {
            return bounds;
        }

        public void setBounds(Date [] dates){
            bounds = dates;
        }
    }

    public Visualizations subsetForWeek(int week) {


        Calendar cal = Calendar.getInstance();

        cal.add(Calendar.WEEK_OF_YEAR, week);

        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

        Date start = cal.getTime();

        cal.add(Calendar.DAY_OF_MONTH, 6);

        /*cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 0);*/

        Date end = cal.getTime();

        Visualizations vis = new Visualizations();


        for (Visualizations.Subplot subplot: this.AllSubplots){

            Subplot nsub = vis.AddGraph(subplot);

            for (TimeSeries series: subplot.data){
                nsub.data.add( series.inTimeRange(start, end) );
            }

            nsub.setBounds(new Date[]{start, end});

        }

        return vis;

    }

    private Subplot AddGraph(Subplot subplot) {

        Subplot result = new Subplot();
        result.name = subplot.name;
        AllSubplots.add(result);

        return result;
    }



    public Subplot AddGraph(String name){

        Subplot result = new Subplot();
        result.name = name;
        AllSubplots.add(result);

        return  result;

    }

    public ArrayList<Subplot> AllSubplots = new ArrayList<>();

}
