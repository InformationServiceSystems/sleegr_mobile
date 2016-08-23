package com.iss.android.wearable.datalayer;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by micha on 23.08.2016.
 */
public class JSONFactory {
    public static JSONObject getJSON(ISSRecordData tosend) {
        JSONObject json = new JSONObject();
        try {
            json.put("Id", DataSyncService.getUserID());
            json.put("type", tosend.MeasurementType);
            json.put("date", tosend.Date);
            json.put("time", tosend.Timestamp);
            json.put("tag", tosend.ExtraData);
            json.put("val0", tosend.Value1);
            json.put("val1", tosend.Value2);
            json.put("val2", tosend.Value3);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }
}
