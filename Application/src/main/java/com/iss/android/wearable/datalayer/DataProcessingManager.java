package com.iss.android.wearable.datalayer;

import android.provider.ContactsContract;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by Euler on 1/7/2016.
 */
public class DataProcessingManager {

    public static  String [] GetAllStates(){

        return new String[] {
                "Resting",
                "Swimming",
                "Cycling",
                "Running",
                "Athletics"
        };

    }

    public static double[] getCooldownParameters(ArrayList<ISSRecordData> data) {

        Calendar startTime = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd_HH:mm:ss");

        try {
            startTime.setTime(sdf.parse(data.get(0).Timestamp));
        } catch (ParseException e) {
        }

        Calendar currentTime = Calendar.getInstance();

        ArrayList<Double> heartRates = new ArrayList<>();
        ArrayList<Double> times = new ArrayList<>();

        for (ISSRecordData record : data) {

            if (record.MeasurementType != 21) {
                continue;
            }

            try {
                currentTime.setTime(sdf.parse(record.Timestamp));
            } catch (ParseException e) {
            }

            double time = (currentTime.getTime().getTime() - startTime.getTime().getTime()) / 1000;
            double hr = record.Value1;

            heartRates.add(hr);
            times.add(time);

        }

        //double[] params = fitExponent(times, heartRates);
        double[] params = ExponentFitter.fitLowerExpGD(times, heartRates);

        return params;

    }

    public static double sum(ArrayList<Double> v){

        double result = 0;

        for (int i = 0; i < v.size(); i++){
            result += v.get(i);
        }

        return result;

    }

    public static double dot(ArrayList<Double> a, ArrayList<Double> b){

        double result = 0;

        for (int i = 0; i < b.size(); i++){
            result += a.get(i) * b.get(i);
        }

        return result;

    }

    public static double min(ArrayList<Double> arr){

        double result = arr.get(0);

        for (int i = 0; i < arr.size(); i ++){
            if (arr.get(i) < result)
                result = arr.get(i);
        }

        return result;

    }

    public static  double [] fitExponent(ArrayList<Double> X, ArrayList<Double> Y) {

        double minBias = 60; //min(Y) - 0.5;
        double bestObj = 1e+10;
        double[] globalSolution = new double[3];

        for (int iter = 0; iter < 1; iter++) {

            double bias = minBias - iter*2;

            ArrayList<Double> x = new ArrayList<>(X);
            ArrayList<Double> yl = new ArrayList<>();

            for (Double val : Y) {
                yl.add(Math.log(val - bias));
            }

            ArrayList<Double> yt = new ArrayList<>(yl);
            ArrayList<Double> xt = new ArrayList<>(x);

            double xTx = 0;
            double xTe = 0;
            double xTy = 0;

            double eTx = 0;
            double eTe = 0;
            double eTy = 0;

            double[] solution = null;

            for (int k = 0; k < 2; k++) {
                xTx += dot(x, x);
                xTe += sum(x);
                xTy += dot(x, yl);

                eTx += sum(x);
                eTe += x.size();
                eTy += sum(yl);

                double[][] A = new double[][]{{xTx, xTe}, {eTx, eTe}};
                double[] b = new double[]{xTy, eTy};

                solution = Solve2d(A, b);

                // remove values from x that are not less than curve

                ArrayList<Double> x_new = new ArrayList<>();
                ArrayList<Double> yl_new = new ArrayList<>();

                for (int i = 0; i < x.size(); i++) {
                    if (x.get(i) * solution[0] + solution[1] < yl.get(i)) {
                        x_new.add(x.get(i));
                        yl_new.add(yl.get(i));
                    }
                }

                x = x_new;
                yl = yl_new;

            }

            double obj = 0;

            for (int i = 0; i < x.size(); i++) {
                double diff = (xt.get(i) * solution[0] + solution[1] - yt.get(i));
                obj += diff * diff;
            }

            if (obj < bestObj){
                bestObj = obj;
                globalSolution[0] = solution[0];
                globalSolution[1] = solution[1];
                globalSolution[2] = bias;
            }

        }

        return globalSolution;

    }


    public static  boolean TestZerosDiag(double[][] A) {
        return A[0][0] == 0 || A[1][1] == 0;
    }

    public static  void NormalizeRows(double[][] A, double[] b) {

        A[0][1]=A[0][1]/A[0][0];
        b[0]=b[0]/A[0][0];
        A[0][0]=1;

        A[1][0]=A[1][0]/A[1][1];
        b[1]=b[1]/A[1][1];
        A[1][1]=1;

    }

    public static  void TurnToIdentity(double[][] A, double[] b) {
        A[0][0] = A[0][0] - A[1][0] * A[0][1];
        A[0][1] = 0;
        b[0] = b[0] - b[1] * A[0][1];

        A[1][0] = 0;
        A[1][1] = A[1][1] - A[0][1] * A[1][0];
        b[1] = b[1] - b[0] * A[1][0];
    }

