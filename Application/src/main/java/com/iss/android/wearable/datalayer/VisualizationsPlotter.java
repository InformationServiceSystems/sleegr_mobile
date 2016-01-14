package com.iss.android.wearable.datalayer;

import android.content.Context;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.Date;

/**
 * Created by Euler on 1/12/2016.
 */
public class VisualizationsPlotter {

    public static void Plot(Visualizations vis, GraphView [] graphs, TextView [] labels, Context context){

        for (int i = 0; i < vis.AllSubplots.size(); i++){

            Visualizations.Subplot subplot = vis.AllSubplots.get(i);
            labels[i].setText(subplot.name);

            GraphView graph = graphs[i];
            graph.removeAllSeries();

            for (TimeSeries series: subplot.data){
                PutTimeSeriesToGraph(graph, series, context);
            }

            graph.getGridLabelRenderer().setLabelVerticalWidth(100);

        }

    }

    public static void PutTimeSeriesToGraph(GraphView graph,  TimeSeries series, Context context){

        LineGraphSeries<DataPoint> data_values = new LineGraphSeries<DataPoint>(new DataPoint[] {});
        //data_values.setDrawDataPoints(true);
        data_values.setColor(series.color);
        data_values.setTitle(series.name);

        boolean anything = false;
        for (int i = 0; i < series.Values.size(); i++){

            if (series.Values.get(i).y < 0){
                continue;
            }
            anything = true;

            Date x = series.Values.get(i).x;
            Double y = series.Values.get(i).y;

            data_values.appendData(new DataPoint(x, y), true, series.Values.size());
        }

        if (!anything){
            return;
        }

        graph.addSeries(data_values);

        graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(context));
        //graph.getGridLabelRenderer().setNumHorizontalLabels(3);

        graph.getLegendRenderer().setVisible(true);
        graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);


    }

}
