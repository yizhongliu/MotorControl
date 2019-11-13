package pri.tool.nativeplayer;

import android.media.MediaPlayer;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class NativePlayer {

    private static final String TAG = "NEPlayer";
    static {
        System.loadLibrary("native-lib");
    }

    //准备过程错误码
    public static final int ERROR_CODE_FFMPEG_PREPARE = -1000;
    //播放过程错误码
    public static final int ERROR_CODE_FFMPEG_PLAY = -2000;

    //打不开视频
    public static final int FFMPEG_CAN_NOT_OPEN_URL = (ERROR_CODE_FFMPEG_PREPARE - 1);

    //找不到媒体流信息
    public static final int FFMPEG_CAN_NOT_FIND_STREAMS = (ERROR_CODE_FFMPEG_PREPARE - 2);

    //找不到解码器
    public static final int FFMPEG_FIND_DECODER_FAIL = (ERROR_CODE_FFMPEG_PREPARE - 3);

    //无法根据解码器创建上下文
    public static final int FFMPEG_ALLOC_CODEC_CONTEXT_FAIL = (ERROR_CODE_FFMPEG_PREPARE - 4);

    //根据流信息 配置上下文参数失败
    public static final int FFMPEG_CODEC_CONTEXT_PARAMETERS_FAIL = (ERROR_CODE_FFMPEG_PREPARE - 5);

    //打开解码器失败
    public static final int FFMPEG_OPEN_DECODER_FAIL = (ERROR_CODE_FFMPEG_PREPARE - 6);

    //没有音视频
    public static final int FFMPEG_NOMEDIA = (ERROR_CODE_FFMPEG_PREPARE - 7);

    //读取媒体数据包失败
    public static final int FFMPEG_READ_PACKETS_FAIL = (ERROR_CODE_FFMPEG_PLAY - 1);

    //直播地址或媒体文件路径
    private String dataSource;
    private SurfaceHolder surfaceHolder;

    private int rotate;

    public NativePlayer() {
        initNative();
    }

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
        prepareNative(dataSource, rotate);
    }

    /**
     * 开始播放
     */
    public void start() {
        startNative();
    }

    /**
     * 供native反射调用
     * 表示播放器准备好了可以开始播放了
     */
    public void onPrepared() {
        if (onpreparedListener != null) {
            onpreparedListener.onPrepared();
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
            onCompletionListener.OnCompletion();
        }
    }

    public void setOnpreparedListener(OnpreparedListener onpreparedListener) {
        this.onpreparedListener = onpreparedListener;
    }

    public void setOnErrorListener(OnErrorListener onErrorListener) {
        this.onErrorListener = onErrorListener;
    }
    public void setOnProgressListener(OnProgressListener onProgressListener) {
        this.onProgressListener = onProgressListener;
    }

    public void setOnCompletionListener(OnCompletionListener onCompletionListener) {
        this.onCompletionListener = onCompletionListener;
    }



    /**
     * 资源释放
     */
    public void release() {
        //     surfaceHolder.removeCallback(this);
        releaseNative();
    }

    /**
     * 停止播放
     */
    public void stop() {
        stopNative();
    }

    public void pause() {pauseNative();}

    /**
     * 获取总的播放时长
     * @return
     */
    public int getDuration(){
        return getDurationNative();
    }

    /**
     * 播放进度跳转
     * @param playProgress
     */
    public void seekTo(final int playProgress) {
        new Thread(){
            @Override
            public void run() {
                seekToNative(playProgress);
            }
        }.start();
    }

    public void setSurface(Surface surface) {
        setSurfaceNative(surface);
    }

    public interface OnpreparedListener {
        void onPrepared();
    }

    public interface OnErrorListener {
        void onError(int errorCode);
    }

    public interface OnProgressListener {
        void onProgress(int progress);
    }

    public interface OnCompletionListener {
        void OnCompletion();
    }

    private OnErrorListener onErrorListener;
    private OnpreparedListener onpreparedListener;
    private OnProgressListener onProgressListener;
    private OnCompletionListener onCompletionListener;

    private native void initNative();

    private native void prepareNative(String dataSource, int rotate);

    private native void startNative();
    private native void stopNative();
    private native void pauseNative();
    private native void releaseNative();
    private native int getDurationNative();
    private native void seekToNative(int playProgress);

    private native void setSurfaceNative(Surface surface);

}
