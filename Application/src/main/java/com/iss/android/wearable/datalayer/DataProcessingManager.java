package com.iss.android.wearable.datalayer;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import static com.iss.android.wearable.datalayer.DateTimeManager.getDateFromToday;
import static com.iss.android.wearable.datalayer.DateTimeManager.getDayFromToday;

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

    // Wrapper for when there is no timeshift
    public static double[] getCooldownParameters(ArrayList<ISSRecordData> data) {
        return  getCooldownParameters(data, 0);
    }

    // A method returning parameters describing a curve fitting the measured cooldown heart rates
    public static double[] getCooldownParameters(ArrayList<ISSRecordData> data, double timeShift) {

        Calendar startTime = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd_HH:mm:ss");


        Calendar currentTime = Calendar.getInstance();

        ArrayList<Double> heartRates = new ArrayList<>();
        ArrayList<Double> times = new ArrayList<>();

        // convert sequence to arrays X and Y of time and corresponding values
        for (ISSRecordData record : data) {

            if (record.MeasurementType != 21) {
                continue;
            }

            try {
                currentTime.setTime(sdf.parse(record.Date + "_" + record.Timestamp));
            } catch (ParseException e) {
                Log.d("why?", e.toString());
            }

            if(startTime == null){
                try {
                    startTime = Calendar.getInstance();
                    startTime.setTime(sdf.parse(record.Date + "_" + record.Timestamp));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            double time = (currentTime.getTime().getTime() - startTime.getTime().getTime()) / 1000;

            // add the time shift, in case data does not start from the zero time
            time = time + timeShift;
            double hr = record.Value1;

            heartRates.add(hr);
            times.add(time);

        }

        if (heartRates.size() == 0)
            return null;

        //double[] params = fitExponent(times, heartRates);
        double[] params = ExponentFitter.fitLowerExpGD(times, heartRates);

        return params;

    }

    // A method calculating the sum of double values in a list
    public static double sum(ArrayList<Double> v){

        double result = 0;

        for (int i = 0; i < v.size(); i++){
            result += v.get(i);
        }

        return result;

    }

    // A method calculating the dot product of two vectors.
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

    // A method checking if a given matrix' diagonal is zero.
    public static  boolean TestZerosDiag(double[][] A) {
        return A[0][0] == 0 || A[1][1] == 0;
    }

    // A method normalising the rows of a given matrix
    // (or I'd guess so from the name, although I memorise normalising differently)
    public static  void NormalizeRows(double[][] A, double[] b) {

        A[0][1]=A[0][1]/A[0][0];
        b[0]=b[0]/A[0][0];
        A[0][0]=1;

        A[1][0]=A[1][0]/A[1][1];
        b[1]=b[1]/A[1][1];
        A[1][1]=1;

    }

    // Maybe it's better you describe these methods
    public static  void TurnToIdentity(double[][] A, double[] b) {
        A[0][0] = A[0][0] - A[1][0] * A[0][1];
        A[0][1] = 0;
        b[0] = b[0] - b[1] * A[0][1]; //Isn't this multiplication superfluous since it's always 0?

        A[1][0] = 0;
        A[1][1] = A[1][1] - A[0][1] * A[1][0]; // 0 times 0?
        b[1] = b[1] - b[0] * A[1][0]; //dito
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




    static ArrayList<ISSRecordData> extractLastCooldown(ArrayList<ISSRecordData> lastActivity){

        int i = lastActivity.size() - 1;

        while(!lastActivity.get(i).ExtraData.contains("Cooling")){
            i--;
            if (i < 0){
                return null;
            }
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

    static ArrayList<ISSRecordData> extractFirstTraining(ArrayList<ISSRecordData> lastActivity){

        int i = 0;

        while(lastActivity.get(i).ExtraData.contains("Cooling")){
            i++;
            if (i == lastActivity.size()){
                return null;
            }

        }

        // get training measures

        ArrayList<ISSRecordData> accumulator = new ArrayList<>();

        while(i < lastActivity.size() && !lastActivity.get(i).ExtraData.contains("Cooling")){
            accumulator.add( lastActivity.get(i));
            i++;
        }

        return accumulator;

    }

    static ArrayList<ISSRecordData> extractLastTraining(ArrayList<ISSRecordData> lastActivity){

        int i = lastActivity.size() - 1;

        while(lastActivity.get(i).ExtraData.contains("Cooling")){
            i--;
        }

        if (i < 0){
            return null ;
        }

        // get training measures

        ArrayList<ISSRecordData> accumulator = new ArrayList<>();

        while(i >= 0 && !lastActivity.get(i).ExtraData.contains("Cooling")){
            accumulator.add(0, lastActivity.get(i));
            i--;
        }

        return accumulator;

    }

    public static double getRPErating(ArrayList<ISSRecordData> data){

        for (ISSRecordData recordData: data){
            if (recordData.MeasurementType == 1024){
                return recordData.Value1;
            }
        }

        return 0;

    }

    public static double [] getAVGHR(ArrayList<ISSRecordData> lastTraining) {

        double sum = 0;
        double count = 0;

        for (ISSRecordData record: lastTraining){

            if (record.MeasurementType != 21)
                continue;

            sum += record.Value1;
            count ++;

        }

        double avg = sum / count;

        double dev = 0;
        count = 0;

        for (ISSRecordData record: lastTraining){

            if (record.MeasurementType != 21)
                continue;

            dev += (record.Value1 - avg);
            count ++;

        }

        dev = dev / count;

        return new double [] {avg, dev};

    }

    public static TimeSeries ComputeExponent(double [] p, TimeSeries series){

        long start = series.Values.get(0).x.getTime();
        TimeSeries result = new TimeSeries(series.name + ", exp. fit");

        for (int i =0; i < series.Values.size(); i++){

            double x = (series.Values.get(i).x.getTime()  - start) / 1000.0;
            double y = Math.exp(-x / p[0]) *p[1] + p[2];

            result.AddValue(series.Values.get(i).x, y);

        }

        return result;
    }

}
