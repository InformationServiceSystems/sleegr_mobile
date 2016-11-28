package com.iss.android.wearable.datalayer;

import android.database.Cursor;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;

import static com.iss.android.wearable.datalayer.MainActivity.getContext;

/**
 * Created by micha on 23.08.2016.
 */
class FhirFactory {
    private static JSONObject getHrJson(ISSRecordData tosend) {
        JSONObject json = new JSONObject();
        try {
            JSONArray coding = new JSONArray();
            JSONObject codingElement = new JSONObject();
            codingElement.put("system", "http://loinc.org");
            codingElement.put("code", "8867-4");
            codingElement.put("display", "Heart rate");
            coding.put(codingElement);
            JSONObject code = new JSONObject();
            code.put("coding", coding);
            json.put("code", code);

            json.put("valueDateTime", ISSDictionary.convertToFhirDate(tosend.Date, tosend.Timestamp));

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

    static JSONObject constructFhirObservation(Cursor mCursor, ArrayList<ISSRecordData> records, String devices) {
        JSONObject mainObject = new JSONObject();

        JSONArray componentElements = new JSONArray();
        String sensorDeviceName = "";
        for (ISSRecordData record : records) {
            JSONObject recordJson = new JSONObject();
            switch (record.MeasurementType) {
                case 21:
                    sensorDeviceName = record.getSensorDeviceName();
                    recordJson = getHrJson(record);
                    break;
                case 1:
                    sensorDeviceName = record.getSensorDeviceName();
                    recordJson = getAccelerometerJson(record);
                    break;
                case 4:
                    sensorDeviceName = record.getSensorDeviceName();
                    recordJson = getGyroscopeJson(record);
                    break;
            }
            componentElements.put(recordJson);
        }

        try {
            mainObject.put("status", "final");

            JSONObject category = new JSONObject();
            JSONArray coding = new JSONArray();
            JSONObject categoryObject = new JSONObject();
            categoryObject.put("display", mCursor.getString(1));
            coding.put(categoryObject);
            category.put("coding", coding);
            mainObject.put("category", category);


            JSONObject subject = new JSONObject();
            subject.put("display", UserData.getEmail()); //We could switch to name; but email is required, name isn't.
            mainObject.put("subject", subject);

            JSONObject device = new JSONObject();
            device.put("reference", getContext().getResources().getString(R.string.server_json_get_device));
            device.put("display", devices);
            mainObject.put("device", device);

            mainObject.put("effectiveDateTime", ISSDictionary.convertToFhirDate(mCursor.getString(2)));
            mainObject.put("component", componentElements);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.d("JSONObject", mainObject.toString());


        return mainObject;
    }

    private static JSONObject getAccelerometerJson(ISSRecordData tosend) {
        JSONObject json = new JSONObject();
        try {
            JSONArray coding = new JSONArray();
            JSONObject codingElement = new JSONObject();
            codingElement.put("system", "http://loinc.org");
            codingElement.put("code", "8867-4");
            codingElement.put("display", "Accelerometer");
            coding.put(codingElement);
            JSONObject code = new JSONObject();
            code.put("coding", coding);
            json.put("code", code);

            json.put("valueDateTime", ISSDictionary.convertToFhirDate(tosend.Date, tosend.Timestamp));

            JSONObject valueQuantity = new JSONObject();
            valueQuantity.put("unit", "m/s2");
            valueQuantity.put("system", "http://unitsofmeasure.org");
            valueQuantity.put("code", "m/s2");
            valueQuantity.put("value", bytecode(tosend.Value1, tosend.Value2, tosend.Value3));
            json.put("valueQuantity", valueQuantity);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    private static JSONObject getGyroscopeJson(ISSRecordData tosend) {
        JSONObject json = new JSONObject();
        try {
            JSONArray coding = new JSONArray();
            JSONObject codingElement = new JSONObject();
            codingElement.put("system", "http://loinc.org");
            codingElement.put("code", "8867-4");
            codingElement.put("display", "Gyroscope");
            coding.put(codingElement);
            JSONObject code = new JSONObject();
            code.put("coding", coding);
            json.put("code", code);

            json.put("valueDateTime", ISSDictionary.convertToFhirDate(tosend.Date, tosend.Timestamp));

            JSONObject valueQuantity = new JSONObject();
            valueQuantity.put("unit", "rad/s");
            valueQuantity.put("system", "http://unitsofmeasure.org");
            valueQuantity.put("code", "rad/s");
            valueQuantity.put("value", bytecode(tosend.Value1, tosend.Value2, tosend.Value3));
            json.put("valueQuantity", valueQuantity);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    private static BigDecimal round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd;
    }

    private static BigDecimal bytecode(float value1, float value2, float value3) {

        BigDecimal value1Decimal;
        BigDecimal value2Decimal;
        BigDecimal value3Decimal;
        value1Decimal = round(value1, 2);
        value2Decimal = round(value2, 2);
        value3Decimal = round(value3, 2);
        BigDecimal result = new BigDecimal(1000000).multiply(value1Decimal).add(new BigDecimal(1000).multiply(value2Decimal)).add(value3Decimal);
        return result;
    }
}
