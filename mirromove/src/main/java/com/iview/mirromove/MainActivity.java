package com.iview.mirromove;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Network;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import com.iview.mirromove.util.MsgType;
import com.iview.stepmotor.MotorControl;
import com.tbruyelle.rxpermissions2.Permission;
import com.tbruyelle.rxpermissions2.RxPermissions;

import io.reactivex.functions.Consumer;


public class MainActivity extends AppCompatActivity {

    private final static String TAG = "MainActivity";

    private NetWorkService netWorkService;
    private SocketService socketService;
    private OnNetWorkListner netWorkListner;


    private TextView ipText;

    private final static int port = 8091;

    private ImagePriView imagePriView;
    private SurfaceView surfaceView;

    boolean bImageShow = false;
    boolean bVideoShow = false;

    private MessageReceiver mMessageReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "onCreate");

        checkPermission();

        init();

        Intent socketIntent = new Intent(this, SocketService.class);
        bindService(socketIntent, socketConnection, BIND_AUTO_CREATE);

        Intent netIntent = new Intent(this, NetWorkService.class);
        bindService(netIntent, netConnection, BIND_AUTO_CREATE);

        registerBroadcastReceive();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        if (netWorkService != null) {
            netWorkService.stopNetworkListener();
        }

        unbindService(netConnection);
        unbindService(socketConnection);

        unregisterBroadcastReceive();
    }

    private void init() {
        netWorkListner = new OnNetWorkListner();

        ipText = findViewById(R.id.ipText);

        imagePriView = findViewById(R.id.imgView);
        surfaceView = findViewById(R.id.videoView);


        mMessageReceiver = new MessageReceiver();
    }

    private ServiceConnection socketConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.e(TAG, "onServiceConnected");

            socketService = ((SocketService.SocketBinder) iBinder).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            socketService = null;
        }
    };

    private ServiceConnection netConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.e(TAG, "onServiceConnected");

            netWorkService = ((NetWorkService.NetWorkBinder) iBinder).getService();
            netWorkService.setNetWorkListener(netWorkListner);
            netWorkService.startNetworkListener();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            netWorkService = null;
        }
    };

    class OnNetWorkListner implements NetWorkListener {

        @Override
        public void onAvailable(Network network) {
            Log.d(TAG, "onAvailable");

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ipText.setText(netWorkService.getLocalIpAddress() + ":" + port);
                }
            });

            while (socketService == null) {
                try {
                    Thread.sleep(1000) ;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            socketService.startTcpServer(port);
        }

        @Override
        public void onLost(Network netWork) {
            ipText.setText("没有网络");
            Log.d(TAG, "onLost");

            if (socketService != null) {
                socketService.stopTcpServer();
            }
        }
    }

    public class MessageReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            try {
            //    if (intent.getAction())) {

            //    }
                String messge = intent.getStringExtra("message");

                Log.e(TAG, "getStringExtra:" + messge);
                if (messge.equals(MsgType.ACTION_SHOW_VIDEO)) {
                    if (bImageShow) {
                        imagePriView.setVisibility(View.INVISIBLE);

                    }
                } else if (messge.equals(MsgType.ACTION_SHOW_IMAGE)) {
                    if (bVideoShow) {

                    } else {
                        bImageShow = true;
                        String path = Environment.getExternalStorageDirectory() + "/Pictures/test11.jpg";
                        Bitmap bitmap = BitmapFactory.decodeFile(path);
                        imagePriView.setVisibility(View.VISIBLE);
                        imagePriView.setImageBitmap(bitmap);
                    }
                }
            } catch (Exception e){
            }
        }
    }

    private void registerBroadcastReceive() {

        IntentFilter filter = new IntentFilter();
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        filter.addAction(MsgType.INTENT_ACTION_MEDIA);
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, filter);
    }

    private void unregisterBroadcastReceive() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }



    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //requestLocationPermission();
            checkRxPermission();
        } else {
        }
    }

    public void checkRxPermission() {
        RxPermissions rxPermission = new RxPermissions(this);
        rxPermission
                .requestEach(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(new Consumer<Permission>() {
                    @Override
                    public void accept(Permission permission) throws Exception {
                        if (permission.granted) {
                            Log.d(TAG, " permission accept");
                        } else if (permission.shouldShowRequestPermissionRationale) {
                            // 用户拒绝了该权限，没有选中『不再询问』（Never ask again）,那么下次再次启动时，还会提示请求权限的对话框
                            Log.e(TAG, permission.name + " is denied. More info should be provided.");
                            finish();
                        } else {
                            // 用户拒绝了该权限，并且选中『不再询问』
                            Log.d(TAG, permission.name + " is denied.");
                            finish();
                        }
                    }
                });
    }
}
