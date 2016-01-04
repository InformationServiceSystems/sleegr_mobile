package com.iss.android.wearable.datalayer;

import android.os.Environment;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by Euler on 12/19/2015.
 */
public class DataStorageManager {


    public  static void InitializeTriathlonFolder(){

        if(!userDataFolder.exists()){
            userDataFolder.mkdir();
        }

    }

    static File sleepData = new File(Environment.getExternalStorageDirectory().toString() + "/sleep-data/sleep-export.csv");
    static File userDataFolder = new File(Environment.getExternalStorageDirectory().toString() , "triathlon" );

    public static byte[] FileToBytes(File file) {

        int size = (int) file.length();
        byte[] bytes = new byte[size];

        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(bytes, 0, bytes.length);
            buf.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return bytes;
    }

    public static String getDayFromToday(int dayoffset){

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cl = Calendar.getInstance();
        cl.add(Calendar.DAY_OF_YEAR, -dayoffset);
        String currentDateandTime = df.format(cl.getTime());

        return currentDateandTime;

    }

    public static String getProperUserID(String UserID){

        return UserID.replace("@", "_at_");

    }

    public static String getFileTemplate(int dayoffset, String UserID){


        return getProperUserID(UserID) +  "-" + getDayFromToday(dayoffset);

    }

    public static String getStateKey(String mark){

        int idx = mark.indexOf(":");

        if (idx < 0){
            return mark;
        }

        return mark.substring(0, idx);

    }

    public static String getKey(ISSRecordData record){

        String mark = record.ExtraData;

        if (mark == null){
            return null;
        }

        return  getStateKey(mark);

    }

    public static void SaveBinnedData(ArrayList<ISSRecordData> accumulator, String UserID, String activityType){

        // get date of first record
        Calendar cal = Calendar.getInstance();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd_HH:mm:ss");

        try {
            cal.setTime(sdf.parse(accumulator.get(0).Timestamp));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        cal.add(Calendar.HOUR, -4); // this is in case someone goes to sleep at like around 3 am, we assume that it still corresponds to previous day

        SimpleDateFormat folderFormat = new SimpleDateFormat("yyyy-MM-dd");
        String folderName = folderFormat.format(cal.getTime());

        // create folder for the time, if not created already
        File dayFolder = new File( userDataFolder, folderName);

        if(!dayFolder.exists()){
            boolean result = dayFolder.mkdir();
            //DataSyncService.itself.OutputEvent("create folder: " + result);
        }

        CSVManager.WriteNewCSVdata(
                new File(dayFolder, getProperUserID(UserID) + "_" + folderName + "_" + activityType + ".csv"),
                CSVManager.RecordsToCSV(accumulator).toString()
        );

    }

    public static void SaveNewDataToFile(ArrayList<ISSRecordData> data, String UserID) {

        try {

            if (data.size() < 1){
                return;
            }

            ArrayList<ISSRecordData> accumulator = new ArrayList<ISSRecordData>();
            String previousKey = getKey(data.get(0));

            // separate data into different folders
            for(ISSRecordData record: data){
                String key = getKey(record);

                if (!previousKey.equals(key)){
                    SaveBinnedData(accumulator, UserID, previousKey);
                    previousKey = key;
                    accumulator.clear();
                }

                accumulator.add(record);
            }

            if (accumulator.size() > 0){
                SaveBinnedData(accumulator, UserID, previousKey);
            }


        } catch (Exception e) {

            e.printStackTrace();

        }

    }


    public static ArrayList<File> GetAllFilesToUpload(String UserID, int timeSpan){

        ArrayList<File> result = new  ArrayList<File>();
        result.add(sleepData);

        for (int i = 0; i < timeSpan; i++){

            File dayFolder = new File( userDataFolder, getDayFromToday(i));
            File[] files = dayFolder.listFiles();

            if (files != null){
                for (File file : files) {
                    result.add(file);
                }
            }

        }

        return result;
    }

}
