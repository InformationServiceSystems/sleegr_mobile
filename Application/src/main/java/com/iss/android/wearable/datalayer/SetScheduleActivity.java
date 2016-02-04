package com.iss.android.wearable.datalayer;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class SetScheduleActivity extends Activity implements AdapterView.OnItemSelectedListener {

    private int[] SpinnerIds = new int[]
            {R.id.SpinnerOne, R.id.SpinnerTwo, R.id.SpinnerThree, R.id.SpinnerFour, R.id.SpinnerFive,
                    R.id.SpinnerSix, R.id.SpinnerSeven};
    private int[] RPE_array = new int[7];
    Date start = new Date();
    Date end = new Date();

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

        int[] dupes = checkForDupes();
        prepareSpinners(dupes);
    }

    private int[] checkForDupes() {
        TimeSeries series = DataStorageManager.readUserSchedule();
        series = series.inTimeRange(start, end);
        int[] dupes = new int[7];
        if (series != null) {
            if (series.Values.size() > 7) {
                for (int i = (series.Values.size() - 1); i > (series.Values.size() - 8); i--) {
                    dupes[(i) - series.Values.size() + 7] = series.Values.get(i).y.intValue();
                }
            } else {
                for (int i = 0; i < series.Values.size(); i++) {
                    dupes[i] = series.Values.get(i).y.intValue();
                }
            }
        }
        return dupes;
    }

    private void prepareSpinners(int[] dupes) {
        int j = 0;
        for (int i : SpinnerIds) {
            Spinner spinner = (Spinner) findViewById(i);
            Log.d("id", spinner.toString());
            // Create an ArrayAdapter using the string array and a default spinner layout
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                    R.array.RPE_Difficulties, android.R.layout.simple_spinner_item);
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            // Apply the adapter to the spinner
            spinner.setAdapter(adapter);
            spinner.setOnItemSelectedListener(this);
            spinner.setSelection(dupes[j]);
            j++;
        }
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
                finish(); // added this to close the activity when the rpe's are inserted.
                break;
        }
    }

    private boolean commit(int[] array) {
        Date date = this.start;
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        for (int i = 0; i <= array.length - 1; i++) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd");
            String dateString = sdf.format(date);
            Log.d(dateString, String.valueOf(array[i]));
            cal.add(Calendar.DAY_OF_MONTH, 1);
            date = cal.getTime();
            String scheduleString = dateString + "," + String.valueOf(array[i]) + "\n";
            DataStorageManager.storeScheduleLine(scheduleString);
        }
        return true;
    }
}
