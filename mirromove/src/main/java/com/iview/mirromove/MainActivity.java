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
import android.graphics.Matrix;
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

import com.iview.mirromove.net.HttpServerImpl;
import com.iview.mirromove.util.MediaFileUtil;
import com.iview.mirromove.util.MsgType;
import com.iview.stepmotor.MotorControl;
import com.tbruyelle.rxpermissions2.Permission;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.IOException;
import java.io.PushbackInputStream;


import io.reactivex.functions.Consumer;
import pri.tool.nativeplayer.NativePlayer;


public class MainActivity extends Activity {

    private final static String TAG = "MainActivity";

    private final static int MSG_IMAGE_DISMISS = 0;
    private final static int MSG_VIDEO_COMPLETE = 1;

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


    private HandlerThread mBackHandlerThread;
    private Handler mBackHandler;

    private String imageUrl;

    private Matrix matrix = new Matrix();

    HttpServerImpl httpServer;


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

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }

        if (netWorkService != null) {
            netWorkService.stopNetworkListener();
        }

        unbindService(netConnection);
        unbindService(socketConnection);

        if (httpServer != null) {
            httpServer.stop();
            httpServer = null;
        }

    }

    private void init() {
        netWorkListner = new OnNetWorkListner();

        ipText = findViewById(R.id.ipText);

        imagePriView = findViewById(R.id.imgView);

        surfaceView = findViewById(R.id.videoView);
        surfaceCallback = new SurfaceCallback();
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(surfaceCallback);


        controlCallBack = new ControlCallBack();

        mPlayer = new NativePlayer();

        mPlayer.setOnpreparedListener(new NativePlayer.OnpreparedListener() {
            @Override
            public void onPrepared() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                      //  seekBar.setMax(player.getDuration());
//                    int width = mPlayer.getVideoWidth();
//                    int height = mPlayer.getVideoHeight();
//                    if (width != 0 && height != 0) {
//                        changeSurfaceSize(width, height, 0);
//                    }
                    }
                });
           //     player.usePlayClockTime();

                mPlayer.start();
            }
        });

        mPlayer.setOnErrorListener(new NativePlayer.OnErrorListener() {
            @Override
            public void onError(int errorCode) {

            }
        });

        mPlayer.setOnCompletionListener(new NativePlayer.OnCompletionListener() {
            @Override
            public void OnCompletion() {
                Log.e(TAG, "onCompletion");
                mBackHandler.sendEmptyMessage(MSG_VIDEO_COMPLETE);
            }
        });

//        mPlayer.setOnCompletionListener(new NativePlayer.OnCompletionListener() {
//            @Override
//            public void onCompletion() {
//                if (controlService != null) {
//                    Log.e(TAG, "Video on Completion");
//                    controlService.onShowComplete();
//                }
//            }
//        });

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

            if (httpServer == null) {
                String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/httpserver";
                httpServer = new HttpServerImpl(path);
            }
            try {
                httpServer.start();
                Log.e(TAG, "httpServer start");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onLost(Network netWork) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ipText.setText(R.string.network_unavailable);
                }
            });

            Log.d(TAG, "onLost");

            if (socketService != null) {
                socketService.stopTcpServer();
            }

            if (httpServer != null) {
                httpServer.stop();
            }
        }
    }


    public void startPlay(String url, String param) {

        mPlayer.setDataSource(url);
    }

    private void play() {
        if (mPlayer != null) {
            mPlayer.start();
        }
    }

//    private void pause() {
//        if (mPlayer != null) {
//            mPlayer.pause();
//        }
//    }
//
//    private void seek(int progress) {
//        if (mPlayer != null) {
//            mPlayer.seek(progress);
//        }
//    }

    private void prepare() {
        mPlayer.prepare();
    }

//    private void open(String url, String param) {
//        mPlayer.open(url, param);
//    }


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
                mPlayer.setSurface(mVideoSurface);
            }
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.d(TAG, "surfaceCreated Called");

            bVideoShow = true;

