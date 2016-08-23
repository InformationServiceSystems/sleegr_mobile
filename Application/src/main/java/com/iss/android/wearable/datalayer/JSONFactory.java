package com.iss.android.wearable.datalayer;

import android.database.Cursor;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by micha on 23.08.2016.
 */
public class JSONFactory {
    public static JSONObject getJSON(ISSRecordData tosend) {
        JSONObject json = new JSONObject();
        try {
            json.put("Id", DataSyncService.getUserID());
            json.put("Measurement_Id", tosend.measurementID);
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

    public static JSONObject constructMeasurement(Cursor mCursor, ArrayList<ISSRecordData> records) {
        JSONObject mainObject = new JSONObject();

        JSONArray values = new JSONArray();
        for (ISSRecordData record: records){
            JSONObject recordJson = getJSON(record);
            values.put(recordJson);
        }

        try {
            mainObject.put("Id", mCursor.getInt(0));
            mainObject.put("Type", mCursor.getString(1));
            mainObject.put("Timestamp", mCursor.getString(2));
            mainObject.put("values", values);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.d("JSONObject", mainObject.toString());


        return mainObject;
    }
}
