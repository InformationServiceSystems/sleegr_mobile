package com.iss.android.wearable.datalayer;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static com.iss.android.wearable.datalayer.DataSyncService.getUserID;
import static com.iss.android.wearable.datalayer.ISSRecordData.resolver;
import static com.iss.android.wearable.datalayer.MainActivity.getContext;

public class SetScheduleActivity extends Activity implements AdapterView.OnItemSelectedListener {

    Date start = new Date();
    Date end = new Date();
    private int[] SpinnerIds = new int[]
            {R.id.SpinnerOne, R.id.SpinnerTwo, R.id.SpinnerThree, R.id.SpinnerFour, R.id.SpinnerFive,
                    R.id.SpinnerSix, R.id.SpinnerSeven};
    private int[] RPE_array = new int[7];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_schedule);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        this.start = cal.getTime();
        cal.add(Calendar.DAY_OF_MONTH, 6);
        this.end = cal.getTime();
        prepareSpinners();
    }

    // Initializes the spinners and sets them to the values that have been saved.
    private void prepareSpinners() {
        int j = 0;
        for (int i : SpinnerIds) {
            Spinner spinner = (Spinner) findViewById(i);
            // Create an ArrayAdapter using the string array and a default spinner layout
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                    R.array.RPE_Difficulties, android.R.layout.simple_spinner_item);
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            // Apply the adapter to the spinner
            spinner.setAdapter(adapter);
            spinner.setOnItemSelectedListener(this);
            spinner.setSelection(getScheduleValueForOffset(j));
            j++;
        }
    }

    private int getScheduleValueForOffset(int j) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(start);
        cal.add(Calendar.DAY_OF_YEAR, j);
        int scheduleValue = queryForScheduleValue(cal.getTime());
        switch (scheduleValue) {
            case 11:
                insertScheduleValue(cal.getTime());
                return 0;
            default:
                return scheduleValue;
        }
    }

    private void insertScheduleValue(Date time) {
        ContentValues values = new ContentValues();
        values.put(ISSContentProvider.DATE, ISSDictionary.dateToDayString(time));
        values.put(ISSContentProvider.VALUE, 0);
        Log.d("values", values.toString());
        resolver.insert(ISSContentProvider.SCHEDULE_CONTENT_URI, values);
    }

    private void updateScheduleValue(Date time, int scheduleValue) {
        ContentValues values = new ContentValues();
        String mSelectionClause = ISSContentProvider.DATE + " = '" + ISSDictionary.dateToDayString(time) + "'";
        values.put(ISSContentProvider.VALUE, scheduleValue);
        Log.d("values", values.toString());
        resolver.update(
                ISSContentProvider.SCHEDULE_CONTENT_URI,   // the user dictionary content URI
                values,                       // the columns to update
                mSelectionClause,                    // the column to select on
                null                     // the value to compare to
        );
    }

    private int queryForScheduleValue(Date time) {
        String dayString = ISSDictionary.dateToDayString(time);
        String mSelectionClause = ISSContentProvider.DATE + " = '" + dayString + "'";
        String[] mSelectionArgs = {};
        Uri CONTENT_URI = ISSContentProvider.SCHEDULE_CONTENT_URI;
        String[] mProjection =
                {
                        ISSContentProvider._ID,
                        ISSContentProvider.DATE,
                        ISSContentProvider.VALUE
                };
        String mSortOrder = ISSContentProvider.DATE + " ASC";

        Cursor mCursor = getContext().getContentResolver().query(
                CONTENT_URI,                       // The content URI of the database table
                mProjection,                       // The columns to return for each row
                mSelectionClause,                  // Either null, or the word the user entered
                mSelectionArgs,                    // Either empty, or the string the user entered
                mSortOrder);
        // Some providers return null if an error occurs, others throw an exception
        if (null == mCursor) {
            return 11;
        } else if (mCursor.getCount() < 1) {
            mCursor.close();
            return 11;
        } else {
            while (mCursor.moveToNext()) {
                return mCursor.getInt(2);
            }
            mCursor.close();
        }
        return 11;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (parent.getId()) {
            case R.id.SpinnerOne:
                RPE_array[0] = position;
                break;
            case R.id.SpinnerTwo:
                RPE_array[1] = position;
                break;
            case R.id.SpinnerThree:
                RPE_array[2] = position;
                break;
            case R.id.SpinnerFour:
                RPE_array[3] = position;
                break;
            case R.id.SpinnerFive:
                RPE_array[4] = position;
                break;
            case R.id.SpinnerSix:
                RPE_array[5] = position;
                break;
            case R.id.SpinnerSeven:
                RPE_array[6] = position;
                break;
        }

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.cancelSchedule:
                this.finish();
                break;
            case R.id.saveSchedule:
                commit(RPE_array);
                Toast toast = Toast.makeText(this, "Schedule has successfully been saved", Toast.LENGTH_SHORT);
                toast.show();
                finish(); // added this to close the activity when the rpe's are inserted.
                break;
        }
    }

    // Starts the avalanche of methods needed to store the input schedule values
    private boolean commit(int[] array) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(start);
        for (int i = 0; i <= array.length - 1; i++) {
            updateScheduleValue(cal.getTime(), array[i]);
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        return true;
    }
}