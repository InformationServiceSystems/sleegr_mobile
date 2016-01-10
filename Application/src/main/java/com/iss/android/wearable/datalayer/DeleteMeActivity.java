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

public class DeleteMeActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_me);


        LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>(new DataPoint[] {});
        LineGraphSeries<DataPoint> reqs = new LineGraphSeries<DataPoint>(new DataPoint[] {});
        reqs.setColor(Color.GREEN);

        double[] data = getIntent().getExtras().getDoubleArray("data");
        double[] datareq = getIntent().getExtras().getDoubleArray("reqr");

        //double [] data = new double[] {1,2,3,4};

        GraphView graph = (GraphView) findViewById(R.id.graph);

        graph.addSeries(series);
        graph.addSeries(reqs);

        series.setTitle("intensity");
        reqs.setTitle("scheduled intensity");
        graph.getLegendRenderer().setVisible(true);
        graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);

        for (int i = 0; i < data.length; i++){
            series.appendData(new DataPoint( i, data[i] ) , true, 10);
            reqs.appendData(new DataPoint( i, datareq[i] ) , true, 10);
        }





    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_delete_me, menu);
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
