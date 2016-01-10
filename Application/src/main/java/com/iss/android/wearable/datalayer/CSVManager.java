package com.iss.android.wearable.datalayer;

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


    public static StringBuilder RecordsToCSV(List<ISSRecordData> exampleData ){

        StringBuilder bld = new StringBuilder();

        String separator = ",";

        for (int i = 0; i < exampleData.size(); i++) {
            ISSRecordData record = exampleData.get(i);

            bld.append(record.toString());
            bld.append("\r\n");
        }

        return bld;

    }

    public static void WriteNewCSVdata(File file, String data){

        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
            out.print(data);
            out.close();
        } catch (IOException e) {
            //exception handling left as an exercise for the reader
            DataSyncService.itself.OutputEvent("error occured: "  + e.toString());
        }

    }

    public static List<ISSRecordData> ReadCSVdata(String date, String userID,  String activity){


        //iaroslogos_at_gmail.com-2016-01-06_Running.csv

        List<ISSRecordData>  result = new ArrayList<ISSRecordData>();

        File file = new File(DataStorageManager.userDataFolder, date  + File.separator + userID + "-" + date + "_" + activity + ".csv");

        if (!file.exists()){
            file = new File(DataStorageManager.userDataFolder, date  + File.separator + userID + "_" + date + "_" + activity + ".csv");
        }

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

}
