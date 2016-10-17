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
public class FhirFactory {
    public static JSONObject getJSON(ISSRecordData tosend) {
        JSONObject json = new JSONObject();
        try {
            JSONObject code = new JSONObject();
            code.put("system", "Android");
            code.put("code", tosend.MeasurementType);
            code.put("display", "Heart Rate Measurement");
            json.put("code", code);

            json.put("valueTime", tosend.Timestamp);
            json.put("valueDate", tosend.Date);
            json.put("tag", tosend.ExtraData);

            JSONObject valueQuantity = new JSONObject();
            valueQuantity.put("unit", "BPM");
            valueQuantity.put("system", "default");
            valueQuantity.put("code", "default");
            valueQuantity.put("value", tosend.Value1);
            json.put("valueQuantity", valueQuantity);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public static JSONObject constructFhirObservation(Cursor mCursor, ArrayList<ISSRecordData> records) {
        JSONObject mainObject = new JSONObject();

        JSONArray componentElements = new JSONArray();
        String sensorDeviceName = "";
        for (ISSRecordData record : records) {
            sensorDeviceName = record.getSensorDeviceName();
            JSONObject recordJson = getJSON(record);
            componentElements.put(recordJson);
        }

        try {
            mainObject.put("status", "final");
            mainObject.put("code", "dummyCode");

            JSONObject subject = new JSONObject();
            subject.put("ref", UserData.getIdToken());
            subject.put("display", UserData.getEmail()); //We could switch to name; but email is required, name isn't.
            mainObject.put("subject", subject);

            JSONObject device = new JSONObject();
            device.put("display", sensorDeviceName);
            mainObject.put("device", device);

            JSONObject component = new JSONObject();

            /* mainObject.put("Id", mCursor.getInt(0));
            mainObject.put("Type", mCursor.getString(1));
            mainObject.put("Timestamp", mCursor.getString(2));*/
            component.put("elements", componentElements);
            mainObject.put("component", component);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.d("JSONObject", mainObject.toString());


        return mainObject;
    }
}
