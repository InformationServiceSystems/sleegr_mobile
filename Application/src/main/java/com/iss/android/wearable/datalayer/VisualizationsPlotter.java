package com.iss.android.wearable.datalayer;

import android.content.Context;
import android.widget.TextView;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LabelFormatter;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.BaseSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Euler on 1/12/2016.
 */
public class VisualizationsPlotter {

    public static void Plot(Visualizations vis, GraphView [] graphs, TextView [] labels, int numLabels, LabelFormatter labelFormatter){

        for (int i = 0; i < vis.AllSubplots.size(); i++){

            Visualizations.Subplot subplot = vis.AllSubplots.get(i);
            labels[i].setText(subplot.name);

            GraphView graph = graphs[i];

            Date[] dates = subplot.getBounds();
            if (dates != null){
                graph.getViewport().setXAxisBoundsManual(true);
                graph.getViewport().setMinX(dates[0].getTime());
                graph.getViewport().setMaxX(dates[1].getTime());
            }

            graph.removeAllSeries();

            for (TimeSeries series: subplot.data){
                PutTimeSeriesToGraph(graph, series);
            }

            graph.getGridLabelRenderer().setLabelVerticalWidth(120);

            graph.getGridLabelRenderer().setLabelFormatter(labelFormatter);
            graph.getGridLabelRenderer().setNumHorizontalLabels(numLabels);

            graph.getLegendRenderer().setVisible(true);
            graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);





        }

    }

    public static void PutTimeSeriesToGraph(GraphView graph,  TimeSeries series){

        BaseSeries<DataPoint> data_values= null;

        switch (series.LineType){
            case Line:
                data_values = new LineGraphSeries<DataPoint>(new DataPoint[] {});
                break;
            case Bar:
                data_values = new BarGraphSeries<DataPoint>(new DataPoint[] {});
                ((BarGraphSeries<DataPoint>)data_values).setSpacing(20);
                break;
        }

        //data_values.setDrawDataPoints(true);
        data_values.setColor(series.color);
        data_values.setTitle(series.name);

        boolean anything = false;
        for (int i = 0; i < series.Values.size(); i += 1) { // the step needs to be one, otherwise the weekly plot breaks

            if (series.Values.get(i).y < 0){
                continue;
            }
            anything = true;

            Date x = series.Values.get(i).x;
            Double y = series.Values.get(i).y;



            try {
                data_values.appendData(new DataPoint(x, y), true, series.Values.size());
            }catch (Exception ex)
            {
                System.out.print(ex.toString());
            }
        }

        if (!anything){
            return;
        }

        graph.addSeries(data_values);

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
