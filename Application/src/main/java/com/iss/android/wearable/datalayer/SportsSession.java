package com.iss.android.wearable.datalayer;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * Created by Michael on 04.01.2016.
 */
public class SportsSession {
    String type;
    Calendar start;
    Calendar cooldown;
    Calendar end;
    Integer[] heartrate;

    public SportsSession(String type, Calendar start, Calendar cooldown, Calendar end, Integer[] heartrate) {
        this.type = type;
        this.start = start;
        this.cooldown = cooldown;
        this.end = end;
        this.heartrate = heartrate;
    }

    public static void printSession(SportsSession session) {
        Log.d("Session:", "printSession: " + session.type + ": from " + session.start + " to "
                + session.cooldown + " ending at " + ", initial value: " + session.heartrate[1]);
    }

    public static ArrayList<SportsSession> retrieveSessions(Calendar date) {
        ArrayList<SportsSession> sessions = new ArrayList<SportsSession>();
        //retrieve all csv files for the given date
        File[] ListOfCsvs = retrieveCsvs(date);
        if (ListOfCsvs != null) {
            for (File f : ListOfCsvs) {
                //read and interpret every csv file, returns a table with the following columns:
                //0: UserID, 1: Measurement type, 2:Exact time, 3: Type of activity,
                //4, 5, 6: Measurements
                ArrayList<String[]> TableOfActivities = new ArrayList<String[]>();
                TableOfActivities = readCsv(f);
                for (SportsSession session : transformToSessions(TableOfActivities)) {
                    sessions.add(session);
                    printSession(session);
                }
            }
        }
        return sessions;
    }

    public static File[] retrieveCsvs(Calendar date) {
        int year = date.get(Calendar.YEAR);
        int month = date.get(Calendar.MONTH);
        int day = date.get(Calendar.DAY_OF_MONTH);
        String filename = year + "-" + month + 1 + "-" + String.format("%02d", day);
        Log.d("date ", filename);
        File userDataFolder = new File(Environment.getDataDirectory().toString(), "triathlon");
        File folder = new File(userDataFolder, "filename");
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles != null) {
            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isFile()) {
                    System.out.println("File " + listOfFiles[i].getName());
                } else if (listOfFiles[i].isDirectory()) {
                    System.out.println("Directory " + listOfFiles[i].getName());
                }
            }
        } else {
            Log.d("Files", "none");
        }

        return listOfFiles;
    }

    public static ArrayList<String[]> readCsv(File file) {
        String csvFile = file.getAbsolutePath();
        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";
        //String[] is the representation of a line in a table, the arrayList of indefinite size
        //is all lines appended. Have to use ugly lists because csv files are indefinitely large.
        ArrayList<String[]> matrix = new ArrayList<>();
        int i = 0;

        try {

            br = new BufferedReader(new FileReader(csvFile));
            while ((line = br.readLine()) != null) {

                // use comma as separator
                String[] lineContents = line.split(cvsSplitBy);
                System.out.println(lineContents[0]);
                matrix.set(i, lineContents);
                i++;
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        System.out.println("Done");
        return matrix;
    }

    public static ArrayList<ISSRecordData> convertToIRD(ArrayList<String[]> TableOfActivities) {
        ArrayList<ISSRecordData> records = new ArrayList<ISSRecordData>();
        for (String[] s : TableOfActivities) {
            ISSRecordData record = new ISSRecordData(Integer.valueOf(s[0]),
                    Integer.valueOf(s[1]), s[2], s[3], Float.valueOf(s[4]), Float.valueOf(s[5]),
                    Float.valueOf(s[6]));
            records.add(record);
        }
        return records;
    }

    public static ArrayList<SportsSession> transformToSessions(ArrayList<String[]> TableOfActivities) {
        ArrayList<SportsSession> sessions = new ArrayList<SportsSession>();
        String type = TableOfActivities.get(1)[3];
        Calendar start = new GregorianCalendar(1, 1, 1);
        Calendar cooldown = new GregorianCalendar(1, 1, 1);
        Calendar end = new GregorianCalendar(1, 1, 1);
        ArrayList<Integer> heartrate = new ArrayList<Integer>();
        //I iterate through the lines of the table
        for (int i = 0; i < TableOfActivities.size(); i++) {
            String[] s = TableOfActivities.get(i);
            //We're only interested in heart rate measurements atm
            switch (s[1]) {
                case "21":
                    //Add the heartrate value to the list of heartrate values
                    heartrate.add(Integer.valueOf(s[4]));
                    //I check if this is the last line that contains cooling.
                    //If yes, that means the current session is over and a new one begins.
                    if (s[3].contains("Cooling")) {
                        if (i < TableOfActivities.size() + 1) {
                            if (!TableOfActivities.get(i + 1)[3].contains("Cooling")) {
                                end = convertToDate(s[2]);
                                //Ugly, but Java doesn't support nested functions. JavaScript does.
                                Integer[] hr = heartrate.toArray(new Integer[heartrate.size()]);
                                SportsSession session = new SportsSession(type, start, cooldown, end, hr);
                                end = new GregorianCalendar(1, 1, 1);
                                start = new GregorianCalendar(1, 1, 1);
                                cooldown = new GregorianCalendar(1, 1, 1);
                                heartrate = new ArrayList<Integer>();
                                sessions.add(session);
                                start = convertToDate(TableOfActivities.get(i + 1)[2]);
                            }
                        }
                    }
                    //If this line doesn't contain "Cooling" but the next one does,
                    //that means a recoveryCD phase just started.
                    if (!s[3].contains("Cooling")) {
                        if (i < TableOfActivities.size() + 1) {
                            if (TableOfActivities.get(i + 1)[3].contains("Cooling")) {
                                cooldown = convertToDate(TableOfActivities.get(i + 1)[2]);
                            }
                        }
                    }
                    //Initial starting time
                    if (start.equals(new GregorianCalendar(1, 1, 1))) {
                        start = convertToDate(s[2]);
                    }
                    //Last end time
                    if (i == TableOfActivities.size() - 1) {
                        end = convertToDate(s[2]);
                        //Still ugly af
                        Integer[] hr = heartrate.toArray(new Integer[heartrate.size()]);
                        SportsSession session = new SportsSession(type, start, cooldown, end, hr);
                        end = new GregorianCalendar(1, 1, 1);
                        start = new GregorianCalendar(1, 1, 1);
                        cooldown = new GregorianCalendar(1, 1, 1);
                        heartrate = new ArrayList<Integer>();
                        sessions.add(session);
                    }
                    break;
            }
        }


        return sessions;
    }

    private static Calendar convertToDate(String s) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd_HH:mm:ss", Locale.getDefault());
        try {
            cal.setTime(sdf.parse(s));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return cal;
    }
}
