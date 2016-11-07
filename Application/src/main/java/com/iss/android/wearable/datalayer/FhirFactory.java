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
    public static JSONObject getHrJson(ISSRecordData tosend) {
        JSONObject json = new JSONObject();
        try {
            JSONObject code = new JSONObject();
            code.put("system", "http://loinc.org");
            code.put("code", "8867-4");
            code.put("display", "Heart rate");
            json.put("code", code);

            json.put("valueDateTime", tosend.Date+"T"+tosend.Timestamp);
            json.put("tag", tosend.ExtraData);

            JSONObject valueQuantity = new JSONObject();
            valueQuantity.put("unit", "Hz");
            valueQuantity.put("system", "http://unitsofmeasure.org");
            valueQuantity.put("code", "Hz");
            valueQuantity.put("value", tosend.Value1);
            json.put("valueQuantity", valueQuantity);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public static JSONObject constructHrFhirObservation(Cursor mCursor, ArrayList<ISSRecordData> records) {
        JSONObject mainObject = new JSONObject();

        JSONArray componentElements = new JSONArray();
        String sensorDeviceName = "";
        for (ISSRecordData record : records) {
            sensorDeviceName = record.getSensorDeviceName();
            JSONObject recordJson = getHrJson(record);
            componentElements.put(recordJson);
        }

        try {
            mainObject.put("status", "final");
            JSONObject CodeableConcept = new JSONObject();
            CodeableConcept.put("system", "http://loinc.org");
            CodeableConcept.put("code", "8867-4");
            CodeableConcept.put("display", "Heart rate");
            mainObject.put("code", CodeableConcept);

            JSONObject subject = new JSONObject();
            subject.put("ref", UserData.getIdToken());
            subject.put("display", UserData.getEmail()); //We could switch to name; but email is required, name isn't.
            mainObject.put("subject", subject);

            JSONObject device = new JSONObject();
            device.put("display", sensorDeviceName);
            mainObject.put("device", device);

            /* mainObject.put("Id", mCursor.getInt(0));
            mainObject.put("Type", mCursor.getString(1));
            mainObject.put("Timestamp", mCursor.getString(2));*/
            mainObject.put("component", componentElements);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.d("JSONObject", mainObject.toString());


        return mainObject;
    }
}
