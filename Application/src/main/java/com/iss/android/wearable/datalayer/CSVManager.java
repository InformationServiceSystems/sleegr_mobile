package com.iss.android.wearable.datalayer;

import android.content.ContentValues;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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

    // A method constructing a stringbuilder that transforms a list of ISSRecordData into a large String.
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

    // A method that writes a String to a file, iff it not already is stored in given file. Only relevant for the RPE schedule.
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
            // Close the input stream
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
        } catch (IOException e) {
            System.out.print(e.toString());
        }

    }

    // A method that reads sleepData from the file defined in DataStorageManager
    // and transcribes it into a HashMap
    public static HashMap<String, Double[]> ReadSleepData() {

        HashMap<String, Double[]> result = new HashMap<>();

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

                // split the line

                String[] split = line.split("\",\"");
                // Sometimes Google Sleep fucks up and inserts an extra line. Need this to rule that out
                if (split.length<3 || split[3].length()<12){
                    continue;
                }
                String date = split[3].substring(0, 12);
                String[] date_split = date.split(". ");
                date = date_split[2] + "-" + date_split[1] + "-" + date_split[0];
                Double value = Double.parseDouble(split[12]);
                Double length = Double.parseDouble(split[5]);
                result.put(date, new Double[]{value, length});

            }

        } catch (IOException e) {

            System.out.print(e);

        }

        return result;

    }

}