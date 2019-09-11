package com.iview.mirromove;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import com.iview.stepmotor.MotorControl;

public class MotorControlHelper {
    private final static String TAG = "MotorControlHelper";

    private static MotorControlHelper motorControlHelper = null;
    private Context mContext;

    HandlerThread mHandlerThread;
    Handler mHandler;

    public static final int HMotor = 1;
    public static final int VMotor = 2;

    public static final int HMotorLeftDirection = 1;
    public static final int HMotorRightDirection = 0;

    public static final int VMotorUpDirection = 0;
    public static final int VMotorDownDirection = 1;

    public final static int MSG_CONTROL_H_MOTOR = 0;
    public final static int MSG_CONTROL_D_MOTOR = 1;
    public final static int MSG_STOP_D_MOTOR = 2;

    private boolean bMotorRunning = false;

    private MotorCallBack motorCallBack = null;

    private MotorControlHelper(Context context) {
        this.mContext = context;
        init();
    }

    public static MotorControlHelper getInstance(Context context) {
        synchronized (MotorControlHelper.class) {
            if (motorControlHelper == null) {
                motorControlHelper = new MotorControlHelper(context.getApplicationContext());
            }
        }
        return motorControlHelper;
    }

    public void init() {
        initBackThread();
    }

    private void initBackThread() {
        mHandlerThread = new HandlerThread("motor_control");
        mHandlerThread.start();

        mHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_STOP_D_MOTOR:
                        MotorControl.stopMotorRunning(MotorControl.HMotor);
                        MotorControl.stopMotorRunning(MotorControl.VMotor);

                        bMotorRunning = false;

                        if (motorCallBack != null) {
                            motorCallBack.onExecute();
                        }
                        break;
                }
            }
        };
    }

    /**
     * 控制马达转动， 转动结束会通过 UnitySendMessage 通知unity
     * @param motorId: 马达id
    {
    @link HMotor 水平方向马达
    @link VMotor 垂直方向马达
    }
     * @param steps : PWM脉冲数量
     * @param dir : 转动方向
     * {
    @link    HMotorLeftDirection
    @link    HMotorRightDirection
    @link    VMotorUpDirection
    @link    VMotorDownDirection
     * }
     */
    public int controlMotor(final int motorId, final int steps, final int dir, final int delay) {
        if (steps != 0 && delay != 0) {
            MotorControl.controlMotor(motorId, steps, dir, delay);

//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    Log.e(TAG, "controlMotor");
//                    int endPos =  MotorControl.controlMotor(motorId, steps, dir, delay);
//                }
//            }).start();
        }

        return 0;
    }


    public int controlMultiMotor(final int hDir, final int hDelay, final int vDir, final int vDelay, final int duration) {

        Log.e(TAG, "ControlService hDir:" + hDir + ", hDelay:" + hDelay + ", vDir:" + vDir + ",vDelay:" + vDelay + ", duration:" + duration);

        bMotorRunning = true;

        MotorControl.setMotorSpeed(MotorControl.HMotor, hDelay);
        MotorControl.setMotorDirection(MotorControl.HMotor, hDir);
        if (hDelay != 0) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    int endPos =  MotorControl.startMotorRunning(MotorControl.HMotor);
                }
            }).start();
        }

        MotorControl.setMotorSpeed(MotorControl.VMotor, vDelay);
        MotorControl.setMotorDirection(MotorControl.VMotor, vDir);
        if (vDelay != 0) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    int endPos = MotorControl.startMotorRunning(MotorControl.VMotor);
                }
            }).start();
        }

        mHandler.sendEmptyMessageDelayed(MSG_STOP_D_MOTOR, duration);

        return 0;
    }

    public boolean getMotorRunning() {
        return bMotorRunning;
    }

    public void setMotorCallBack(MotorCallBack motorCallBack) {
        this.motorCallBack = motorCallBack;
    }
}
