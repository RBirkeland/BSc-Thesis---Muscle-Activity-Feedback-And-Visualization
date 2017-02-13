package com.example.rene.emg;

import java.net.Socket;

/**
 * Created by Rene on 22/03/2016.
 */
public class EMGSocketSingleton {

    private static Socket socket;

    public static void setSocket(Socket socketpass) {
        EMGSocketSingleton.socket = socketpass;
    }

    public static Socket getSocket() {
        return EMGSocketSingleton.socket;
        //return socket;
    }
}
