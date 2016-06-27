package com.iss.android.wearable.datalayer;

import java.util.ArrayList;

/**
 * Created by Euler on 1/8/2016.
 */
public class ExponentFitter_Old {

    public static double [] computeFitGrd(double[] p, ArrayList<Double> X, ArrayList<Double> Y){

        double log2 = 1;
        double norm = 0;
        double Da = 0, Db = 0, Dc = 0;

        for (int i = 0; i < X.size(); i++) {

            double a = p[0], b = p[1], c = p[2];
            double p2xa = Math.exp( -X.get(i) / a);
            double value_in_brackets = (b*p2xa - Y.get(i)+c);

            norm += value_in_brackets * value_in_brackets / X.size();

            double da = log2 * X.get(i) * b * p2xa * value_in_brackets / (a*a);
            double db = log2 * p2xa * value_in_brackets;
            double dc = value_in_brackets;

            Da += da;
            Db += db;
            Dc += dc;
        }

        Da = Da / X.size();
        Db = Db / X.size();
        Dc = Dc / X.size();
        //norm = Da*Da + Db*Db + Dc*Dc;

        return new double [] {Da, Db, Dc, norm} ;

    }

    public static double vect_norm(double [] vector){
        double result = 0;

        for (int i = 0; i < vector.length; i++) {
            result += vector[i] * vector[i];
        }

        return result;
    }

    public static void applyAdam(double [] p, double alpha, double [] gr, double [] gn){

        for (int i = 0; i < p.length; i++) {
            gn[i] = gn[i] * 0.9 + gr[i]*gr[i] * 0.1;
            p[i] = p[i] - alpha * gr[i] / Math.sqrt( gn[i] + 1e-10 );
        }

    }



    public static double minval( ArrayList<Double> Y){

        double min = Y.get(0);

        for (int i = 0; i < Y.size(); i++) {
            if (min > Y.get(i)) {
                min = Y.get(i);
            }
        }

        return min;

    }

    public static double maxval( ArrayList<Double> Y){

        double mx = Y.get(0);

        for (int i = 0; i < Y.size(); i++) {
            if (mx < Y.get(i)) {
                mx = Y.get(i);
            }
        }

        return mx;

    }


    public static double [] fitLowerExpGD(ArrayList<Double> Xr, ArrayList<Double> Yr){


        if (Xr.size() == 0){
            return new double[]{1000.0,0.0,0.0};
        }

        // start with the maximum value in the data; this allows to reduce initial HR "heating" artifact



        double maxY = maxval(Yr);

        boolean afterMax = false;

        ArrayList<Double> X = new ArrayList<>();
        ArrayList<Double> Y = new ArrayList<>();

        for (int i = 0 ; i < Xr.size(); i++){

            if (Yr.get(i) == maxY)
                afterMax = true;

            if (i > 30)
                afterMax = true; // we assume that heating up takes around two minutes

            if (afterMax){
                X.add(Xr.get(i));
                Y.add(Yr.get(i));
            }

        }

        // check if there is nothing but noise ....
        if (X.size() == 0){
            return new double[]{1000.0,0.0,0.0};
        }

        double [] result = new double [] {1000, 100, 50 };
        double [] grdscl = new double [] {1, 1, 1 };

        double norm = 100;
        double pval = 0;

        int maxidx = 10000; // protects against convergence problems

        while(maxidx > 0){
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
