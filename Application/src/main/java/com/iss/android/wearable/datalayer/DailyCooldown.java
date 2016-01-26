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

    List<ISSRecordData> cooldown = new ArrayList<>();
    List<ISSRecordData> cooldown2min = new ArrayList<>();
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

        cooldown = CSVManager.ReadCSVdata(daystr, userID, "Cooldown");

        if (cooldown == null){
            ArrayList<ISSRecordData> lastActivity = DataProcessingManager.readLastSportsActivity(daystr, userID);

            // if there are no activities for the day ...
            if (lastActivity != null){
                cooldown = DataProcessingManager.extractLastCooldown(lastActivity);
            }
        }

        // extract first 5 min of cooldown
        if (cooldown != null){
            //
            long first = cooldown.get(0).getTimestamp().getTime();
            int i = 0;

            while (cooldown.get(i).getTimestamp().getTime() - first < 1000 * 60 * 5){
                cooldown2min.add(cooldown.get(i));
                i++;
            }

        }

        ArrayList<List<ISSRecordData>> resting = CSVManager.ReadSplitCSVdata(daystr, userID, "Resting");

        for (List<ISSRecordData> rest: resting){

            Calendar cal = Calendar.getInstance();
            cal.setTime(rest.get(0).getTimestamp());

            if (cal.get(Calendar.HOUR_OF_DAY) > 18){
                eveningHRdata.addAll(rest);
            }
            else
            {
                morningHRdata.addAll(rest);
            }
        }

        // process cooldown to get recovery curves
        if (cooldown != null){

            cooldown2min.addAll(eveningHRdata);
            double[] cooldownParameters = DataProcessingManager.getCooldownParameters((ArrayList<ISSRecordData>) cooldown);
            double[] cooldownParameters2min = DataProcessingManager.getCooldownParameters((ArrayList<ISSRecordData>) cooldown2min);

            alphaAllData = cooldownParameters[0];
            alpha2min = cooldownParameters2min[0];

            cooldownSeries = ConvertToTS((ArrayList<ISSRecordData>) this.cooldown, "Cooldown");
            expAllData = DataProcessingManager.ComputeExponent(cooldownParameters, cooldownSeries);
            exp2min = DataProcessingManager.ComputeExponent(cooldownParameters2min, cooldownSeries);

            Visualizations.Subplot subplot = visualizations.AddGraph("Cooldown analysis");
            subplot.Add(cooldownSeries, Color.BLUE);
            subplot.Add(expAllData, Color.GREEN);
            subplot.Add(exp2min, Color.RED);

        }

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
