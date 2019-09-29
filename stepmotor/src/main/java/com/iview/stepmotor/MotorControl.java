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
        return controlMotor(motorId, steps, dir, delay, true);
    }

    public static int controlMotor(int motorId, int steps, int dir, int delay, boolean bCheckLimitSwitch) {
        return nativeControlMotor(motorId, steps, dir, delay, bCheckLimitSwitch);
    }

    public static int setMotorSpeed(int motorId, int delay) {
        return nativeSetMotorSpeed(motorId, delay);
    }

    public static int setMotorDirection(int motorId, int direction) {
        return nativeSetMotorDirection(motorId, direction);
    }

    public static int startMotorRunning(int motorId) {
        return startMotorRunning(motorId, true);
    }

    public static int startMotorRunning(int motorId, boolean bCheckLimitSwitch) {
        return nativeStartMotorRunning(motorId, bCheckLimitSwitch);
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

    public static void setKeyStone(int angle) {
        nativeSetKeyStone(angle);
    }

    public static void controlMultiMotors(int hSteps, int vSteps, int hDir, int vDir, int delay) {
        controlMultiMotors(hSteps, vSteps, hDir, vDir, delay, true);
    }

    public static void controlMultiMotors(int hSteps, int vSteps, int hDir, int vDir, int delay, boolean bCheckLimitSwitch) {
        nativeControlMultiMotors(hSteps, vSteps, hDir, vDir, delay, bCheckLimitSwitch);
    }

    public static void stopMultiMotors() {
        nativeStopMultiMotors();
    }

    private native static int nativeControlMotor(int motorId, int steps, int dir, int delay, boolean bCheckLimitSwitch);
    private native static int nativeSetMotorSpeed(int motorId, int delay);
    private native static int nativeSetMotorDirection(int motorId, int direction);
    private native static int nativeStartMotorRunning(int motorId, boolean bCheckLimitSwitch);
    private native static int nativeStopMotorRunning(int motorId);
    private native static boolean nativeGetMotorEnable(int motorId);

    private native static void nativeSwitchProjector(int enable);

    private native static void nativeControlMultiMotors(int hSteps, int vSteps, int hDir, int vDir, int delay, boolean bCheckLimitSwitch);
    private native static void nativeStopMultiMotors();

    private native static void nativeSetKeyStone(int angle);

}
