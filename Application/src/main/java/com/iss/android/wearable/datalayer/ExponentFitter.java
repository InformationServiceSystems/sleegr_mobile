package com.iss.android.wearable.datalayer;

import java.util.ArrayList;

/**
 * Created by Euler on 1/8/2016.
 */
public class ExponentFitter {

    private static double H = 150.0;

    static double fExp(double[] p, double x) {
        // double a = p[0], t = p[1], c = p[2];
        return (H - p[2]) * Math.exp(-(x - p[1]) / p[0]) + p[2];
    }

    private static double[] computeFitGrd(double[] p, ArrayList<Double> X, ArrayList<Double> Y) {
        // the form of the curve as given in fExp
        // to compute the derivatives of norm of differences, use
        //          http://www.derivative-calculator.net/

        double log2 = 1;
        double norm = 0;
        double Da = 0, Dt = 0, Dc = 0;

        for (int i = 0; i < X.size(); i++) {

            double a = p[0], t = p[1], c = p[2];
            double x = X.get(i);

            double expValue = Math.exp(-(x - t) / a);
            double diff = (H - c) * expValue + c - Y.get(i);

            double da = diff * (H - c) * (x - t) * expValue / (a * a);
            double dt = diff * (H - c) * expValue / a;
            double dc = diff * (1 - expValue);

            Da += da;
            Dt += dt;
            Dc += dc;

            norm += diff * diff;

        }

        Da = Da / X.size();
        Dt = Dt / X.size();
        Dc = Dc / X.size();
        //norm = Da*Da + Db*Db + Dc*Dc;

        return new double[]{Da, Dt, Dc, norm};

    }

    public static double vect_norm(double[] vector) {
        double result = 0;

        for (int i = 0; i < vector.length; i++) {
            result += vector[i] * vector[i];
        }

        return result;
    }

    private static void applyAdam(double[] p, double alpha, double[] gr, double[] gn) {

        for (int i = 0; i < p.length; i++) {
            gn[i] = gn[i] * 0.9 + gr[i] * gr[i] * 0.1;
            p[i] = p[i] - alpha * gr[i] / Math.sqrt(gn[i] + 1e-10);
        }

    }

    public static double minval(ArrayList<Double> Y) {

        double min = Y.get(0);

        for (int i = 0; i < Y.size(); i++) {
            if (min > Y.get(i)) {
                min = Y.get(i);
            }
        }

        return min;

    }

    private static double maxval(ArrayList<Double> Y) {

        double mx = Y.get(0);

        for (int i = 0; i < Y.size(); i++) {
            if (mx < Y.get(i)) {
                mx = Y.get(i);
            }
        }

        return mx;

    }

    static double[] fitLowerExpGD(ArrayList<Double> Xr, ArrayList<Double> Yr) {


        if (Xr.size() == 0) {
            return new double[]{1000.0, 0.0, 0.0};
        }

        // start with the maximum value in the data; this allows to reduce initial HR "heating" artifact


        double maxY = maxval(Yr);

        boolean afterMax = false;

        ArrayList<Double> X = new ArrayList<>();
        ArrayList<Double> Y = new ArrayList<>();

        for (int i = 0; i < Xr.size(); i++) {

            if (Yr.get(i) == maxY)
                afterMax = true;

            if (i > 30)
                afterMax = true; // we assume that heating up takes around two minutes

            if (afterMax) {
                X.add(Xr.get(i));
                Y.add(Yr.get(i));
            }

        }

        // check if there is nothing but noise ....
        if (X.size() == 0) {
            return new double[]{1000.0, 0.0, 0.0};
        }

        double[] result = new double[]{200.0, 0.0, 60.0};
        double[] grdscl = new double[]{1, 1, 1};

        double norm = 100;
        double pval = 0;

        int maxidx = 20000; // protects against convergence problems

        while (maxidx > 0) {
            maxidx--;
            double[] grad = computeFitGrd(result, X, Y);
            double alpha = 1;

            applyAdam(result, alpha, grad, grdscl);

            norm = grad[3];

            if (Math.abs(pval - norm) < 1e-4) {
                break;
            }

            pval = norm;

            //System.out.println(norm);
            //System.out.println(result[0] + "," + result[1] + "," + result[2]);
        }

        return result;

    }

}
