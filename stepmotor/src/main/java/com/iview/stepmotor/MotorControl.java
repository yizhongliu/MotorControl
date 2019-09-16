package com.iview.stepmotor;

public class MotorControl {
    static {
        System.loadLibrary("stepmotor");
    }

    public static final int HMotor = 1;
    public static final int VMotor = 2;

    public static final int HMotorLeftDirection = 1;
    public static final int HMotorRightDirection = 0;

    public static final int VMotorUpDirection = 1;
    public static final int VMotorDownDirection = 0;

    public static int controlMotor(int motorId, int steps, int dir, int delay) {
        return nativeControlMotor(motorId, steps, dir, delay);
    }

    public static int setMotorSpeed(int motorId, int delay) {
        return nativeSetMotorSpeed(motorId, delay);
    }

    public static int setMotorDirection(int motorId, int direction) {
        return nativeSetMotorDirection(motorId, direction);
    }

    public static int startMotorRunning(int motorId) {
        return nativeStartMotorRunning(motorId);
    }

    public static int stopMotorRunning(int motorId) {
        return nativeStopMotorRunning(motorId);
    }

    public static boolean getMotorEnable(int motorId) {
        return nativeGetMotorEnable(motorId);
    }

    public static void swtichProjector(int enable) {
        nativeSwitchProjector(enable);
    }

    public static void controlMultiMotors(int hSteps, int vSteps, int hDir, int vDir, int delay) {
        nativeControlMultiMotors(hSteps, vSteps, hDir, vDir, delay);
    }

    public static void stopMultiMotors() {
        nativeStopMultiMotors();
    }

    private native static int nativeControlMotor(int motorId, int steps, int dir, int delay);
    private native static int nativeSetMotorSpeed(int motorId, int delay);
    private native static int nativeSetMotorDirection(int motorId, int direction);
    private native static int nativeStartMotorRunning(int motorId);
    private native static int nativeStopMotorRunning(int motorId);
    private native static boolean nativeGetMotorEnable(int motorId);

    private native static void nativeSwitchProjector(int enable);

    private native static void nativeControlMultiMotors(int hSteps, int vSteps, int hDir, int vDir, int delay);
    private native static void nativeStopMultiMotors();

}
