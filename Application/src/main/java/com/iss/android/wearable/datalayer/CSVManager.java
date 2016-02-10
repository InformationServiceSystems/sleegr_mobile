package com.iss.android.wearable.datalayer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Euler on 12/18/2015.
 */
public class CSVManager {

    public static StringBuilder RecordsToCSV(List<ISSRecordData> exampleData) {

        StringBuilder bld = new StringBuilder();

        String separator = ",";

        for (int i = 0; i < exampleData.size(); i++) {
            ISSRecordData record = exampleData.get(i);

            bld.append(record.toString());
            bld.append("\r\n");
        }

        return bld;

    }

    public static void WriteNewCSVdata(File file, String data) {
        boolean edited = false;
        String current_date = data.substring(0, 10);
        try {
            // Open the file that is the first argument
            FileInputStream fstream = new FileInputStream(file);
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String strLine;
            StringBuilder fileContent = new StringBuilder();
            //Read File Line By Line
            while ((strLine = br.readLine()) != null) {
                // Print the content on the console
                String tokens[] = strLine.split(",");
                if (tokens.length > 0) {
                    // Here, tokens[0] will have the value of the current date
                    if (tokens[0].equals(current_date)) {
                        tokens[1] = data.substring(11, 12);
                        String newLine = tokens[0] + "," + tokens[1];
                        fileContent.append(newLine);
                        fileContent.append("\n");
                        edited = true;
                    } else {
                        // update content as it is
                        fileContent.append(strLine);
                        fileContent.append("\n");
                    }
                }
            }
            // Now fileContent will have updated content , which you can override into file
            //Close the input stream
            fstream.close();
            FileWriter fstreamWrite = new FileWriter(file);
            BufferedWriter out = new BufferedWriter(fstreamWrite);
            out.write(fileContent.toString());
            out.flush();
            if (!edited) {
                PrintWriter outpw = new PrintWriter(out);
                outpw.print(data);
                outpw.close();
            }
            out.close();
        } catch (
                IOException e
                )

        {
            //exception handling left as an exercise for the reader

            System.out.print(e.toString());

        }

    }

    public static HashMap<String, Double> ReadSleepData() {

        HashMap<String, Double> result = new HashMap<>();

        if (!DataStorageManager.sleepData.exists()) {
            return result;
        }

        // read file line by line
        try (BufferedReader br = new BufferedReader(new FileReader(DataStorageManager.sleepData))) {

            String line;

            // every second line contains data, other lines contain column names
            boolean skip = false;
            while ((line = br.readLine()) != null) {

                skip = !skip;

                if (skip)
                    continue;

                // split thee line

                String[] split = line.split("\",\"");

                String date = split[3].substring(0, 12);
                String[] date_split = date.split(". ");
                date = date_split[2] + "-" + date_split[1] + "-" + date_split[0];
                Double value = Double.parseDouble(split[12]);

                result.put(date, value);


            }

        } catch (IOException e) {

            System.out.print(e);

        }

        return result;

    }

    public static boolean UserActivityExists(String date, String userID, String activity) {

        File file = new File(DataStorageManager.userDataFolder, date + File.separator + userID + "-" + date + "_" + activity + ".csv");

        if (!file.exists()) {
            file = new File(DataStorageManager.userDataFolder, date + File.separator + userID + "_" + date + "_" + activity + ".csv");
        }

        if (!file.exists()) {
            return false;
        }

        return true;

    }


    public static List<ISSRecordData> ReadCSVdata(String date, String userID, String activity) {


        //iaroslogos_at_gmail.com-2016-01-06_Running.csv

        userID = DataStorageManager.getProperUserID(userID);

        List<ISSRecordData> result = new ArrayList<ISSRecordData>();

        File file = new File(DataStorageManager.userDataFolder, date + File.separator + userID + "-" + date + "_" + activity + ".csv");

        if (!file.exists()) {
            file = new File(DataStorageManager.userDataFolder, date + File.separator + userID + "_" + date + "_" + activity + ".csv");
        }

        if (!file.exists()) {
            return null;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {

            String line;

            while ((line = br.readLine()) != null) {

                ISSRecordData recordData = ISSRecordData.fromString(line);

                if (recordData == null) {
                    continue;
                }

                result.add(recordData);

            }

        } catch (IOException e) {

            System.out.print(e);

        }

        if (result.size() == 0) {
            return null;
        }

        return result;

    }

    public static ArrayList<List<ISSRecordData>> ReadSplitCSVdata(String date, String userID, String activity) {


        ArrayList<List<ISSRecordData>> result = new ArrayList<List<ISSRecordData>>();

        ArrayList<ISSRecordData> allRecords = (ArrayList<ISSRecordData>) ReadCSVdata(date, userID, activity);

        // now split records into different ones by time
        if (allRecords != null) {
            if (allRecords.size() == 0) {
                return result;
            }

            ArrayList<ISSRecordData> accumulator = new ArrayList<>();
            long previousTime = allRecords.get(0).getTimestamp().getTime();

            for (ISSRecordData record : allRecords) {
                long currentTime = record.getTimestamp().getTime();

                // more than 1 hour difference in time stamps means that these are different measures
                if (currentTime - previousTime > 60 * 60 * 1000) {
                    result.add(accumulator);
                    accumulator = new ArrayList<>();
                }

                accumulator.add(record);
                previousTime = currentTime;

            }

            result.add(accumulator);
        }

        return result;

    }


}
