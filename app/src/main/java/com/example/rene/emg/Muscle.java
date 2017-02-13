package com.example.rene.emg;

import java.io.Serializable;

/**
 * Created by Rene on 23/02/2016.
 */
public class Muscle implements Serializable {

    private static final String RIGHT = "R";
    private static final String LEFT = "L";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public char getSide() {
        return side;
    }

    public void setSide(char side) {
        this.side = side;
    }

    public int getSensorNr() {
        return sensorNr;
    }

    public void setSensorNr(int sensorNr) {
        this.sensorNr = sensorNr;
    }

    private String name;
    private char side;
    private int sensorNr;
    private volatile double max;
    private volatile double RMS;

    public double getRMS() {
        return RMS;
    }

    public void setRMS(double RMS) {
        this.RMS = RMS;
    }

    public double getMax() {
        return max;
    }

    public void setMax(double max) {
        this.max = max;
    }

    public Muscle(String name, char side, int sensorNr) {
        this.name = name;
        this.side = side;
        this.sensorNr = sensorNr;
        max = -1;
        RMS = -1;
    }
}
