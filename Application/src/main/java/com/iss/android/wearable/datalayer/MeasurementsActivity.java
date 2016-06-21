package com.iss.android.wearable.datalayer;

import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class MeasurementsActivity extends ListActivity  {

    TextView content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measurements);

        List<Integer> measurementIDs = new ArrayList<>();
        String mSelectionClause = "";
        Calendar date = new GregorianCalendar();
        Date time = date.getTime();
        String dateString = ISSDictionary.dateToDayString(time);
        String[] mSelectionArgs = {};
        Uri CONTENT_URI = ISSContentProvider.MEASUREMENT_CONTENT_URI;
        String[] mProjection =
                {
                        ISSContentProvider._ID,
                        ISSContentProvider.TIMESTAMP
                };
        String mSortOrder = ISSContentProvider.TIMESTAMP + " ASC";

        Cursor mCursor = MainActivity.getContext().getContentResolver().query(
                CONTENT_URI,                       // The content URI of the database table
                mProjection,                       // The columns to return for each row
                mSelectionClause,                  // Either null, or the word the user entered
                mSelectionArgs,                    // Either empty, or the string the user entered
                mSortOrder);
        // Some providers return null if an error occurs, others throw an exception
        if (null == mCursor) {
            // If the Cursor is empty, the provider found no matches
        } else if (mCursor.getCount() < 1) {
            // If the Cursor is empty, the provider found no matches
        } else {
            while (mCursor.moveToNext()) {
                if (ISSDictionary.dateToDayString(ISSDictionary.DateStringToDate(mCursor.getString(1))).equals(dateString)) {
                    measurementIDs.add(mCursor.getInt(0));
                    Log.d("Found", String.valueOf(mCursor.getInt(0)));
                }
            }
        }
        Integer[] values = measurementIDs.toArray(new Integer[measurementIDs.size()]);

        // Define a new Adapter
        // First parameter - Context
        // Second parameter - Layout for the row
        // Third - the Array of data

        ListView MeasurementsList = (ListView) findViewById(android.R.id.list);

        ListAdapter customAdapter = new ListAdapter(this, R.layout.listitem_measurements, values);

        // Assign adapter to List
        MeasurementsList.setAdapter(customAdapter);
    }


    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

        super.onListItemClick(l, v, position, id);

        // ListView Clicked item index
        int itemPosition     = position;

        // ListView Clicked item value
        String  itemValue    = (String) l.getItemAtPosition(position);

        // content.setText("Click : \n  Position :"+itemPosition+"  \n  ListItem : " +itemValue);

    }

    public class ListAdapter extends ArrayAdapter<Integer> {

        private Integer[] items;

        public ListAdapter(Context context, int resource, Integer[] items) {
            super(context, resource, items);
            this.items = items;
            Log.d("Array", Arrays.toString(this.items));
        }

        @Override
        public Integer getItem(int position)
        {
            Log.d("Requested item", String.valueOf(position));
            return items[position];
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View v = convertView;

            if (v == null) {
                LayoutInflater vi;
                vi = LayoutInflater.from(getContext());
                v = vi.inflate(R.layout.listitem_measurements, null);
            }

            int p = getItem(position);

            if (p != 0) {
                TextView measurementslist_TextView = (TextView) v.findViewById(R.id.measurementslist_TextView);
                Log.d("Call", measurementslist_TextView.toString());
                Calendar date = new GregorianCalendar();
                // Fill the GraphView with data for the current date
                DailyData dailyData = new DailyData(date.getTime());
                Log.d("Requested view for", String.valueOf(p));
                GraphView graph = (GraphView) v.findViewById(R.id.output);
                new PlotGraphsTask(graph, v.getContext(), p, measurementslist_TextView).execute(dailyData);
            }

            return v;
        }

        private class PlotGraphsTask extends AsyncTask<DailyData, Void, Void> {
            public GraphView graph;
            public Context context;
            public Integer MID;
            public ArrayList<Date> Times;
            public ArrayList<Float> Values;
            public String measurementType;
            public TextView measurementslist_TextView;

            public PlotGraphsTask(GraphView arggraph, Context argcontext, Integer p, TextView measurementslist_TextView) {
                Log.d("Receive", measurementslist_TextView.toString());
                this.graph = arggraph;
                this.context = argcontext;
                this.MID = p;
                Times = new ArrayList<>();
                Values = new ArrayList<>();
                this.measurementslist_TextView = measurementslist_TextView;
            }

            protected Void doInBackground(DailyData... cooldown) {
                ArrayList<ISSRecordData> data = new ArrayList<ISSRecordData>();
                Uri CONTENT_URI = ISSContentProvider.RECORDS_CONTENT_URI;

                String mSelectionClause = ISSContentProvider.MEASUREMENT_ID + " = "+MID+" AND " + ISSContentProvider.MEASUREMENT + " = 21";
                String[] mSelectionArgs = {};
                String[] mProjection =
                        {
                                ISSContentProvider._ID,
                                ISSContentProvider.USERID,
                                ISSContentProvider.MEASUREMENT,
                                ISSContentProvider.DATE,
                                ISSContentProvider.TIMESTAMP,
                                ISSContentProvider.EXTRA,
                                ISSContentProvider.VALUE1,
                                ISSContentProvider.VALUE2,
                                ISSContentProvider.VALUE3,
                                ISSContentProvider.MEASUREMENT_ID
                        };
                String mSortOrder = ISSContentProvider.TIMESTAMP + " ASC";

                // Does a query against the table and returns a Cursor object
                Cursor mCursor = MainActivity.getContext().getContentResolver().query(
                        CONTENT_URI,                       // The content URI of the database table
                        mProjection,                       // The columns to return for each row
                        mSelectionClause,                  // Either null, or the word the user entered
                        mSelectionArgs,                    // Either empty, or the string the user entered
                        mSortOrder);                       // The sort order for the returned rows

                // Some providers return null if an error occurs, others throw an exception
                if (null == mCursor) {
                    // If the Cursor is empty, the provider found no matches
                } else if (mCursor.getCount() < 1) {
                    // If the Cursor is empty, the provider found no matches
                } else {
                    while (mCursor.moveToNext()) {
                        ISSRecordData record = ISSDictionary.CursorToISSRecordData(mCursor);
                        data.add(record);
                        measurementType = record.ExtraData;
                        Log.d("Found", record.toString());
                    }
                }
                for (ISSRecordData d: data){
                    Times.add(d.getTimestamp());
                    Values.add(d.Value1);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                if (measurementType.equals("Cooldown") || measurementType.equals("Recovery") || measurementType.equals("EveningHR") || measurementType.equals("MorningHR")){
                graph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
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
                graph.getGridLabelRenderer().setNumHorizontalLabels(5);
                if(Times != null && Times.size() > 0) {
                    LineGraphSeries<DataPoint> series = new LineGraphSeries<>();
                    for (int i = 0; i < Times.size(); i++) {
                        series.appendData(new DataPoint(Times.get(i), Values.get(i)), true, Times.size());
                        Log.d(Times.get(i).toString(), Values.get(i).toString());
                    }
                    graph.getViewport().setMinX(Times.get(0).getTime());
                    graph.getViewport().setMaxX(Times.get(Times.size()-1).getTime());
                    graph.getViewport().setXAxisBoundsManual(true);
                    graph.getViewport().setYAxisBoundsManual(true);
                    graph.getViewport().setMinY(30);
                    graph.getViewport().setMaxY(200);
                    series.setColor(Color.BLUE);
                    graph.addSeries(series);
                }
                    Log.d("Fill", measurementslist_TextView.toString());
                    String text = measurementType + " measurement taken at: " + ISSDictionary.dateToTimeString(Times.get(0));
                    this.measurementslist_TextView.setText(text);
                }
                else {
                    String text = measurementType + " measurement";
                    this.measurementslist_TextView.setText(text);
                }

            }
        }

    }
}
