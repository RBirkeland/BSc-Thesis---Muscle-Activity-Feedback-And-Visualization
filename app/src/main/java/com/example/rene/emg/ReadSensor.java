package com.example.rene.emg;

import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.RecursiveAction;
import com.androidplot.xy.*;
import android.os.PowerManager.WakeLock;


class ReadSensor {

    private WakeLock wakeLock;                        // Object to control screen locking
    private String ip;                                // IP address of the server (from the bundle)
    private Thread t;                                // Worker thread to read data from the socket
    private boolean running;                        // Flag to indicate if the worker thread should continue
    private long accSampleIdx = 0;                    // Current ACC sample number
    private long emgSampleIdx = 0;                    // Current EMG sample number
    private int HISTORY_SZ = 2000;                    // Number of samples to display on the screen at once
    private SimpleXYSeries emgSeries = null;        // Series representing the EMG channel
    protected volatile ArrayList<Number> emgHistory = null;    // Buffer to hold samples received for the EMG channel
    private SimpleXYSeries accSeries1 = null;        // Series representing the ACC X channel
    private ArrayList<Number> accHistory1 = null;    // Buffer to hold samples received for the ACC X channel
    private SimpleXYSeries accSeries2 = null;        // Series representing the ACC Y channel
    private ArrayList<Number> accHistory2 = null;    // Buffer to hold samples received for the ACC Y channel
    private SimpleXYSeries accSeries3 = null;        // Series representing the ACC Z channel
    private ArrayList<Number> accHistory3 = null;    // Buffer to hold samples received for the ACC Z channel
    private ArrayList<Integer> sensors;              // Sensor we would like to plot (from the bundle)
    private Socket commSock, emgSock, accSock;        // Communication sockets

    // The number of bytes to read a complete set of data from all 16 sensors.
    // The ratio 1728:384 maintains the 27:2 sample ratio and 16:48 channel ratio between EMG and ACC data.
    final int EMG_BYTE_BUFFER = 1728 * 4;

    protected Muscle muscle;
    protected byte[] emgBytes;
    protected double rms;
    protected double max;

    private boolean maxFound = false;

    public ReadSensor(Muscle m, byte[] emgBytes) {
        this.muscle = m;
        this.emgBytes = emgBytes;
        emgHistory = new ArrayList<Number>();
        max = -1;

        readBytes();
    }



    protected void readBytes() {
        //System.out.println("READSENSOR - Thread ID: "+Thread.currentThread().getId()+" -- Thread name: "+Thread.currentThread().getName());
        // Demultiplex, parse the byte array, and add the appropriate samples to the history buffer.
        for (int i = 0; i < emgBytes.length / 4; i++) { //use i+16 instead
            if (i % 16 == muscle.getSensorNr()-1) {
                float f = ByteBuffer.wrap(emgBytes, 4 * i, 4).getFloat();
                //System.out.println("This should be sensor 1: " + muscle.getSensorNr() + "\nFloat: " + f);
                emgHistory.add(f * 1000); // convert V -> mV
            }
        }
        System.out.print("\nMuscle" + muscle.getSensorNr() + ": ");
        for(int i = 0;i<emgHistory.size();i++)
            System.out.print(emgHistory.get(i)+" ");
        System.out.println("\n");
        calculateRMS();
        //findMaxValue();
        //emgHistory.clear();
    }

    //Calculate root mean square (RMS)
    protected void calculateRMS() {
        double ms = 0;
        for(Number n : emgHistory) {
            double d = n.doubleValue();
            ms+= d*d;
        }
        ms/= emgHistory.size();
        rms = Math.sqrt(ms);
        muscle.setRMS(rms);
    }

    public double getRMS() {
        return rms;
    }

    public double getMax() { return max; }

    public Muscle getMuscle() {
        return muscle;
    }
}