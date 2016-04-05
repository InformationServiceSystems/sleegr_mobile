package com.iss.android.wearable.datalayer;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.widget.TableLayout;
import android.widget.TableRow;
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

    static AverageActivity itself;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        itself = this;
        setContentView(R.layout.activity_average);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Log.d("Position", "onCreate");
        new LoadingOperation().execute("");
    }

    private class LoadingOperation extends AsyncTask<String, Void, String> {

        ArrayList<ArrayList<File>> listOfFiles;
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        double[] morningHR = new double[30];
        int[] morningMeasures = new int[30];
        double[] dayHR = new double[30];
        int[] dayMeasures = new int[30];
        double[] eveningHR = new double[30];
        int[] eveningMeasures = new int[30];

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
                                Log.d("Registered value", String.valueOf(value));
                                // Now that we've got the time of the day this value was measured at, we sort it in correctly
                                sortIn(date, value, i);
                            }
                        }
                    } catch (Exception e) {
                        Log.d("Error:", e.getMessage());
                    }
                }
                // Divide the summed up heart rate value by the number of values
                if (morningMeasures[i] > 0) morningHR[i]/=morningMeasures[i];
                if (dayMeasures[i] > 0) dayHR[i]/=dayMeasures[i];
                if (eveningMeasures[i] > 0) eveningHR[i]/=eveningMeasures[i];
            }
            return "Executed";
        }

        private void sortIn(Date date, Double value, int i) {
            try {
                if (date.before(dateFormat.parse("12:00:00")) && date.after(dateFormat.parse("03:00:00"))){
                    morningMeasures[i]++;
                    morningHR[i]+=value;
                    Log.d("Sorted", value + " into morning " + i);
                } else if (date.before(dateFormat.parse("21:00:00")) && date.after(dateFormat.parse("12:00:00"))){
                    dayMeasures[i]++;
                    dayHR[i]+=value;
                    Log.d("Sorted", value + " into day " + i);
                } else if (date.before(dateFormat.parse("03:00:00")) && date.after(dateFormat.parse("21:00:00"))){
                    eveningMeasures[i]++;
                    eveningHR[i]+=value;
                    Log.d("Sorted", value + " into evening " + i);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            DecimalFormat df = new DecimalFormat("#0.00");
            TableLayout table = (TableLayout)findViewById(R.id.tableLayout);

            TextView morningHeaderTV = new TextView(itself);
            morningHeaderTV.setText("_______");
            TextView dayHeaderTV = new TextView(itself);
            dayHeaderTV.setText("_______");
            TextView eveningHeaderTV = new TextView(itself);
            eveningHeaderTV.setText("_______");

            TableRow rowHeader = new TableRow(itself);

            rowHeader.addView(morningHeaderTV);
            rowHeader.addView(dayHeaderTV);
            rowHeader.addView(eveningHeaderTV);

            table.addView(rowHeader);
            for (int i = 0; i<30; i++){
                TableRow dataRow = new TableRow(itself);
                TextView morningHRTV = new TextView(itself);
                morningHRTV.setText(String.valueOf(df.format(morningHR[i])));
                TextView dayHRTV = new TextView(itself);
                dayHRTV.setText(String.valueOf(df.format(dayHR[i])));
                TextView eveningHRTV = new TextView(itself);
                eveningHRTV.setText(String.valueOf(df.format(eveningHR[i])));
                dataRow.addView(morningHRTV);
                dataRow.addView(dayHRTV);
                dataRow.addView(eveningHRTV);
                table.addView(dataRow);
            }
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }

}
