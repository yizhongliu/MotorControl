package pri.tool.nativeplayer;

import android.media.MediaPlayer;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class NativePlayer implements SurfaceHolder.Callback {

    static {
        System.loadLibrary("native-lib");
    }

    private final static String TAG = "NativePlayer";

    //直播地址或媒体文件路径
    private String dataSource;
    private int rotate;
    private SurfaceHolder surfaceHolder;

    private OnErrorListener onErrorListener;
    private OnPreparedListener onPreparedListener;
    private OnProgressListener onProgressListener;
    private OnCompletionListener onCompletionListener;

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public void setRotate(int rotate) {
        this.rotate = rotate;
    }

    /**
     * 播放准备工作
     */

    public void prepare() {
        Log.e(TAG, "prepare");
        nativePrepare(dataSource, rotate);
    }

    public void start() {
        nativeStart();
    }

    public void setSurface(Surface surface) {
        nativeSetSurface(surface);
    }

    public void setSurfaceView(SurfaceView surfaceView) {
        if (null != surfaceHolder) {
            surfaceHolder.removeCallback(this);
        }
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
    }


    /**
     * 资源释放
     */
    public void release() {
     //   surfaceHolder.removeCallback(this);,
        Log.e(TAG, "release");
        nativeRelease();
    }

    /**
     * 停止播放
     */
    public void stop() {
        Log.e(TAG, "stop");
        nativeStop();
    }

    /**
     * 供native反射调用
     * 表示播放器准备好了可以开始播放了
     */
    public void onPrepared() {
        if (onPreparedListener != null) {
            onPreparedListener.onPrepared();
        }
    }

    /**
     * 供native反射调用
     * 表示出错了
     */
    public void onError(int errorCode) {
        if (null != onErrorListener) {
            onErrorListener.onError(errorCode);
        }
    }

    public void onProgress(int progress) {
        if (null != onProgressListener) {
            onProgressListener.onProgress(progress);
        }
    }

    public void onCompletion() {
        if (null != onCompletionListener) {
            onCompletionListener.onCompletion();
        }
    }

    /**
     * 获取总的播放时长
     * @return
     */
    public int getDuration(){
        return nativeGetDuration();
    }

    public int getVideoWidth() {
        return nativeGetVideoWidth();
    }

    public int getVideoHeight() {
        return nativeGetVideoHeight();
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
 //       nativeSetSurface(surfaceHolder.getSurface());
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    public void setOnErrorListener(OnErrorListener listener) {
        this.onErrorListener = listener;
    }

    public void setOnPreparedListener(OnPreparedListener listener) {
        this.onPreparedListener = listener;
    }

    public void setOnProgressListener(OnProgressListener listener) {
        this.onProgressListener = listener;
    }

    public void setOnCompletionListener(OnCompletionListener listener) {
        this.onCompletionListener = listener;
    }

    public interface OnPreparedListener {
        void onPrepared();
    }

    public interface OnErrorListener {
        void onError(int errorCode);
    }

    public interface OnProgressListener {
        void onProgress(int progress);
    }

    public interface OnCompletionListener {
        void onCompletion();
    }

    public void startNetTimeProvider(String ip, int port) {
        nativeStartNetTimeProvider(ip, port);
    }

    public void stopNetTimeProvider() {
        nativeStopNetTimeProvider();
    }

    public void startNetTimeClient(String ip, int port) {
        nativeStartNetTimeClient(ip, port);
    }

    public void stopNetTimeClient() {
        nativeStopNetTimeClient();
    }

    public void usePlayClockTime() {
        nativeUsePlayClockTime();
    }


    private native void nativePrepare(String dataSource, int rotate);

    private native void nativeStart();

    private native void nativeSetSurface(Object surface);

    private native void nativeStop();

    private native void nativeRelease();

    private native int nativeGetDuration();

    private native int nativeGetVideoWidth();

    private native int nativeGetVideoHeight();

    private native void nativeStartNetTimeProvider(String ip, int port);
    private native void nativeStopNetTimeProvider();
    private native void nativeStartNetTimeClient(String ip, int port);
    private native void nativeStopNetTimeClient();
    private native void nativeUsePlayClockTime();

    public native void nativeMnLeak();

}
