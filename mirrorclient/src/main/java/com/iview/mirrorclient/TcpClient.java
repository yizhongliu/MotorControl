package com.iview.mirrorclient;

import android.util.Log;

import java.io.DataInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class TcpClient implements Runnable {

    private final static String TAG = "TcpClient";

    private String serverIp = "192.168.0.117";
    private int serverPort = 8091;

    private PrintWriter pw;
    private InputStream inputStream;
    private DataInputStream dataInputStream;

    private boolean isRun = true;

    protected Socket socket = null;
    byte buff[] = new byte[4096];
    private String rcvMsg;
    private int rcvLen;

    public TcpClient(String ip, int port) {
        this.serverIp = ip;
        this.serverPort = port;
    }

    public void closeSelf() {
        isRun = false;
    }

    public void send(String msg) {
        pw.println(msg);
        pw.flush();
    }
    @Override
    public void run() {
        try {
            socket = new Socket(serverIp, serverPort);
            socket.setSoTimeout(5000);
            pw =new PrintWriter(socket.getOutputStream(), true);
            inputStream = socket.getInputStream();
            dataInputStream = new DataInputStream(inputStream);

        } catch (Exception e) {
            e.printStackTrace();
        }

        while (isRun) {
            try {
                rcvLen = dataInputStream.read(buff);
                rcvMsg = new String(buff, 0, rcvLen, "utf-8");
                Log.d(TAG, "receive new msg:" + rcvMsg);
            } catch (Exception e) {

            }
        }

        try {
            pw.close();
            inputStream.close();
            dataInputStream.close();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
