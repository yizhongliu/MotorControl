package com.iview.mirromove;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.iview.mirromove.util.JSONParser;
import com.iview.mirromove.util.MsgType;

import java.util.ArrayList;
import java.util.List;

public class ControlService extends Service {
    private final static String TAG = "ControlService";

    private final static int MSG_PATH_PLAN_START = 0;
    private final static int MSG_PATH_PLAN_MOVE = 1;
    private final static int MSG_PATH_PLAN_SHOW = 2;
    private final static int MSG_PATH_PLAN_PREVIEW = 3;
    private final static int MSG_PATH_PLAN_STOP = 4;
    private final static int MSG_PATH_PLAN_RUN = 5;
    private final static int MSG_PATH_PLAN_EXECUTE = 6;
    private final static int MSG_PATH_PLAN_RUN_STOP = 7;

    private MessageReceiver mMessageReceiver;

    private List<PathPlanning> pathPlanningList = new ArrayList<>();

    private HandlerThread mHandlerThread;
    private Handler mHandler;


    private boolean bMotorReset = false;
    private boolean bPathPlanRunning = false;
    private boolean bPathPlanning = false;

    private PlayCallBack playCallBack = null;

    private int cmdIndex;

    MyMotorCallBack motorCallBack = new MyMotorCallBack();

    private ControlService.ControlBinder mBinder = new ControlService.ControlBinder();

    public class ControlBinder extends Binder {
        public ControlService getService() {
            return ControlService.this;
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

        registerBroadcastReceive();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();

        unregisterBroadcastReceive();
    }

    private void init() {
        mMessageReceiver = new MessageReceiver();

        initBackThread();

        MotorControlHelper.getInstance(this).setMotorCallBack(motorCallBack);
    }

