package com.iss.android.wearable.datalayer;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TimePicker;

import java.util.Calendar;

public class TrainingStartTimeActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training_start_time);


        NumberPicker hourPicker = (NumberPicker) findViewById(R.id.hourPicker);
        NumberPicker minutePicker = (NumberPicker) findViewById(R.id.minutePicker);

        Calendar cal = Calendar.getInstance();

        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(60);
        hourPicker.setValue(cal.get(Calendar.HOUR_OF_DAY));

        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(60);
        minutePicker.setValue(cal.get(Calendar.MINUTE));

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_training_start_time, menu);
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

    public void onSubmit(View view){
        NumberPicker hourPicker = (NumberPicker) findViewById(R.id.hourPicker);
        NumberPicker minutePicker = (NumberPicker) findViewById(R.id.minutePicker);

        float hour = hourPicker.getValue();
        float minute = minutePicker.getValue();

        // TODO: Think about what to do here. Maybe introduce an extra table where every measurement is stored?
        // SensorsDataService.itself.TrainingEnd(hour, minute);
        finish();
    }

}
