package com.example.rene.emg;

import java.io.Serializable;
import java.net.Socket;

/**
 * Created by Rene on 22/03/2016.
 */
public class ComSocketSingleton implements Serializable {

    private static Socket socket;

    public static void setSocket(Socket socketpass) {
        ComSocketSingleton.socket = socketpass;
    }

    public static Socket getSocket() {
        return ComSocketSingleton.socket;
        //return socket;
    }
}
