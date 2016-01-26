package com.iss.android.wearable.datalayer;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class SelectRPE extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_rpe);

        initializeRPE();

    }

    // android studio has gone full retard on me,
    // so I initialize the items from code isntead of xml
    private void initializeRPE() {

        String[] arraySpinner = new String[]{
                "0 Rest",
                "1 Very easy",
                "2 Easy",
                "3 Moderate",
                "4 Somewhat hard",
                "5 Hard",
                "6 —",
                "7 Very hard",
                "8 —",
                "9 —",
                "10 Maximal"
        };

        ListView s = (ListView) findViewById(R.id.listView);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arraySpinner);
        s.setAdapter(adapter);

        s.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (SensorsDataService.itself == null) {
                    return;
                }

                SensorsDataService.itself.AddTrainingScore(position);
                finish();

            }
        });

    }

}
