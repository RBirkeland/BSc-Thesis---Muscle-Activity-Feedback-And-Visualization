package com.example.rene.emg;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import android.os.PowerManager;
import android.text.Layout;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class MuscleSelection extends Activity {

    private ArrayList<Integer> sensors = new ArrayList<Integer>();
    private ArrayList<Muscle> muscles;
    private String ip;
    private PowerManager.WakeLock wakeLock;						// Object to control screen locking

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_muscle_selection);

        PowerManager powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "Full Wake Lock");

        sensors = getIntent().getExtras().getIntegerArrayList("Sensors");
        ip = getIntent().getExtras().getString("IP");

        Spinner s1 = (Spinner) findViewById(R.id.spinner1);
        Spinner s2 = (Spinner) findViewById(R.id.spinner2);
        Spinner s3 = (Spinner) findViewById(R.id.spinner3);
        Spinner s4 = (Spinner) findViewById(R.id.spinner4);
        Spinner s5 = (Spinner) findViewById(R.id.spinner5);
        Spinner s6 = (Spinner) findViewById(R.id.spinner6);

        LinearLayout l1 = (LinearLayout) findViewById(R.id.l1);
        LinearLayout l2 = (LinearLayout) findViewById(R.id.l2);
        LinearLayout l3 = (LinearLayout) findViewById(R.id.l3);
        LinearLayout l4 = (LinearLayout) findViewById(R.id.l4);
        LinearLayout l5 = (LinearLayout) findViewById(R.id.l5);
        LinearLayout l6 = (LinearLayout) findViewById(R.id.l6);

        Button b = (Button) findViewById(R.id.muscleButton);

        final String[] muscleList = new String[6];

        //for(Integer s : sensors) System.out.println(muscleList[s]);

        for(Integer i : sensors) {
            switch(i) {
                case 1 : l1.setVisibility(View.VISIBLE);
                    break;
                case 2 : l2.setVisibility(View.VISIBLE);
                    break;
                case 3 : l3.setVisibility(View.VISIBLE);
                    break;
                case 4 : l4.setVisibility(View.VISIBLE);
                    break;
                case 5 : l5.setVisibility(View.VISIBLE);
                    break;
                case 6 : l6.setVisibility(View.VISIBLE);
                    break;
            }
        }

        muscles = new ArrayList<Muscle>();

        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                    for (Integer i : sensors) {
                        String tmp = muscleList[i-1];
                        String splits[] = tmp.split(" ");
                        muscles.add(new Muscle(splits[1], splits[0].toCharArray()[0], i));
                    }
                    for (Muscle m : muscles) {
                        System.out.println("Muscle: "+m.getName() + " " + m.getSide() + " " + m.getSensorNr());
                    }

                Intent i = new Intent(getApplicationContext(), ConnectionActivity.class);
                i.putExtra("Muscles", muscles);
                i.putExtra("IP", ip);
                startActivity(i);
                }
            });


            //Listen for spinner actions
            s1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {
                public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                    String item = parent.getItemAtPosition(pos).toString();
                    muscleList[0] = item;
                }

                public void onNothingSelected(AdapterView<?> parent) {}
            });

        s2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String item = parent.getItemAtPosition(pos).toString();
                muscleList[1] = item;
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        s3.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String item = parent.getItemAtPosition(pos).toString();
                muscleList[2] = item;
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        s4.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String item = parent.getItemAtPosition(pos).toString();
                muscleList[3] = item;
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        s5.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String item = parent.getItemAtPosition(pos).toString();
                muscleList[4] = item;
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        s6.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String item = parent.getItemAtPosition(pos).toString();
                muscleList[5] = item;
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
}

