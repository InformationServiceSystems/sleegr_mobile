package com.iss.android.wearable.datalayer;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by micha on 25.04.2016.
 */

// TODO: Fill up this class in the constructor
class DailyData {
    // This is an object containing all data available for a single day.
    ArrayList<ISSRecordData> data = new ArrayList<ISSRecordData>();
    private Double DALDA;
    private Double RPE;
    private Double alpha2min;
    private Double alphaAllData;
    private Double deepSleep;
    private Double morningHR;
    private Double eveningHR;

    DailyData(Date time) {
        data = DataStorageManager.getData(time);
    }

    Double getDALDA() {
        return DALDA;
    }

    Double getRPE() {
        return RPE;
    }

    Double getAlpha2min() {
        return alpha2min;
    }

    Double getAlphaAllData() {
        return alphaAllData;
    }

    Double getDeepSleep() {
        return deepSleep;
    }

    Double getMorningHR() {
        return morningHR;
    }

    Double getEveningHR() {
        return eveningHR;
    }
}
