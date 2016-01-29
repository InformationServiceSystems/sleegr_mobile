package com.iss.android.wearable.datalayer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;

import static com.iss.android.wearable.datalayer.DateTimeManager.getDayFromToday;

public class SelectAvailableData extends Activity {


    ArrayList<String> allOptions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_available_data);


        final File userDataFolder = DataStorageManager.userDataFolder;

        File[] folders = userDataFolder.listFiles();

        for (int i = 0; i < 60; i++){

            String val = getDayFromToday(i);

            // get all files in the folder

            File folder = new File(userDataFolder, val);

            if (!folder.exists()){
                continue;
            }

            File[] files = folder.listFiles();

            for (File file:files){

                if (file.isDirectory())
                    continue;

                String fname = file.toString();

                if (!fname.endsWith(".csv"))
                    continue;


                String action = fname.substring(fname.lastIndexOf("_")+1, fname.length() - 4);
                allOptions.add(folder.getName() + " " + action);


            }

        }

        ListView view = (ListView) findViewById(R.id.availableActivityView);
        ArrayAdapter<String> myarrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, allOptions);
        view.setAdapter(myarrayAdapter);

        view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View v, int position, long id) {

                String item = allOptions.get(position);

                String[] split = item.split(" ");

                String date = split[0];
                String activity = split[1];

                Intent i = new Intent(SelectAvailableData.this, DisplayActivityFile.class);
                i.putExtra("date", date);
                i.putExtra("activity", activity);
                startActivity(i);

            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_select_available_data, menu);
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