//            mVideoSurface = holder.getSurface();
//            if (mPlayer != null) {
//                mPlayer.setSurface(mVideoSurface);
//            }

            mPlayer.prepare();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d(TAG, "surfaceDestroyed Called");
            bVideoShow = false;

//            mVideoSurface = null;
//            if (mPlayer != null) {
//                mPlayer.setSurface(mVideoSurface);
//            }
        }
    }




    public class ControlCallBack implements PlayCallBack {

        @Override
        public void play(final String url, final int time, final int rotation) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {


                    if (MediaFileUtil.isImageFileType(url)) {
                        imageUrl =  Environment.getExternalStorageDirectory() + "/" + url;
                        if (bVideoShow) {
                            Log.e(TAG, "stop player");
                            if (mPlayer != null) {
                                mPlayer.stop();
                      //          mPlayer.release();
                                Log.e(TAG, "stop mPlayer");
                                surfaceView.setVisibility(View.INVISIBLE);
                                bVideoShow = false;
                            }
                        }

                        bImageShow = true;

                        Bitmap bitmap = BitmapFactory.decodeFile(imageUrl);

                 //       matrix.setRotate(rotation);
                 //       bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),bitmap.getHeight(), matrix, true);

                        imagePriView.setVisibility(View.VISIBLE);

                        imagePriView.setImageBitmap(bitmap);
                        imagePriView.setRotation(rotation);

                        if (time != -1) {
                            mBackHandler.sendEmptyMessageDelayed(MSG_IMAGE_DISMISS, time);
                        }
                    } else if (MediaFileUtil.isVideoFileType(url)) {
                        String absUrl =  Environment.getExternalStorageDirectory() + "/" + url;
                        //先隐藏图片显示
                        if (bImageShow) {
                            imagePriView.setVisibility(View.INVISIBLE);
                            bImageShow = false;
                        }

                        if (bVideoShow) {
                            if (mPlayer != null) {
                                mPlayer.stop();
                   //             mPlayer.release();
                            }
                        }

                        // dataSource = Environment.getExternalStorageDirectory() + "/Billons.mp4";
                    //    startPlay(absUrl, "video_hwaccel=0;video_rotate=30");
                        Log.e(TAG, "start player");
                        mPlayer.setDataSource(absUrl);
                        mPlayer.setRotate(rotation);

                        if (bVideoShow) {
                            mPlayer.setSurface(mVideoSurface);
                            mPlayer.prepare();
                        } else {
                            surfaceView.setVisibility(View.VISIBLE);
                        }
                    }
                }
            });
        }

        @Override
        public void setParam(final int rotation) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (bImageShow) {
                    //    imagePriView.setRotation(rotation);

//                        Bitmap bitmap = BitmapFactory.decodeFile(imageUrl);
//
//                   //     Bitmap bitmap = ((BitmapDrawable) getResources().getDrawable(R.drawable.pic)).getBitmap();
//                        // 设置旋转角度
//                        matrix.setRotate(rotation);
//
//
//                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),bitmap.getHeight(), matrix, true);
//
//                        imagePriView.setImageBitmap(bitmap);
                        imagePriView.setRotation(rotation);
                    } else if (bVideoShow) {
                   //     mPlayer.setParam(NativePlayer.PARAM_VDEV_D3D_ROTATE, (int) rotation);
                    }
                }
            });
        }

        @Override
        public void stop() {
            if (bVideoShow) {
                if (mPlayer != null) {
                    mPlayer.stop();
              //      mPlayer.release();
//                                Log.e(TAG, "stop mPlayer");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            surfaceView.setVisibility(View.INVISIBLE);
                        }
                    });
                    bVideoShow = false;
                }
            }

            if (bImageShow) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imagePriView.setVisibility(View.INVISIBLE);
                    }
                });

                bImageShow = false;
            }
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
                    case MSG_VIDEO_COMPLETE:
                        mPlayer.stop();
                        controlService.onShowComplete();
                        break;
                }
            }
        };
    }

    @Override
    public void onBackPressed() {
        if (mPlayer != null) {
            mPlayer.stop();
        }
        finish();
        return;
    }
}
