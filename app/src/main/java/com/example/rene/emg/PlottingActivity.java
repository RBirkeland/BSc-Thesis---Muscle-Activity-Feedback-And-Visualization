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
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.StrictMode;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import com.androidplot.series.XYSeries;
import com.androidplot.ui.AnchorPosition;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.LineAndPointRenderer;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XLayoutStyle;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.YLayoutStyle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;


/**
 * This activity is responsible for starting the Delsys SDK stream, reading the data,
 * plotting it, and handling zoom operations. 
 */
public class PlottingActivity extends Activity implements OnTouchListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.plots);

        // Retrieve information about the sensor and server from the bundle extras.
        sensors = (ArrayList<Integer>) getIntent().getSerializableExtra("Sensors");

        m1 = new Muscle("", 'r',1);
        m2 = new Muscle("",'l',2);

        emgSock = EMGSocketSingleton.getSocket();
        commSock = ComSocketSingleton.getSocket();

        // Name the activity.
        setTitle("Plotting Sensors");

        PowerManager powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "Full Wake Lock");

        // Register touch event handlers for plot zooming.
        XYPlot emgPlot = (XYPlot)findViewById(R.id.emgXYPlot);
        emgPlot.setOnTouchListener(this);
        XYPlot emgPlot2 = (XYPlot)findViewById(R.id.emg2XYPlot);
        emgPlot2.setOnTouchListener(this);

    }

    /**
     * Called when the activity is brought to the foreground.  The digital streams are kept
     * inactive unless the activity is visible, to conserve system resources.
     */
    @Override
    protected void onResume() {
        super.onResume();

        // Keep the screen alive, even if it is not touched.
        wakeLock.acquire();

        // Set up the EMG plot.
        emgHistory = new ArrayList<Number>();
        emgSeries = new SimpleXYSeries("EMG Sensor 1");
        XYPlot emgPlot = (XYPlot)findViewById(R.id.emgXYPlot);
        // Remove any previous series if the activity is just being brought to the foreground.
        for (XYSeries x : emgPlot.getSeriesSet())
            emgPlot.removeSeries(x);
        emgPlot.addSeries(emgSeries, LineAndPointRenderer.class, new LineAndPointFormatter(Color.rgb(0, 255, 0), Color.TRANSPARENT, Color.TRANSPARENT));
        emgPlot.setDomainLabel("Sample Index");
        emgPlot.getDomainLabelWidget().setVisible(false);
        emgPlot.setRangeLabel("mV");
        emgPlot.getDomainLabelWidget().pack();
        emgPlot.setRangeBoundaries(-5, 5, BoundaryMode.FIXED);
        emgPlot.setTitle("EMG Sensor 1");
        emgPlot.position(
                emgPlot.getLegendWidget(),
                20,
                XLayoutStyle.ABSOLUTE_FROM_RIGHT,
                35,
                YLayoutStyle.ABSOLUTE_FROM_BOTTOM,
                AnchorPosition.RIGHT_BOTTOM);
        emgPlot.disableAllMarkup();

        // Set up the EMG2 plot. ReneAdd
        emgHistory2 = new ArrayList<Number>();
        emgSeries2 = new SimpleXYSeries("EMG Sensor 2");
        XYPlot emgPlot2 = (XYPlot)findViewById(R.id.emg2XYPlot);
        // Remove any previous series if the activity is just being brought to the foreground.
        for (XYSeries x : emgPlot2.getSeriesSet())
            emgPlot2.removeSeries(x);
        emgPlot2.addSeries(emgSeries2, LineAndPointRenderer.class, new LineAndPointFormatter(Color.rgb(0, 0, 255), Color.TRANSPARENT, Color.TRANSPARENT));
        emgPlot2.setDomainLabel("Sample Index");
        emgPlot2.getDomainLabelWidget().setVisible(false);
        emgPlot2.setRangeLabel("mV");
        emgPlot2.getDomainLabelWidget().pack();
        emgPlot2.setRangeBoundaries(-5, 5, BoundaryMode.FIXED);
        emgPlot2.setTitle("EMG Sensor 2");
        emgPlot2.position(
                emgPlot2.getLegendWidget(),
                20,
                XLayoutStyle.ABSOLUTE_FROM_RIGHT,
                35,
                YLayoutStyle.ABSOLUTE_FROM_BOTTOM,
                AnchorPosition.RIGHT_BOTTOM);
        emgPlot2.disableAllMarkup();
        running = true;

        t = new Thread(new Runnable() {

            public void run() {
                readBytes();
            }
        });
        t.setPriority(Thread.MIN_PRIORITY); // Prioritize touch handling and screen updates above network communication.
        t.start();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Allow the screen to sleep when plotting is inactive.
        wakeLock.release();

        // Stop the thread that reads data from the socket, and wait for it to complete.
        running = false;
        if (t != null && t.isAlive()) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Close connections to the server.  Check if they were initialized correctly to avoid exceptions.
        try {
            if (commSock != null) {
                PrintWriter writer = new PrintWriter(commSock.getOutputStream());
                writer.write("STOP\r\n\r\n"); // request the end of data streaming
                writer.flush();
                BufferedReader reader = new BufferedReader(new InputStreamReader(commSock.getInputStream()));
                do
                {
                    while (!reader.ready())
                        ;
                } while (!reader.readLine().contains("OK"));
                commSock.close();
            }
            if (emgSock != null)
                emgSock.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void readBytes() {

        // Allocate space to store data incoming from the network.
        byte[] emgBytes = new byte[EMG_BYTE_BUFFER];

        while(running) {
            try {
                // Wait until a complete set of data is ready on the socket.  The
                while (running && emgSock.getInputStream().available() < EMG_BYTE_BUFFER) {
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

            synchronized (this) {
                // Demultiplex, parse the byte array, and add the appropriate samples to the history buffer.

                for (int i = 0; i < EMG_BYTE_BUFFER / 4; i++) {
                    if (i % 16 == (m1.getSensorNr()-1)) {
                        float f = ByteBuffer.wrap(emgBytes, 4 * i, 4).getFloat();
                        emgHistory.add(f * 1000); // convert V -> mV
                    } else if (i % 16 == (m2.getSensorNr()-1)) {
                        float f = ByteBuffer.wrap(emgBytes, 4 * i, 4).getFloat();
                        emgHistory2.add(f * 1000); // convert V -> mV
                    }
                }
            }

            // If there is no touch zoom action in progress, update the plots with the newly acquired data.
            if (mode == NONE && running) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        UpdatePlots();
                    }
                });
            }
        }
    }

    private void UpdatePlots() {
        synchronized (this) {
            // Move data from the history buffers to the plot's internal data storage
            for (Number x : emgHistory)
                emgSeries.addLast(emgSampleIdx++, x);

            for (Number x : emgHistory2)
                emgSeries2.addLast(emgSampleIdx2++, x);

            // Clear the history buffers
            emgHistory.clear();
            emgHistory2.clear();

            // Maintain a fixed duration of data on the plots.
            while (emgSeries.size() > HISTORY_SZ) {
                emgSeries.removeFirst();
            }

            while (emgSeries2.size() > HISTORY_SZ) {
                emgSeries2.removeFirst();
            }

            // Trigger a redraw of each plot
            XYPlot emgPlot = (XYPlot) findViewById(R.id.emgXYPlot);
            emgPlot.redraw();

            XYPlot emgPlot2 = (XYPlot) findViewById(R.id.emg2XYPlot);
            emgPlot2.redraw();

        }
    }

    /////////////// Touch zoom and pan implementation ////////////////////////////////////////////
