package com.iview.mirromove;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.iview.mirromove.util.JSONParser;
import com.iview.mirromove.util.MsgType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketService extends Service {
    private final static String TAG = "SocketService";

    private TcpServer tcpServer;

    Handler handler = null;
    private int port;
    Socket client;
    private byte buffer[] = new byte[500];
    InputStream inputStream;
    OutputStream outputStream;
    ServerSocket serverSocket;

    ExecutorService exec;

    private SocketBinder mBinder = new SocketService.SocketBinder();

    public class SocketBinder extends Binder {
        public SocketService getService() {
            return SocketService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        init();
    }

    private void init() {
        exec = Executors.newCachedThreadPool();
    }

    public void startTcpServer(int port) {
        tcpServer = new TcpServer(port);
        exec.execute(tcpServer);
    }

    public void stopTcpServer() {
        if (tcpServer != null) {
            tcpServer.closeSelf();
        }
    }

    public class TcpServer implements Runnable {

        private int port = 8091;
        private boolean isListen = true;
        public ArrayList<ReceiveSocketThread> receiveSocketThreads = new ArrayList<ReceiveSocketThread>();

        public TcpServer(int port) {
            this.port = port;
        }

        public void setIsListen(boolean listen) {
            isListen = listen;
        }

        public void closeSelf() {
            isListen = false;
            for (ReceiveSocketThread receiveSocketThread : receiveSocketThreads) {
                receiveSocketThread.isRun = false;
            }

            receiveSocketThreads.clear();
        }

        private Socket getSocket(ServerSocket serverSocket) {
            try {
                return serverSocket.accept();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        public void run() {
            try {
                ServerSocket serverSocket = new ServerSocket(port);
                serverSocket.setSoTimeout(5000);

                while (isListen) {
                    Log.d(TAG, "Start listen");

                    Socket socket = getSocket(serverSocket);
                    if (socket != null) {
                        new ReceiveSocketThread(socket);
                    }
                }

                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public class ReceiveSocketThread extends Thread {
            Socket socket = null;
            private PrintWriter pw;
            private InputStream is = null;
            private OutputStream os = null;
            private String ip = null;
            private boolean isRun = true;

            ReceiveSocketThread(Socket socket) {
                this.socket = socket;
                ip = socket.getInetAddress().toString();
                Log.i(TAG, "ReceiveSocketThread: new ip connect :" + ip);

                try {
                    socket.setSoTimeout(5000);
                    os = socket.getOutputStream();
                    is = socket.getInputStream();
                    pw = new PrintWriter(os,true);
                    start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            public void send(String msg) {
                pw.println(msg);
                pw.flush();
            }

            @Override
            public void run() {
                byte buff[] = new byte[4096];
                String rcvMsg;
                int rcvLen;

                receiveSocketThreads.add(this);

                while (isRun && !socket.isClosed() && !socket.isInputShutdown()) {
                    try {
                        if ((rcvLen = is.read(buff)) != -1) {
                            rcvMsg = new String(buff, 0, rcvLen);
                            Log.d(TAG, "receive msg:" + rcvMsg);

                            parseMsg(rcvMsg);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void parseMsg(String msg) {
        JSONParser jsonParser = new JSONParser(msg);

        String type = jsonParser.getType();
        String action = jsonParser.getAction();

        if (type.equals(MsgType.TYPE_PATH_PLANNING)
            || (type.equals(MsgType.TYPE_CONTROL))
            || (type.equals(MsgType.TYPE_AUTO_RUNNING))) {
            Intent pathIntent = new Intent(MsgType.INTENT_ACTION_PATH_PLANNING);
            pathIntent.putExtra("message", msg);
            LocalBroadcastManager.getInstance(this).sendBroadcast(pathIntent);
        }
    }
}
