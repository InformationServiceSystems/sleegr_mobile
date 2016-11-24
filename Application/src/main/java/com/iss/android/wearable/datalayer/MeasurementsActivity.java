package com.iss.android.wearable.datalayer;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
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

import static com.iss.android.wearable.datalayer.MainActivity.getContext;

public class MeasurementsActivity extends ListActivity {

    TextView content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measurements);

        Intent intent = getIntent();
        String intentDate = intent.getStringExtra("Date");
        /**
         *Constructs a list of all measurements taken this day.
         */
        List<Integer> measurementIDs = new ArrayList<>();
        List<String> timestampList = new ArrayList<>();
        List<String> typeList = new ArrayList<>();
        String mSelectionClause = "";
        Calendar date = new GregorianCalendar();
        Date time = date.getTime();
        String dateString = ISSDictionary.dateToDayString(time);
        String[] mSelectionArgs = {};
        Uri CONTENT_URI = ISSContentProvider.MEASUREMENT_CONTENT_URI;
        String[] mProjection =
                {
                        ISSContentProvider._ID,
                        ISSContentProvider.TIMESTAMP,
                        ISSContentProvider.TYPE
                };
        String mSortOrder = ISSContentProvider.TIMESTAMP + " ASC";

        Cursor mCursor = getContext().getContentResolver().query(
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
            mCursor.close();
        } else {
            while (mCursor.moveToNext()) {
                if (ISSDictionary.dateToDayString(ISSDictionary.DateStringToDate(mCursor.getString(1))).equals(intentDate)) {
                    measurementIDs.add(mCursor.getInt(0));
                    timestampList.add(ISSDictionary.makeTimestampBeautiful(mCursor.getString(1)));
                    typeList.add(mCursor.getString(2));
                }
            }
            mCursor.close();
        }

        /**
         * 3 values get transferred to the list adapter:
         * 1: The measurement IDs, so I can query the database for the corresponding values
         * 2: The timestamps, so I can display on the upper level when the measurement was made
         * 3: The measurement, so I can display what kind of measurement the data belongs to.
         */

        Integer[] values = measurementIDs.toArray(new Integer[measurementIDs.size()]);
        String[] timestamps = timestampList.toArray(new String[measurementIDs.size()]);
        String[] types = typeList.toArray(new String[measurementIDs.size()]);

        // Define a new Adapter
        // First parameter - Context
        // Second parameter - Layout for the row
        // Third - the Array of data

        ExpandableListView MeasurementsList = (ExpandableListView) findViewById(android.R.id.list);

        ExpandableListAdapter customAdapter = new ExpandableListAdapter(this, R.layout.listitem_group_measurements, values, timestamps, types);

        // Assign adapter to List
        MeasurementsList.setAdapter(customAdapter);
    }

    public class ExpandableListAdapter extends BaseExpandableListAdapter {

        private final int resource;
        private final Context context;
        /**
         * new stuff
         */

        private Integer[] items;
        private Integer[] childItems;
        private String[] measurementTimestamps;
        private String[] types;

        ExpandableListAdapter(Context context, int resource, Integer[] values, String[] timestamps, String[] types) {
            this.context = context;
            this.resource = resource;
            this.items = values;
            this.childItems = new Integer[]{1};
            this.measurementTimestamps = timestamps;
            this.types = types;
            Log.d("Array", Arrays.toString(this.items));
        }

        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }


        public View getChildView(final int groupPosition, final int childPosition,
                                 boolean isLastChild, View convertView, ViewGroup parent) {
            View v = convertView;

            if (v == null) {
                LayoutInflater vi;
                vi = LayoutInflater.from(getContext());
                v = vi.inflate(R.layout.listitem_child_measurements, null);
            }

            int p = (int) getGroup(groupPosition);

            if (p != 0) {
                TextView AValue = (TextView) v.findViewById(R.id.AValue);
                TextView TValue = (TextView) v.findViewById(R.id.TValue);
                TextView CValue = (TextView) v.findViewById(R.id.CValue);
                TextView Load = (TextView) v.findViewById(R.id.Load);
                Calendar date = new GregorianCalendar();
                // Fill the GraphView with data for the current date
                DailyData dailyData = new DailyData(date.getTime());
                Log.d("Requested view for", String.valueOf(p));
                GraphView graph = (GraphView) v.findViewById(R.id.output);
                new PlotGraphsTask(graph, v.getContext(), p, AValue, TValue, CValue, Load).execute(dailyData);
            }
            return v;
        }

        public int getChildrenCount(int groupPosition) {
            return childItems.length;
        }

        public Object getGroup(int groupPosition) {
            Log.d("Requested item", String.valueOf(groupPosition));
            return items[groupPosition];
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return childItems[childPosition];
        }

        public int getGroupCount() {
            return items.length;
        }

        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        public View getGroupView(int groupPosition, boolean isExpanded,
                                 View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater infalInflater = LayoutInflater.from(getContext());
                convertView = infalInflater.inflate(R.layout.listitem_group_measurements,
                        null);
            }
            TextView item = (TextView) convertView.findViewById(R.id.measurementslist_TextView);
            item.setTypeface(null, Typeface.BOLD);
            item.setText(ISSDictionary.translate(types[groupPosition]) + " at " + measurementTimestamps[groupPosition]);
            return convertView;
        }

        public boolean hasStableIds() {
            return true;
        }

        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        public Context getContext() {
            return context;
        }

        public int getResource() {
            return resource;
        }

        private class PlotGraphsTask extends AsyncTask<DailyData, Void, Void> {
            GraphView graph;
            public Context context;
            Integer MID;
            ArrayList<Date> Times;
            ArrayList<Float> HRValues;
            ArrayList<Double> FittedCurve;
            String measurementType;
            double[] CDParams;
            private TextView AValue;
            private TextView TValue;
            private TextView CValue;
            private TextView Load;

            PlotGraphsTask(GraphView arggraph, Context argcontext, Integer p, TextView AValue, TextView TValue, TextView CValue, TextView Load) {
                this.graph = arggraph;
                this.context = argcontext;
                this.MID = p;
                Times = new ArrayList<>();
                HRValues = new ArrayList<>();
                FittedCurve = new ArrayList<>();
                this.AValue = AValue;
                this.TValue = TValue;
                this.CValue = CValue;
                this.Load = Load;
            }

            protected Void doInBackground(DailyData... cooldown) {
                ArrayList<ISSRecordData> data = new ArrayList<ISSRecordData>();
                Uri CONTENT_URI = ISSContentProvider.RECORDS_CONTENT_URI;

                String mSelectionClause = ISSContentProvider.MEASUREMENT_ID + " = " + MID + " AND " + ISSContentProvider.MEASUREMENT + " = 21";
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
                                ISSContentProvider.SENSOR,
                                ISSContentProvider.MEASUREMENT_ID
                        };
                String mSortOrder = ISSContentProvider.TIMESTAMP + " ASC";

                // Does a query against the table and returns a Cursor object
                Cursor mCursor = getContext().getContentResolver().query(
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
                    mCursor.close();
                } else {
                    while (mCursor.moveToNext()) {
                        ISSRecordData record = ISSDictionary.CursorToISSRecordData(mCursor);
                        data.add(record);
                        measurementType = record.ExtraData;
                        Log.d("Found", record.toString());
                    }
                    this.CDParams = DataProcessingManager.getCooldownParameters(data);
                    if (CDParams != null) {
                        for (ISSRecordData d : data) {
                            Times.add(d.getTimestamp());
                            HRValues.add(d.Value1);
                            Log.d("x value", String.valueOf((d.getTimestamp().getTime() - Times.get(0).getTime()) / 1000));
                            Log.d("y value", String.valueOf(ExponentFitter.fExp(CDParams, (d.getTimestamp().getTime() - Times.get(0).getTime()) / 1000)));
                            FittedCurve.add(ExponentFitter.fExp(CDParams, (d.getTimestamp().getTime() - Times.get(0).getTime()) / 1000));
                        }
                    }
                    mCursor.close();
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                if (measurementType.equals("Cooldown")) {
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
                    if (Times != null && Times.size() > 0) {
                        LineGraphSeries<DataPoint> series = new LineGraphSeries<>();
                        for (int i = 0; i < Times.size(); i++) {
                            series.appendData(new DataPoint(Times.get(i), HRValues.get(i)), false, Times.size() + 20);
                        }
                        LineGraphSeries<DataPoint> FittedCurveSeries = new LineGraphSeries<>();
                        for (int i = 0; i < Times.size(); i++) {
                            FittedCurveSeries.appendData(new DataPoint(Times.get(i), FittedCurve.get(i)), false, Times.size() + 20);
                        }
                        graph.getViewport().setMinX(Times.get(0).getTime());
                        graph.getViewport().setMaxX(Times.get(Times.size() - 1).getTime());
                        graph.getViewport().setXAxisBoundsManual(true);
                        graph.getViewport().setYAxisBoundsManual(true);
                        graph.getViewport().setMinY(0);
                        graph.getViewport().setMaxY(200);
                        series.setColor(ISSDictionary.getGraphSeriesColor(measurementType));
                        FittedCurveSeries.setColor(Color.GREEN);
                        graph.addSeries(FittedCurveSeries);
                        graph.addSeries(series);
                    }
                    this.AValue.setText("A: " + String.valueOf(CDParams[0]));
                    this.TValue.setText("T: " + String.valueOf(CDParams[1]));
                    this.CValue.setText("C: " + String.valueOf(CDParams[2]));
                    this.Load.setText("Load: " + String.valueOf(CDParams[0] * CDParams[2]));
                } else if (measurementType.equals("TrainingHR") || measurementType.equals("Recovery") || measurementType.equals("EveningHR") || measurementType.equals("MorningHR")) {
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
                    if (Times != null && Times.size() > 0) {
                        LineGraphSeries<DataPoint> series = new LineGraphSeries<>();
                        for (int i = 0; i < Times.size(); i++) {
                            series.appendData(new DataPoint(Times.get(i), HRValues.get(i)), false, Times.size() + 20);
                        }
                        LineGraphSeries<DataPoint> FittedCurveSeries = new LineGraphSeries<>();
                        for (int i = 0; i < Times.size(); i++) {
                            FittedCurveSeries.appendData(new DataPoint(Times.get(i), FittedCurve.get(i)), false, Times.size() + 20);
                        }
                        graph.getViewport().setMinX(Times.get(0).getTime());
                        graph.getViewport().setMaxX(Times.get(Times.size() - 1).getTime());
                        graph.getViewport().setXAxisBoundsManual(true);
                        graph.getViewport().setYAxisBoundsManual(true);
                        graph.getViewport().setMinY(0);
                        graph.getViewport().setMaxY(200);
                        series.setColor(ISSDictionary.getGraphSeriesColor(measurementType));
                        series.setColor(Color.parseColor("#3b5998"));
                        FittedCurveSeries.setColor(Color.GREEN);
                        //graph.addSeries(FittedCurveSeries);
                        graph.addSeries(series);
                    }
                }

            }
        }

    }
}
