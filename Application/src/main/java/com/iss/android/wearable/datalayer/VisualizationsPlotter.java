package com.iss.android.wearable.datalayer;

import android.content.Context;
import android.util.Log;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Euler on 1/12/2016.
 */
public class VisualizationsPlotter {

    static double maxX = 0;

    public static void Plot(Visualizations vis, GraphView[] graphs, TextView[] labels, Context context, String range) {

        for (int i = 0; i < vis.AllSubplots.size(); i++) {

            Visualizations.Subplot subplot = vis.AllSubplots.get(i);
            labels[i].setText(subplot.name);

            GraphView graph = graphs[i];
            graph.removeAllSeries();

            for (TimeSeries series : subplot.data) {
                PutTimeSeriesToGraph(graph, series, context, range);
            }

            graph.getGridLabelRenderer().setLabelVerticalWidth(100);

        }

    }

    public static void PutTimeSeriesToGraph(GraphView graph, TimeSeries series, Context context, String range) {

        LineGraphSeries<DataPoint> data_values = new LineGraphSeries<DataPoint>(new DataPoint[]{});
        //data_values.setDrawDataPoints(true);
        data_values.setColor(series.color);
        data_values.setTitle(series.name);

        boolean anything = false;
        for (int i = 0; i < series.Values.size(); i += 1) {

            if (series.Values.get(i).y < 0) {
                continue;
            }
            anything = true;

            Date x = series.Values.get(i).x;
            Double y = series.Values.get(i).y;
            if (range.equals("week")) {
                x = getDateWithOutTime(x);
                SimpleDateFormat daydf = new SimpleDateFormat("dd.MM kk:mm");
                Log.d("Date", daydf.format(x));
            }

            data_values.appendData(new DataPoint(x, y), true, series.Values.size());
        }

        if (data_values.getHighestValueX() > maxX) {
            maxX = data_values.getHighestValueX();
        }

        if (!anything) {
            return;
        }

        graph.addSeries(data_values);

        if (range.equals("week")) {
            graph.getViewport().setMinX(data_values.getLowestValueX());
            graph.getViewport().setMaxX(maxX);
            graph.getViewport().setXAxisBoundsManual(true);
            graph.getGridLabelRenderer().setNumHorizontalLabels(7);
            SimpleDateFormat weekdf = new SimpleDateFormat("EEE");
            graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(context, weekdf));
        }
        if (range.equals("day")) {
            SimpleDateFormat daydf = new SimpleDateFormat("kk:mm");
            graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(context, daydf));
        }
        //graph.getGridLabelRenderer().setNumHorizontalLabels(3);

        graph.getLegendRenderer().setVisible(true);
        graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);


    }

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

}
