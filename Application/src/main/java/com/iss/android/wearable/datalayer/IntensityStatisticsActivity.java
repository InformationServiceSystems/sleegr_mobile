package com.iss.android.wearable.datalayer;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class IntensityStatisticsActivity extends Activity {

    Visualizations visualizations = null;

    int week = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intensity_statistics);
        ActionBar actionBar = this.getActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        try {

            int indent = 100;

            visualizations = (Visualizations) Serializer.DeserializeFromBytes(getIntent().getExtras().getByteArray("visualizations"));

            VisualizeWeek(week);

        }catch (Exception ex){

        }

    }

    public void visualizeData(Visualizations vis){

        GraphView [] graphs = new GraphView[]{
                (GraphView) findViewById(R.id.graph1),
                (GraphView) findViewById(R.id.graph2),
                (GraphView) findViewById(R.id.graph3),
                (GraphView) findViewById(R.id.graph4)};

        TextView [] labels = new TextView[]{
                (TextView) findViewById(R.id.textV1),
                (TextView) findViewById(R.id.textV2),
                (TextView) findViewById(R.id.textV3),
                (TextView) findViewById(R.id.textV4)};

        VisualizationsPlotter.Plot(vis, graphs, labels, new GraphStyler() {
            @Override
            public void styleGraph(GraphView graphView, Visualizations.Subplot subplot) {

                // set graph label format
                graphView.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
                    @Override
                    public String formatLabel(double value, boolean isValueX) {
                        if (isValueX) {
                            // show normal x values

                            Calendar mCalendar = Calendar.getInstance();
                            mCalendar.setTimeInMillis((long) value);
                            String day = new SimpleDateFormat("EEE").format(mCalendar.getTime());

                            return day;
                        } else {
                            // show currency for y values
                            return super.formatLabel(value, isValueX);
                        }
                    }
                });

                // set the number of labels
                graphView.getGridLabelRenderer().setNumHorizontalLabels(7);

            }
        });

    }




    public void VisualizeWeek(int week){

        Visualizations vis = visualizations.subsetForWeek(week);
        visualizeData(vis);
        TextView weekView = (TextView)findViewById(R.id.weekTextView);
        Calendar cal = Calendar.getInstance();

        cal.add(Calendar.WEEK_OF_YEAR, week);

        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

        Date start = cal.getTime();

        cal.add(Calendar.DAY_OF_MONTH, 6);

        Date end = cal.getTime();
        android.text.format.DateFormat df = new android.text.format.DateFormat();
        weekView.setText("Displaying week: " + DateFormat.format("MM/dd/yyyy", start) + " - " + DateFormat.format("MM/dd/yyyy", end));

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
        else if (id == R.id.next_session){
            week ++;
            VisualizeWeek(week);
        }
        else if (id == R.id.previous_session){
            week --;
            VisualizeWeek(week);
        }
        else if (id == R.id.home_week){
            week = 0;
            VisualizeWeek(week);
        } else if (id == android.R.id.home) {
            onBackPressed();
        }

        return true;
    }
}
