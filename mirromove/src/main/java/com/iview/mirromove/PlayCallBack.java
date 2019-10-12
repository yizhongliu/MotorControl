package com.iview.mirromove;

public interface  PlayCallBack {
    void play(String url, int time, int rotation);
    void setParam(int rotation);
    void stop();
    void autoRunStart();
    void autoRunStop();
}
