package com.iss.android.wearable.datalayer;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class IntensityStatisticsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intensity_statistics);


        GraphView graph = (GraphView) findViewById(R.id.crv_graph);
        double[] intensity_from_data = getIntent().getExtras().getDoubleArray("intensity_from_data");
        double[] intensity_required = getIntent().getExtras().getDoubleArray("intensity_required");
        PutDataToGraph(graph, intensity_from_data, intensity_required, "intensity", "int. req.", 0, 5000);

        graph = (GraphView) findViewById(R.id.rpe_graph);
        double[] rpe_from_data = getIntent().getExtras().getDoubleArray("rpe_from_data");
        double[] rpe_required = getIntent().getExtras().getDoubleArray("rpe_required");
        PutDataToGraph(graph, rpe_from_data, rpe_required, "RPE", "RPE req.",0,10);

    }

    public void PutDataToGraph(GraphView graph, double[] data, double [] req, String label_data, String label_req, double min, double max ){

        LineGraphSeries<DataPoint> data_values = new LineGraphSeries<DataPoint>(new DataPoint[] {});
        data_values.setDrawDataPoints(true);
        data_values.setColor(Color.RED);
        LineGraphSeries<DataPoint> requirements = new LineGraphSeries<DataPoint>(new DataPoint[] {});
        requirements.setColor(Color.GREEN);
        requirements.setDrawDataPoints(true);

        //double [] data = new double[] {1,2,3,4};

        graph.addSeries(data_values);
        graph.addSeries(requirements);

        data_values.setTitle(label_data);
        requirements.setTitle(label_req);
        graph.getLegendRenderer().setVisible(true);
        graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);

        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(min);
        graph.getViewport().setMaxY(max);

        for (int i = 0; i < data.length; i++){
            int lastidx = data.length - i - 1;
            if (data[lastidx] > 0){
                data_values.appendData(new DataPoint(i, data[lastidx]), true, 100);
            }
            if (req[lastidx] > 0) {
                requirements.appendData(new DataPoint(i, req[lastidx]), true, 100);
            }
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_intensity_statistics, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
