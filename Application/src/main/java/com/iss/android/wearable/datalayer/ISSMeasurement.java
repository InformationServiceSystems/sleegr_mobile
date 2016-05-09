package com.iss.android.wearable.datalayer;

import java.io.Serializable;

/**
 * Created by micha on 03.05.2016.
 */
public class ISSMeasurement implements Serializable{
    public long _ID;
    public String type;
    public String timestamp;

    public ISSMeasurement(long _ID, String type, String timestamp){
        this._ID = _ID;
        this.type = type;
        this.timestamp = timestamp;
    }
}