// Members and methods below are related to the touch zooming and panning of plots and are  //
// not critical to receiving the EMG or ACC data.                                           //
//////////////////////////////////////////////////////////////////////////////////////////////
    static final private int NONE = 0;
    static final private int ONE_FINGER_DRAG = 1;
    static final private int TWO_FINGERS_DRAG_X = 2;
    static final private int TWO_FINGERS_DRAG_Y = 3;
    private int mode = NONE;

    private PointF minXY;
    private PointF maxXY;
    private int oldBorderColor;

    private float distBetweenFingersY;
    private float distBetweenFingersX;
    private float touchDownY;

    public boolean onTouch(View v, MotionEvent event) {
        XYPlot emgPlot = (XYPlot)findViewById(R.id.emgXYPlot);
        XYPlot accPlot = (XYPlot)findViewById(R.id.emg2XYPlot);
        XYPlot targetPlot;

        if (v == emgPlot)
            targetPlot = emgPlot;
        else
            targetPlot = accPlot;

        switch(event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: // Start gesture
                mode = ONE_FINGER_DRAG;
                //Set of internal variables for keeping track of the boundaries
                targetPlot.calculateMinMaxVals();
                minXY = new PointF(targetPlot.getCalculatedMinX().floatValue(),
                        targetPlot.getCalculatedMinY().floatValue()); //initial minimum data point
                //absolute minimum value for the domain boundary maximum
                maxXY = new PointF(targetPlot.getCalculatedMaxX().floatValue(),
                        targetPlot.getCalculatedMaxY().floatValue()); //initial maximum data point

                touchDownY = event.getY(0);
                oldBorderColor = targetPlot.getBorderPaint().getColor();
                targetPlot.getBorderPaint().setColor(Color.RED);
                targetPlot.redraw();
                break;
            case MotionEvent.ACTION_POINTER_DOWN: // second finger
                distBetweenFingersY = spacingY(event);
                distBetweenFingersX = spacingX(event);
                // the distance check is done to avoid false alarms
                if (spacing(event) > 5f)
                {
                    if (spacingY(event) > spacingX(event))
                    {
                        mode = TWO_FINGERS_DRAG_Y;
                    }
                    else
                    {
                        mode = TWO_FINGERS_DRAG_X;
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mode == ONE_FINGER_DRAG)
                {
                    panY((event.getY(0) - touchDownY) / targetPlot.getHeight());
                    touchDownY = event.getY(0);
                    targetPlot.setRangeBoundaries(minXY.y, maxXY.y, BoundaryMode.AUTO);
                    UpdatePlots();

                } else if (mode == TWO_FINGERS_DRAG_Y) {
                    final float oldDistY = distBetweenFingersY;
                    distBetweenFingersY = spacingY(event);
                    zoomY(oldDistY / distBetweenFingersY);
                    targetPlot.setRangeBoundaries(minXY.y, maxXY.y, BoundaryMode.AUTO);
                    targetPlot.redraw();
                } else if (mode == TWO_FINGERS_DRAG_X)
                {
                    final float oldDistX = distBetweenFingersX;
                    distBetweenFingersX = spacingX(event);
                    zoomX(oldDistX / distBetweenFingersX);
                    UpdatePlots();

                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                targetPlot.getBorderPaint().setColor(oldBorderColor);
                targetPlot.redraw();
                break;
        }
        return true;
    }

    private double spacing(MotionEvent event) {
        final float x = event.getX(0) - event.getX(1);
        final float y = event.getY(0) - event.getY(1);
        return Math.sqrt(x * x + y * y);
    }

    private float spacingY(MotionEvent event) {
        return event.getY(0) - event.getY(1);
    }

    private float spacingX(MotionEvent event) {
        return event.getX(0) - event.getX(1);
    }

    private void zoomY(float scale) {
        final float rangeSpan = maxXY.y - minXY.y;
        final float rangeMidPoint = maxXY.y - rangeSpan / 2.0f;
        float offset = rangeSpan * scale / 2.0f;
        offset = Math.min(Math.max(offset, 1), 10);
        minXY.y = rangeMidPoint - offset;
        maxXY.y = rangeMidPoint + offset;
    }

    private void panY(float delta) {
        final float rangeSpan = maxXY.y - minXY.y;
        minXY.y += delta * rangeSpan;
        maxXY.y += delta * rangeSpan;
    }

    private void zoomX(float scale)
    {
        synchronized (this)
        {
            HISTORY_SZ = Math.min(Math.max((int)(HISTORY_SZ * scale), 500), 2000);
        }
    }
    //////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////


    private WakeLock wakeLock;						// Object to control screen locking
    private Thread t;								// Worker thread to read data from the socket
    private boolean running;						// Flag to indicate if the worker thread should continue
    private long emgSampleIdx = 0;					// Current EMG sample number
    private int HISTORY_SZ = 2000;					// Number of samples to display on the screen at once
    private SimpleXYSeries emgSeries = null;		// Series representing the EMG channel
    private ArrayList<Number> emgHistory = null;	// Buffer to hold samples received for the EMG channel

    private Socket commSock, emgSock;		// Communication sockets

    private SimpleXYSeries emgSeries2 = null;
    private ArrayList<Number> emgHistory2 = null;
    private long emgSampleIdx2 = 0;
    //private ArrayList<Muscle> muscles = new ArrayList<Muscle>();
    private ArrayList<Integer> sensors = new ArrayList<Integer>();
    private Muscle m1;
    private Muscle m2;

    // The number of bytes to read a complete set of data from all 16 sensors.
    // The ratio 1728:384 maintains the 27:2 sample ratio and 16:48 channel ratio between EMG and ACC data.
    private final int EMG_BYTE_BUFFER = 1728 * 4;

}