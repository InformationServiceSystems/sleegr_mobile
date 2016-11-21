package com.iss.android.wearable.datalayer;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by micha on 03.05.2016.
 */
class ISSRPEAnswers implements Serializable {
    private String Measurement_ID;
    HashMap<String, Integer> Answers;

    public ISSRPEAnswers(String Measurement_ID, HashMap<String, Integer> Answers) {
        this.Measurement_ID = Measurement_ID;
        this.Answers = Answers;
    }
}
