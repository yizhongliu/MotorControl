package com.iview.mirromove;

import android.net.Network;

public interface NetWorkListener {
    void onAvailable(Network network);
    void onLost(Network netWork);
}
