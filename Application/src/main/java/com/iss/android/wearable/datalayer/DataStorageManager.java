package com.iss.android.wearable.datalayer;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import static com.iss.android.wearable.datalayer.DateTimeManager.getDayFromToday;

/**
 * Created by Euler on 12/19/2015.
 */
public class DataStorageManager {


    // a method responsible for creating the folders to write into
    public  static void InitializeTriathlonFolder(){

        if(!userDataFolder.exists()){
            userDataFolder.mkdir();
        }

    }

    // A method reading the schedule.csv file and parsing it to TimeSeries
    public static TimeSeries readUserSchedule(){

        TimeSeries result = new TimeSeries("RPE required");

        File csvSchedule = new File(userDataFolder, "schedule.csv");

        if (!csvSchedule.exists())
            return result;

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd");

        try (BufferedReader br = new BufferedReader(new FileReader(csvSchedule))) {
            String line;
            while ((line = br.readLine()) != null) {

                String dateStr = line.substring(0, 10);
                String contents = line.substring(11);

                Date date = dateFormat.parse(dateStr);
                Double value = Double.parseDouble(contents);

                result.AddValue(date,value);

            }
        } catch (Exception e) {

        }

        return result;

    }


    // Environment.getDataDirectory().toString()
    // I use here external storage directory, as the previous versions of the
    // app use the external directory. In case ext. storage is not available, use
    // Environment.getDataDirectory().toString()

    // I overwrote it so that it now takes Internal Storage, which is more fitting for in-app-data.
    // All other methods wouldn't let me write when using the scheduled RPE values.
    // static String dataFolder = MainActivity.getContext().getFilesDir().toString();
    static String dataFolder = Environment.getExternalStorageDirectory().getAbsolutePath();
    static File sleepData = new File(dataFolder + "/sleep-data/sleep-export.csv");
    static File userDataFolder = new File(dataFolder + "/triathlon");

    // A method that converts a file into a bytearray
    public static byte[] FileToBytes(File file) {

        int size = (int) file.length();
        byte[] bytes = new byte[size];

        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(bytes, 0, bytes.length);
            buf.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bytes;
    }

    // A method that transcribes the UserID. Currently replaces "@" with "_at_"
    public static String getProperUserID(String UserID){

        return UserID.replace("@", "_at_");

    }

    public static String getFileTemplate(int dayoffset, String UserID){


        return getProperUserID(UserID) +  "-" + getDayFromToday(dayoffset);

    }

    // sub-method of getKey
    public static String getStateKey(String mark){

        int idx = mark.indexOf(":");

        if (idx < 0){
            return mark;
        }

        return mark.substring(0, idx);

    }

    // A method that, together with getStateKey, returns the state, e.g. "resting", "idle" or "cooldown"
    public static String getKey(ISSRecordData record){

        String mark = record.ExtraData;

        if (mark == null){
            return null;
        }

        return  getStateKey(mark);

    }

    // A method that stores a list of ISSRecordData belonging to the same type in a file.
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

        CSVManager.AppendStringToFile(
                new File(dayFolder, getProperUserID(UserID) + "_" + folderName + "_" + activityType + ".csv"),
                CSVManager.RecordsToCSV(accumulator).toString()
        );

    }

    // A method that divides a list of ISSRecordData by type and makes them be stored seperately in SaveBinnedData
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

    // A method that determines all the *.csv Files in a given timespan.
    public static ArrayList<ArrayList<File>> GetAllFilesToUpload(String UserID, int timeSpan){

        ArrayList<ArrayList<File>> result = new  ArrayList<>();
        //result.add(sleepData);

        for (int i = 0; i < timeSpan; i++){
            File dayFolder = new File( userDataFolder, getDayFromToday(i));
            File[] files = dayFolder.listFiles();

            ArrayList<File> day = new ArrayList<>();

            if (files != null){
                for (File file : files) {

                    if (!file.getName().contains(".csv")){
                        continue;
                    }

                    day.add(file);
                }
            }

            result.add(day);

        }

        return result;
    }

    // A method responsible for creating a file in which the scheduled RPE values will be saved (in CSVManager.WriteNewCSVdata)
    public static void storeScheduleLine(String scheduleString) {
        File file = new File(userDataFolder, "schedule.csv");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                Log.d("Creating file", "failed");
                e.printStackTrace();
            }
        }
        CSVManager.WriteNewCSVdata(file, scheduleString);
    }
}
