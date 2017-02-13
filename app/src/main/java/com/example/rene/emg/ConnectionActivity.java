package com.example.rene.emg;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;


public class ConnectionActivity extends Activity {

    private static Socket commSock, emgSock, accSock;		// Communication sockets
    private String ip;								// IP address of the server (from the bundle)
    private Thread t;								// Worker thread to read data from the socket
    private ArrayList<Muscle> muscles = new ArrayList<Muscle>();
    private boolean selectedPlot = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);

        muscles = (ArrayList<Muscle>) getIntent().getSerializableExtra("Muscles");
        ip = getIntent().getExtras().getString("IP");

        try {
            selectedPlot = getIntent().getExtras().getBoolean("selectedPlot");
        } catch(Exception e) {
            System.out.println("User did not chose plot");
        }

        connect();

        ArrayList<Socket> sockets = new ArrayList<Socket>();
        sockets.add(commSock);
        sockets.add(emgSock);
        sockets.add(accSock);

        if(selectedPlot) {
            Intent i = new Intent(getApplicationContext(), PlottingActivity.class);
            i.putExtra("Muscles", muscles);
            startActivity(i);

        } else {
            Intent i = new Intent(getApplicationContext(), CalibrationActivity.class);
            i.putExtra("Muscles", muscles);
            startActivity(i);
        }
    }

    private void connect() {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        try {
            // Initialize TCP/IP communication to the server.
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    System.out.println("Thread running");
                    try {
                        t.sleep(3000);
                        if(commSock == null) {
                            System.out.println("ComSock could not connect. Exiting");
                            System.exit(0);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
            Thread t = new Thread(runnable);
            t.start();

            System.out.println("Trying to connect to socket...");
            commSock = new Socket(ip, 50040); // commands
            emgSock = new Socket(ip, 50041); // EMG data
            accSock = new Socket(ip, 50042); // ACC data

            //Save in singleton
            ComSocketSingleton.setSocket(commSock);
            EMGSocketSingleton.setSocket(emgSock);

            PrintWriter writer = new PrintWriter(commSock.getOutputStream());
            writer.write("ENDIAN BIG\r\nSTART\r\n\r\n"); // request the start of data streaming
            writer.flush();
            // Read the reply
            BufferedReader reader = new BufferedReader(new InputStreamReader(commSock.getInputStream()));
            String tmp;
            while (!(tmp = reader.readLine()).contains("OK")) {
                if (tmp.contains("CANNOT COMPLETE")) {
                    // Retry, the last activity may not have fully stopped
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    writer.write("ENDIAN BIG\r\nSTART\r\n\r\n"); // request the start of data streaming
                    writer.flush();
                }
                while (!reader.ready())
                    ;
            }
        } catch (UnknownHostException e1) {
            e1.printStackTrace();
            // Notify the user of an error.
            Toast.makeText(this, "Cannot connect to server.", Toast.LENGTH_LONG).show();
            System.out.println("Cannot connect to server.");
            finishActivity(0);
            startActivity(new Intent(this, SensorSelectionActivity.class));
            System.exit(0);
            return;
        } catch (IOException e1) {
            e1.printStackTrace();
            // Notify the user of an error.
            Toast.makeText(this, "Cannot connect to server.", Toast.LENGTH_LONG).show();
            System.out.println("Cannot connect to server.");
            finishActivity(0);
            startActivity(new Intent(this, SensorSelectionActivity.class));
            System.exit(0);
            return;
        }
        System.out.println("Connection success");
    }

}
