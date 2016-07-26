package com.iss.android.wearable.datalayer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
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

    // A method appending a String to a file. Relevant for ISSRecordData.
    public static void AppendStringToFile(File file, String data) {

        try {
            PrintWriter out = new PrintWriter(new FileOutputStream(file), true);
            out.print(data);
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


    }

    // A method translating the *.csv data in the given file into a list of ISSRecordData
    public static List<ISSRecordData> ReadCSVdata(File file) {

        List<ISSRecordData> result = new ArrayList<ISSRecordData>();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {

            String line;

            while ((line = br.readLine()) != null) {

                try {
                    ISSRecordData recordData = ISSRecordData.fromString(line);

                    if (recordData == null) {
                        continue;
                    }

                    result.add(recordData);
                } catch (Exception ex) {

                    System.out.print(ex.toString());
                }

            }

        } catch (IOException e) {

            System.out.print(e);

        }

        if (result.size() == 0) {
            return null;
        }

        return result;

    }


}
