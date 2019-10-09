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

import com.iview.mirromove.data.PathPlanning;
import com.iview.mirromove.util.CommandListProvider;
import com.iview.mirromove.util.JSONParser;
import com.iview.mirromove.util.MsgType;
import com.iview.stepmotor.MotorControl;

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

    private final static int MSG_CONTROL_SHOW = 20;
    private final static int MSG_CONTROL_MOVE = 21;
    private final static int MSG_CONTROL_SET_PARAM = 22;

    private final static int MSG_AUTORUNNING_START = 40;
    private final static int MSG_AUTORUNNING_STOP = 41;


    private final static int BASE_HDELAY = 100;
    private final static int BASE_VDELAY = 60;

    private final static int MAX_HDELAY = 300;
    private final static int MAX_VDELAY = 1200;

    private final static int BASE_HSTEP = 200;
    private final static int BASE_VSTEP = 100;
    private final static int BASE_DELAY = 200;

    int receiveCount = 0;
    int saveCount = 0;

    private MessageReceiver mMessageReceiver;

    private ArrayList<PathPlanning> pathPlanningList = new ArrayList<>();
    private ArrayList<PathPlanning> execPathPlanningList = new ArrayList<>();

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

                        receiveCount = 0;
                        saveCount = 0;
                        break;
                    case MSG_CONTROL_MOVE:
                    case MSG_PATH_PLAN_MOVE:
                        Log.e(TAG, "handle message MSG_PATH_PLAN_MOVE");
                        while (MotorControlHelper.getInstance(ControlService.this).getMotorRunning()) {
                        }

                        saveCount++;

                        String message = msg.getData().getString("message");
                        HandlePathPlanningMove(message);
