package com.iss.android.wearable.datalayer;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Euler on 10/8/2015.
 */
public class ISSRecordData implements Serializable {

    static final long serialVersionUID = 1L;
    public int UserID;
    public int MeasurementType;
    public String Date;
    public String Timestamp;
    public String ExtraData = null;
    public float Value1;
    public float Value2;
    public float Value3;

    public static ISSRecordData fromString(String str) {

        String[] split = str.split(",");

        if (split == null) {
            return null;
        }

        int UID = Integer.parseInt(split[0]);
        int MType = Integer.parseInt(split[1]);
        String date = split[2];
        String timestamp = split[3];
        String extraData = split[4];
        float v1 = Float.parseFloat(split[5]);
        float v2 = Float.parseFloat(split[6]);
        float v3 = Float.parseFloat(split[7]);

        return new ISSRecordData(UID, MType, date, timestamp, extraData, v1, v2, v3);

    }

    // Converts an ISSRecordData object to a string like how it is represented in the *.csv
    public String toString() {

        String sep = ",";
        return UserID + sep + MeasurementType + sep + Date + sep + Timestamp + sep + ExtraData + sep + Value1 + sep + Value2 + sep + Value3;

    }

    public ISSRecordData(int UID, int MType, String date, String timestamp, String extraData, float v1, float v2, float v3) {

        UserID = UID;
        MeasurementType = MType;
        Date = date;
        Timestamp = timestamp;
        ExtraData = extraData;
        Value1 = v1;
        Value2 = v2;
        Value3 = v3;

    }

    public static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd_HH:mm:ss");

    public Date getTimestamp() {

        Calendar time = Calendar.getInstance();

        try {
            time.setTime(sdf.parse(this.Timestamp));
        } catch (ParseException e) {

        }

        return time.getTime();

    }


}