    private void initBackThread() {
        mHandlerThread = new HandlerThread("execute-command");
        mHandlerThread.start();

        mHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                Log.e(TAG, "handle msg.what:" + msg.what);
                switch (msg.what) {
                    case MSG_PATH_PLAN_START:
                        Log.e(TAG, "handle message MSG_PATH_PLAN_START");
                        bPathPlanning = true;

                        cmdIndex = 0;
                        HandlePathPlanningStart();
                        break;
                    case MSG_PATH_PLAN_MOVE:
                        Log.e(TAG, "handle message MSG_PATH_PLAN_MOVE");
                        if (MotorControlHelper.getInstance(ControlService.this).getMotorRunning() == false) {
                            String message = msg.getData().getString("message");
                            HandlePathPlanningMove(message);
                        }
                        break;
                    case MSG_PATH_PLAN_SHOW:
                        Log.e(TAG, "handle message MSG_PATH_PLAN_SHOW");
                        String message = msg.getData().getString("message");
                        HandlePathPlanningShow(message);
                        break;
                    case MSG_PATH_PLAN_PREVIEW:
                        Log.e(TAG, "handle message MSG_PATH_PLAN_PREVIEW");
                        String preMessage = msg.getData().getString("message");
                        HandlePathPlanningPreview(preMessage);
                        break;
                    case MSG_PATH_PLAN_STOP:
                        Log.e(TAG, "handle message MSG_PATH_PLAN_STOP");
                        bPathPlanning = false;
                        break;
                    case MSG_PATH_PLAN_RUN:
                        Log.e(TAG, "handle message MSG_PATH_PLAN_RUN");
                        if (!pathPlanningList.isEmpty()) {
                            bPathPlanRunning = true;
                            cmdIndex = 0;
                        }

                        MotorControlHelper.getInstance(ControlService.this).controlMotor(MotorControlHelper.HMotor, 100000, MotorControlHelper.HMotorLeftDirection, 400);
                        MotorControlHelper.getInstance(ControlService.this).controlMotor(MotorControlHelper.VMotor, 100000, MotorControlHelper.VMotorUpDirection, 1000);

                        mHandler.sendEmptyMessage(MSG_PATH_PLAN_EXECUTE);

                        break;
                    case MSG_PATH_PLAN_RUN_STOP:
                        Log.e(TAG, "handle message MSG_PATH_PLAN_RUN_STOP");
                        bPathPlanRunning = false;
                        break;
                    case MSG_PATH_PLAN_EXECUTE:
                        Log.e(TAG, "handle message MSG_PATH_PLAN_EXECUTE");
                        if (bPathPlanning == true) {
                            if (cmdIndex < pathPlanningList.size()) {
                                String action = pathPlanningList.get(cmdIndex).getAction();

                                if (action.equals(MsgType.ACTION_MOVE)) {
                                    int angle = pathPlanningList.get(cmdIndex).getAngle();
                                    Log.e(TAG, "move angle:" + angle);

                                    controlMotorMove(angle);
                                } else if (action.equals(MsgType.ACTION_SHOW)) {
                                    String url = pathPlanningList.get(cmdIndex).getUrl();
                                    double rotation = pathPlanningList.get(cmdIndex).getRotateAngle();
                                    int showTime = pathPlanningList.get(cmdIndex).getImgDisplayTime();

                                    if (playCallBack != null) {
                                        playCallBack.play(url, showTime, rotation);
                                    }
                                }

                                cmdIndex++;
                            } else {
                                mHandler.sendEmptyMessage(MSG_PATH_PLAN_RUN);
                            }
                        }
                        break;
                }

            }
        };
    }



    private void registerBroadcastReceive() {
        IntentFilter filter = new IntentFilter();
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        filter.addAction(MsgType.INTENT_ACTION_PATH_PLANNING);
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, filter);
    }

    private void unregisterBroadcastReceive() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }

    public class MessageReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String messge = intent.getStringExtra("message");

                Log.e(TAG, "getStringExtra:" + messge);

                JSONParser jsonParser = new JSONParser(messge);

                String type = jsonParser.getType();
                String action = jsonParser.getAction();

                Log.e(TAG, "message paser type:" + type + ", action:" + action);

                if (bMotorReset == true) {
                    Log.e(TAG, "Motor rest");
                    return;
                }

                if (type.equals(MsgType.TYPE_PATH_PLANNING)) {
                    if (action.equals(MsgType.ACTION_START)) {
                        mHandler.sendEmptyMessage(MSG_PATH_PLAN_START);
                    } else if (action.equals(MsgType.ACTION_MOVE)) {

                        Message moveMessage = new Message();
                        moveMessage.what = MSG_PATH_PLAN_MOVE;
                        Bundle bundle = new Bundle();
                        bundle.putString("message" , messge);
                        moveMessage.setData(bundle);

                        mHandler.sendMessage(moveMessage);
                    } else if (action.equals(MsgType.ACTION_SHOW)) {
                        Message showMessage = new Message();
                        showMessage.what = MSG_PATH_PLAN_SHOW;
                        Bundle bundle = new Bundle();
                        bundle.putString("message" , messge);
                        showMessage.setData(bundle);

                        mHandler.sendMessage(showMessage);
                    } else if (action.equals(MsgType.ACTION_PREVIEW)) {
                        Message previewMessage = new Message();
                        previewMessage.what = MSG_PATH_PLAN_PREVIEW;
                        Bundle bundle = new Bundle();
                        bundle.putString("message" , messge);
                        previewMessage.setData(bundle);

                        mHandler.sendMessage(previewMessage);
                    } else if (action.equals(MsgType.ACTION_RUN)) {
                        Message runMessage = new Message();
                        runMessage.what = MSG_PATH_PLAN_RUN;
                        Bundle bundle = new Bundle();
                        bundle.putString("message" , messge);
                        runMessage.setData(bundle);

                        mHandler.sendMessage(runMessage);
                    } else if (action.equals(MsgType.ACTION_RUN_STOP)) {
                        mHandler.sendEmptyMessage(MSG_PATH_PLAN_RUN_STOP);
                    }
                }

            } catch (Exception e){
            }
        }
    }

    private void HandlePathPlanningStart() {

        bMotorReset = true;

        if (bPathPlanRunning == true) {
            bPathPlanRunning = false;

            pathPlanningList.clear();
        }

        MotorControlHelper.getInstance(this).controlMotor(MotorControlHelper.HMotor, 100000, MotorControlHelper.HMotorLeftDirection, 400);
        MotorControlHelper.getInstance(this).controlMotor(MotorControlHelper.VMotor, 100000, MotorControlHelper.VMotorUpDirection, 1000);

        bMotorReset = false;
    }

    private void HandlePathPlanningMove(String message) {

        JSONParser jsonParser = new JSONParser(message);
        int angle = jsonParser.getAngle();

        controlMotorMove(angle);

        PathPlanning pathPlanning = new PathPlanning("Move", angle, 0, null, 0);
        pathPlanningList.add(pathPlanning);
    }

    private void HandlePathPlanningShow(String message) {
        JSONParser jsonParser = new JSONParser(message);
        String url = jsonParser.getUrl();
        double rotation = jsonParser.getRotation();
        int showTime = jsonParser.getShowTime();

        PathPlanning pathPlanning = new PathPlanning("Show", 0, rotation, url, showTime);
        pathPlanningList.add(pathPlanning);
    }

    private void HandlePathPlanningPreview(String message) {
        JSONParser jsonParser = new JSONParser(message);
        String url = jsonParser.getUrl();
        double rotation = jsonParser.getRotation();
        int showTime = jsonParser.getShowTime();

        if (playCallBack != null) {
            playCallBack.play(url, showTime, rotation);
        }
    }

    public void controlMotorMove(int angle) {
        Log.e(TAG, "controlMotorMove:" + angle);
        int hDir = MotorControlHelper.HMotorLeftDirection;
        int hDelay = 1000;
        int vDir = MotorControlHelper.VMotorUpDirection;
        int vDelay = 1000;
        int duration = 500;
        if (angle == 0) {
            hDir = MotorControlHelper.HMotorRightDirection;
            vDelay = 0;
        } else if (angle == 90) {
            vDir = MotorControlHelper.VMotorUpDirection;
            hDelay = 0;
        } else if (angle == 180) {
            hDir = MotorControlHelper.HMotorLeftDirection;
            vDelay = 0;
        } else if (angle == 270) {
            vDir = MotorControlHelper.VMotorDownDirection;
            hDelay = 0;
        } else if (angle > 0 && angle < 90) {
            hDir = MotorControlHelper.HMotorRightDirection;
            vDir = MotorControlHelper.VMotorUpDirection;

            double tag = Math.tan(Math.toRadians(angle));

            hDelay = 500;
            vDelay = (int)(hDelay * 4 * tag);

        } else if (angle > 90 && angle < 180) {
//            hDir = MotorControlHelper.HMotorLeftDirection;
//            vDir = MotorControlHelper.VMotorUpDirection;
        }

        MotorControlHelper.getInstance(this).controlMultiMotor(hDir, hDelay, vDir, vDelay, duration);
    }


    public class PathPlanning {
        private String action;
        private int angle;
        private double rotateAngle;
        private String url;

        public int getImgDisplayTime() {
            return imgDisplayTime;
        }

        public void setImgDisplayTime(int imgDisplayTime) {
            this.imgDisplayTime = imgDisplayTime;
        }

        private int imgDisplayTime;

        PathPlanning(String action, int angle, double rotateAngle, String url, int imgDisplayTime) {
            this.action = action;
            this.angle = angle;
            this.rotateAngle = rotateAngle;
            this.url = url;
            this.imgDisplayTime = imgDisplayTime;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public int getAngle() {
            return angle;
        }

        public void setAngle(int angle) {
            this.angle = angle;
        }

        public double getRotateAngle() {
            return rotateAngle;
        }

        public void setRotateAngle(double rotateAngle) {
            this.rotateAngle = rotateAngle;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    public void setPlayCallBack(PlayCallBack playCallBack) {
        this.playCallBack = playCallBack;
    }

    public class MyMotorCallBack implements MotorCallBack {

        @Override
        public void onExecute() {
            if (bPathPlanRunning) {
                Log.e(TAG, "onExecute");
                mHandler.sendEmptyMessage(MSG_PATH_PLAN_EXECUTE);
            }
        }
    }

    public void onShowComplete() {
        if (bPathPlanRunning) {
            Log.e(TAG, "onShowComplete");
            mHandler.sendEmptyMessage(MSG_PATH_PLAN_EXECUTE);
        }
    }
}