//                        if (MotorControlHelper.getInstance(ControlService.this).getMotorRunning() == false) {
//                            HandlePathPlanningMove(message);
//                        }
                        break;
                    case MSG_PATH_PLAN_SHOW:
                        Log.e(TAG, "handle message MSG_PATH_PLAN_SHOW");
                        String messageMove = msg.getData().getString("message");
                        HandlePathPlanningShow(messageMove);
                        break;
                    case MSG_CONTROL_SHOW:
                    case MSG_PATH_PLAN_PREVIEW:
                        Log.e(TAG, "handle message MSG_PATH_PLAN_PREVIEW");
                        String preMessage = msg.getData().getString("message");
                        HandlePathPlanningPreview(preMessage);
                        break;
                    case MSG_PATH_PLAN_STOP:
                        Log.e(TAG, "handle message MSG_PATH_PLAN_STOP");
                        bPathPlanning = false;
                        try {
                            CommandListProvider.getInstance(ControlService.this).saveCmdList(pathPlanningList);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case MSG_AUTORUNNING_START:
                    case MSG_PATH_PLAN_RUN:
                        Log.e(TAG, "handle message MSG_PATH_PLAN_RUN");

                        try {
                            execPathPlanningList = CommandListProvider.getInstance(ControlService.this).loadCmdList();
                        } catch (Exception e) {
                            e.printStackTrace();
                            return;
                        }
                        if (!execPathPlanningList.isEmpty()) {
                            bPathPlanRunning = true;
                            cmdIndex = 0;
                        }

                        if (playCallBack != null) {
                            playCallBack.stop();
                        }

                        Log.e(TAG, "receiveCount:" + receiveCount + ", saveCount:" + saveCount + ", ListSize:" + execPathPlanningList.size());
                        MotorControl.swtichProjector(MotorControl.PROJECTOR_OFF);

                        MotorControlHelper.getInstance(ControlService.this).controlMotor(MotorControlHelper.HMotor, 1000000, MotorControlHelper.HMotorLeftDirection, 100, true);
                        MotorControlHelper.getInstance(ControlService.this).controlMotor(MotorControlHelper.VMotor, 1000000, MotorControlHelper.VMotorUpDirection, 60, true);

                        mHandler.sendEmptyMessage(MSG_PATH_PLAN_EXECUTE);

                        break;
                    case MSG_AUTORUNNING_STOP:
                    case MSG_PATH_PLAN_RUN_STOP:
                        Log.e(TAG, "handle message MSG_PATH_PLAN_RUN_STOP");
                        MotorControl.swtichProjector(MotorControl.PROJECTOR_ON);
                        bPathPlanRunning = false;
                        break;
                    case MSG_PATH_PLAN_EXECUTE:
                        Log.e(TAG, "handle message MSG_PATH_PLAN_EXECUTE");
                        if (bPathPlanRunning == true) {
                            if (cmdIndex < execPathPlanningList.size()) {
                                String action = execPathPlanningList.get(cmdIndex).getAction();

                                if (action.equals(MsgType.ACTION_MOVE)) {
                                    MotorControl.swtichProjector(MotorControl.PROJECTOR_OFF);
                                    int angle = execPathPlanningList.get(cmdIndex).getAngle();
                                    Log.e(TAG, "move angle:" + angle);

                                    controlMotorMove(angle);

                                    if (playCallBack != null) {
                                        playCallBack.stop();
                                    }
                                } else if (action.equals(MsgType.ACTION_SHOW)) {
                                    MotorControl.swtichProjector(MotorControl.PROJECTOR_ON);

                                    String url = execPathPlanningList.get(cmdIndex).getUrl();
                                    int rotation = execPathPlanningList.get(cmdIndex).getRotateAngle();
                                    int showTime = execPathPlanningList.get(cmdIndex).getImgDisplayTime();
                                    int keyStone = execPathPlanningList.get(cmdIndex).getKeystone();

                                    MotorControl.setKeyStone(keyStone);

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
                    case MSG_CONTROL_SET_PARAM:
                        String paramMessage = msg.getData().getString("message");
                        HandleSetParam(paramMessage);
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

                if (type.equals(MsgType.TYPE_CONTROL)) {
                    //路径规划 和 自动运行模式下， 不响应指令
                    if (bPathPlanning || bPathPlanRunning) {
                        Log.e(TAG, "bPathPlanning/bPathPlanRunning is true ");
                        return;
                    }

                    if (action.equals(MsgType.ACTION_MOVE)) {
                        mHandler.removeMessages(MSG_CONTROL_MOVE);
                        Message moveMessage = new Message();
                        moveMessage.what = MSG_CONTROL_MOVE;
                        Bundle bundle = new Bundle();
                        bundle.putString("message" , messge);
                        moveMessage.setData(bundle);
                        mHandler.sendMessage(moveMessage);
                    } else if (action.equals(MsgType.ACTION_SHOW)) {
                        Message showMessage = new Message();
                        showMessage.what = MSG_CONTROL_SHOW;
                        Bundle bundle = new Bundle();
                        bundle.putString("message" , messge);
                        showMessage.setData(bundle);
                        mHandler.sendMessage(showMessage);
                    } else if (action.equals(MsgType.ACTION_SET_PARAM)) {
                        Message showMessage = new Message();
                        showMessage.what = MSG_CONTROL_SET_PARAM;
                        Bundle bundle = new Bundle();
                        bundle.putString("message" , messge);
                        showMessage.setData(bundle);
                        mHandler.sendMessage(showMessage);
                    }
                }

                if (type.equals(MsgType.TYPE_PATH_PLANNING)) {
                    if (bPathPlanRunning) {
                        Log.e(TAG, "bPathPlanRunning is true ");
                        return;
                    }
                    if (action.equals(MsgType.ACTION_START)) {
                        mHandler.sendEmptyMessage(MSG_PATH_PLAN_START);
                    } else if (action.equals(MsgType.ACTION_STOP)) {
                        mHandler.sendEmptyMessage(MSG_PATH_PLAN_STOP);
                    } else if (action.equals(MsgType.ACTION_MOVE)) {

                        mHandler.removeMessages(MSG_PATH_PLAN_MOVE);

                        Message moveMessage = new Message();
                        moveMessage.what = MSG_PATH_PLAN_MOVE;
                        Bundle bundle = new Bundle();
                        bundle.putString("message" , messge);
                        moveMessage.setData(bundle);

                        receiveCount++;

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
                    }  else if (action.equals(MsgType.ACTION_SET_PARAM)) {
                        Message showMessage = new Message();
                        showMessage.what = MSG_CONTROL_SET_PARAM;
                        Bundle bundle = new Bundle();
                        bundle.putString("message" , messge);
                        showMessage.setData(bundle);
                        mHandler.sendMessage(showMessage);
                    }
                }


                if (type.equals(MsgType.TYPE_AUTO_RUNNING)) {
                    if (bPathPlanning) {
                        Log.e(TAG, "bPathPlanning is true ");
                        return;
                    }

                    if (action.equals(MsgType.ACTION_START)) {
                        Message runMessage = new Message();
                        runMessage.what = MSG_AUTORUNNING_START;
                        Bundle bundle = new Bundle();
                        bundle.putString("message" , messge);
                        runMessage.setData(bundle);
                        mHandler.sendMessage(runMessage);
                    } else if (action.equals(MsgType.ACTION_STOP)) {
                        Message runMessage = new Message();
                        runMessage.what = MSG_AUTORUNNING_STOP;
                        Bundle bundle = new Bundle();
                        bundle.putString("message", messge);
                        runMessage.setData(bundle);
                        mHandler.sendMessage(runMessage);
                    }
                }

            } catch (Exception e){
            }
        }
    }

    private void HandlePathPlanningStart() {

        bMotorReset = true;

        if (playCallBack != null) {
            playCallBack.stop();
        }

        if (bPathPlanRunning == true) {
            bPathPlanRunning = false;
        }

        pathPlanningList.clear();

        MotorControlHelper.getInstance(this).controlMotor(MotorControlHelper.HMotor, 1000000, MotorControlHelper.HMotorLeftDirection, 100, true);
        MotorControlHelper.getInstance(this).controlMotor(MotorControlHelper.VMotor, 1000000, MotorControlHelper.VMotorUpDirection, 200, true);

        bMotorReset = false;
    }

    private void HandlePathPlanningMove(String message) {

        JSONParser jsonParser = new JSONParser(message);
        int angle = jsonParser.getAngle();
        String type = jsonParser.getType();

        controlMotorMove(angle);

        if (type.equals(MsgType.TYPE_PATH_PLANNING)) {
            PathPlanning pathPlanning = new PathPlanning("Move", angle, 0, null, 0, 0);
            pathPlanningList.add(pathPlanning);
            Log.e(TAG, "add pathPlanningList :" + "lissize:" + pathPlanningList.size() + ", saveCount:" + saveCount);
        }

    }

    private void HandlePathPlanningShow(String message) {
        JSONParser jsonParser = new JSONParser(message);
        String type = jsonParser.getType();

        String url = jsonParser.getUrl();
        int rotation = jsonParser.getRotation();
        int showTime = jsonParser.getShowTime();
        int keyStone = jsonParser.getKeystone();

        if (type.equals(MsgType.TYPE_PATH_PLANNING)) {
            PathPlanning pathPlanning = new PathPlanning("Show", 0, rotation, url, showTime, keyStone);
            pathPlanningList.add(pathPlanning);
        }
    }

    private void HandlePathPlanningPreview(String message) {
        JSONParser jsonParser = new JSONParser(message);
        String url = jsonParser.getUrl();
        int rotation = jsonParser.getRotation();
        int showTime = jsonParser.getShowTime();
        int keyStone = jsonParser.getKeystone();

        MotorControl.setKeyStone(keyStone);

        if (playCallBack != null) {
            playCallBack.play(url, showTime, rotation);
        }
    }

    private void HandleSetParam(String message) {
        JSONParser jsonParser = new JSONParser(message);
        int rotation = jsonParser.getRotation();
        int keystone = jsonParser.getKeystone();

        MotorControl.setKeyStone(keystone);

        if (playCallBack != null) {
            playCallBack.setParam(rotation);
        }
    }

    public void controlMotorMove(int angle) {
        Log.e(TAG, "controlMotorMove:" + angle);
        int hDir = MotorControlHelper.HMotorLeftDirection;
        int hDelay = BASE_HDELAY;
        int vDir = MotorControlHelper.VMotorUpDirection;
        int vDelay = BASE_VDELAY;
        int duration = 300;

        int hSteps = BASE_HSTEP;
        int vSteps = BASE_VSTEP;
        int delay = BASE_DELAY;

        int[] steps = new int[2];

        if (angle == 0 || angle == 360) {
            hDir = MotorControlHelper.HMotorRightDirection;
            vDelay = 0;
            vSteps = 0;
        } else if (angle == 90) {
            vDir = MotorControlHelper.VMotorDownDirection;
            hDelay = 0;
            hSteps = 0;
            delay = BASE_DELAY * 2;
        } else if (angle == 180) {
            hDir = MotorControlHelper.HMotorLeftDirection;
            vDelay = 0;
            vSteps = 0;
        } else if (angle == 270) {
            vDir = MotorControlHelper.VMotorUpDirection;
            hDelay = 0;
            hSteps = 0;
            delay = BASE_DELAY * 2;
        } else if (angle > 0 && angle < 90) {
            hDir = MotorControlHelper.HMotorRightDirection;
            vDir = MotorControlHelper.VMotorDownDirection;

//            double tag = Math.tan(Math.toRadians(angle));
//
//            hDelay = 500;
//            vDelay = (int)(hDelay * 4 * Math.abs(tag));
//            hDelay = caculateHDelay(angle);
//            vDelay = caculateVDelay(angle);

            steps = caculateSteps(angle);
            hSteps = steps[0];
            vSteps = steps[1];

        } else if (angle > 90 && angle < 180) {
            hDir = MotorControlHelper.HMotorLeftDirection;
            vDir = MotorControlHelper.VMotorDownDirection;

//            double tag = Math.tan(Math.toRadians(angle));
//
//            hDelay = 500;
//            vDelay = (int)(hDelay * 4 * Math.abs(tag));
//              hDelay = caculateHDelay(angle);
//              vDelay = caculateVDelay(angle);

            steps = caculateSteps(angle);
            hSteps = steps[0];
            vSteps = steps[1];

        } else if (angle > 180 && angle < 270) {
            hDir = MotorControlHelper.HMotorLeftDirection;
            vDir = MotorControlHelper.VMotorUpDirection;

//            double tag = Math.tan(Math.toRadians(angle));
//
//            hDelay = 500;
//            vDelay = (int)(hDelay * 4 * Math.abs(tag));

//            hDelay = caculateHDelay(angle);
//            vDelay = caculateVDelay(angle);

            steps = caculateSteps(angle);
            hSteps = steps[0];
            vSteps = steps[1];
        } else if (angle > 270 && angle < 360) {
            hDir = MotorControlHelper.HMotorRightDirection;
            vDir = MotorControlHelper.VMotorUpDirection;

//            double tag = Math.tan(Math.toRadians(angle));
//
//            hDelay = 500;
//            vDelay = (int)(hDelay * 4 * Math.abs(tag));
//            hDelay = caculateHDelay(angle);
//            vDelay = caculateVDelay(angle);

            steps = caculateSteps(angle);
            hSteps = steps[0];
            vSteps = steps[1];
        }

   //     MotorControlHelper.getInstance(this).controlMultiMotor(hDir, hDelay, vDir, vDelay, duration);

        MotorControlHelper.getInstance(this).controlMultiMotor2(hSteps, vSteps, hDir, vDir, delay, false);
    }

    public int caculateHDelay(int angle) {
        int hdelay = BASE_HDELAY;
        double sin = Math.cos(Math.toRadians(angle));
        if (sin != 0) {
            hdelay = (int) (hdelay / Math.abs(sin));
            if (hdelay > MAX_HDELAY) {
                hdelay = MAX_HDELAY;
            }
        }

        return hdelay;
    }

    public int caculateVDelay(int angle) {
        int vdelay = BASE_VDELAY;
        double cos = Math.sin(Math.toRadians(angle));
        vdelay = (int) (vdelay / Math.abs(cos));
        if (vdelay > MAX_VDELAY) {
            vdelay = MAX_VDELAY;
        }

        return vdelay;
    }


    public int caculateHStep(int angle) {
        int hStep = BASE_HSTEP;
        double sin = Math.cos(Math.toRadians(angle));
        if (sin != 0) {
            hStep = (int) (hStep * Math.abs(sin));
        }

        return hStep;
    }

    public int caculateVStep(int angle) {
        int vStep = BASE_VSTEP;
        double cos = Math.sin(Math.toRadians(angle));
        vStep = (int) (vStep * Math.abs(cos));


        return vStep;
    }

    public int[] caculateSteps(int angle) {
        int[] steps = {BASE_HSTEP, BASE_VSTEP};
        steps[1] = caculateVStep(angle);
        steps[0] = caculateHStep(angle);
        int div;
        if (steps[0] != 0 && steps[1] != 0) {
            if (steps[0] > steps[1]) {
                div = steps[0] / steps[1];
                if (div != 1 && (div % 2) != 0 ) {
                    div = div + 1;
                }

                steps[0] = steps[1] * div;
            } else {
                div = steps[1] / steps[0];
                if (div != 1 && (div % 2) != 0 ) {
                    div = div + 1;
                }

                steps[1] = steps[0] * div;
            }
        }

        return steps;
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
