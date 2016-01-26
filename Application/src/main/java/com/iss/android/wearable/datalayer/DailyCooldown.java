package com.iss.android.wearable.datalayer;

import android.graphics.Color;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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

    public double alphaAllData = -1;
    public double alpha2min = -1;
    public double morningHR = -1;
    public double eveningHR = -1;

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

        double[] morningEveningHRs = DataProcessingManager.getMorningEveningHRs(daystr, userID);

        morningHR = morningEveningHRs[0];
        eveningHR = morningEveningHRs[1];

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
