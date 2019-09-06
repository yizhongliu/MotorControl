package com.iview.motorcontrol;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.iview.stepmotor.MotorControl;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public final static String TAG = "MainActivity";

    Button button1;
    Button button2;
    Button button3;
    Button button4;
    Button button5;

    HandlerThread mHandlerThread;
    Handler mHandler;

    List<MotorData> mMotorDataList = new ArrayList<>();

    public final static int MSG_CONTROL_H_MOTOR = 0;
    public final static int MSG_CONTROL_D_MOTOR = 1;

    int[] delayTime = {1000, 1000, 3000, 3000};
    int cmdIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initData();
    }

    private void initView() {
        button1 = findViewById(R.id.button1);
        button1.setOnClickListener(this);

        button2 = findViewById(R.id.button2);
        button2.setOnClickListener(this);

        button3 = findViewById(R.id.button3);
        button3.setOnClickListener(this);

        button4 = findViewById(R.id.button4);
        button4.setOnClickListener(this);

        button5 = findViewById(R.id.button5);
        button5.setOnClickListener(this);
    }

    private void initMotorData() {
        mMotorDataList.clear();


        mMotorDataList.add(new MotorData(500, 0, 1000, 0, 5000));
        mMotorDataList.add(new MotorData(500, 0, 1000, 0, 5000));
        mMotorDataList.add(new MotorData(500, 0, 1000, 0, 5000));
        mMotorDataList.add(new MotorData(500, 0, 1000, 0, 5000));
        mMotorDataList.add(new MotorData(500, 0, 1000, 0, 5000));

        mMotorDataList.add(new MotorData(500, 0, 1000, 1, 5000));
        mMotorDataList.add(new MotorData(500, 0, 1000, 1, 5000));
        mMotorDataList.add(new MotorData(500, 0, 1000, 1, 5000));
        mMotorDataList.add(new MotorData(500, 0, 1000, 1, 5000));
        mMotorDataList.add(new MotorData(500, 0, 1000, 1, 5000));




//        mMotorDataList.add(new MotorData(4000, 1, 100, 1, 0));
//   //     mMotorDataList.add(new MotorData(1000, 0, 1000, 1, 500));
//    //    mMotorDataList.add(new MotorData(1000, 0, 500, 1, 300));
//
//
//        mMotorDataList.add(new MotorData(1000, 0, 500, 0, 500));
//        mMotorDataList.add(new MotorData(1000, 0, 1000, 0, 500));
//        mMotorDataList.add(new MotorData(1000, 0, 500, 0, 300));
//
//
//        mMotorDataList.add(new MotorData(1000, 0, 500, 1, 500));
//        mMotorDataList.add(new MotorData(1000, 0, 1000, 1, 500));
//        mMotorDataList.add(new MotorData(1000, 0, 500, 1, 300));

    }

    int direction = 0;
    private void initData() {

        initMotorData();

        mHandlerThread = new HandlerThread("Motor Thread");
        mHandlerThread.start();

        mHandler = new Handler(mHandlerThread.getLooper()) {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_CONTROL_H_MOTOR:

                        if (cmdIndex > delayTime.length - 1) {
                            MotorControl.stopMotorRunning(MotorControl.HMotor);
                            return;
                        }

                        if (direction == 0) {
                            direction = 1;
                        } else if (direction == 1) {
                            direction = 0;
                        }

                        MotorControl.setMotorSpeed(MotorControl.HMotor, 500);
                        MotorControl.setMotorDirection(MotorControl.HMotor, direction);

                        int postDelayTime = delayTime[cmdIndex];
                        Log.e(TAG, "postDelayTime:" + postDelayTime + " , cmdIndex:" + cmdIndex + ", direction:" + direction);
                        cmdIndex++;
                        mHandler.sendEmptyMessageDelayed(MSG_CONTROL_H_MOTOR, postDelayTime);

                        break;

                    case MSG_CONTROL_D_MOTOR:
                        if (cmdIndex > mMotorDataList.size() - 1) {
                            MotorControl.stopMotorRunning(MotorControl.HMotor);
                            MotorControl.stopMotorRunning(MotorControl.VMotor);
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

                  //      Log.e(TAG, "postDelayTime:" + postDelayTime + " , cmdIndex:" + cmdIndex + ", direction:" + direction);

                        mHandler.sendEmptyMessageDelayed(MSG_CONTROL_D_MOTOR, mMotorDataList.get(cmdIndex).runningTime);
                        cmdIndex++;
                        break;

                }
            }
        };
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button1:
                MotorControl.controlMotor(MotorControl.HMotor, 10000, 1, 500);
                break;
            case R.id.button2:
                MotorControl.controlMotor(MotorControl.HMotor,10000, 0, 500);
                break;
            case R.id.button3:
                MotorControl.controlMotor(MotorControl.VMotor,10000, 1, 1000 );
          //      MotorControl.controlMotor(MotorControl.VMotor, 3000, 1, 500);
//                cmdIndex = 0;
//                MotorControl.setMotorSpeed(MotorControl.VMotor, 500);
//                MotorControl.setMotorDirection(MotorControl.VMotor, direction);
//
//                new Thread() {
//                    @Override
//                    public void run() {
//                        MotorControl.startMotorRunning(MotorControl.VMotor);
//                    }
//                }.start();
//
//                mHandler.sendEmptyMessageDelayed(MSG_CONTROL_H_MOTOR, delayTime[cmdIndex]);
//                cmdIndex++;
                break;
            case R.id.button4:
                MotorControl.controlMotor(MotorControl.VMotor,10000, 0, 1000 );
                //MotorControl.controlMotor(MotorControl.VMotor, 3000, 0, 500);
//                cmdIndex = 0;
//                MotorControl.setMotorSpeed(MotorControl.HMotor, 500);
//                MotorControl.setMotorDirection(MotorControl.HMotor, direction);
//
//                new Thread() {
//                    @Override
//                    public void run() {
//                        MotorControl.startMotorRunning(MotorControl.HMotor);
//                    }
//                }.start();
//
//                mHandler.sendEmptyMessageDelayed(MSG_CONTROL_H_MOTOR, delayTime[cmdIndex]);
                cmdIndex++;
                break;
            case R.id.button5:

                cmdIndex = 0;

//                MotorControl.setMotorSpeed(MotorControl.HMotor, mMotorDataList.get(cmdIndex).hSpeed);
//                MotorControl.setMotorDirection(MotorControl.HMotor, mMotorDataList.get(cmdIndex).hDirection);
//
//                new Thread() {
//                    @Override
//                    public void run() {
//                        MotorControl.startMotorRunning(MotorControl.HMotor);
//                    }
//                }.start();
//
//                MotorControl.setMotorSpeed(MotorControl.VMotor, mMotorDataList.get(cmdIndex).vSpeed);
//                MotorControl.setMotorDirection(MotorControl.VMotor, mMotorDataList.get(cmdIndex).vDirection);
//
//                new Thread() {
//                    @Override
//                    public void run() {
//                        MotorControl.startMotorRunning(MotorControl.VMotor);
//                    }
//                }.start();
//
//                mHandler.sendEmptyMessageDelayed(MSG_CONTROL_D_MOTOR, mMotorDataList.get(cmdIndex).runningTime);
//                cmdIndex++;

                mHandler.sendEmptyMessageDelayed(MSG_CONTROL_D_MOTOR, 0);

                break;
        }
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
