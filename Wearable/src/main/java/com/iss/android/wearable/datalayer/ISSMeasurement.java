package com.iss.android.wearable.datalayer;

import java.io.Serializable;

/**
 * Created by micha on 03.05.2016.
 */
class ISSMeasurement implements Serializable {
    long _ID;
    public String type;
    private String timestamp;
    private static final long serialVersionUID = 1L;

    ISSMeasurement(long _ID, String type, String timestamp) {
        this._ID = _ID;
        this.type = type;
        this.timestamp = timestamp;
    }
}
