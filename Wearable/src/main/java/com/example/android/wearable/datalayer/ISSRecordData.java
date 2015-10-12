package com.example.android.wearable.datalayer;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by Euler on 10/8/2015.
 */
public class ISSRecordData implements Serializable {

    static final long serialVersionUID = 1L;
    public int UserID;
    public int MeasurementType;
    public String Timestamp;
    public String ExtraData = null;
    public float Value;

    public ISSRecordData(int UID, int MType, String timestamp, String extraData, float value){

        UserID = UID;
        MeasurementType = MType;
        Timestamp = timestamp;
        ExtraData = extraData;
        Value = value;

    }

}
