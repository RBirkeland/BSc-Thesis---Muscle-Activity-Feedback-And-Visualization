package com.example.rene.emg;

/**
 * Created by Rene on 01/04/2016.
 */
public class CalibrationReadSensor  extends ReadSensor {

    public CalibrationReadSensor(Muscle m, byte[] emgBytes) {
        super(m, emgBytes);
        readBytes();
        findMaxValue();
    }

    public void findMaxValue() {
        //System.out.println("Finding Maximum for muscle: "+ muscle.getSide() + muscle.getName()+"\nMax: "+ muscle.getMax()+" RMS: "+rms);
        max = muscle.getMax();
        /*for(Number n : emgHistory) {
            double nDouble = n.doubleValue();
            if(nDouble > max) max = nDouble;
        }
        muscle.setMax(max);*/
        //System.out.println("Muscle Nr: "+ muscle.getSensorNr() +" - Max: "+max);
        if(rms > max) muscle.setMax(rms);
    }
}
