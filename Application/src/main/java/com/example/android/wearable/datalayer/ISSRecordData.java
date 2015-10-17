package com.example.android.wearable.datalayer;

import java.io.Serializable;

/**
 * Created by Euler on 10/8/2015.
 */
public class ISSRecordData implements Serializable {

    static final long serialVersionUID = 1L;
    public int UserID;
    public int MeasurementType;
    public String Timestamp;
    public String ExtraData = null;
    public float Value1;
    public float Value2;
    public float Value3;

    public ISSRecordData(int UID, int MType, String timestamp, String extraData, float v1, float v2, float v3){

        UserID = UID;
        MeasurementType = MType;
        Timestamp = timestamp;
        ExtraData = extraData;
        Value1 = v1;
        Value2 = v2;
        Value3 = v3;

    }

}