    public static  double [] Solve2d(double[][] A, double[] b) {

        if (TestZerosDiag(A))
            A = new double [][] { A[1], A[0] };

        if (TestZerosDiag(A))
            return null;

        NormalizeRows(A, b);
        TurnToIdentity(A, b);

        double [] result = new double[] { b[0] / A[0][0], b[1] / A[1][1] };

        return result;
    }

    public static String getDayFromToday(int dayoffset){

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cl = Calendar.getInstance();
        cl.add(Calendar.DAY_OF_YEAR, -dayoffset);
        String currentDateandTime = df.format(cl.getTime());

        return currentDateandTime;

    }


    static ArrayList<ISSRecordData> extractLastCooldown(ArrayList<ISSRecordData> lastActivity){

        int i = lastActivity.size() - 1;

        while(!lastActivity.get(i).ExtraData.contains("Cooling")){
            i--;
        }

        if (i < 0){
            return null ;
        }

        // get all the cooling measurements

        ArrayList<ISSRecordData> accumulator = new ArrayList<>();

        while(lastActivity.get(i).ExtraData.contains("Cooling")){
            accumulator.add(0, lastActivity.get(i));
            i--;
        }

        return accumulator;

    }

    static ArrayList<ISSRecordData> readLastSportsActivity(String day, String UserID){

        String[] activities = {
                "Swimming",
                "Cycling",
                "Running",
                "Athletics"
        };

        // select the last activity for a certain day
        ArrayList<ISSRecordData> lastActivity = null;

        for (String act: activities){

            ArrayList<ISSRecordData> inner = (ArrayList<ISSRecordData>) CSVManager.ReadCSVdata(day, UserID, act);

            if (inner == null){
                continue;
            }

            if (lastActivity == null){
                lastActivity = inner;
                continue;
            }

            long diff = lastActivity.get(lastActivity.size()-1).getTimestamp().getTime() - inner.get(inner.size() - 1).getTimestamp().getTime();

            if (diff < 0){
                lastActivity = inner;
            }

        }

        return  lastActivity;

    }

    static ArrayList<ISSRecordData> readRestingDataAfter(String day, String UserID, long time){

        List<ISSRecordData> eveningHR = CSVManager.ReadCSVdata(day, UserID, "Resting");
        List<ISSRecordData> restValues = new ArrayList<ISSRecordData>();

        if (eveningHR != null){


            for (ISSRecordData recordData: eveningHR){
                if (recordData.getTimestamp().getTime() - time > 0){
                    restValues.add(recordData);
                }
            }

            // add the evening values


        }

        return (ArrayList<ISSRecordData>) restValues;

    }

    public static double getRPErating(ArrayList<ISSRecordData> data){

        for (ISSRecordData recordData: data){
            if (recordData.MeasurementType == 1024){
                return recordData.Value1;
            }
        }

        return 0;

    }

    public static double [] GetRecoveryParameters(String day, String UserID){

        double [] result = new double[]{-1, -1};

        ArrayList<ISSRecordData> lastActivity = readLastSportsActivity(day, UserID);

        // if there are no activities for the day ...
        if (lastActivity == null){
            return result;
        }

        // get the last cooldown
        ArrayList<ISSRecordData> lastCooldown = extractLastCooldown(lastActivity);

        result[1] = getRPErating(lastCooldown);

        if (lastCooldown == null){
            return result;
        }

        if (lastCooldown.size() < 100){
            return result;
        }

        // get the recovery measurement after the exercises
        /*ArrayList<ISSRecordData> resting = readRestingDataAfter(day, UserID, lastCooldown.get(lastCooldown.size() - 1).getTimestamp().getTime());
        for (int i = 0; i < 5; i++){
            lastCooldown.addAll(resting);
        }*/


        // extract cooldown parameters
        double[] cooldownParameters = getCooldownParameters(lastCooldown);

        // exponential model with bias is fit to the data:
        // f(x) = exp(a*x + b) + c
        // parameter a corresponds to how fast the curve decays
        result[0] = cooldownParameters[0];

        return result;

    }

    public static double [][] GetDailyRecoveryParameters(int days, String UserID){

        double [] crvs = new double[days];
        double [] rpes = new double[days];

        for (int offset = 0; offset < days; offset++){

            DataSyncService.OutputEventSq("Processing the day " + offset + " ... ");

            try {
                double[] parameters = GetRecoveryParameters(getDayFromToday(offset), UserID);
                crvs[offset] = parameters[0];
                rpes[offset] = parameters[1];
            }
            catch (Exception ex){
                DataSyncService.OutputEventSq(ex.toString());
            }
        }

        return new double[][] {crvs, rpes} ;
    }

}
