package com.iss.android.wearable.readingHR;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Euler on 12/18/2015.
 */
public class CSVManager {


    // Transcribes a list of ISSRecordData to a StringBuilder
    public static StringBuilder RecordsToCSV(List<ISSRecordData> exampleData ){
        Log.d("Wrote string", exampleData.toString());

        StringBuilder bld = new StringBuilder();

        String separator = ",";

        for (int i = 0; i < exampleData.size(); i++) {
            ISSRecordData record = exampleData.get(i);

            bld.append(record.toString());
            bld.append("\r\n");
        }

        return bld;

    }

    // Writes a string to a *.csv file
    public static void WriteNewCSVdata(File file, String data){
        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
            out.print(data);
            out.close();
        } catch (IOException e) {
            Log.e("exception", e.toString());
        }

    }

    // Reads a *.csv and writes it into a list of ISSRecordData
    public static List<ISSRecordData> ReadCSVdata(File file){

        List<ISSRecordData>  result = new ArrayList<ISSRecordData>();

        if (!file.exists()){
            return null;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {

            String line;

            while ((line = br.readLine()) != null) {

                ISSRecordData recordData = ISSRecordData.fromString(line);

                if (recordData == null){
                    continue;
                }

                result.add(recordData);

            }

        } catch (IOException e) {

            System.out.print(e);

        }

        if (result.size() == 0){
            return  null;
        }

        return result;

    }

    // Finds the file given a date, user and activity type and reads it to a list of ISSRecordData
    public static List<ISSRecordData> ReadCSVdata(String date, String userID,  String activity){


        //iaroslogos_at_gmail.com-2016-01-06_Running.csv

        userID = DataStorageManager.getProperUserID(userID);


        File file = new File(DataStorageManager.userDataFolder, date  + File.separator + userID + "-" + date + "_" + activity + ".csv");

        if (!file.exists()){
            file = new File(DataStorageManager.userDataFolder, date  + File.separator + userID + "_" + date + "_" + activity + ".csv");
        }

        return ReadCSVdata(file);

    }

    public static ArrayList<List<ISSRecordData>> ReadSplitCSVdata(String date, String userID,  String activity){


        ArrayList<List<ISSRecordData>> result = new ArrayList<List<ISSRecordData>>();

        ArrayList<ISSRecordData> allRecords = (ArrayList<ISSRecordData>) ReadCSVdata(date, userID, activity);

        // now split records into different ones by time

        if (allRecords.size() == 0){
            return result;
        }

        ArrayList<ISSRecordData> accumulator = new ArrayList<>();
        long previousTime = allRecords.get(0).getTimestamp().getTime();

        for (ISSRecordData record: allRecords){
            long currentTime = record.getTimestamp().getTime();

            // more than 1 hour difference in time stamps means that these are different measures
            if (currentTime - previousTime > 60*60*1000){
                result.add(accumulator);
                accumulator = new ArrayList<>();
            }

            accumulator.add(record);
            previousTime = currentTime;

        }

        result.add(accumulator);

        return result;

    }



}
