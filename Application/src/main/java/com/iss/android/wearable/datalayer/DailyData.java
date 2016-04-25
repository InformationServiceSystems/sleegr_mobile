package com.iss.android.wearable.datalayer;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by micha on 25.04.2016.
 */
public class DailyData {
    // This is an object containing all data available for a single day.
    ArrayList<ISSRecordData> data = new ArrayList<ISSRecordData>();
    private Double DALDA;
    private Double RPE;
    private Double alpha2min;
    private Double alphaAllData;
    private Double deepSleep;
    private Double morningHR;
    private Double eveningHR;
    public Visualizations visualizations;

    public DailyData(Date time) {
        data = DatabaseManager.getData(time);
    }

    public Double getDALDA() {
        return DALDA;
    }

    public Double getRPE() {
        return RPE;
    }

    public Double getAlpha2min() {
        return alpha2min;
    }

    public Double getAlphaAllData() {
        return alphaAllData;
    }

    public Double getDeepSleep() {
        return deepSleep;
    }

    public Double getMorningHR() {
        return morningHR;
    }

    public Double getEveningHR() {
        return eveningHR;
    }

    public Visualizations getVisualizations() {
        return visualizations;
    }
}
