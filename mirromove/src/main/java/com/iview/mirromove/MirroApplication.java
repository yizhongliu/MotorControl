package com.iview.mirromove;

import android.app.Application;
import android.content.Context;

public class MirroApplication extends Application {
    private static Context context;

    public final static int CONTROL_STATE = 0;
    public final static int PATH_PLANNING_STATE = 1;
    public final static int AUTO_RUNNING_STATE = 2;

    private int runningState = CONTROL_STATE;
    private String runningPath = null;

    @Override
    public void onCreate() {
        super.onCreate();

        context = getApplicationContext();
    }

    public static Context getContext() {
        return context;
    }

    public int getRunningState() {
        return runningState;
    }

    public void setRunningState(int runningState) {
        this.runningState = runningState;
    }

    public String getRunningPath() {
        return runningPath;
    }

    public void setRunningPath(String runningPath) {
        this.runningPath = runningPath;
    }
}
