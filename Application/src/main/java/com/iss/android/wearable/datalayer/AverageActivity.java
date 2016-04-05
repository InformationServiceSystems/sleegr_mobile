package com.iss.android.wearable.datalayer;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class AverageActivity extends Activity {

    // This averaging currently just takes the arithmetical mean. It is not entirely correct,
    // since we're basically reviewing a time series, but for now it should suffice. Maybe later we
    // can make this averaging more elaborate.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_average);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Log.d("Position", "onCreate");
        new LoadingOperation().execute("");
    }

    private class LoadingOperation extends AsyncTask<String, Void, String> {

        ArrayList<ArrayList<File>> listOfFiles;
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        Double morningHR = 0.0;
        int morningMeasures = 0;
        Double dayHR = 0.0;
        int dayMeasures = 0;
        Double eveningHR = 0.0;
        int eveningMeasures = 0;

        @Override
        protected String doInBackground(String... params) {
            Log.d("Position", "doInBackground");
            // Determine the dates today and 30 days ago and retrieve the csvs
            listOfFiles = DataStorageManager.GetAllFilesToUpload(DataSyncService.getUserID(),30);
            // Iterate over all heart rate values and assign their value to morning, day or evening
            for (int i = 0; i< listOfFiles.size(); i++){
                for (File f: listOfFiles.get(i)){
                    try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                        Log.d("Position", "read a file named " + f.getName());
                        String line;
                        while ((line = br.readLine()) != null) {
                            String[] parts = line.split(",");
                            String type = parts[1]; // Measurement Type (Heart rate etc.)
                            String stringDate = parts[2]; // Date
                            String stringValue = parts[4]; // Value
                            if (type.equals("21")) {

                                Date date = dateFormat.parse(stringDate.substring(11));
                                Double value = Double.parseDouble(stringValue);
                                Log.d("value", String.valueOf(value));
                                // Now that we've got the time of the day this value was measured at, we sort it in correctly
                                sortIn(date, value);
                            }
                        }
                    } catch (Exception e) {
                        Log.d("Error:", e.getMessage());
                    }
                }
            }
            // Divide the summed up heart rate value by the number of values
            if (morningMeasures > 0)morningHR/=morningMeasures;
            if (dayMeasures > 0)dayHR/=dayMeasures;
            if (eveningMeasures > 0)eveningHR/=eveningMeasures;
            return "Executed";
        }

        private void sortIn(Date date, Double value) {
            try {
                if (date.before(dateFormat.parse("12:00:00")) && date.after(dateFormat.parse("03:00:00"))){
                    morningMeasures++;
                    morningHR+=value;
                } else if (date.before(dateFormat.parse("21:00:00")) && date.after(dateFormat.parse("12:00:00"))){
                    dayMeasures++;
                    dayHR+=value;
                } else if (date.before(dateFormat.parse("03:00:00")) && date.after(dateFormat.parse("21:00:00"))){
                    eveningMeasures++;
                    eveningHR+=value;
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            DecimalFormat df = new DecimalFormat("#0.00");
            TextView morningText = (TextView) findViewById(R.id.MorningHR);
            morningText.setText(String.valueOf(df.format(morningHR)));
            TextView dayText = (TextView) findViewById(R.id.DayHR);
            dayText.setText(String.valueOf(df.format(dayHR)));
            TextView eveningText = (TextView) findViewById(R.id.EveningHR);
            eveningText.setText(String.valueOf(df.format(eveningHR)));
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }

}
