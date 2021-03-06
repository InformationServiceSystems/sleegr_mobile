/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iss.android.wearable.datalayer;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.iss.android.wearable.datalayer.utils.CredentialsManager;
import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;

/**
 * Receives its own events using a listener API designed for foreground activities. Updates a data
 * item every second while it is open. Also allows user to take a photo and send that as an asset
 * to the paired wearable.
 */
public class MainActivity extends FragmentActivity implements
        ManageDateFragment.OnFragmentInteractionListener {
    static final int NUM_ITEMS = 30;
    private static final String TAG = "MainActivity";
    public static android.content.Context itself;
    MyAdapter mAdapter;
    ViewPager mPager;
    int mCurrentTabPosition = 30;
    PendingIntent pendingInt = null;
    private ListView mDataItemList;
    private Handler mHandler;
    private Calendar date = new GregorianCalendar();
    private final ViewPager.SimpleOnPageChangeListener mPageChangeListener = new ViewPager.SimpleOnPageChangeListener() {

        @Override
        public void onPageSelected(final int position) {
            onTabChanged(mPager.getAdapter(), mCurrentTabPosition, position);
            mCurrentTabPosition = position;
            checkPositon(position);
        }
    };
    private DataUpdateReceiver dataUpdateReceiver;

    // A method which returns a double value as a formatted String in the form d.dd
    static String formatDouble(Double value) {
        if (value == null)
            return "not measured";

        NumberFormat formatter = new DecimalFormat("#0.00");
        return formatter.format(value);
    }

    public static Context getContext() {
        return itself;
    }

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        mHandler = new Handler();
        itself = this;
        setContentView(R.layout.main_activity);
        setupViews();
        // Stores DataItems received by the local broadcaster or from the paired watch.
        MainActivity.DataItemAdapter mDataItemListAdapter = new MainActivity.DataItemAdapter(this, android.R.layout.simple_list_item_1);
        mDataItemList.setAdapter(mDataItemListAdapter);

        mAdapter = new MyAdapter(getSupportFragmentManager());

        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        mPager.setCurrentItem(30);
        checkPositon(29);
        mPager.setOnPageChangeListener(mPageChangeListener);

        //Initialising stuff that happens somewhere else
        UserData.setIdToken(CredentialsManager.getCredentials(getContext()).getIdToken());
        UserData.setRefreshToken(CredentialsManager.getCredentials(getContext()).getRefreshToken());

        TextView text = (TextView) findViewById(R.id.text);
        String datestring = String.valueOf(date.get(GregorianCalendar.DAY_OF_MONTH));
        text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("Date", ISSDictionary.dateToDayString(date.getTime()));
                onShowMeasurements(ISSDictionary.dateToDayString(date.getTime()));
            }
        });
        text.setText(datestring);
        TextView day = (TextView) findViewById(R.id.day);
        day.setText(String.format("%1$tA", date).substring(0, 3).toUpperCase());
        datestring = String.valueOf(date.get(GregorianCalendar.MONTH) + 1) + " / " + String.valueOf(date.get(GregorianCalendar.YEAR));
        TextView year = (TextView) findViewById(R.id.year);
        year.setText(datestring);

        // Watch for button clicks.
        ImageButton button = (ImageButton) findViewById(R.id.left);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mPager.setCurrentItem(mPager.getCurrentItem() - 1);
            }
        });
        button = (ImageButton) findViewById(R.id.right);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mPager.setCurrentItem(mPager.getCurrentItem() + 1);
            }
        });

        if (DataSyncService.itself == null) {
            Intent intent = new Intent(this, DataSyncService.class);
            startService(intent);
        }

        //noinspection WrongConstant
        pendingInt = PendingIntent.getActivity(this, 0, new Intent(getIntent()), getIntent().getFlags());
    }

    // A method which determines the current position of the ViewPager.
    // Depending on the position the button to move left or right is made invisible.
    private void checkPositon(int currentItem) {
        if (currentItem >= 29) {
            ImageButton button = (ImageButton) findViewById(R.id.right);
            button.setVisibility(View.INVISIBLE);
        } else if (currentItem == 0) {
            ImageButton button = (ImageButton) findViewById(R.id.left);
            button.setVisibility(View.INVISIBLE);
        } else {
            ImageButton lbutton = (ImageButton) findViewById(R.id.left);
            ImageButton rbutton = (ImageButton) findViewById(R.id.right);
            lbutton.setVisibility(View.VISIBLE);
            rbutton.setVisibility(View.VISIBLE);
        }
    }

    // If the lower fragment was swiped, change the content of the upper fragment accordingly
    protected void onTabChanged(final PagerAdapter adapter, final int oldPosition, final int newPosition) {
        //Calc if swipe was left to right, or right to left
        if (oldPosition > newPosition) {
            TextView text = (TextView) findViewById(R.id.text);
            date.add(Calendar.DATE, -1);
            String datestring = String.valueOf(date.get(GregorianCalendar.DAY_OF_MONTH));
            text.setText(datestring);
            TextView day = (TextView) findViewById(R.id.day);
            day.setText(String.format("%1$tA", date).substring(0, 3).toUpperCase());
            datestring = String.valueOf(date.get(GregorianCalendar.MONTH) + 1) + " / " + String.valueOf(date.get(GregorianCalendar.YEAR));
            TextView year = (TextView) findViewById(R.id.year);
            year.setText(datestring);
        } else {
            TextView text = (TextView) findViewById(R.id.text);
            date.add(Calendar.DATE, 1);
            String datestring = String.valueOf(date.get(GregorianCalendar.DAY_OF_MONTH));
            text.setText(datestring);
            TextView day = (TextView) findViewById(R.id.day);
            day.setText(String.format("%1$tA", date).substring(0, 3).toUpperCase());
            datestring = String.valueOf(date.get(GregorianCalendar.MONTH) + 1) + " / " + String.valueOf(date.get(GregorianCalendar.YEAR));
            TextView year = (TextView) findViewById(R.id.year);
            year.setText(datestring);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            /*case R.id.button4:
                onShowMeasurements(ISSDictionary.dateToDayString(new Date()));
                return true;
            case R.id.averageValues:
                onShowAverages();
                return true;*/
            case R.id.webAppActivity:
                onStartWebAppActivity();
                return true;
            case R.id.measuringActivity:
                onStartMeasuringActivity();
                return true;
            case R.id.logout:
                try {
                    try {
                        onLogout();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            case R.id.setSchedule:
                onsetSchedule();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void onStartWebAppActivity() {
        Intent intent = new Intent(this, WebAppActivity.class);
        startActivity(intent);
    }

    private void onStartMeasuringActivity() {
        final Intent StartMeasuringActivity = new Intent(this, MeasuringActivity.class);
        startActivity(StartMeasuringActivity);
    }

    private void onShowMeasurements(String dateString) {
        Log.d("DateString", dateString);
        final Intent ShowMeasurements = new Intent(this, MeasurementsActivity.class);
        ShowMeasurements.putExtra("Date", dateString);
        startActivity(ShowMeasurements);
    }

    // A method which starts the SetScheduleActivity
    private void onsetSchedule() {
        final Intent setSchedule = new Intent(this, SetScheduleActivity.class);
        startActivity(setSchedule);
    }

    @Override
    public void changeDisplayDate(Calendar calendar) {
    }

    // A method handling the click on the Server Sync button.
    public void onServerSync(View view) {
        Toast.makeText(this, "Started sync with server", Toast.LENGTH_SHORT).show();
        RecomputeSynchronize();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (dataUpdateReceiver == null) dataUpdateReceiver = new DataUpdateReceiver();
        IntentFilter intentFilter = new IntentFilter(DataSyncService.NEW_MESSAGE_AVAILABLE);
        registerReceiver(dataUpdateReceiver, intentFilter);

    }

    @Override
    public void onPause() {
        super.onPause();
        if (dataUpdateReceiver != null) unregisterReceiver(dataUpdateReceiver);

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    // puts a String to the system message textview
    public void OutputEvent(final String content) {
        final String cont = content;
        showToast(getApplicationContext(), cont, Toast.LENGTH_SHORT);
        if (cont.equals("Received a measurement")) {
            finish();
            startActivity(getIntent());
            this.overridePendingTransition(0, 0);
        }

    }

    private void showToast(final Context context, final String message, final int length) {
        Activity mActivity = (Activity) MainActivity.itself;
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, length).show();
            }
        });
    }

    // A method starting the login activity if no view is supplied.
    public void onLogout() throws IOException, JSONException {
        new LogoutTask().execute();

    }

    /**
     * Sets up UI components and their callback handlers.
     */
    private void setupViews() {
        mDataItemList = (ListView) findViewById(R.id.data_item_list);
        //mStartActivityBtn = findViewById(R.id.start_wearable_activity);
    }

    // A method that starts the quest to retrieve data from the watch if the button is clicked
    public void onWatchSync() {

        if (DataSyncService.itself != null) {
            DataSyncService.itself.RequestDataFromWatch();
        }

    }

    // A method that predicts the recovery based on history data
    public double predictRecovery(ArrayList<Double> pastX, ArrayList<Double> pastY, Double futureX) {


        double result = -1;

        // collect values of past records into bins

        double[] bins = new double[11];
        double[] counts = new double[11];

        for (int i = 0; i < 11; i++) {
            bins[i] = 0;
            counts[i] = 0;
        }

        for (int i = 0; i < pastY.size(); i++) {

            if (pastX.get(i) < 0) {
                continue;
            }

            bins[((int) Math.round(pastX.get(i)))] += pastY.get(i);
            counts[((int) Math.round(pastX.get(i)))] += 1;
        }

        for (int i = 0; i < 11; i++) {
            if (counts[i] == 0) {
                continue;
            }
            bins[i] = bins[i] / counts[i];
        }

        smoothenBins(bins, counts);

        result = bins[((int) Math.round(futureX))];

        return result;

    }

    // A method that smoothens a double array to remove noise
    public void smoothenBins(double[] bins, double[] counts) {

        double cv = 0;

        for (int i = 0; i < bins.length; i++) {
            if (counts[i] > 0) {
                cv = bins[i];
            }
            bins[i] = cv;
        }

        for (int i = bins.length - 1; i >= 0; i--) {
            if (counts[i] > 0) {
                cv = bins[i];
            }
            bins[i] = cv;
        }

    }

    // A method that computes a date given a certain date and an offset in days
    public Date offsetDate(Date input, int offset) {

        Calendar clnd = Calendar.getInstance();
        clnd.setTime(input);
        clnd.add(Calendar.DAY_OF_MONTH, -offset);
        return clnd.getTime();

    }

    public TimeSeries predictTimeSeries(TimeSeries xReq, TimeSeries xActual, TimeSeries yActual, int offset) {

        TimeSeries result = new TimeSeries(yActual.name + ", pred.");

        for (int i = 0; i < xReq.Values.size() - offset; i++) {

            Date date = xReq.Values.get(i).x;
            Double val = xReq.Values.get(i).y;

            TimeSeries xActBefore = xActual.beforeDate(date);
            TimeSeries yActBefore = yActual.beforeDate(date);

            if (xActBefore.Values.size() == 0) {
                continue;
            }

            HashMap<String, Double> predValues = yActBefore.toDictionary();

            ArrayList<Double> xvalues = new ArrayList<>();
            ArrayList<Double> yvalues = new ArrayList<>();

            // generate training set
            for (int j = 0; j < xActBefore.Values.size(); j++) {

                Date locdate = offsetDate(xActBefore.Values.get(j).x, offset);
                double x = xActBefore.Values.get(j).y;

                if (!predValues.containsKey(TimeSeries.formatData(locdate))) {
                    continue;
                }

                double y = predValues.get(TimeSeries.formatData(locdate));

                xvalues.add(x);
                yvalues.add(y);

            }

            double prediction = predictRecovery(xvalues, yvalues, val);
            result.AddValue(offsetDate(date, offset), prediction);
        }

        return result;

    }

    public void onSyncClick(View view) {
        onWatchSync();
    }

    // Sends data of the last 30 days to the server
    private void RecomputeSynchronize() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    // recompute the params for last 60 days
                    new UserParameters(30);

                    DataSyncService.itself.ShareDataWithServer();

                } catch (Exception ex) {
                    OutputEvent(ex.toString());
                }
            }
        }).start();

    }

    private TimeSeries ComputeCompliences(TimeSeries requirements, TimeSeries values, int timewindow) {

        TimeSeries result = new TimeSeries(values.name + ", avg. divergence");

        for (int i = 0; i < values.Values.size(); i++) {

            Date x = requirements.Values.get(i).x;

            TimeSeries req = requirements.beforeDate(x);
            TimeSeries vals = values.beforeDate(x);

            ArrayList<Double> seq1 = new ArrayList<>();
            ArrayList<Double> seq2 = new ArrayList<>();

            // construct data
            HashMap<String, Double> reqVals = req.toDictionary();

            for (int j = 0; j < vals.Values.size(); j++) {

                int last = vals.Values.size() - j - 1;

                Date d = vals.Values.get(last).x;

                if (!reqVals.containsKey(TimeSeries.formatData(d)))
                    continue;

                Double yp = reqVals.get(TimeSeries.formatData(d));
                Double y = vals.Values.get(last).y;

                seq1.add(0, yp);
                seq2.add(0, y);

                if (seq1.size() >= timewindow) {
                    break;
                }

            }

            if (seq1.size() == 0) {
                result.AddValue(x, -1);
                continue;
            }

            result.AddValue(x, ComplienceMeasure(seq1, seq2));

        }


        return result;

    }

    // Computes the normalised difference between to vectors.
    private double ComplienceMeasure(ArrayList<Double> seq1, ArrayList<Double> seq2) {

        double result = 0;

        for (int i = 0; i < seq1.size(); i++) {
            result += Math.abs(seq1.get(i) - seq2.get(i)) / seq1.size();
        }

        return result;

    }

    // Constructs the lower fragment which is responsible for showing the data of the selected day
    public static class SessionsFragment extends Fragment {
        int mNum;

        static SessionsFragment newInstance(int num) {
            SessionsFragment f = new SessionsFragment();

            // Supply num input as an argument.
            Bundle args = new Bundle();
            args.putInt("num", num);
            f.setArguments(args);

            return f;
        }


        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mNum = getArguments() != null ? getArguments().getInt("num") : 1;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            // Inflate the layout for this fragment
            View v = inflater.inflate(R.layout.fragment_pager_list, container, false);
            Calendar date = new GregorianCalendar();
            date.add(Calendar.DATE, -29 + mNum);
            // Fill the GraphView with data for the current date
            DailyData dailyData = new DailyData(date.getTime());
            GraphView graph = (GraphView) v.findViewById(R.id.graphtoday);
            TextView text = (TextView) v.findViewById(R.id.textV1);
            new PlotGraphsTask(graph, text, v.getContext(), date.getTime()).execute(dailyData);
            TextView[] labels = new TextView[]{text};
            HashMap<String, Double[]> sleepData = CSVManager.ReadSleepData();
            Log.d("Sleep data", sleepData.toString());
            Log.d("Access Token", CredentialsManager.getCredentials(getContext()).getAccessToken());
            Log.d("ID Token", CredentialsManager.getCredentials(getContext()).getIdToken());
            Log.d("Refresh Token", CredentialsManager.getCredentials(getContext()).getRefreshToken());

            // Fill the TextViews below with the appropriate data
            /*TextView intctr = (TextView) v.findViewById(R.id.intensityCtr);
            intctr.setText("Intensity ctr.: " + formatDouble(dailyData.getAlpha2min()));
            TextView intensity = (TextView) v.findViewById(R.id.intensity);
            intensity.setText("Intensity: " + formatDouble(dailyData.getAlphaAllData()));

            TextView meHR = (TextView) v.findViewById(R.id.morningEveningHR);
            meHR.setText("HR: " + formatDouble(dailyData.getMorningHR()) + " / " + formatDouble(dailyData.getEveningHR()));

            TextView dalda = (TextView) v.findViewById(R.id.dalda);
            dalda.setText("DALDA scale: " + formatDouble(dailyData.getDALDA()));
            TextView rpe = (TextView) v.findViewById(R.id.rpe);
            rpe.setText("RPE scale: " + formatDouble(dailyData.getRPE()));
            TextView sleep = (TextView) v.findViewById(R.id.sleep);
            sleep.setText("Deep Sleep Cycles: " + formatDouble(dailyData.getDeepSleep()));*/
            return v;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
        }

        public void refresh() {
            newInstance(mNum);
        }
    }

    private static class PlotGraphsTask extends AsyncTask<DailyData, Void, Void> {
        public GraphView graph;
        public TextView text;
        public Context context;
        public Date time;
        public ArrayList<ArrayList<Date>> Times;
        public ArrayList<ArrayList<Float>> Values;
        private ArrayList<String> Types;

        public PlotGraphsTask(GraphView arggraph, TextView argtext, Context argcontext, Date time) {
            this.graph = arggraph;
            this.text = argtext;
            this.context = argcontext;
            this.time = time;
            Times = new ArrayList<>();
            Values = new ArrayList<>();
            Types = new ArrayList<>();
        }

        protected Void doInBackground(DailyData... cooldown) {
            ArrayList<Integer> measurementIdList = queryForMeasurements(time);
            for (Integer measurementId : measurementIdList) {
                queryForRecords(measurementId);
            }
            return null;
        }

        private void queryForRecords(Integer measurementId) {
            ArrayList<ISSRecordData> data = new ArrayList<>();
            ArrayList<Float> values = new ArrayList<>();
            ArrayList<Date> times = new ArrayList<>();
            Uri CONTENT_URI = ISSContentProvider.RECORDS_CONTENT_URI;

            String mSelectionClause = ISSContentProvider.MEASUREMENT_ID + " = " + measurementId + " AND " + ISSContentProvider.MEASUREMENT + " = 21";
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
                    Log.d("Found", record.toString());
                }
                mCursor.close();
            }
            for (ISSRecordData d : data) {
                times.add(d.getTimestamp());
                values.add(d.Value1);
            }
            Times.add(times);
            Values.add(values);
        }

        private ArrayList<Integer> queryForMeasurements(Date time) {
            ArrayList<Integer> measurementIdList = new ArrayList<>();
            String mSelectionClause = "";
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
                    if (ISSDictionary.dateToDayString(ISSDictionary.DateStringToDate(mCursor.getString(1))).equals(ISSDictionary.dateToDayString(time))) {
                        Log.d("Found measurement", ISSDictionary.dateToDayString(ISSDictionary.DateStringToDate(mCursor.getString(1))) + " for day " + ISSDictionary.dateToDayString(time));
                        measurementIdList.add(mCursor.getInt(0));
                        Types.add(mCursor.getString(2));
                    }
                }
                mCursor.close();
            }
            return measurementIdList;
        }

        @Override
        protected void onPostExecute(Void result) {
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
            /*graph.getViewport().setXAxisBoundsManual(true);*/
            graph.getViewport().setYAxisBoundsManual(true);
            graph.getViewport().setMinY(30);
            graph.getViewport().setMaxY(200);
            for (int j = 0; j < Types.size(); j++) {
                graph.getGridLabelRenderer().setNumHorizontalLabels(5);
                if (Times.get(j) != null && Times.get(j).size() > 0) {
                    LineGraphSeries<DataPoint> series = new LineGraphSeries<>();
                    for (int i = 0; i < Times.get(j).size(); i++) {
                        series.appendData(new DataPoint(Times.get(j).get(i), Values.get(j).get(i)), true, Times.get(j).size());
                        Log.d(Times.get(j).get(i).toString(), Values.get(j).get(i).toString());
                    }
                    series.setColor(ISSDictionary.getGraphSeriesColor(Types.get(j)));
                    series.setTitle(Types.get(j));
                    graph.addSeries(series);
                }
            }

        }
    }

    // Adapter for the Fragments
    public static class MyAdapter extends FragmentStatePagerAdapter {
        public MyAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return NUM_ITEMS;
        }

        @Override
        public Fragment getItem(int position) {
            return SessionsFragment.newInstance(position);
        }
    }

    /**
     * A View Adapter for presenting the Event objects in a list
     */
    private static class DataItemAdapter extends ArrayAdapter<Event> {

        private final Context mContext;

        public DataItemAdapter(Context context, int unusedResource) {
            super(context, unusedResource);
            mContext = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(android.R.layout.two_line_list_item, null);
                convertView.setTag(holder);
                holder.text1 = (TextView) convertView.findViewById(android.R.id.text1);
                holder.text2 = (TextView) convertView.findViewById(android.R.id.text2);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            Event event = getItem(position);
            holder.text1.setText(event.title);
            holder.text2.setText(event.text);
            return convertView;
        }

        private class ViewHolder {

            TextView text1;
            TextView text2;
        }
    }

    // this is used to communicate with Service
    private class DataUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(DataSyncService.NEW_MESSAGE_AVAILABLE)) {
                OutputEvent(intent.getExtras().getString("message"));
            }
        }
    }

    private class Event {

        String title;
        String text;

        public Event(String title, String text) {
            this.title = GetTimeNow();
            this.text = text;
        }

        public String GetTimeNow() {

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HH:mm:ss");
            String currentDateandTime = sdf.format(new Date());
            return currentDateandTime;

        }
    }

    class LogoutTask extends AsyncTask<String, Void, Void> {

        private Exception exception;

        protected Void doInBackground(String... urls) {
            try {

                Log.d("entered", "doinBackground");
                String uri = getString(R.string.server_logout);
                URL object = new URL(uri);

                JSONObject userJSON = new JSONObject();
                userJSON.put("deviceID", UserData.getDeviceID());

                String header = "bearer ";
                String Token = CredentialsManager.getCredentials(MainActivity.getContext()).getIdToken();
                header += Token;

                HttpURLConnection con = (HttpURLConnection) object.openConnection();
                con.setDoOutput(true);
                con.setDoInput(true);
                con.setRequestProperty("Content-Type", "application/json");
                con.setRequestProperty("Accept", "application/json");
                con.setRequestProperty("Authorization", header);
                con.setRequestMethod("POST");
                OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
                wr.write(userJSON.toString());
                wr.flush();

                StringBuilder sb = new StringBuilder();
                int HttpResult = con.getResponseCode();
                if (HttpResult == HttpURLConnection.HTTP_OK) {
                    Log.d("if", "entered");
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(con.getInputStream(), "utf-8"));
                    String line = null;
                    while ((line = br.readLine()) != null) {
                        sb.append(line + "\n");
                    }
                    br.close();
                    System.out.println("" + sb.toString());
                    Log.d("message", sb.toString());
                    Log.d("Server response", sb.toString());
                } else {
                    Log.d("else", "entered");
                    Log.d("HTTP response", String.valueOf(HttpResult));
                    System.out.println(con.getResponseMessage());
                    Log.d("Server response", sb.toString());
                }
            } catch (Exception e) {
                this.exception = e;
                Log.e("error", e.toString());
            }
            return null;
        }

        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            CredentialsManager.deleteCredentials(getContext());
            getContext().startActivity(new Intent(getContext(), Auth0Activity.class));
            // TODO: check this.exception
            // TODO: do something with the feed
        }
    }

}
