#include <jni.h>
#include <string>
#include "NEFFmpeg.h"
#include <android/native_window_jni.h>

extern "C" {
#include "include/libavutil/avutil.h"
}

JavaVM *javaVM = 0;
JavaCallHelper *javaCallHelper = 0;
NEFFmpeg *ffmpeg = 0;
ANativeWindow *window = 0;
pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;//静态初始化mutex

//extern "C" JNIEXPORT jstring JNICALL
//Java_com_netease_player_MainActivity_stringFromJNI(
//        JNIEnv *env,
//        jobject /* this */) {
//    std::string hello = "Hello from C++";
////    return env->NewStringUTF(hello.c_str());
//    return env->NewStringUTF(av_version_info());
//}

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    javaVM = vm;
    return JNI_VERSION_1_4;
}

//1，data;2，linesize；3，width; 4， height
void renderFrame(uint8_t *src_data, int src_lineSize, int width, int height) {
    pthread_mutex_lock(&mutex);
    if (!window) {
        pthread_mutex_unlock(&mutex);
        return;
    }
    ANativeWindow_setBuffersGeometry(window, width,
                                     height,
                                     WINDOW_FORMAT_RGBA_8888);
    ANativeWindow_Buffer window_buffer;
    if (ANativeWindow_lock(window, &window_buffer, 0)) {
        ANativeWindow_release(window);
        window = 0;
        pthread_mutex_unlock(&mutex);
        return;
    }
    //把buffer中的数据进行赋值（修改）
    uint8_t *dst_data = static_cast<uint8_t *>(window_buffer.bits);
    int dst_lineSize = window_buffer.stride * 4;//ARGB
    //逐行拷贝
    for (int i = 0; i < window_buffer.height; ++i) {
        memcpy(dst_data + i * dst_lineSize, src_data + i * src_lineSize, dst_lineSize);
    }
    ANativeWindow_unlockAndPost(window);
    pthread_mutex_unlock(&mutex);
}

extern "C"
JNIEXPORT void JNICALL
Java_pri_tool_nativeplayer_NativePlayer_prepareNative(JNIEnv *env, jobject instance, jstring dataSource_, jint rotate) {
    const char *dataSource = env->GetStringUTFChars(dataSource_, 0);

    LOGE("prepareNative");

 //   javaCallHelper = new JavaCallHelper(javaVM, env, instance);
    LOGE("new javaCallHelper");
    ffmpeg = new NEFFmpeg(javaCallHelper, const_cast<char *>(dataSource), rotate);
    ffmpeg->setRenderCallback(renderFrame);
    ffmpeg->prepare();

    env->ReleaseStringUTFChars(dataSource_, dataSource);
}

extern "C"
JNIEXPORT void JNICALL
Java_pri_tool_nativeplayer_NativePlayer_startNative(JNIEnv *env, jobject instance) {
    if (ffmpeg) {
        ffmpeg->start();
    }

}

extern "C"
JNIEXPORT void JNICALL
Java_pri_tool_nativeplayer_NativePlayer_setSurfaceNative(JNIEnv *env, jobject instance, jobject surface) {
    pthread_mutex_lock(&mutex);
    //先释放之前的显示窗口
    if (window) {
        ANativeWindow_release(window);
        window = 0;
    }
    //创建新的窗口用于视频显示
    window = ANativeWindow_fromSurface(env, surface);
    pthread_mutex_unlock(&mutex);

}

extern "C"
JNIEXPORT void JNICALL
Java_pri_tool_nativeplayer_NativePlayer_releaseNative(JNIEnv *env, jobject thiz) {
    pthread_mutex_lock(&mutex);
    if (window) {
        //把老的释放
        ANativeWindow_release(window);
        window = 0;
    }
    pthread_mutex_unlock(&mutex);
    DELETE(ffmpeg);
}
extern "C"
JNIEXPORT void JNICALL
Java_pri_tool_nativeplayer_NativePlayer_stopNative(JNIEnv *env, jobject thiz) {
    if (ffmpeg) {
        ffmpeg->stop();
    }

    DELETE(ffmpeg);
}
extern "C"
JNIEXPORT jint JNICALL
Java_pri_tool_nativeplayer_NativePlayer_getDurationNative(JNIEnv *env, jobject thiz) {
    if (ffmpeg) {
        return ffmpeg->getDuration();
    }
    return 0;
}

extern "C"
JNIEXPORT void JNICALL
Java_pri_tool_nativeplayer_NativePlayer_seekToNative(JNIEnv *env, jobject thiz, jint playProgress) {
    if (ffmpeg) {
        ffmpeg->seekTo(playProgress);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_pri_tool_nativeplayer_NativePlayer_initNative(JNIEnv *env, jobject thiz) {
    javaCallHelper = new JavaCallHelper(javaVM, env, thiz);
}