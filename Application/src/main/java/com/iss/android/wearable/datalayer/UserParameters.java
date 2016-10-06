package com.iss.android.wearable.datalayer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import static com.iss.android.wearable.datalayer.DateTimeManager.getDateFromToday;

/**
 * Created by Euler on 1/23/2016.
 */
public class UserParameters {

    public UserParameters(int timespan) {

        // load RPE if available

        // compute the timespan
        TimeSeries userRPE = new TimeSeries("User RPE");
        TimeSeries userDALDA = new TimeSeries("User DALDA");
        TimeSeries alpha = new TimeSeries("Alpha, all data");
        TimeSeries alpha2min = new TimeSeries("Alpha, 2 min training");
        TimeSeries userDS = new TimeSeries("Deep sleep");

        for (int i = 0; i < timespan; i++) {

            try {

                int percent = i * 100 / timespan;
                DataSyncService.OutputEventSq(percent + "%");

                Date date = getDateFromToday(i);
                DailyData dailyData = new DailyData(date);

                if (dailyData.getDALDA() != null) {
                    userDALDA.AddFirstValue(date, dailyData.getDALDA());
                }

                if (dailyData.getRPE() != null) {
                    userRPE.AddFirstValue(date, dailyData.getRPE());
                }
                if (dailyData.getAlpha2min() != null) {
                    alpha2min.AddFirstValue(date, dailyData.getAlpha2min());
                }
                if (dailyData.getAlphaAllData() != null) {
                    alpha.AddFirstValue(date, dailyData.getAlphaAllData());
                }

                if (dailyData.getDeepSleep() != null) {
                    userDS.AddFirstValue(date, dailyData.getDeepSleep());
                }

            } catch (Exception ex) {
                DataSyncService.OutputEventSq(ex.toString());
            }

            DataSyncService.OutputEventSq("100%");

        }

    }


    // Deletes values in bins where counts is <=0
    public void smoothenBins(double[] bins, double[] counts) {

        double cv = 0;

        for (int i = 0; i < bins.length; i++) {
            if (counts[i] > 0) {
                cv = bins[i];
            }
            bins[i] = cv;
        }

        for (int i = bins.length - 1; i >= 0; i--) {
            if (counts[i] > 0) {
                cv = bins[i];
            }
            bins[i] = cv;
        }

    }

    // Another method which only computes the date given a certain date and an offset.
    public Date offsetDate(Date input, int offset) {

        Calendar clnd = Calendar.getInstance();
        clnd.setTime(input);
        clnd.add(Calendar.DAY_OF_MONTH, -offset);
        return clnd.getTime();

    }

    // Produces a predictive TimeSeries given actual timeseries.
    public TimeSeries predictTimeSeries(TimeSeries xReq, TimeSeries xActual, TimeSeries yActual, int offset) {

        TimeSeries result = new TimeSeries(yActual.name + ", pred.");

        for (int i = 0; i < xReq.Values.size() - offset; i++) {

            Date date = xReq.Values.get(i).x;
            Double val = xReq.Values.get(i).y;

            TimeSeries xActBefore = xActual.beforeDate(date);
            TimeSeries yActBefore = yActual.beforeDate(date);

            if (xActBefore.Values.size() == 0) {
                continue;
            }

            HashMap<String, Double> predValues = yActBefore.toDictionary();

            ArrayList<Double> xvalues = new ArrayList<>();
            ArrayList<Double> yvalues = new ArrayList<>();

            // generate training set
            for (int j = 0; j < xActBefore.Values.size(); j++) {

                Date locdate = offsetDate(xActBefore.Values.get(j).x, offset);
                double x = xActBefore.Values.get(j).y;

                if (!predValues.containsKey(TimeSeries.formatData(locdate))) {
                    continue;
                }

                double y = predValues.get(TimeSeries.formatData(locdate));

                xvalues.add(x);
                yvalues.add(y);

            }

            double prediction = predictRecovery(xvalues, yvalues, val);
            result.AddValue(offsetDate(date, offset), prediction);
        }

        return result;

    }

    // Predicts the recovery rate from history values.
    public double predictRecovery(ArrayList<Double> pastX, ArrayList<Double> pastY, Double futureX) {


        double result = -1;

        // collect values of past records into bins

        double[] bins = new double[11];
        double[] counts = new double[11];

        for (int i = 0; i < 11; i++) {
            bins[i] = 0;
            counts[i] = 0;
        }

        for (int i = 0; i < pastY.size(); i++) {

            if (pastX.get(i) < 0) {
                continue;
            }

            bins[((int) Math.round(pastX.get(i)))] += pastY.get(i);
            counts[((int) Math.round(pastX.get(i)))] += 1;
        }

        for (int i = 0; i < 11; i++) {
            if (counts[i] == 0) {
                continue;
            }
            bins[i] = bins[i] / counts[i];
        }

        smoothenBins(bins, counts);

        result = bins[((int) Math.round(futureX))];

        return result;

    }

    // Computes regulatory conformity of values to the requirements in a given timewindow
    private TimeSeries ComputeCompliences(TimeSeries requirements, TimeSeries values, int timewindow) {

        TimeSeries result = new TimeSeries(values.name + ", avg. divergence");

        for (int i = 0; i < requirements.Values.size(); i++) {

            Date x = requirements.Values.get(i).x;

            TimeSeries req = requirements.beforeDate(x);
            TimeSeries vals = values.beforeDate(x);

            ArrayList<Double> seq1 = new ArrayList<>();
            ArrayList<Double> seq2 = new ArrayList<>();

            // construct data
            HashMap<String, Double> reqVals = req.toDictionary();

            for (int j = 0; j < vals.Values.size(); j++) {

                int last = vals.Values.size() - j - 1;

                Date d = vals.Values.get(last).x;

                if (!reqVals.containsKey(TimeSeries.formatData(d)))
                    continue;

                Double yp = reqVals.get(TimeSeries.formatData(d));
                Double y = vals.Values.get(last).y;

                seq1.add(0, yp);
                seq2.add(0, y);

                if (seq1.size() >= timewindow) {
                    break;
                }

            }

            if (seq1.size() == 0) {
                result.AddValue(x, -1);
                continue;
            }

            result.AddValue(x, ComplienceMeasure(seq1, seq2));

        }


        return result;

    }

    private double ComplienceMeasure(ArrayList<Double> seq1, ArrayList<Double> seq2) {

        double result = 0;

        for (int i = 0; i < seq1.size(); i++) {
            result += Math.abs(seq1.get(i) - seq2.get(i)) / seq1.size();
        }

        return result;

    }

}
