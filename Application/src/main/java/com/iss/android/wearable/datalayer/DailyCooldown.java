package com.iss.android.wearable.datalayer;

import android.graphics.Color;

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

    Visualizations visualizations = new Visualizations();

    public DailyCooldown(Date day){

        // read necessary data
        String userID = DataSyncService.getUserID();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String daystr = df.format(day);

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


        if (cooldown.size()>0)
            alpha2min = ComputeExponentFit(controlledData, allData, subplot, "Controlled data", Color.RED);
        alphaAllData = ComputeExponentFit(allData, allData, subplot, "All data", Color.GREEN);


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

    }


    Double ComputeExponentFit(ArrayList<ISSRecordData> data,ArrayList<ISSRecordData> allData, Visualizations.Subplot subplot, String label, int color){

        if (data.size() == 0){
            return null;
        }


        // check if timeshift might be needed (if the data was collected right after the exercise)

        Double timeshift = 0.0;

        for (ISSRecordData record: data){
            if (record.MeasurementType == ISSRecordData.MEASUREMENT_TRAINING_END){
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


        double[] cooldownParameters = DataProcessingManager.getCooldownParameters((ArrayList<ISSRecordData>) data, timeshift);

        Double alpha = cooldownParameters[0];

        cooldownSeries = ConvertToTS((ArrayList<ISSRecordData>) allData, label);
        expAllData = DataProcessingManager.ComputeExponent(cooldownParameters, cooldownSeries);

        subplot.Add(expAllData, color);

        return alpha;

    }

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
