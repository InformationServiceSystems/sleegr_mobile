package com.iss.android.wearable.datalayer;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DisplayActivityFile extends Activity {


    ArrayList<List<ISSRecordData>> sessions = null;
    int session = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_activity_file);

        String date = getIntent().getExtras().getString("date");
        String activity = getIntent().getExtras().getString("activity");
        String userID = DataSyncService.itself.UserID;

        sessions = CSVManager.ReadSplitCSVdata(date, userID, activity);

        try {
            DisplaySession(session);
        }catch (Exception ex){
            System.out.println(ex.toString());
        }

    }

    private void DisplaySession(int session) {

        ArrayList<ISSRecordData> data = (ArrayList<ISSRecordData>) sessions.get(session);
        
        TextView textView = (TextView) findViewById(R.id.sessionNumView);

        Visualizations vis = processData(data);

        GraphView[] graphs = new GraphView[]{
                (GraphView) findViewById(R.id.graph1)};

        TextView [] labels = new TextView[]{
                (TextView) findViewById(R.id.textV1)};

        VisualizationsPlotter.Plot(vis, graphs, labels, new GraphStyler() {
            @Override
            public void styleGraph(GraphView graphView, Visualizations.Subplot subplot) {

                // set label formatting
                graphView.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
                    @Override
                    public String formatLabel(double value, boolean isValueX) {
                        if (isValueX) {
                            // show normal x values

                            Calendar mCalendar = Calendar.getInstance();
                            mCalendar.setTimeInMillis((long) value);
                            String time = new SimpleDateFormat("HH:mm").format(mCalendar.getTime());

                            return time;
                        } else {
                            // show currency for y values
                            return super.formatLabel(value, isValueX);
                        }
                    }
                });

                // set number of labels on the horiz axis
                graphView.getGridLabelRenderer().setNumHorizontalLabels(5);

            }
        });

        textView.setText("Displaying session:" + (session + 1) + "/" + (sessions.size() ));

    }



    private Visualizations processData(ArrayList<ISSRecordData> data) {

        Visualizations vis = new Visualizations();
        Visualizations.Subplot subplot = vis.AddGraph("HR visualization");

        ArrayList<ISSRecordData> firstTraining = DataProcessingManager.extractFirstTraining(data);

        double[] avghr = DataProcessingManager.getAVGHR(firstTraining);

        TimeSeries trainingHR = ConvertToTS(firstTraining, "Training");
        subplot.Add(trainingHR, Color.RED);


        TimeSeries avgHR = new TimeSeries("Avg. HR");

        for (int i = 0; i < trainingHR.Values.size(); i++){
            avgHR.AddValue(trainingHR.Values.get(i).x, avghr[0]);
        }

        subplot.Add(avgHR, Color.YELLOW);


        ArrayList<ISSRecordData> lastCooldown = DataProcessingManager.extractLastCooldown(data);

        if (lastCooldown == null){
            return vis;
        }

        TimeSeries cooldownHR = ConvertToTS(lastCooldown, "Cooldown");
        subplot.Add(cooldownHR, Color.BLUE);

        double[] cooldownParameters = DataProcessingManager.getCooldownParameters(lastCooldown);
        TimeSeries exponent = DataProcessingManager.ComputeExponent(cooldownParameters, cooldownHR);
        subplot.Add(exponent, Color.GREEN);

        return vis;

    }

    private TimeSeries ConvertToTS(ArrayList<ISSRecordData> firstTraining, String name) {

        TimeSeries series = new TimeSeries(name);

        for (ISSRecordData recordData: firstTraining){
            if (recordData.MeasurementType != 21){
                continue;
            }

            series.AddValue(recordData.getTimestamp(), recordData.Value1);

        }

        return  series;

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_display_activity_file, menu);
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
        }else if (id == R.id.next_session){
            session ++;
            DisplaySession(session);
        }
        else if (id == R.id.previous_session){
            session --;
            DisplaySession(session);
        }


        return super.onOptionsItemSelected(item);
    }
}
