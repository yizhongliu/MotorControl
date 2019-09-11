package com.iview.mirromove;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Network;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.iview.mirromove.util.MediaFileUtil;
import com.iview.mirromove.util.MsgType;
import com.iview.stepmotor.MotorControl;
import com.tbruyelle.rxpermissions2.Permission;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.IOException;
import java.io.PushbackInputStream;

import common.tool.ffmpegplayer.NativePlayer;
import io.reactivex.functions.Consumer;


public class MainActivity extends Activity {

    private final static String TAG = "MainActivity";

    private final static int MSG_IMAGE_DISMISS = 0;

    private NetWorkService netWorkService;
    private SocketService socketService;
    private ControlService controlService;
    private OnNetWorkListner netWorkListner;


    private TextView ipText;

    private final static int port = 8091;

    private ImageView imagePriView;

    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private Surface mVideoSurface;
    private SurfaceCallback surfaceCallback;

    private ControlCallBack controlCallBack;

    public String dataSource;
    public NativePlayer mPlayer = null;

    boolean bImageShow = false;
    boolean bVideoShow = false;

    boolean bSurfaceCreate = false;

    private MessageReceiver mMessageReceiver;

    private HandlerThread mBackHandlerThread;
    private Handler mBackHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "onCreate");

        checkPermission();

        init();

        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) {
            View v = getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= 19) {
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN;
            //                  | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            decorView.setSystemUiVisibility(uiOptions);
        }


        Intent socketIntent = new Intent(this, SocketService.class);
        bindService(socketIntent, socketConnection, BIND_AUTO_CREATE);

        Intent netIntent = new Intent(this, NetWorkService.class);
        bindService(netIntent, netConnection, BIND_AUTO_CREATE);

        Intent controlIntent = new Intent(this, ControlService.class);
        bindService(controlIntent, controlConnection, BIND_AUTO_CREATE);

        registerBroadcastReceive();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        if (mPlayer != null) {
            mPlayer.close();
            mPlayer = null;
        }

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
        surfaceCallback = new SurfaceCallback();
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(surfaceCallback);

        mMessageReceiver = new MessageReceiver();

        controlCallBack = new ControlCallBack();

        initBackThread();
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

    private ServiceConnection controlConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.e(TAG, "onServiceConnected");

            controlService = ((ControlService.ControlBinder) iBinder).getService();
            controlService.setPlayCallBack(controlCallBack);

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

                    //先隐藏图片显示
                    if (bImageShow) {
                        imagePriView.setVisibility(View.INVISIBLE);
                        bImageShow = false;
                    }

                    dataSource = Environment.getExternalStorageDirectory() + "/Billons.mp4";
                    startPlay(dataSource, "video_hwaccel=0;video_rotate=0");

                    if (bVideoShow) {

                    } else {
                        surfaceView.setVisibility(View.VISIBLE);
                    }
                } else if (messge.equals(MsgType.ACTION_SHOW_IMAGE)) {
                    if (bVideoShow) {
                        if (mPlayer != null) {
                            mPlayer.close();
                            surfaceView.setVisibility(View.INVISIBLE);
                        }
                    }

                    bImageShow = true;
                    String path = Environment.getExternalStorageDirectory() + "/Pictures/test11.jpg";
                    Bitmap bitmap = BitmapFactory.decodeFile(path);
                    imagePriView.setVisibility(View.VISIBLE);
                    imagePriView.setImageBitmap(bitmap);

                }
            } catch (Exception e){
            }
        }
    }

    public void startPlay(String url, String param) {
        if (mPlayer != null) {
            mPlayer.open(url, param);
            mPlayer.setDisplaySurface(mVideoSurface);
        } else {
            mPlayer = new NativePlayer(url, mHandler, param);
            mPlayer.setDisplaySurface(mVideoSurface);
        }
    }

    private void play() {
        if (mPlayer != null) {
            mPlayer.play();
        }
    }

    private void pause() {
        if (mPlayer != null) {
            mPlayer.pause();
        }
    }

    private void seek(int progress) {
        if (mPlayer != null) {
            mPlayer.seek(progress);
        }
    }

    private void prepare() {
        mPlayer.prepare();
    }

    private void open(String url, String param) {
        mPlayer.open(url, param);
    }


    public void changeSurfaceSize(int videoWidth, int videoHeight, int mode) {
        Log.d(TAG, "before change videoWidth:" + videoWidth + ", videoHeight:" + videoHeight);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;

        Log.d(TAG, "Window Display width:" + dm.widthPixels + ", height:" + dm.heightPixels);

        float maxRatio;
        maxRatio = Math.max(((float) videoWidth/(float) width),(float) videoHeight/(float) height);

        videoHeight = (int) Math.ceil((float) videoHeight / (float) maxRatio);
        videoWidth = (int) Math.ceil((float) videoWidth / (float) maxRatio);

        Log.d(TAG, "after change: videoWidth:" + videoWidth + ", videoHeight:" + videoHeight);

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(videoWidth, videoHeight);
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        surfaceView.setLayoutParams(layoutParams);
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

    public class SurfaceCallback implements SurfaceHolder.Callback {
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d(TAG, "surfaceChanged Called");

            mVideoSurface = holder.getSurface();
            if (mPlayer != null) {
                mPlayer.setDisplaySurface(mVideoSurface);
            }
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.d(TAG, "surfaceCreated Called");

            bVideoShow = true;

            mVideoSurface = holder.getSurface();
            if (mPlayer != null) {
                mPlayer.setDisplaySurface(mVideoSurface);
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d(TAG, "surfaceDestroyed Called");
            bVideoShow = false;

            mVideoSurface = null;
            if (mPlayer != null) {
                mPlayer.setDisplaySurface(mVideoSurface);
            }
        }
    }



    private static final int MSG_UPDATE_PROGRESS  = 1;
    private static final int MSG_UDPATE_VIEW_SIZE = 2;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_PROGRESS: {
                    mHandler.sendEmptyMessageDelayed(MSG_UPDATE_PROGRESS, 200);
                    int progress = mPlayer != null ? (int)mPlayer.getParam(NativePlayer.PARAM_MEDIA_POSITION) : 0;
                    if (progress >= 0) {
                  //      mSeekBar.setProgress(progress);
                    }
                }
                break;

                case MSG_UDPATE_VIEW_SIZE: {
//                    if (mPlayer != null && mPlayer.initVideoSize(mVideoViewW, mVideoViewH, mVideo)) {
//                        mVideo.setVisibility(View.VISIBLE);
//                    }
                }
                break;
                case NativePlayer.MSG_OPEN_DONE: {

                    if (mPlayer != null) {

                        int videoWidth = (int)mPlayer.getParam(NativePlayer.PARAM_VIDEO_WIDTH);
                        int videoHeight = (int)mPlayer.getParam(NativePlayer.PARAM_VIDEO_HEIGHT);
                        changeSurfaceSize(videoWidth, videoHeight, 0);

                        mPlayer.setDisplaySurface(mVideoSurface);

                        play();
                    }
                    Log.e(TAG, "NativePlayer.MSG_OPEN_DONE");
                }
                break;
                case NativePlayer.MSG_OPEN_FAILED: {
                    String str = "Open fail";
                    Toast.makeText(MainActivity.this, str, Toast.LENGTH_LONG).show();
                }
                break;
                case NativePlayer.MSG_PLAY_COMPLETED: {
                    Log.e(TAG, "Play complete");
                //    changePlayerUiState(false);
                //    mIsCompleted = true;
                    if (controlService != null) {
                        controlService.onShowComplete();
                    }
                }
                break;
                case NativePlayer.MSG_VIDEO_RESIZED: {
                }
                break;
            }
        }
    };

    public class ControlCallBack implements PlayCallBack {

        @Override
        public void play(final String url, final int time, final double rotation) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String absUrl =  Environment.getExternalStorageDirectory() + "/" + url;

                    if (MediaFileUtil.isImageFileType(absUrl)) {
                        if (bVideoShow) {
                            if (mPlayer != null) {
                                mPlayer.close();
                                surfaceView.setVisibility(View.INVISIBLE);
                            }
                        }

                        bImageShow = true;
                        //   String path = Environment.getExternalStorageDirectory() + "/Pictures/test11.jpg";

                        Bitmap bitmap = BitmapFactory.decodeFile(absUrl);
                        imagePriView.setVisibility(View.VISIBLE);
                        imagePriView.setRotation((float) rotation);
                        imagePriView.setImageBitmap(bitmap);

                        mBackHandler.sendEmptyMessageDelayed(MSG_IMAGE_DISMISS, time);
                    } else if (MediaFileUtil.isVideoFileType(absUrl)) {
                        //先隐藏图片显示
                        if (bImageShow) {
                            imagePriView.setVisibility(View.INVISIBLE);
                            bImageShow = false;
                        }

                        // dataSource = Environment.getExternalStorageDirectory() + "/Billons.mp4";
                        startPlay(absUrl, "video_hwaccel=0;video_rotate=0");

                        if (bVideoShow) {

                        } else {
                            surfaceView.setVisibility(View.VISIBLE);
                        }
                    }
                }
            });


        }
    }

    private void initBackThread() {
        mBackHandlerThread = new HandlerThread("show_thread");
        mBackHandlerThread.start();

        mBackHandler = new Handler(mBackHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_IMAGE_DISMISS:
                        if (controlService != null) {
                            Log.e(TAG, "MSG_IMAGE_DISMISS");
                            controlService.onShowComplete();
                        }
                        break;
                }
            }
        };
    }
}
