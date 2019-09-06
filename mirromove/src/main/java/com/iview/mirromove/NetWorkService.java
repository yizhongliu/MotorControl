package com.iview.mirromove;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

public class NetWorkService extends Service {

    private final static String TAG = "NetWorkService";

    private NetWorkListener netWorkListener;

    private ConnectivityManager.NetworkCallback nectworkCallback;

    private NetWorkBinder mBinder = new NetWorkBinder();

    public class NetWorkBinder extends Binder {
        public NetWorkService getService() {
            return NetWorkService.this;
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void setNetWorkListener(NetWorkListener netWorkListener) {
        this.netWorkListener = netWorkListener;
    }

    @Override
    public void onCreate() {
        Log.e(TAG, "onCreate");
        super.onCreate();

        init();
     //   startNetworkListener();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
     //   stopNetworkListener();
    }

    private void init() {
        if (Build.VERSION.SDK_INT >= 21) {
            nectworkCallback = new ConnectivityManager.NetworkCallback() {
                /**
                 * Called when the framework connects and has declared a new network ready for use.
                 * This callback may be called more than once if the {@link Network} that is
                 * satisfying the request changes.
                 */
                @TargetApi(Build.VERSION_CODES.M)
                @Override
                public void onAvailable(Network network) {
                    super.onAvailable(network);
                    Log.d(TAG, "onAvailable");
                    if (netWorkListener != null) {
                        netWorkListener.onAvailable(network);
                    }
                }

                @Override
                public void onLost(Network network) {
                    super.onLost(network);
                    if (netWorkListener != null) {
                        netWorkListener.onLost(network);
                    }
                }

            };
        }
    }

    public void startNetworkListener() {
        startNetworkListener(NetworkCapabilities.TRANSPORT_WIFI);
        startNetworkListener(NetworkCapabilities.TRANSPORT_ETHERNET);
        startNetworkListener(NetworkCapabilities.TRANSPORT_CELLULAR);
    }

    public void startNetworkListener(int type) {
        final int networkType = type;
        if (Build.VERSION.SDK_INT >= 21) {
            final ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkRequest.Builder builder = new NetworkRequest.Builder();

            // 设置指定的网络传输类型(蜂窝传输) 等于手机网络
            builder.addTransportType(type);

            // 设置感兴趣的网络功能
            builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);

            NetworkRequest request = builder.build();

            connectivityManager.registerNetworkCallback(request, nectworkCallback);
        }
    }

    public void stopNetworkListener() {
        if (Build.VERSION.SDK_INT >= 21) {
            final ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            connectivityManager.unregisterNetworkCallback(nectworkCallback);
        }
    }

    public static String int2ip(int ipInt) {
        StringBuilder sb = new StringBuilder();
        sb.append(ipInt & 0xFF).append(".");
        sb.append((ipInt >> 8) & 0xFF).append(".");
        sb.append((ipInt >> 16) & 0xFF).append(".");
        sb.append((ipInt >> 24) & 0xFF);
        return sb.toString();
    }

    public String getLocalIpAddress() {
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int i = wifiInfo.getIpAddress();
            return int2ip(i);
        } catch (Exception ex) {
            return " 获取IP出错鸟!!!!请保证是WIFI,或者请重新打开网络!\n" + ex.getMessage();
        }
        // return null;
    }

}
