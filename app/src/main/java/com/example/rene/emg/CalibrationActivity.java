package com.example.rene.emg;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import org.w3c.dom.Text;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ForkJoinPool;

public class CalibrationActivity extends Activity {

    private PowerManager.WakeLock wakeLock;						// Object to control screen locking
    private Socket commSock, emgSock;        // Communication sockets
    private String ip;                                // IP address of the server (from the bundle)
    private ArrayList<Muscle> muscles = new ArrayList<Muscle>();
    private ArrayList<Socket> sockets = new ArrayList<Socket>();
    final int EMG_BYTE_BUFFER = 1728 * 4;
    private byte[] emgBytes;
    boolean running = true;
    //private ForkJoinPool pool;
    private int progressStatus = 0;
    private TextView testText;

    private boolean updated = true;
    private boolean calculated = false;
    private ProgressBar progress;


    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration);

        PowerManager powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "Full Wake Lock");

        muscles = (ArrayList<Muscle>) getIntent().getSerializableExtra("Muscles");
        //sockets = (ArrayList<Socket>) getIntent().getExtras().getSerializable("Sockets");

        commSock = ComSocketSingleton.getSocket();
        emgSock = EMGSocketSingleton.getSocket();

        //progress.setProgress(0);

        //pool = new ForkJoinPool(muscles.size());

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        final Button button = (Button) findViewById(R.id.caliButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                progressStatus = 0;

                final int calibrationSize = 250;

                HashMap<Muscle, double[]> allMuscles = new HashMap<Muscle, double[]>();

                for(Muscle m: muscles) allMuscles.put(m, new double[calibrationSize]);

                while(progressStatus < calibrationSize) {
                    readData();

                    for(Muscle m : muscles) {
                        allMuscles.get(m)[progressStatus] = m.getRMS();
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateScreen();
                        }
                    });

                    progressStatus++;
                }

                for(Muscle m : muscles) {
                    double[] tmp = allMuscles.get(m);
                    Arrays.sort(tmp);
                    for(int i = 0; i<tmp.length;i++) {
                        //System.out.println("Muscle: "+m.getSide()+m.getName()+"RMS: " +tmp[i]);
                    }
                    //Find the percentile 90%
                    double newMax = tmp[(int) (calibrationSize*0.9)];
                    System.out.println("PERCENTILE VALUE: "+newMax);
                    m.setMax(newMax);
                }
                nextActivity();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        wakeLock.acquire();
    }

    private void nextActivity() {
        System.out.println("Done Calibrating!");

        for (Muscle m : muscles) {
            System.out.println("Max recorded:" + m.getSide() + " " + m.getName() + ": " + m.getMax());
        }
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Intent intent = new Intent(getApplicationContext(), SquatActivity.class);
        intent.putExtra("Muscles", muscles);
        startActivity(intent);
    }


    private int readData() {

        synchronized (this) {
            while(!updated) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            emgBytes = new byte[EMG_BYTE_BUFFER];

            try {
                // Wait until a complete set of data is ready on the socket.  The
                while (emgSock.getInputStream().available() < EMG_BYTE_BUFFER) {
                    //System.out.println("Sleeping");
                    Thread.sleep(50);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Enough data gathered");
            try {
                // Read a complete group of multiplexed samples
                emgSock.getInputStream().read(emgBytes, 0, EMG_BYTE_BUFFER);
            } catch (IOException e) {
                e.printStackTrace();
            }

            //Read from all sensors
            for (Muscle m : muscles) {
                new CalibrationReadSensor(m, emgBytes);
            }

            calculated = true;
            updated = false;
            notify();
            return 1;
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
            progress = (ProgressBar) findViewById(R.id.progBar);
            testText = (TextView) findViewById(R.id.testText);
            testText.setText(progressStatus+"");
            progress.setProgress(progressStatus);
            updated = true;
            calculated = false;
            notify();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Calibration Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.example.rene.emg/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        wakeLock.release();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Calibration Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.example.rene.emg/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }
}
