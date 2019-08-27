package com.iview.stepmotor;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import com.unity3d.player.UnityPlayer;
import com.unity3d.player.UnityPlayerActivity;

import java.util.ArrayList;
import java.util.List;

public class MotorControlUnity extends UnityPlayerActivity {
    private static String TAG = "MotorControlUnity";

    public static final int HMotor = 1;
    public static final int VMotor = 2;

    public static final int HMotorLeftDirection = 1;
    public static final int HMotorRightDirection = 0;

    public static final int VMotorUpDirection = 1;
    public static final int VMotorDownDirection = 0;

    public final static int MSG_CONTROL_H_MOTOR = 0;
    public final static int MSG_CONTROL_D_MOTOR = 1;
    public final static int MSG_STOP_D_MOTOR = 2;

    HandlerThread mHandlerThread;
    Handler mHandler;

    List<MotorData> mMotorDataList = new ArrayList<>();
    int cmdIndex = 0;

    private String unityPacakage;
    private String unityMethod;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initData();
    }

    private void initData() {
        mHandlerThread = new HandlerThread("UnityMotor Thread");
        mHandlerThread.start();

        mHandler = new Handler(mHandlerThread.getLooper()) {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_CONTROL_D_MOTOR:
                        if (mMotorDataList.isEmpty() || cmdIndex > mMotorDataList.size() - 1) {
                            MotorControl.stopMotorRunning(MotorControl.HMotor);
                            MotorControl.stopMotorRunning(MotorControl.VMotor);
                            UnityPlayer.UnitySendMessage(unityPacakage, unityMethod, "startMotorRunning end");
                            return;
                        }

                        MotorControl.setMotorSpeed(MotorControl.HMotor, mMotorDataList.get(cmdIndex).hSpeed);
                        MotorControl.setMotorDirection(MotorControl.HMotor, mMotorDataList.get(cmdIndex).hDirection);

                        if (mMotorDataList.get(cmdIndex).hSpeed != 0 && MotorControl.getMotorEnable(MotorControl.HMotor) == false) {
                            new Thread() {
                                @Override
                                public void run() {
                                    MotorControl.startMotorRunning(MotorControl.HMotor);
                                }
                            }.start();
                        } else if (mMotorDataList.get(cmdIndex).hSpeed == 0) {
                            MotorControl.stopMotorRunning(MotorControl.HMotor);
                        }

                        MotorControl.setMotorSpeed(MotorControl.VMotor, mMotorDataList.get(cmdIndex).vSpeed);
                        MotorControl.setMotorDirection(MotorControl.VMotor, mMotorDataList.get(cmdIndex).vDirection);

                        if (mMotorDataList.get(cmdIndex).vSpeed != 0 && MotorControl.getMotorEnable(MotorControl.VMotor) == false) {
                            new Thread() {
                                @Override
                                public void run() {
                                    MotorControl.startMotorRunning(MotorControl.VMotor);
                                }
                            }.start();
                        } else if (mMotorDataList.get(cmdIndex).vSpeed == 0) {
                            MotorControl.stopMotorRunning(MotorControl.VMotor);
                        }

                        mHandler.sendEmptyMessageDelayed(MSG_CONTROL_D_MOTOR, mMotorDataList.get(cmdIndex).runningTime);
                        UnityPlayer.UnitySendMessage(unityPacakage, unityMethod, "startMotorRunning " + cmdIndex);
                        cmdIndex++;
                        break;
                    case MSG_STOP_D_MOTOR:
                        MotorControl.stopMotorRunning(MotorControl.HMotor);
                        MotorControl.stopMotorRunning(MotorControl.VMotor);
                        break;

                }
            }
        };
    }

    /**
     * 设置unity回调信息， android会通过 UnitySendMessage(String var0, String var1, String var2) 回调unity接口
     * @param unityPacakage : var0
     * @param unityMethod : var1
     */
    public void setUnityCallBack(String unityPacakage, String unityMethod) {
        this.unityPacakage = unityPacakage;
        this.unityMethod = unityMethod;
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
            new Thread(new Runnable() {
                @Override
                public void run() {
                    int endPos =  MotorControl.controlMotor(motorId, steps, dir, delay);
                    UnityPlayer.UnitySendMessage(unityPacakage, unityMethod, "controlMotor " + endPos);
                }
            }).start();
        }

        return 0;
    }

    /**
     * 同时控制两个马达转动， 转动结束会通过 UnitySendMessage 通知unity
     * @param hDir : 水平马达转动方向
     * {
          @link    HMotorLeftDirection
          @link    HMotorRightDirection
     * }
     * @param hDelay: 水平马达脉冲持续时间， 单位us， 数值越小速度越快
     * @param vDir ： 垂直马达转动方向
     * {
        @link    VMotorUpDirection
        @link    VMotorDownDirection
     * }
     * @param vDelay 垂直马达脉冲持续时间，单位us， 数值越小速度越快
     * @param duration 马达转动时间， 单位ms
     */
    public int controlMultiMotor(final int hDir, final int hDelay, final int vDir, final int vDelay, final int duration) {

        MotorControl.setMotorSpeed(MotorControl.HMotor, hDelay);
        MotorControl.setMotorDirection(MotorControl.HMotor, hDir);
        if (hDelay != 0) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    int endPos =  MotorControl.startMotorRunning(MotorControl.HMotor);;
                    UnityPlayer.UnitySendMessage(unityPacakage, unityMethod, "controlMultiMotor HMotor" + endPos);
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
                    UnityPlayer.UnitySendMessage(unityPacakage, unityMethod, "controlMultiMotor VMotor" + endPos);
                }
            }).start();
        }

        mHandler.sendEmptyMessageDelayed(MSG_STOP_D_MOTOR, duration);

        return 0;
    }

    /**
     * 同时控制两个马达转动， 转动结束会通过 UnitySendMessage 通知unity
     * @param hAngle : 水平马达转动角度
     * @param vAngle ： 垂直马达转动角度
     * @param duration 马达转动时间， 单位ms
     */
    public int controlMultiMotor(float hAngle, float vAngle, final int duration) {
        return 0;
    }

    /**
     * 停止马达转动
     * @param motorId
     * {
     *     @link HMotor 水平方向马达
     *     @link VMotor 垂直方向马达
     * }
     */
    public void stopMotor(int motorId) {
        MotorControl.stopMotorRunning(motorId);
    }

    /**
     * 清空马达转动指令列表
     */
    public void clearMotorDataList() {
        mMotorDataList.clear();
    }

    /**
     * 添加马达转动指令
     * @param runningTime ：马达转动时间
     * @param hDirection ： 水平马达转动方向
     * @param hSpeed ： 水平马达转动速度
     * @param vDirection ： 垂直马达转动方向
     * @param vSpeed ： 垂直马达转动速度
     */
    public void addMotorData(int runningTime, int hDirection, int hSpeed, int vDirection, int vSpeed) {
        mMotorDataList.add(new MotorData(runningTime, hDirection, hSpeed, vDirection, vSpeed));
    }

    /**
     * 让马达按照设置的直接开始运行，每开始执行一条指令会通过接口通知unity
     * UnityPlayer.UnitySendMessage(unityPacakage, unityMethod, "startMotorRunning " + cmdIndex);
     */
    public void startMotorRunning() {
        cmdIndex = 0;
        mHandler.sendEmptyMessageDelayed(MSG_CONTROL_D_MOTOR, 0);
    }

    public class MotorData {
        public int runningTime;
        public int hDirection;
        public int hSpeed;
        public int vDirection;
        public int vSpeed;

        MotorData(int runningTime, int hDirection, int hSpeed, int vDirection, int vSpeed) {
            this.runningTime = runningTime;
            this.hDirection = hDirection;
            this.hSpeed = hSpeed;
            this.vDirection = vDirection;
            this.vSpeed = vSpeed;
        }
    }
}
