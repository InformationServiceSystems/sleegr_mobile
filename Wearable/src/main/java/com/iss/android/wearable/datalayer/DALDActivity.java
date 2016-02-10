package com.iss.android.wearable.datalayer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

public class DALDActivity extends Activity {
    String[] daldaItems;
    String time;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dald);

        Intent myIntent = getIntent();

        String[] rpeValues = myIntent.getStringArrayExtra("rpeValues");
        time = myIntent.getStringExtra("time");
        daldaItems = myIntent.getStringArrayExtra("daldaItems");
        if (time.equals("evening")) {
            createRPEradioButtons(rpeValues);
        }
        createDALDAradioButtons(daldaItems);
        createSubmitButton();

    }

    void createSubmitButton(){

        Button button = new Button(this);
        button.setText("Submit");
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (!CheckDataFilled()){

                    new AlertDialog.Builder(DALDActivity.this)
                            .setTitle("Form incomplete")
                            .setMessage("Please fill the form completely")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // continue with delete
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // do nothing
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();

                    return;

                }

                if (time.equals("evening")) {
                    SensorsDataService.itself.AddTrainingScore(SelectedIndex(rpe), "RPE");
                }

                for (int i = 0; i < rgs.length; i++) {
                    SensorsDataService.itself.AddTrainingScore(SelectedIndex(rgs[i])-1, daldaItems[i]);
                }

                finish();

            }
        });

        LinearLayout layout = (LinearLayout) findViewById(R.id.linearLayout);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(button);

    }

    public boolean CheckDataFilled(){

        if (time.equals("evening")) {
            if (SelectedIndex(rpe) < 0)
                return false;
        }
        for (int i = 0; i < rgs.length; i++){
            if (SelectedIndex(rgs[i]) < 0)
                return false;
        }

        return true;

    }

    public int SelectedIndex(RadioGroup radioButtonGroup){

        int radioButtonID = radioButtonGroup.getCheckedRadioButtonId();
        View radioButton = radioButtonGroup.findViewById(radioButtonID);
        int idx = radioButtonGroup.indexOfChild(radioButton);

        return idx;

    }

    RadioGroup rpe = null;
    // do not have time to mess with list view custom elements,
    // so I just create all radio buttons by myself
    void createRPEradioButtons(String [] rpeValues){

        RadioButton[] rb = null;
        LinearLayout layout = (LinearLayout) findViewById(R.id.linearLayout);
        layout.setOrientation(LinearLayout.VERTICAL);

        TextView view = new TextView(this);
        view.setText("On a scale from 0 to 10, select how hard you trained during the day:");
        layout.addView(view);

        rb = new RadioButton[rpeValues.length];
        rpe = new RadioGroup(this); //create the RadioGroup
        rpe.setOrientation(RadioGroup.VERTICAL);//or RadioGroup.VERTICAL

        for (int i = 0; i < rpeValues.length; i++){
            rb[i]  = new RadioButton(this);
            rb[i].setText(rpeValues[i]);
            rpe.addView(rb[i]);
        }

        layout.addView(rpe);

    }

    RadioGroup [] rgs = null;

    void createDALDAradioButtons(String[] daldaItems) {


        LinearLayout layout = (LinearLayout) findViewById(R.id.linearLayout);
        layout.setOrientation(LinearLayout.VERTICAL);

        TextView view = new TextView(this);
        view.setText("For different aspects of your life, please select if it is a) worse than usual b) as normal c) better than usual:");
        layout.addView(view);

        rgs = new RadioGroup[daldaItems.length];
        String [] answrs = new String[] {"a", "b", "c"};

        for (int r = 0; r < daldaItems.length; r++) {

            RadioButton[] rbg = new RadioButton[3];

            rgs[r] = new RadioGroup(this); //create the RadioGroup
            rgs[r].setOrientation(RadioGroup.HORIZONTAL);//or RadioGroup.VERTICAL

            for (int i = 0; i < 3; i++){
                rbg[i]  = new RadioButton(this);
                rbg[i].setText(answrs[i]);
                rgs[r].addView(rbg[i]);
            }

            view = new TextView(this);
            view.setText(daldaItems[r]);
            layout.addView(view);
            layout.addView(rgs[r]);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_dald, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
