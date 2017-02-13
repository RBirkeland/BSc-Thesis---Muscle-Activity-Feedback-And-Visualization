/*

Copyright (C) 2011 Delsys, Inc.

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in 
the Software without restriction, including without limitation the rights to 
use, copy, modify, merge, publish, and distribute the Software, and to permit 
persons to whom the Software is furnished to do so, subject to the following 
conditions:

The above copyright notice and this permission notice shall be included in all 
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, 
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE 
SOFTWARE.

*/

package com.example.rene.emg;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;


public class SensorSelectionActivity extends Activity {
    String[] sensors = new String[16];
    //Called when the activity is first created.
    ArrayList<Integer> selectedItems = new ArrayList<Integer>();
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        setTitle("Muscle Activiation");

        //Intent intent = new Intent(getApplicationContext(), Bluetooth.class);
        //startActivity(intent);

        for (int i = 1; i <= 16; i++) {
            sensors[i - 1] = i+"";
        }

        ListView lv = (ListView)findViewById(R.id.sensor_list);
        lv.setAdapter(new ArrayAdapter<String>(this, R.layout.rowlayout, R.id.txt_lan, sensors));
        lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = ((TextView) view).getText().toString();
                if (selectedItems.contains(selectedItem))
                    selectedItems.remove(selectedItem); //uncheck item
                else
                    selectedItems.add(Integer.parseInt(selectedItem)); //check item
            }
        });
    }

    //Visualization button listener
    public void buttonClickVis(View v){
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        //Hide keyboard
        if(imm.isAcceptingText())
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);

        if(selectedItems.isEmpty())
            Toast.makeText(getApplicationContext(),"Please select sensors", Toast.LENGTH_LONG).show();
        else {
            Intent i = new Intent(getApplicationContext(), MuscleSelection.class);
            Collections.sort(selectedItems);
            i.putExtra("Sensors", selectedItems);
            i.putExtra("IP", ((EditText) findViewById(R.id.editText1)).getText().toString());

            startActivity(i);
        }
    }

    //Plotting button listener
    public void buttonClickPlot(View v){
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        //Hide keyboard
        if(imm.isAcceptingText())
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);

        if(selectedItems.isEmpty())
            Toast.makeText(getApplicationContext(),"Please select sensors", Toast.LENGTH_LONG).show();
        else if(selectedItems.size() != 2) {
            Toast.makeText(getApplicationContext(),"Please select 2 sensors to plot", Toast.LENGTH_LONG).show();
        } else {
            Intent i = new Intent(getApplicationContext(), ConnectionActivity.class);
            Collections.sort(selectedItems);
            i.putExtra("Sensors", selectedItems);
            i.putExtra("IP", ((EditText) findViewById(R.id.editText1)).getText().toString());
            i.putExtra("selectedPlot", true);

            startActivity(i);
        }
    }

    public class MyView extends View {
        public MyView(Context context) {
            super(context);

        }

        @Override
        protected void onDraw(Canvas canvas) {
            // TODO Auto-generated method stub
            super.onDraw(canvas);
            int x = getWidth();
            int y = getHeight();
            int radius;
            radius = 100;
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            canvas.drawPaint(paint);
            // Use Color.parseColor to define HTML colors
            paint.setColor(Color.parseColor("#CD5C5C"));
            canvas.drawCircle(x / 2, y / 2, radius, paint);
        }
    }


}