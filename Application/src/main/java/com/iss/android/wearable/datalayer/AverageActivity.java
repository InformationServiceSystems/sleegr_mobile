package com.iss.android.wearable.datalayer;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.io.File;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import static com.iss.android.wearable.datalayer.DateTimeManager.getDateFromToday;
import static com.iss.android.wearable.datalayer.DateTimeManager.getDayFromToday;

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
        double[] deepSleepHours = new double[30];
        double[] sleepHours = new double[30];
        // Reads Sleep Data from the sleep-export file.
        HashMap<String, Double[]> sleepData = CSVManager.ReadSleepData();

        @Override
        protected String doInBackground(String... params) {
            // Determine the dates today and 30 days ago and retrieve the csvs
            /*listOfFiles = DataStorageManager.GetAllFilesToUpload(DataSyncService.getUserID(),30);
            // Iterate over all heart rate values and assign their value to morning, day or evening
            for (int i = 0; i< listOfFiles.size(); i++){
                for (File f: listOfFiles.get(i)){
                    try (BufferedReader broadcastReceiver = new BufferedReader(new FileReader(f))) {
                        String line;
                        while ((line = broadcastReceiver.readLine()) != null) {
                            String[] parts = line.split(",");
                            String type = parts[1]; // Measurement Type (Heart rate etc.)
                            String stringDate = parts[2]; // Date
                            String stringValue = parts[4]; // Value
                            if (type.equals("21")) {
                                Date date = dateFormat.parse(stringDate.substring(11));
                                Double value = Double.parseDouble(stringValue);
                                // Now that we've got the time of the day this value was measured at, we sort it in correctly
                                sortIn(date, value, i);
                            }
                        }
                    } catch (Exception e) {
                    }
                }
                // Divide the summed up heart rate value by the number of values
                if (morningMeasures[i] > 0) morningHR[i]/=morningMeasures[i];
                if (dayMeasures[i] > 0) dayHR[i]/=dayMeasures[i];
                if (eveningMeasures[i] > 0) eveningHR[i]/=eveningMeasures[i];
            }
            for (int i = 0; i < 30; i++){
                sortSleepIn(i, sleepData);
            }*/
            return "Executed";
        }

        private void sortSleepIn(int i, HashMap<String, Double[]> sleepData) {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            Date relevantDay = getDateFromToday(i);
            String relevantDayString = df.format(relevantDay);
            Double[] valueLength = sleepData.get(relevantDayString);
            if (valueLength != null) {
                Log.d("Sleep lengths are", String.valueOf(valueLength[0]) + "," + String.valueOf(valueLength[1]));
                if (valueLength[0] > 0) {
                    deepSleepHours[i] = valueLength[0];
                } else {
                    deepSleepHours[i] = 0;
                }
                if (valueLength[1] > 0) {
                    sleepHours[i] = valueLength[1];
                } else {
                    sleepHours[i] = 0;
                }
            } else {
                deepSleepHours[i] = 0;
                sleepHours[i] = 0;
            }
        }

        private void sortIn(Date date, Double value, int i) {
            try {
                if (date.before(dateFormat.parse("12:00:00")) && date.after(dateFormat.parse("03:00:00"))) {
                    morningMeasures[i]++;
                    morningHR[i] += value;
                } else if (date.before(dateFormat.parse("21:00:00")) && date.after(dateFormat.parse("12:00:00"))) {
                    dayMeasures[i]++;
                    dayHR[i] += value;
                } else if (date.before(dateFormat.parse("03:00:00")) && date.after(dateFormat.parse("21:00:00"))) {
                    eveningMeasures[i]++;
                    eveningHR[i] += value;
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            // Writes all the retrieved data into the table. Headers are declared manually,
            // the rest is filled in programmatically.
            DecimalFormat df = new DecimalFormat("#0.##");
            TableLayout table = (TableLayout) findViewById(R.id.tableLayout);


            for (int i = 0; i < 30; i++) {
                String date = getDayFromToday(i);
                date = reformat(date);

                TableRow dataRow = new TableRow(itself);

                TextView dateHRTV = new TextView(itself);
                dateHRTV.setText(date);
                TextView morningHRTV = new TextView(itself);
                morningHRTV.setText(String.valueOf(df.format(morningHR[i])));
                TextView dayHRTV = new TextView(itself);
                dayHRTV.setText(String.valueOf(df.format(dayHR[i])));
                TextView eveningHRTV = new TextView(itself);
                eveningHRTV.setText(String.valueOf(df.format(eveningHR[i])));
                TextView deepSleepTV = new TextView(itself);
                deepSleepTV.setText(String.valueOf(df.format(deepSleepHours[i])));
                TextView sleepHoursTV = new TextView(itself);
                sleepHoursTV.setText(String.valueOf(df.format(sleepHours[i])));

                dataRow.addView(dateHRTV);
                dataRow.addView(morningHRTV);
                dataRow.addView(dayHRTV);
                dataRow.addView(eveningHRTV);
                dataRow.addView(deepSleepTV);
                dataRow.addView(sleepHoursTV);

                table.addView(dataRow);
            }
        }

        private String reformat(String date) {
            String[] dateArr = date.split("-");
            date = dateArr[2] + "." + dateArr[1];
            return date;
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }

}
