package com.example.rene.emg;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

public class SquatActivity extends Activity {
    private PowerManager.WakeLock wakeLock;						// Object to control screen locking
    private ArrayList<ReadSensor> rightSide = new ArrayList<ReadSensor>();
    private ArrayList<ReadSensor> leftSide = new ArrayList<ReadSensor>();
    private Socket commSock, emgSock;        // Communication sockets
    private volatile ArrayList<Muscle> muscles = new ArrayList<Muscle>();
    private final int EMG_BYTE_BUFFER = 1728 * 4;
    private byte[] emgBytes;
    private boolean running = true;
    //private ForkJoinPool pool;
    private static final char LEFT = 'L';
    private static final char RIGHT = 'R';

    private HashMap<Muscle, ArrayList<Double>> mainMap = new HashMap<>();
    private HashMap<Muscle, Double> averageMap = new HashMap<>();

    private boolean calculated = false;
    private boolean updated = true;

    TextView leftQuad;
    TextView rightQuad;
    TextView rightHam;
    TextView leftHam;
    TextView leftCalf;
    TextView rightCalf;

    Button start;

    ImageView scanImage;
    Point p1;
    Point p2;
    Bitmap bmpCircle;
    Canvas canvasCircle;

    Stack points;
    Canvas canvasLine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_squat);

        PowerManager powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "Full Wake Lock");

        scanImage = (ImageView) findViewById(R.id.scan_image_one);
        start = (Button) findViewById(R.id.startButton);

        //leftSide = (ArrayList<ReadSensor>) getIntent().getSerializableExtra("LeftSide");
        //rightSide = (ArrayList<ReadSensor>) getIntent().getSerializableExtra("RightSide");

        muscles = (ArrayList<Muscle>) getIntent().getSerializableExtra("Muscles");
        //sockets = (ArrayList<Socket>) getIntent().getExtras().getSerializable("Sockets");
        commSock = ComSocketSingleton.getSocket();
        emgSock = EMGSocketSingleton.getSocket();

        leftQuad = (TextView) findViewById(R.id.leftQuad);
        rightQuad = (TextView) findViewById(R.id.rightQuad);
        rightHam = (TextView) findViewById(R.id.rightHam);
        leftHam = (TextView) findViewById(R.id.leftHam);
        leftCalf = (TextView) findViewById(R.id.rightCalf);
        rightCalf = (TextView) findViewById(R.id.leftCalf);

        //pool = new ForkJoinPool(muscles.size());

        //Populate hashmap of muscles
        for (Muscle m : muscles) {
            mainMap.put(m, new ArrayList<Double>());
        }

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Timer timer = new Timer();
                timer.schedule(new TimerTask() {

                    public void run() {

                        Thread t = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                calculate();
                            }
                        });
                        t.start();

                        try {
                            t.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateScreen();
                            }
                        });
                    }
                }, 0, 500);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Keep the screen alive, even if it is not touched.
        wakeLock.acquire();
    }

    private void calculate() {

        synchronized (this) {
            while(!updated) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            //System.out.println("Calculating....");

            final int sampleSize = 20;

            //Get all RMS values
            for (int i = 0; i < sampleSize; i++) {
                readBytes();
                for (Muscle m : muscles) {
                    mainMap.get(m).add(m.getRMS());
                    //System.out.println("Muscle: " + m.getSide() + " " + m.getName() + " -- " + "Adding " + m.getRMS());
                }
            }

            //Find average
            for (Muscle m : muscles) {
                double tmp = 0;
                for (Double d : mainMap.get(m)) {
                    tmp += d;
                    //System.out.println("tmp is: " + tmp);
                }
                double average = tmp / mainMap.get(m).size();
                //average = Math.round(average);
                //System.out.println("Muscle: " + m.getSide()+m.getName() + " - Average: " + average);
                averageMap.put(m, average);
                mainMap.get(m).clear();
            }
            calculated = true;
            updated = false;

            notify();
        }
    }

    private void updateScreen() {

        synchronized (this) {
            while(!calculated) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            //System.out.println("Updating Screen....");

            //System.out.println("SETVALUES - Thread ID: "+Thread.currentThread().getId()+" -- Thread name: "+Thread.currentThread().getName());
            for (Muscle m : muscles) {
                //System.out.println("setValues: Using muscle: " + m.getSide() + " " + m.getName());
                if (averageMap.isEmpty()) {
                    System.out.println("Hashmap is empty ...");
                    return;
                }
                double average = averageMap.get(m);
                int percent = (int)((average / m.getMax()) * 100);

                //System.out.println("Muscle: "+m.getSide()+" "+m.getName()+": Average: "+average+" - "+"Percentage: "+percent+"%");

                if (m.getSide() == RIGHT) {
                    if (m.getName().equals("Quad")) {
                        rightQuad.setText(percent + "%");
                    } else if (m.getName().equals("Hamstring")) {
                        rightHam.setText(percent + "%");
                    } else {
                        rightCalf.setText(percent + "%");
                    }
                } else {
                    if (m.getName().equals("Quad")) {
                        leftQuad.setText(percent + "%");
                    } else if (m.getName().equals("Hamstring")) {
                        leftHam.setText(percent + "%");
                    } else {
                        leftCalf.setText(percent + "%");
                    }
                }
                onDrawCircle(m, percent);
            }
            averageMap.clear();
            updated = true;
            calculated = false;

            notify();
        }
    }

    /*private synchronized void handler() {

        System.out.println("HANDLER - Thread ID: "+Thread.currentThread().getId()+" -- Thread name: "+Thread.currentThread().getName());

        //HashMap<Muscle, Double> hashmap = new HashMap<>();
        final int sampleSize = 10;

        System.out.println("Done populating");

        //while(running) {

        //Get all RMS values
        for (int i = 0; i < sampleSize; i++) {
            readBytes();
            for (Muscle m : muscles) {
                hashmap.get(m).add(m.getRMS());
                System.out.println("Muscle: " + m.getSide() + " " + m.getName() + " -- " + "Adding " + m.getRMS());
            }
        }

        System.out.println("Done first loop");


        final HashMap<Muscle, Double> averageMap = new HashMap<>();

        //Find average
        for (Muscle m : muscles) {
            double tmp = 0;
            for (Double d : hashmap.get(m)) {
                tmp += d;
                System.out.println("tmp is: " + tmp);
            }
            System.out.println("Done Inner loop");
            double average = tmp / hashmap.get(m).size();
            average = Math.round(average);
            System.out.println("Average: " + average);
            averageMap.put(m, average);
            hashmap.get(m).clear();
        }

        System.out.println("Trying to pass hashmap");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                System.out.println("UI THREAD - Thread ID: " + Thread.currentThread().getId() + " -- Thread name: " + Thread.currentThread().getName());
                setValues(averageMap);
                averageMap.clear();
            }
        });

    }*/

    private synchronized void readBytes() {
        //System.out.println("READBYTES - Thread ID: "+Thread.currentThread().getId()+" -- Thread name: "+Thread.currentThread().getName());

        // Allocate space to store data incoming from the network.
        emgBytes = new byte[EMG_BYTE_BUFFER];

        //while(running) {
        try {
            // Wait until a complete set of data is ready on the socket.  The
            while (emgSock.getInputStream().available() < EMG_BYTE_BUFFER) {
                Thread.sleep(50);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            // Read a complete group of multiplexed samples
            emgSock.getInputStream().read(emgBytes, 0, EMG_BYTE_BUFFER);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //System.out.println("Muscle array length: " +muscles.size());

        //Read each sensor and put them in their respective side
        for(Muscle m : muscles) {
            ReadSensor readSensor = new ReadSensor(m, emgBytes);
            //pool.invoke(readSensor);
            //System.out.println("Threads active in the pool: " + pool.getActiveThreadCount());
        }

        //Wait for all threads to finish
        /*try {
            pool.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/

        /*//Wait for all threads to finish
        while(pool.getActiveThreadCount() > 0) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }*/

        //System.out.println("ReadBytes DONE");

        //Calculate total rms
            /*double leftRMS = 0;
            double rightRMS = 0;
            for(ReadSensor s : leftSide) {
                leftRMS += s.getRMS();
            }

            for(ReadSensor s : rightSide) {
                rightRMS += s.getRMS();
            }*/

        //Cleanup
            /*rightSide.clear();
            leftSide.clear();*/


            /*Intent i = new Intent(getApplicationContext(), SquatActivity.class);
            i.putExtra("LeftSide", leftSide);
            i.putExtra("RightSide", rightSide);
            startActivity(i);*/
        //}
    }

    private void onDrawCircle(Muscle m, double percent) {
        bmpCircle = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888);
        canvasCircle = new Canvas(bmpCircle);
        scanImage.draw(canvasCircle);

        int red;
        int green;

        int maxCol = 255;

        if (percent<=50) {
            green = maxCol;
            red = (int) (maxCol * (percent*2)/100);
        } else if(percent<=100){
            green = (int)  (maxCol * (100-(percent-50)*2)/100);
            red = maxCol;
        } else { //Percent is over 100%
            green = 0;
            red = maxCol;
        }



        Paint pnt = new Paint();
        /*if(percent < 33) {

            pnt.setColor(Color.GREEN);
        }
        else if(percent < 66) pnt.setColor(Color.YELLOW);
        else pnt.setColor(Color.RED);*/

        pnt.setARGB(1, red, green, 0);

        canvasCircle.drawCircle(50, 50, 10, pnt);
        scanImage.setImageBitmap(bmpCircle);
    }

    @Override
    protected void onStop() { //CHANGE TO ONSTOP?
        super.onStop();

        // Allow the screen to sleep when plotting is inactive.
        wakeLock.release();

        // Close connections to the server.  Check if they were initialized correctly to avoid exceptions.
        try {
            if (commSock != null) {
                PrintWriter writer = new PrintWriter(commSock.getOutputStream());
                writer.write("STOP\r\n\r\n"); // request the end of data streaming
                writer.flush();
                BufferedReader reader = new BufferedReader(new InputStreamReader(commSock.getInputStream()));
                do {
                    while (!reader.ready())
                        ;
                } while (!reader.readLine().contains("OK"));
                commSock.close();
                System.out.println("CLOSING COMSOCK");
            }
            if (emgSock != null) {
                emgSock.close();
                System.out.println("CLOSING EMGSOCK");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
