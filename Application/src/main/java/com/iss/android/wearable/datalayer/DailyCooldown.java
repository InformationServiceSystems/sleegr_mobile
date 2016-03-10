package com.iss.android.wearable.datalayer;

import android.graphics.Color;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Iaroslav on 1/22/2016.
 */
public class DailyCooldown {

    private double[] paramsNoisy = null;
    private double[] paramsControlled = null;
    List<ISSRecordData> recoveryCD = new ArrayList<>();
    List<ISSRecordData> cooldown = new ArrayList<>();
    List<ISSRecordData> morningHRdata = new ArrayList<>();
    List<ISSRecordData> eveningHRdata = new ArrayList<>();

    public TimeSeries cooldownSeries = null;
    public TimeSeries expAllData = null;
    public TimeSeries exp2min = null;

    public Double alphaAllData = null;
    public Double alpha2min = null;
    public Double morningHR = null;
    public Double eveningHR = null;
    public Double RPE = null;
    public Double DALDA = null;
    public Double DeepSleep = null;
    public Double Steps = null;

    Visualizations visualizations = new Visualizations();

    public DailyCooldown(Date day){

        // make sure that error in computations does not break the whole system
        try {
            InitializeCooldown(day);
        }catch (Exception ex){
        }
    }

    // A method calculating the file size of a given file.
    public long FileSize(File file){
        long result = 0;
        try {
            URL url = file.toURI().toURL();
            InputStream stream = url.openStream();
            result = stream.available();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public long LoadPrecalculatedValues(String daystr, String userID){

        // read all files in folder ending with .csv
        File folder = new File(DataStorageManager.userDataFolder , daystr);
        File csvSize = new File(folder, "csvsizes.sync");

        long totalCSVsSize = 0;

        if (folder.listFiles() == null){
            return totalCSVsSize;
        }

        for(File file: folder.listFiles()){

            if (!file.getName().endsWith(".csv"))
                continue;

            if (file.getName().endsWith("Summary.csv"))
                continue;

            totalCSVsSize += FileSize(file);
        }

        boolean recalculate_cooldown = true;

        try {
            long previous_size = (long)Serializer.DeserializeFromFile(csvSize);
            recalculate_cooldown = previous_size != totalCSVsSize;
        } catch (Exception e) {

        }

        if (!recalculate_cooldown){

            List<ISSRecordData> minedParameters = CSVManager.ReadCSVdata(daystr, userID, "Summary");

            if (minedParameters == null)
                return totalCSVsSize;

            for (ISSRecordData record: minedParameters){

                if (record.MeasurementType == ISSRecordData.MEASUREMENT_ALPHA){
                    alpha2min = Double.valueOf(record.Value1);
                    paramsControlled = new double[] {record.Value1, record.Value2, record.Value3};
                }

                if (record.MeasurementType == ISSRecordData.MEASUREMENT_ALPHA_NOISY){
                    alphaAllData = Double.valueOf(record.Value1);
                    paramsNoisy = new double[] {record.Value1, record.Value2, record.Value3};
                }

                if (record.MeasurementType == ISSRecordData.MEASUREMENT_DALDA){
                    DALDA = Double.valueOf(record.Value1);
                }

                if (record.MeasurementType == ISSRecordData.MEASUREMENT_DEEP_SLEEP){
                    DeepSleep = Double.valueOf(record.Value1);
                }

                if (record.MeasurementType == ISSRecordData.MEASUREMENT_HR_MORNING){
                    morningHR = Double.valueOf(record.Value1);
                }

                if (record.MeasurementType == ISSRecordData.MEASUREMENT_HR_EVENING){
                    eveningHR = Double.valueOf(record.Value1);
                }

                if (record.MeasurementType == ISSRecordData.MEASUREMENT_RPE){
                    RPE = Double.valueOf(record.Value1);
                }

                if (record.MeasurementType == ISSRecordData.MEASUREMENT_STEPS) {
                    Steps = Double.valueOf(record.Value1);
                }

            }

            return -1;
        }

        return totalCSVsSize;

    }



    public void InitializeCooldown(Date day){

        // read necessary data
        String userID = DataSyncService.getUserID();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String daystr = df.format(day);

        ///////////////// here is where the caching is done /////////////////////

        //long totalCSVsSize = LoadPrecalculatedValues(daystr, userID);

        ///////////////// here is where the caching is done /////////////////////

        recoveryCD = CSVManager.ReadCSVdata(daystr, userID, "Recovery");
        cooldown = CSVManager.ReadCSVdata(daystr, userID, "Cooldown");

        // legacy support code
        if (recoveryCD == null && cooldown == null ){
            ArrayList<ISSRecordData> lastActivity = DataProcessingManager.readLastSportsActivity(daystr, userID);

            // if there are no activities for the day ...
            if (lastActivity != null){
                recoveryCD = DataProcessingManager.extractLastCooldown(lastActivity);
            }
        }

        // extract resting heart rate data
        ArrayList<List<ISSRecordData>> resting = CSVManager.ReadSplitCSVdata(daystr, userID, "Resting");

        for (List<ISSRecordData> rest: resting){

            Calendar cal = Calendar.getInstance();
            cal.setTime(rest.get(0).getTimestamp());

            if (cal.get(Calendar.HOUR_OF_DAY) > 18 || cal.get(Calendar.HOUR_OF_DAY) < 4){
                eveningHRdata.addAll(rest);
            }
            else
            {
                morningHRdata.addAll(rest);
            }
        }

        // extract first 5 min of recoveryCD
        Visualizations.Subplot subplot = visualizations.AddGraph("Cooldown analysis");

        // makes things easier
        if (recoveryCD == null){
            recoveryCD = new ArrayList<>();
        }

        if (cooldown == null){
            cooldown = new ArrayList<>();
        }

        ArrayList<ISSRecordData> allData = new ArrayList<>();
        allData.addAll(cooldown);
        allData.addAll(recoveryCD);
        allData.addAll(eveningHRdata);


        ArrayList<ISSRecordData> controlledData = new ArrayList<>();
        controlledData.addAll(cooldown);
        controlledData.addAll(eveningHRdata);

        /*if (eveningHRdata.size() == 0 && cooldown.size() > 0){

            Calendar clnd = Calendar.getInstance();
            clnd.setTime(cooldown.get(0).getTimestamp());
            clnd.set(Calendar.HOUR_OF_DAY, 23);
            clnd.set(Calendar.MINUTE, 0);
            ISSRecordData restDataImpute = cooldown.get(0);
            for (int i = 0; i < 60 * 3; i += 3){
                clnd.add(Calendar.SECOND, 3);
                ISSRecordData rec = new ISSRecordData(0, ISSRecordData.MEASUREMENT_HR, ISSRecordData.sdf.format(clnd.getTime()), "Resting", 50.0f,0,0);
                eveningHRdata.add(rec);
            }
        }*/

        cooldownSeries = ConvertToTS(allData, "Raw data");
        subplot.Add(cooldownSeries, Color.BLUE);

        /*
        if (cooldown.size()>0){
            paramsControlled = ComputeExponentFit(paramsControlled, controlledData, allData, subplot, "Controlled data", Color.RED);
            if (paramsControlled != null)
                alpha2min = paramsControlled[0];
        }*/

        // impute cooldown artificially if not measured (which is the expected scenario)
        if (cooldown.size() < 0){
            Date endTraining = extractStartOfExercise(allData);
            if (endTraining != null){
                // read the cooldown profile, if exists
                cooldown = CSVManager.ReadUserHRProfile();
                if(cooldown != null){
                    // set up the timestamps for imputation for the cooldown
                    for (ISSRecordData record: cooldown){
                        Calendar stamp = Calendar.getInstance();
                        stamp.setTime(endTraining);
                        stamp.add(Calendar.SECOND, (int) record.Value2);
                        record.Timestamp = ISSRecordData.sdf.format(stamp.getTime());
                    }
                }
            }
        }

        paramsNoisy = ComputeExponentFit(paramsNoisy, allData, allData, subplot, "All data", Color.GREEN);
        if (paramsNoisy != null)
            alphaAllData = paramsNoisy[0];


        // get morning / evening hrs. Could use the loaded data, but using existing function is easier
        double[] morningEveningHRs = DataProcessingManager.getMorningEveningHRs(daystr, userID);

        if (morningEveningHRs[0] > 0)
            morningHR = morningEveningHRs[0];
        if (morningEveningHRs[1] > 0)
            eveningHR = morningEveningHRs[1];

        // read the Feedback activity file, if it exists

        List<ISSRecordData> feedback = CSVManager.ReadCSVdata(daystr, userID, "Feedback");

        if (feedback != null){

            DALDA = 0.0;

            for (ISSRecordData record: feedback){
                if (record.ExtraData.contains("RPE")){
                    RPE = Double.valueOf(record.Value1);
                }
                else{
                    DALDA += record.Value1;
                }
            }

        }

        // read the sleep file

        HashMap<String, Double> sleepData = CSVManager.ReadSleepData();

        if (sleepData.containsKey(daystr)){
            DeepSleep = sleepData.get(daystr);
        }


        ///////////////// here is where the caching is done /////////////////////

        /*if (totalCSVsSize < 0)
            return;

        File folder = new File(DataStorageManager.userDataFolder , daystr);
        File csvSize = new File(folder, "csvsizes.sync");
        try {
            Serializer.SerializeToFile(totalCSVsSize, csvSize);
        } catch (IOException e) {
            e.printStackTrace();
        }*/


        File summaryFile = CSVManager.GetCSVfilename(daystr, userID, "Summary");
        String timeStamp = ISSRecordData.sdf.format(day);

        if (summaryFile.exists())
            summaryFile.delete();

        List<ISSRecordData> minedParameters = new ArrayList<>();

        if (alpha2min != null)
            minedParameters.add(new ISSRecordData(0, ISSRecordData.MEASUREMENT_ALPHA, timeStamp, "Summary", (float)((double) paramsControlled[0]),(float)((double) paramsControlled[1]),(float)((double) paramsControlled[2])));
        if (alphaAllData != null)
            minedParameters.add(new ISSRecordData(0, ISSRecordData.MEASUREMENT_ALPHA_NOISY, timeStamp, "Summary", (float)((double) paramsNoisy[0]),(float)((double) paramsNoisy[1]),(float)((double) paramsNoisy[2])));
        if (DALDA != null)
            minedParameters.add(new ISSRecordData(0, ISSRecordData.MEASUREMENT_DALDA, timeStamp, "Summary", (float)((double)DALDA),0,0));
        if (DeepSleep != null)
            minedParameters.add(new ISSRecordData(0, ISSRecordData.MEASUREMENT_DEEP_SLEEP, timeStamp, "Summary", (float)((double)DeepSleep),0,0));
        if (morningHR != null)
            minedParameters.add(new ISSRecordData(0, ISSRecordData.MEASUREMENT_HR_MORNING, timeStamp, "Summary", (float)((double)morningHR),0,0));
        if (eveningHR != null)
            minedParameters.add(new ISSRecordData(0, ISSRecordData.MEASUREMENT_HR_EVENING, timeStamp, "Summary", (float)((double)eveningHR),0,0));
        if (RPE != null)
            minedParameters.add(new ISSRecordData(0, ISSRecordData.MEASUREMENT_RPE, timeStamp, "Summary", (float)((double)RPE),0,0));
        if (Steps != null)
            minedParameters.add(new ISSRecordData(0, ISSRecordData.MEASUREMENT_STEPS, timeStamp, "Summary", (float) ((double) RPE), 0, 0));

       DataStorageManager.SaveNewDataToFile((ArrayList<ISSRecordData>) minedParameters, userID);




        ///////////////// here is where the caching is done /////////////////////

    }

    // A method determining the exact time at which an exercise (list of ISSRecordData) started
    Date extractStartOfExercise(ArrayList<ISSRecordData> data){

        Date result = null;

        for (ISSRecordData record : data) {
            if (record.MeasurementType == ISSRecordData.MEASUREMENT_TRAINING_END) {
                ISSRecordData first = data.get(0);
                Date timestamp = first.getTimestamp();
                Calendar cal = Calendar.getInstance();
                cal.setTime(timestamp);
                long firstTime = cal.getTime().getTime();
                cal.set(Calendar.HOUR_OF_DAY, (int) record.Value1);
                cal.set(Calendar.MINUTE, (int) record.Value2);
                result = cal.getTime();
            }
        }

        return result;

    }

    // No idea what exactly this does, maybe you can fill this out, Iaroslav?
    double [] ComputeExponentFit(double[] loadedParams, ArrayList<ISSRecordData> data, ArrayList<ISSRecordData> allData, Visualizations.Subplot subplot, String label, int color){

        if (data.size() == 0){
            return null;
        }

        double[] parameters = null;

        // check if timeshift might be needed (if the data was collected right after the exercise)

        if (loadedParams == null) {

            Double timeshift = 0.0;

            for (ISSRecordData record : data) {
                if (record.MeasurementType == ISSRecordData.MEASUREMENT_TRAINING_END) {
                    ISSRecordData first = data.get(0);
                    Date timestamp = first.getTimestamp();
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(timestamp);
                    long firstTime = cal.getTime().getTime();
                    cal.set(Calendar.HOUR_OF_DAY, (int) record.Value1);
                    cal.set(Calendar.MINUTE, (int) record.Value2);
                    long endTime = cal.getTime().getTime();

                    timeshift = Double.valueOf(firstTime - endTime) / 1000.0;
                }
            }

            parameters = DataProcessingManager.getCooldownParameters(data, timeshift);

        }
        else{
            parameters = loadedParams;
        }

        if (parameters == null)
            return null;

        Double alpha = parameters[0];

        cooldownSeries = ConvertToTS(allData, label);
        expAllData = DataProcessingManager.ComputeExponent(parameters, cooldownSeries);

        subplot.Add(expAllData, color);

        return parameters;

    }

    // A method transforming measured heart rates into TimeSeries so it can be plotted
    private TimeSeries ConvertToTS(ArrayList<ISSRecordData> firstTraining, String name) {

        TimeSeries series = new TimeSeries(name);

        for (ISSRecordData recordData: firstTraining){
            if (recordData.MeasurementType != 21){
                continue;
            }

            series.AddValue(recordData.getTimestamp(), recordData.Value1);

        }

        return  series;

    }



}
