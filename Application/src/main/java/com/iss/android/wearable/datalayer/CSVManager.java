package com.iss.android.wearable.datalayer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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

            bld.append(record.UserID);
            bld.append(separator);

            bld.append(record.MeasurementType);
            bld.append(separator);

            bld.append(record.Timestamp);
            bld.append(separator);

            bld.append(record.ExtraData);
            bld.append(separator);

            bld.append(record.Value1);
            bld.append(separator);

            bld.append(record.Value2);
            bld.append(separator);

            bld.append(record.Value3);
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
        }

    }

}
